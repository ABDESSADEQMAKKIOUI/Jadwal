# JADWAL — Décisions d'architecture (Lot 0)

Journal des décisions structurantes. Chaque entrée : contexte, décision, conséquence.

## D-001 — Tout passe par Docker
**Contexte.** L'environnement de développement ne dispose ni de JDK ni de Node installés localement ; l'équipe veut un environnement reproductible.
**Décision.** Aucune installation locale requise hors Docker Desktop. Les builds backend (Maven) et frontend (Next.js) sont réalisés **dans** les Dockerfiles (multi-stage). `docker compose up --build` est le seul point d'entrée.
**Conséquence.** Les images `maven:3.9-eclipse-temurin-21`, `node:22-alpine`, `eclipse-temurin:21-jre-alpine`, `postgres:17-alpine`, `redis:7-alpine`, `minio/minio` sont les seules dépendances d'outillage.

## D-002 — Paiements manuels uniquement (pas de paiement en ligne)
**Contexte.** Les abonnements des établissements sont réglés par **virement bancaire, chèque ou espèces**, directement auprès de l'administration de la plateforme.
**Décision.** Aucune intégration de passerelle de paiement (pas de CMI, Stripe, etc.). La plateforme **trace** les paiements : le super-admin les enregistre (montant, mode, référence, date), leur confirmation active l'abonnement. L'école consulte son état d'abonnement et son historique.
**Conséquence.** Le domaine `jadwal-abonnement` porte `PlanAbonnement`, `Abonnement`, `Paiement` avec la règle : somme des paiements CONFIRMÉ ≥ prix du plan → abonnement ACTIF. Une intégration en ligne pourra s'ajouter plus tard sans casser ce modèle.

