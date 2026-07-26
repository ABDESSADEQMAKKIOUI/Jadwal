# JADWAL

JADWAL est une plateforme SaaS de génération d'emplois du temps scolaires, pensée pour le contexte marocain. Elle permet aux établissements (écoles, collèges, lycées) de gérer leur référentiel (enseignants, classes, salles, matières) et de générer automatiquement des emplois du temps, sous un modèle d'abonnement administré par un super-admin.

Le moteur de génération repose sur **Timefold Solver 2.2** et implémente le cahier des 53 règles
(`A-01` → `I-09`) documenté dans [docs/REGLES.md](docs/REGLES.md) : chaque règle est tracée jusqu'à
une contrainte du solveur **et** jusqu'à un test unitaire.

## Prérequis

- **Docker Desktop** — c'est tout. Aucun JDK, aucun Node.js n'est requis en local : tout s'exécute dans des conteneurs.

## Démarrage

**1. Générer les secrets.** Aucun secret n'a de valeur par défaut : le démarrage échoue si l'un
manque, est trop court, ou reprend une valeur ayant circulé publiquement. C'est volontaire — ce
dépôt est public, un secret par défaut y serait lisible par tout le monde et permettrait de forger
un jeton d'administration (voir [docs/DECISIONS.md](docs/DECISIONS.md) D-018).

```bash
cp .env.example .env
```

Puis remplacer chaque `A_REMPLACER` dans `.env` :

```bash
openssl rand -base64 48   # JADWAL_JWT_SECRET  (32 octets minimum)
openssl rand -base64 24   # POSTGRES_PASSWORD, MINIO_ROOT_PASSWORD
```

`JADWAL_ADMIN_PASSWORD` est le mot de passe du compte `admin@jadwal.ma` : 12 caractères minimum.
`.env` est ignoré par git et ne doit jamais être committé.

**2. Démarrer.**

```bash
docker compose up --build -d
```

Le premier build peut prendre plusieurs minutes (téléchargement des dépendances Maven et npm).

> Seul le port du frontend (3000) est exposé sur le réseau. La base, Redis, MinIO et l'API sont
> liés à `127.0.0.1` : joignables depuis la machine pour le développement, jamais depuis le réseau.

### Démarrer avec le jeu de démonstration

Pour explorer le moteur immédiatement, avec un collège complet déjà paramétré (2 niveaux, 4 classes
dédoublées, 10 matières, 11 salles, 14 enseignants dont 2 mixtes, une barrette, la répartition de
service) :

Mettre `JADWAL_DEMO=true` dans `.env`, puis démarrer normalement.

Connexion : `demo@jadwal.ma` / `demo123`. Puis **Génération → Vérifier la faisabilité → Lancer la
génération** : l'emploi du temps des 4 classes est construit en 1 à 2 minutes.

> Ce jeu crée un compte à mot de passe faible et le backend l'annonce par un avertissement au
> démarrage. **À réserver au développement** — ne jamais activer `JADWAL_DEMO` en production.

## URLs

| Service | URL |
|---|---|
| Application (frontend) | http://localhost:3000 |
| API (backend) | http://localhost:8080 |
| Console MinIO | http://localhost:9001 |

## Connexion initiale

Un compte **super-admin** est créé automatiquement au premier démarrage du backend :

- Email : `admin@jadwal.ma`
- Mot de passe : `admin123` (valeur par défaut — à changer via la variable `JADWAL_ADMIN_PASSWORD`, voir `.env.example`)

## Parcours type du super-admin

1. Créer l'**établissement**.
2. Créer le **compte directeur** rattaché à l'établissement.
3. Créer l'**abonnement** de l'établissement.
4. Enregistrer le **paiement** reçu (virement bancaire, espèces ou chèque).
5. L'abonnement s'**active** automatiquement.

> **Note importante** : les paiements sont gérés **hors plateforme** (virement bancaire ou espèces) ; la plateforme ne fait que les **tracer**. Il n'y a aucune intégration de paiement en ligne.

## Parcours type du directeur d'établissement

1. **Référentiel** — grille horaire (jours, unités de 30 min, pauses), niveaux, classes (avec
   dédoublement en sous-groupes), matières, salles, barrettes.
2. **Enseignants** — quotas, habilitations par matière et niveau, indisponibilités (les enseignants
   *mixtes* partagés avec l'Éducation nationale saisissent leur emploi du temps public, converti en
   indisponibilités bloquantes avec temps de trajet).
3. **Maquettes** — volumes hebdomadaires par niveau et matière, patterns de découpage, dédoublement,
   puis **répartition de service** (quel enseignant pour quelle classe).
