# JADWAL — Plateforme de génération d'emplois du temps scolaires

**Description générale du projet · Stack Spring Boot + Next.js**

> Document de référence fourni au démarrage du projet. Les décisions d'implémentation sont tracées dans [DECISIONS.md](DECISIONS.md). Le cahier des 53 règles (`A-01` → `I-09`) est transcrit avec sa matrice de traçabilité dans [REGLES.md](REGLES.md) (source : [regles_emploi_du_temps.pdf](regles_emploi_du_temps.pdf)).
> Deux exigences transverses s'ajoutent au cahier : **tout s'exécute via Docker** (D-001) et **les paiements d'abonnement sont gérés hors plateforme** — virement bancaire, chèque ou espèces, simplement tracés par l'interface d'administration (D-002).

---

## 1. Vision

Jadwal remplace la construction manuelle des emplois du temps (aujourd'hui faite sous Excel, en plusieurs semaines) par un moteur d'optimisation qui produit en quelques minutes un planning respectant l'intégralité des règles pédagogiques, contractuelles et réglementaires de l'établissement.

Trois différenciateurs par rapport aux outils existants :

1. **Gestion native des enseignants mixtes** — les professeurs partagés entre l'Éducation nationale et l'école privée. Leur emploi du temps public est importé (PDF, photo, saisie) et devient une contrainte d'entrée subie.
2. **Équilibrage pédagogique paramétrable** — la répartition des séances reflète le coefficient et la densité de chaque matière, pas seulement la disponibilité des ressources.
3. **Diagnostic de faisabilité avant calcul** — l'outil dit *« il manque 6 h d'enseignant de SVT »* au lieu de renvoyer un échec opaque.

Le cahier des règles (53 règles, référencées `A-01` à `I-09`) constitue la spécification fonctionnelle de référence. Chaque règle est tracée jusqu'à une contrainte du moteur et jusqu'à un test unitaire.

---

## 2. Utilisateurs et rôles

| Rôle | Périmètre |
|---|---|
| **Super-admin** | Gestion des établissements (multi-tenant), abonnements, paiements (manuels), supervision technique |
| **Directeur / Chef d'établissement** | Validation et publication des emplois du temps, tableaux de bord, suivi d'abonnement |
| **Responsable planification** | Référentiel, maquettes pédagogiques, lancement des générations, édition manuelle |
| **Enseignant** | Consultation de son planning, déclaration d'indisponibilité, dépôt de son EDT public, abonnement iCal |
| **Élève / Parent** | Consultation en lecture seule du planning du groupe, notifications de changement |
| **Surveillant / Vie scolaire** | Saisie des absences, gestion des remplacements |

---

## 3. Architecture cible

```
                            ┌──────────────────────────────┐
                            │   Next.js 16 — App Router    │
                            │   Grille D&D · Dashboards    │
                            │   BFF · TanStack Query       │
                            └──────────────┬───────────────┘
                                           │ REST + SSE (JWT)
                            ┌──────────────▼───────────────┐
                            │      API Gateway Spring      │
                            │   Spring Boot 4 · Security   │
                            └──┬──────────┬─────────────┬──┘
                               │          │             │
              ┌────────────────▼──┐  ┌────▼─────────┐ ┌─▼──────────────┐
              │  Module Référentiel│  │Module Solver │ │ Module Ingest  │
              │  JPA · Flyway      │  │Timefold 2.x  │ │ OCR + LLM      │
              └────────────────┬──┘  └────┬─────────┘ └─┬──────────────┘
                               │          │             │
                     ┌─────────▼──────────▼─────────────▼───┐
                     │  PostgreSQL 17  ·  S3/MinIO  ·  Redis│
                     └───────────────────────────────────────┘
```

**Principe structurant :** le solveur ne travaille jamais sur les entités JPA. Un mapper convertit le référentiel persistant en un *modèle de planification* dédié, et inversement.

---

## 4. Stack technique

### Backend

| Composant | Choix | Version cible | Justification |
|---|---|---|---|
| Runtime | Java (Temurin) | **21 LTS** minimum | Baseline imposée par Timefold 2.x |
| Framework | Spring Boot | **4.x** | Timefold 2.x ne supporte que Spring Boot 4.x |
| Moteur d'optimisation | **Timefold Solver** | 2.2+ | Voir §5 |
| Persistance | Spring Data JPA / Hibernate 7 | — | |
| Migrations | Flyway | — | Versioning du schéma indispensable en multi-tenant |
| Base | PostgreSQL | 17 | JSONB pour les paramètres variables, RLS possible pour le multi-tenant |
| Sécurité | Spring Security + JWT | — | Keycloak si SSO établissement requis |
| Cache / pub-sub | Redis | — | Progression du solveur, verrous de génération |
| Stockage fichiers | S3 / MinIO | — | EDT publics déposés, exports PDF |
| Documentation API | springdoc-openapi | — | Génère le client TypeScript du front |
| Observabilité | Actuator + Micrometer → Prometheus / Grafana | — | |
| Build | Maven multi-modules | — | |

### Frontend

| Composant | Choix | Justification |
|---|---|---|
| Framework | **Next.js 16** (App Router) | Turbopack par défaut, React Compiler stable |
| UI | React 19 + TypeScript strict | |
| Style | Tailwind CSS v4 | Cohérence et rapidité |
| État serveur | TanStack Query | Cache, invalidation, optimistic updates sur la grille |
| État local | Zustand | État de la grille en cours d'édition |
| Drag & drop | **dnd-kit** | Accessible, performant sur grilles denses |
| Formulaires | React Hook Form + Zod | Schémas Zod générés depuis l'OpenAPI |
| Graphiques | Recharts | Tableaux de bord d'occupation |
| Temps réel | EventSource (SSE) | Progression du solveur |
| i18n | next-intl | Français / arabe, y compris RTL |

> Les numéros de version évoluent vite. Vérifier au démarrage la matrice de compatibilité Timefold ↔ Spring Boot, c'est le seul couplage réellement bloquant du projet.

---

## 5. Pourquoi Timefold plutôt qu'OR-Tools

Sur une stack Java/Spring, OR-Tools CP-SAT est le mauvais choix : binding JNI sur du C++, sans intégration Spring. Timefold Solver (fork officiel d'OptaPlanner) apporte :

- un **starter Spring Boot** officiel avec auto-configuration ;
- un `SolverManager` injectable, résolutions dans un pool de threads séparé ;
- les **Constraint Streams**, API fluide pour déclarer les contraintes ;
- le **pinning** natif (`@PlanningPin`) — règle `I-03` ;
- le **mode daemon / real-time planning** avec `ProblemChange` — règle `I-07` ;
- un `ConstraintVerifier` : **un test unitaire par règle métier** ;
- le *school timetabling* est le quick start canonique du produit.

Point de vigilance : Timefold 2.x impose Java 21 et Spring Boot 4.x. Non négociable.

---

## 6. Le module Solver

### 6.1 Modèle de planification

`Seance` (`@PlanningEntity`) : groupe, matière, durée en unités de 30 min (règle `A-05`) ; variables planifiées : `creneau`, `salle`, `enseignant` ; `@PlanningPin` pour le verrouillage admin (`I-03`).
`EmploiDuTemps` (`@PlanningSolution`) : créneaux, salles, enseignants (faits + value ranges), séances, `BendableScore` 3 niveaux durs / 3 souples.

### 6.2 Le score reflète la hiérarchie d'arbitrage

| Niveau | Contenu | Règles |
|---|---|---|
| **Hard 0** | Unicité des ressources | `B-01` → `B-05` |
| **Hard 1** | Volumes horaires exacts | `C-01` → `C-07` |
| **Hard 2** | Indisponibilités, habilitations, salles | `D-01` → `D-07`, `E-01` → `E-03` |
| **Soft 0** | Rythme et bien-être | `G-04` → `G-06` |
| **Soft 1** | Équilibrage pédagogique | `F-02` → `F-06` |
| **Soft 2** | Préférences individuelles | `D-08` → `D-10`, `E-04` |

### 6.3 Poids ajustables à l'exécution
Règle `I-01` : pondérations paramétrables par établissement (`ConstraintWeightOverrides` en Timefold 2.x), stockées en base par tenant, injectées avant chaque résolution.

### 6.4 Exécution asynchrone
`SolverManager` + budget temps (`spent-limit=5m`, `unimproved-spent-limit=1m`). Contrôleur non bloquant : `POST /api/generations` → jobId ; progression via SSE.

### 6.5 Contrôles de faisabilité préalables
Règles `H-01` à `H-06` dans un service **indépendant du solveur** : bilan grille, bilan besoin enseignant (dédoublements inclus), bilan offre/besoin, bilan salles spécialisées, bilan disponibilité des enseignants mixtes. Meilleur rapport valeur/effort du projet.

---

## 7. Modèle de données

### Modules métier (packages Maven) — cible

```
jadwal-parent
├── jadwal-common          DTO partagés, exceptions, contexte tenant
├── jadwal-referentiel     Établissements, niveaux, groupes, matières, salles
├── jadwal-abonnement      Plans, abonnements, paiements manuels (ajout D-002)
├── jadwal-enseignant      Enseignants, contrats, habilitations, indisponibilités
├── jadwal-pedagogie       Maquettes, volumes, patterns de découpage
├── jadwal-planning        Séances, versions, publication
├── jadwal-solver          Modèle de planification, contraintes, mappers
├── jadwal-ingestion       Upload, OCR, extraction IA, validation humaine
├── jadwal-export          PDF, Excel, iCal
└── jadwal-api             Contrôleurs REST, sécurité, OpenAPI
```

### Entités principales (cible)

| Entité | Champs structurants |
|---|---|
| `Etablissement` | tenant_id, calendrier, grille horaire (JSONB) |
| `Niveau` | libellé, cycle, ordre |
| `Groupe` | niveau, effectif, `parent_id` (dédoublement — règle `A-04`), type |
| `Matiere` | coefficient, poids cognitif, type de salle requis, durée min/max de séance |
| `MaquettePedagogique` | niveau × matière → volume, max/jour, dédoublement, patterns autorisés |
| `Enseignant` | type (`MIXTE` / `PROPRE`), quota hebdo, matières habilitées |
| `Indisponibilite` | enseignant, source (`ETAT` / `PERSONNELLE` / `ADMIN`), récurrence, statut de validation |
| `Salle` | capacité, type, équipements, bâtiment |
| `Creneau` | jour, début, durée en unités |
| `Seance` | groupe, matière, enseignant, salle, créneau, verrouillée, version |
| `Generation` | job_id, statut, score, paramètres, rapport de faisabilité |
| `Barrette` | groupes alignés sur un même créneau (règle `B-05`) |

Deux choix à figer dès la première migration Flyway du Lot 1 : le **dédoublement** (`Groupe.parent_id`) et les **barrettes**.

---

## 8. Module d'ingestion des EDT publics

Pipeline en cinq temps : dépôt (PDF/photo/saisie, S3, antivirus) → pré-traitement (redressement, dé-bruitage) → extraction hybride (OCR structuré + LLM multimodal, JSON à schéma imposé, score de confiance) → **validation humaine obligatoire** (règle `D-04`, écran deux colonnes) → normalisation en `Indisponibilite` de source `ETAT` avec buffer de trajet (`D-05`). Modification ultérieure → invalidation des séances concernées + alerte (`I-09`). Extractions versionnées.

---

## 9. API REST — surfaces principales (cible)

```
POST   /api/etablissements/{id}/faisabilite       → rapport de bilans (H-01..H-06)
POST   /api/generations                            → lance une résolution, renvoie jobId
GET    /api/generations/{jobId}                    → statut, score, indicateurs
GET    /api/generations/{jobId}/stream             → SSE, progression du score
POST   /api/generations/{jobId}/stop               → arrêt anticipé, conserve le meilleur
GET    /api/generations/{jobId}/analyse            → explication du score par contrainte
GET    /api/plannings/{id}/groupe/{groupeId}       → grille d'un groupe
PATCH  /api/seances/{id}                           → déplacement manuel + validation conflits
POST   /api/seances/{id}/verrouiller               → pinning (I-03)
POST   /api/plannings/{id}/publier                 → publication + notifications
POST   /api/ingestion/edt-public                   → upload fichier
GET    /api/ingestion/{id}/extraction              → grille extraite + confiances
POST   /api/ingestion/{id}/valider                 → validation humaine (D-04)
POST   /api/absences                               → déclenche replanification du jour (I-07)
GET    /api/exports/planning/{id}.{pdf|xlsx|ics}   → exports
```

L'API livrée au Lot 0 (auth, admin plateforme, école) est documentée dans le README.

---

## 10. Frontend Next.js — écrans critiques (cible)

- **La grille drag & drop** : dnd-kit + CSS Grid, créneaux illégaux grisés en temps réel, application optimiste TanStack Query.
- **Le rapport de faisabilité** : feux verts/rouges par bilan, chiffre manquant explicite.
- **La validation d'extraction OCR** : image et grille côte à côte, cellules colorées selon la confiance.
- **Les curseurs de pondération** : six curseurs correspondant aux familles de règles souples.

---

## 11. Sécurité et multi-tenant

- Isolation par `tenant_id`, `TenantContext` alimenté par le JWT, filtre Hibernate global, RLS PostgreSQL en défense en profondeur.
- RBAC par rôle et par périmètre.
- Journal d'audit sur toute modification de planning publié.
- Données personnelles : **déclaration CNDP** (loi 09-08), rétention, chiffrement au repos, URL pré-signées à durée courte pour les fichiers.

---

## 12. Stratégie de tests

| Niveau | Outil | Objet |
|---|---|---|
| Contraintes | `ConstraintVerifier` (Timefold) | Un test par règle du cahier |
| Unitaire | JUnit 5 + AssertJ | Services métier, bilans de faisabilité |
| Intégration | Testcontainers (PostgreSQL) | Repositories, migrations Flyway |
| API | RestAssured / MockMvc | Contrats REST, sécurité, isolation tenant |
| Solveur | Benchmarker Timefold | Non-régression sur la qualité du score |
| Front | Vitest + Testing Library | Composants, logique de grille |
| E2E | Playwright | Parcours complets |

---

## 13. Déploiement

**Développement** — Docker Compose : PostgreSQL, MinIO, Redis, backend, frontend (voir README).

**Production** — trois services : `jadwal-api` (sans état, scale horizontal), `jadwal-solver` (**1 vCPU par résolution simultanée**, service isolé, `-XX:+UseParallelGC`, log solveur à `INFO`, jamais d'instances burstable), `jadwal-web`.

CI/CD : GitHub Actions → build images Docker. Migrations Flyway au démarrage, jamais à la main.

---

## 14. Roadmap

| Lot | Contenu | Durée indicative | Valeur livrée |
|---|---|---|---|
| **L0** | Socle : Spring Boot 4 + Next 16, auth, multi-tenant, Docker, admin plateforme + école, paiements manuels | 3 semaines | Fondations ✔ (ce dépôt) |
| **L1** | Référentiel complet, maquettes, saisie manuelle, grille D&D avec détection de conflits | 6 semaines | **Remplace déjà Excel** |
| **L2** | Bilans de faisabilité `H-01` → `H-06` | 2 semaines | Diagnostic avant solveur |
| **L3** | Solveur Timefold, contraintes dures uniquement | 5 semaines | Premier EDT généré valide |
| **L4** | Contraintes souples, pondérations, équilibrage | 4 semaines | EDT de qualité pédagogique |
| **L5** | Module ingestion EDT publics (OCR + IA + validation) | 5 semaines | Différenciation produit |
| **L6** | Exports, publication, notifications, portails enseignant et élève | 4 semaines | Mise en exploitation |
| **L7** | Absences, remplacements, replanification incrémentale, pilotage | 5 semaines | Produit complet |

**Ne pas sauter L1 et L2.**

---

## 15. Risques principaux

| Risque | Impact | Mitigation |
|---|---|---|
| Modélisation incomplète (dédoublements, barrettes) | Réécriture du schéma et du solveur | Les trancher au L1, jamais après |
| Qualité des données d'entrée | Générations infaisables inexplicables | Bilans `H-*` bloquants |
| Extraction OCR erronée | Planning inapplicable | Validation humaine obligatoire |
| Couplage Timefold ↔ Spring Boot 4 | Blocage de montée de version | Vérifier la matrice à chaque upgrade |
| Sous-dimensionnement du solveur | Lenteur perçue comme un bug | Service isolé, file d'attente explicite |
| Rejet par les utilisateurs | Retour à Excel | L'édition manuelle n'est pas secondaire |
| Conformité loi 09-08 / CNDP | Risque juridique | Déclaration en amont, chiffrement dès le L0 |

---

## 16. Deux décisions à prendre avant le Lot 1

1. **Barrettes** (options et langues mutualisées entre classes) — si oui, la contrainte `B-05` doit exister dès le modèle de planification.
2. **Semaines paires / impaires** — si l'établissement fonctionne à la quinzaine, le modèle de créneau doit porter un numéro de semaine dès l'origine.