## D-003 — Deux interfaces livrées en premier : admin plateforme et école
**Décision.** Le Lot 0 étendu livre : interface **super-admin** (créer les établissements, les comptes directeurs, les plans, les abonnements, enregistrer/confirmer les paiements) et interface **école** (tableau de bord, état d'abonnement, historique des paiements). Les modules métier (référentiel, planning, solveur) arrivent aux lots L1+ conformément à la roadmap.

## D-004 — Versions de départ
**Décision.** Spring Boot **4.0.0** (base éprouvée compatible Timefold 2.x, montée en 4.1.x quand disponible), Java **21 LTS** (Temurin), Next.js **16**, React 19, Tailwind v4, PostgreSQL 17.
**Conséquence.** La matrice Timefold ↔ Spring Boot est à revérifier au démarrage du Lot 3 (seul couplage bloquant du projet).

## D-005 — Modules Maven : 4 au départ, pas 9
**Décision.** Seuls `jadwal-common`, `jadwal-referentiel`, `jadwal-abonnement`, `jadwal-api` sont créés. Les modules `jadwal-enseignant`, `jadwal-pedagogie`, `jadwal-planning`, `jadwal-solver`, `jadwal-ingestion`, `jadwal-export` seront ajoutés au lot qui les concerne — un module vide n'apporte que du risque de build.
**Note.** `jadwal-abonnement` (plans, abonnements, paiements) est un ajout par rapport au découpage cible du cahier : c'est le domaine « plateforme » exigé par D-002/D-003.

## D-006 — Auth : JWT + BFF Next.js
**Décision.** Le backend émet un JWT HS256 (12 h). Le navigateur ne parle **jamais** directement au backend : les route handlers Next.js servent de BFF, le token vit dans un cookie httpOnly `jadwal_token`, le proxy `/api/backend/[...path]` relaie avec `Authorization: Bearer`. Pas de CORS. Auth.js/Keycloak pourront remplacer ce socle si un SSO établissement devient nécessaire.

## D-007 — Multi-tenant applicatif d'abord
**Décision.** Isolation par `etablissement_id` appliquée au niveau service (l'identifiant vient exclusivement du JWT pour `/api/ecole/**`). Filtre Hibernate global + RLS PostgreSQL viendront en défense en profondeur avant la mise en production multi-établissements.

## D-008 — Les deux questions du §16 : tranchées « oui » toutes les deux
**Contexte.** Le cahier impose de trancher barrettes et semaines paires/impaires avant la première ligne de code du moteur, car impossibles à greffer après coup.
**Décision.** Le modèle supporte les deux dès l'origine : entité `Barrette` (+ contrainte `B-05`) et dimension `semaine` (`TOUTES`/`A`/`B`) sur les séances, indisponibilités et maquettes (`volume_unites_b` pour la quinzaine — règle `C-05`). Un établissement qui n'utilise ni l'un ni l'autre laisse simplement ces champs vides.
**Conséquence.** La sémantique de chevauchement intègre la compatibilité de semaine (`TOUTES` chevauche `A` et `B` ; `A` ne chevauche pas `B`) partout : contraintes `B-01`→`B-03`, indisponibilités, validation manuelle.

## D-009 — Timefold Solver 2.2.0
**Contexte.** Seul couplage bloquant du projet (cahier §4). Vérifié en ligne le 2026-07-24.
**Décision.** `timefold-solver-core` / `timefold-solver-spring-boot-starter` **2.2.0** : conçu pour Spring Boot 4 et Java 21 (notre stack exacte). L'API 2.x diffère de la 1.x (BendableScore en `long[]`, `penalize(score).asConstraint(code)`, `SolverManager<Solution>` mono-générique à *event consumers*, `ConstraintWeightOverrides` au lieu de `@ConstraintConfiguration`, `ConstraintVerifier` intégré au core) — le code cible exclusivement la 2.x.
**Conséquence.** Toute montée de version revalide la matrice Timefold ↔ Spring Boot avant merge.

## D-010 — Module solveur pur, découplé de JPA
**Décision.** `jadwal-solver` ne dépend d'aucune entité JPA (recommandation Timefold reprise par le cahier §3) : modèle de planification dédié (`SeancePlan`, `EmploiDuTempsPlan`), `SeanceFactory` (découpage des volumes en séances — règles `C-01`/`C-02`/`C-03`/`C-05`), `FaisabiliteService` (`H-01`→`H-06`) et `ValidationConflits` (`I-04`) y sont **purs** ; un `MappeurSolveur` dans `jadwal-api` fait la conversion base ↔ modèle (buffer trajet `D-05` appliqué au mapping).
**Conséquence.** Les 30+ tests `ConstraintVerifier` (un par règle du cahier) s'exécutent sans base de données ; la traçabilité règle → contrainte → test vit entièrement dans ce module.

## D-012 — Domaine de valeurs restreint par séance (et non pénalité a posteriori)
**Contexte.** Première génération réelle : le solveur laissait 90 violations dures après 60 s (dont 62 sur `D-02`, enseignant non habilité). Les violations étaient réparties uniformément — signature d'une optimisation incomplète, pas d'un bug.
**Décision.** Chaque `SeancePlan` porte son propre domaine (`@ValueRangeProvider` d'entité) : les salles admissibles (type `E-02`, équipements `E-03`, capacité `E-01`) et les enseignants admissibles (affectation imposée `C-06`, sinon habilités `D-02`). Le solveur ne propose jamais une valeur invalide au lieu de la pénaliser après coup.
**Conséquence.** Violations dures passées de 90 à 5, puis à **0** après élargissement de la grille. Les contraintes correspondantes sont conservées (avec leurs tests) : elles restent le filet de sécurité si les données sont incohérentes. Repli explicite sur l'ensemble complet si un filtre ne laisse aucune valeur, pour que la contrainte signale le problème au lieu de bloquer la résolution.

## D-013 — Répartition de service seedée dans la démonstration
**Décision.** Le jeu de démonstration crée 36 affectations (`C-06`) : dans un établissement réel, la direction fixe qui enseigne quoi à chaque classe **avant** de construire l'emploi du temps. L'Informatique reste libre (dédoublement `B-04` : deux enseignants simultanés par classe, le solveur répartit).
**Conséquence.** Combinée à D-012, la variable « enseignant » est fixée pour 104 des 112 séances.

## D-014 — Analyse du score recalculée en Java (Timefold Community)
**Contexte.** `SolutionManager.analyze()` échoue à l'exécution : *« A commercial feature "Score analysis" was requested but it could not be loaded »* — y compris avec `FETCH_MATCH_COUNT`. L'analyse de score est une fonctionnalité **payante** de Timefold.
**Décision.** La règle `I-06` est assurée par `DiagnosticPlanning` (module solveur) : comptage des violations de chaque règle **dure** sur le planning final, réutilisant les prédicats des contraintes (`CalculsPlanning`) pour rester cohérent avec le score, avec un libellé français par règle. Aucune licence requise, testé unitairement.
**Conséquence.** L'écran « Analyse » affiche `règle → nombre de violations → description`. Les règles souples ne sont pas détaillées (seul leur total apparaît dans le score).

## D-015 — Limite connue : alignement des barrettes (`B-05`)
**Contexte.** Sur le jeu de démonstration, le solveur atteint `[0/0/0]hard` (emploi du temps entièrement valide) **sauf** quand la barrette Anglais 2AC est active : il reste alors invariablement 3 violations `B-05`, identiques à 120 s comme à 300 s.
**Analyse.** Le modèle est correct (le mapper encode `idBarrette × 1000 + rang`, donc 3 paires de rang comparées, et le test unitaire `B-05` passe). Aligner une paire exige de déplacer **deux** séances vers un créneau libre commun aux deux classes : un mouvement composé que le jeu de mouvements par défaut de Timefold (`ChangeMove`/`SwapMove`, une variable à la fois) ne génère pas. Chaque mouvement isolé crée un conflit `B-02` intermédiaire et est donc rejeté.
**Décision.** Limite documentée, non masquée : le diagnostic `I-06` la nomme explicitement à l'utilisateur. Correctif identifié pour un lot ultérieur — un `MoveIteratorFactory` produisant le déplacement conjoint des séances d'un même groupe d'alignement (bénéficierait aussi à `B-04`).
**Contournement.** Sans barrette, le même établissement génère un emploi du temps sans aucune violation dure.

## D-016 — Export PDF des emplois du temps
**Contexte.** Les établissements impriment et distribuent les emplois du temps ; le modèle attendu est celui en usage au Maroc (en-tête établissement, titre « EMPLOI DU TEMPS », ligne classe / année scolaire, grille jours × horaires avec case d'angle barrée en diagonale).
**Décision.** Nouveau module Maven **`jadwal-export`**, pur (ni JPA ni Spring), s'appuyant sur **OpenPDF 2.0.3** (LGPL/MPL — pas de licence commerciale, contrairement à iText 7). Il reçoit un modèle neutre (`DocumentPlanning`) et rend le PDF ; `ExportPlanningService` (dans `jadwal-api`) fait la conversion depuis la base. Une page par classe.
**Conséquences.**
- La grille imprimée suit la **grille horaire de l'établissement** (A-07) : une ligne par unité de créneau, libellés horaires calculés depuis `heureDebut` et `dureeUniteMinutes`, bandeau pleine largeur par plage bloquée. Rien n'est codé en dur.
- Les **séances des sous-groupes figurent dans la grille de leur classe** (et non sur des pages séparées quasi vides) : les demi-groupes simultanés (`B-04`) sont fusionnés dans une seule case, une ligne par sous-groupe — comme sur un emploi du temps papier.
- À défaut de logo téléversé, un **monogramme** (initiales de l'établissement) tient la place du logo.
- Le téléchargement passe par une **route Next dédiée** (`/api/exports/plannings`) : le proxy générique `/api/backend/[...path]` réencode les réponses en JSON et ne peut pas relayer un binaire.

## D-017 — Adoption du design system Ynexis
**Contexte.** JADWAL était habillé d'un thème indigo ad hoc. Ynexis dispose déjà d'un design system (celui du tableau de bord *Ynexis AI Call Center*, marque Yakeey) et une maquette « JADWAL Ynexis » a été produite dans claude.ai/design pour porter JADWAL sur cette identité.
**Décision.** Les fichiers de tokens du design system sont copiés **verbatim** dans `frontend/app/tokens/` (source unique des valeurs) et **liés au thème Tailwind** via `@theme` dans `globals.css`. Les utilitaires (`bg-surface-card`, `text-ink-muted`, `border-line-subtle`, `bg-brand`…) rendent donc exactement les valeurs Ynexis : teal `#47a398`, Inter, neutres froids, contrôles de 38 px, rayons 6/8/12 px.
**Pourquoi le pont Tailwind plutôt que l'idiome natif du DS.** Le design system s'utilise nativement en styles inline `var(--token)` (aucune classe CSS). Recopier cet idiome dans une application Next.js/Tailwind aurait été verbeux et non idiomatique ; recopier les valeurs en dur aurait créé deux sources de vérité. Le pont `@theme` garde une seule source (les fichiers de tokens) tout en conservant l'écriture Tailwind.
**Conséquences.**
- Le bundle `_ds_bundle.js` du design system **n'est pas consommé** : c'est un IIFE destiné au runtime de claude.ai/design (`window.YnexisDesignSystem_f04fb4.*`), pas un paquet npm. Les composants de `components/ui/` sont restylés d'après les tokens ; leurs **API publiques sont inchangées** (aucun écran modifié structurellement).
- Les palettes Tailwind par défaut sont remplacées : `gray-*`/`slate-*` → `neutral-*` Ynexis, `indigo-*` → `teal-*`, et les familles `red/green/amber/blue` sont rabattues sur les paliers Ynexis (le DS n'en définit que 50/100/500/600/700).
- Couleurs de matières du jeu de démonstration réalignées sur une palette catégorielle mate. Ce sont des **données** (`matiere.couleur`), pas des tokens : chaque établissement reste libre de les changer.
- Conventions et vocabulaire : [DESIGN-SYSTEM.md](DESIGN-SYSTEM.md). Les échelles typographiques et de rayons y sont dupliquées en valeurs littérales dans `@theme` (Tailwind refuse une auto-référence `var()`) — à garder en phase lors d'une mise à jour du DS.

## D-011 — Identifiants de contraintes = codes du cahier
**Décision.** Chaque contrainte Timefold est nommée par le code exact de sa règle (`asConstraint("B-01")`). Les pondérations par établissement (`I-01`, table `ponderation`) et l'analyse de score (`I-06`, écran d'analyse) sont ainsi directement traçables vers `docs/REGLES.md`.