4. **Génération** — *Vérifier la faisabilité* (bilans `H-01` → `H-06` : diagnostic chiffré et
   actionnable **avant** tout calcul), puis lancer la génération avec un budget de temps.
   La progression du score s'affiche en direct (SSE).
5. **Planning** — grille hebdomadaire par groupe, enseignant ou salle ; déplacement des séances en
   glisser-déposer avec **détection de conflits en temps réel** ; verrouillage des séances ;
   restauration d'une version antérieure.
6. **Export PDF** — depuis l'écran Planning, *Groupe affiché* ou *Tous les groupes* : un PDF prêt à
   imprimer, une page par classe, au format d'emploi du temps en usage dans les établissements
   marocains. Les demi-groupes apparaissent dans la grille de leur classe.

## Arborescence du repo

```
emploi/
├── backend/                 # API Spring Boot (Java 21, Maven multi-modules)
│   ├── jadwal-common/       # Socle commun (sécurité, erreurs, utilitaires)
│   ├── jadwal-referentiel/  # Établissements, utilisateurs, niveaux, groupes,
│   │                        #   matières, salles, grille horaire, barrettes
│   ├── jadwal-abonnement/   # Abonnements et paiements manuels
│   ├── jadwal-enseignant/   # Enseignants, habilitations, indisponibilités
│   ├── jadwal-pedagogie/    # Maquettes, affectations, pondérations
│   ├── jadwal-planning/     # Versions de planning, séances, générations
│   ├── jadwal-solver/       # Moteur Timefold : modèle, contraintes, faisabilité
│   │                        #   (module PUR, sans JPA ni Spring — 51 tests)
│   ├── jadwal-api/          # Module exécutable (contrôleurs REST, SSE, mappers)
│   └── Dockerfile
├── frontend/                # Next.js (React, TypeScript, Tailwind) — pattern BFF
│   └── Dockerfile
├── docs/                    # Spécification, cahier des règles, décisions d'archi
├── docker-compose.yml       # postgres, redis, minio, backend, frontend
├── .env.example             # Variables d'environnement (copier vers .env)
└── .github/workflows/ci.yml # CI : build des deux images Docker
```

## Documentation

| Document | Contenu |
|---|---|
| [docs/PROJET.md](docs/PROJET.md) | Spécification générale du produit |
| [docs/REGLES.md](docs/REGLES.md) | Les 53 règles + matrice de traçabilité règle → contrainte → test |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Journal des décisions d'architecture et limites connues |

## Tests

```bash
docker run --rm -v "$PWD/backend:/app" -v jadwal_m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn -B test
```

59 tests, dont **un test par règle du cahier** (`ConstraintVerifier` de Timefold) : c'est la
traçabilité spécification → code exigée par le cahier.

## Roadmap (lots)

| Lot | Contenu | État |
|---|---|---|
| **L0** | Infrastructure Docker, auth JWT, multi-tenant, admin plateforme, abonnements et paiements manuels | ✔ livré |
| **L1** | Référentiel complet (grille, niveaux, groupes dédoublés, matières, salles, barrettes), enseignants, maquettes, grille D&D avec détection de conflits | ✔ livré |
| **L2** | Bilans de faisabilité `H-01` → `H-06` | ✔ livré |
| **L3** | Solveur Timefold, contraintes dures | ✔ livré |
| **L4** | Contraintes souples, pondérations par établissement, équilibrage | ✔ livré |
| **L5** | Ingestion des emplois du temps publics (OCR + IA + validation humaine, `D-04`) | à faire |
| **L6** | **Export PDF des emplois du temps** ✔ · Excel, iCal, publication, notifications, portails enseignant et élève | partiel |
| **L7** | Absences, remplacements, replanification incrémentale (`I-07`), pilotage | à faire |

### Limite connue

L'alignement des **barrettes** (`B-05`) n'est pas toujours atteint par le solveur : il faut déplacer
conjointement les séances de plusieurs classes vers un créneau libre commun, un mouvement composé que
le jeu de mouvements par défaut de Timefold ne génère pas. Le diagnostic le signale explicitement à
l'utilisateur. Détail et correctif prévu : [docs/DECISIONS.md](docs/DECISIONS.md) (D-015).

## Dépannage

- **Port occupé** (3000, 8080, 5432, 6379, 9000 ou 9001) : arrêtez le service local qui utilise le port, ou modifiez le mappage de ports dans `docker-compose.yml` (partie gauche uniquement, ex. `"3001:3000"`).
- **Réinitialisation complète** (supprime les données PostgreSQL, Redis et MinIO) :

  ```bash
  docker compose down -v
  docker compose up --build -d
  ```

- **Voir les logs d'un service** :

  ```bash
  docker compose logs -f backend
  ```
