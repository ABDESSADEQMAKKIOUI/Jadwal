# Cahier des règles — Générateur d'emplois du temps scolaires

Source de vérité : [regles_emploi_du_temps.pdf](regles_emploi_du_temps.pdf) (53 règles). Ce fichier en est la transcription structurée, augmentée d'une **matrice de traçabilité** : chaque règle → lot de la roadmap → composant qui l'implémente → test qui la prouve.

**Types de règles**

| Type | Signification |
|---|---|
| **DURE** | Violation interdite. Un emploi du temps qui l'enfreint est rejeté. |
| **SOUPLE** | Objectif à optimiser. Violation autorisée avec pénalité pondérée. |
| **CONTRÔLE** | Vérification arithmétique exécutée avant le lancement du solveur. |
| **STRUCT.** | Règle de structuration du référentiel, à appliquer dès le modèle de données. |

**Statuts** : `—` pas encore commencé · `⏳` en cours · `✔` implémenté + testé · `⚠` implémenté et testé unitairement, mais **limite connue** en résolution (voir [DECISIONS.md](DECISIONS.md) D-015).

---

## A. Structure du référentiel

| ID | Règle | Formalisation / paramètre | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| A-01 | L'établissement est découpé en niveaux scolaires ; chaque niveau contient un ou plusieurs groupes (classes) ; chaque groupe porte un effectif. | `Niveau 1..n → Groupe`, `groupe.effectif : entier` | STRUCT. | L1 | Entités `Niveau`, `Groupe` (jadwal-referentiel) | ✔ |
| A-02 | Le volume horaire hebdomadaire par matière est défini au niveau de la maquette pédagogique du niveau ; les groupes en héritent. | `maquette(niveau, matière) → volume_hebdo` | STRUCT. | L1 | Entité `MaquettePedagogique` (jadwal-pedagogie) | ✔ |
| A-03 | Un override de volume au niveau du groupe reste possible (option, section spéciale, redoublants). | `volume(groupe, matière)` override maquette | STRUCT. | L1 | Table d'override groupe×matière | ✔ |
| A-04 | Un groupe peut être dédoublé en sous-groupes pour certaines matières (TP, langues, informatique). Le sous-groupe est une entité planifiable distincte. | `groupe.parent_id`, `dedoublement : none\|partiel\|total` | STRUCT. | L1 | `Groupe.parent_id` — **à figer dès la 1ʳᵉ migration du L1** | ✔ |
| A-05 | Toutes les durées sont exprimées en unités de créneau atomique (30 min recommandé), jamais en heures (gère 1h30, 2h30). | `unité = 30 min` ; 4h = 8 unités | STRUCT. | L1 | `Seance.dureeUnites`, `Creneau.duree` en unités | ✔ |
| A-06 | Une séance est l'objet planifié : elle lie un groupe, une matière, un enseignant, une salle et un créneau. | `séance = (g, m, p, salle, créneau)` | STRUCT. | L1 | Entité `Seance` (jadwal-planning) | ✔ |
| A-07 | La grille horaire (jours ouvrés, créneaux, durées, pauses) est paramétrable par établissement, jamais codée en dur. | `grille : jours × créneaux` | STRUCT. | L1 | `Etablissement.grilleHoraire` (JSONB) | ✔ |

## B. Unicité et non-chevauchement

Aucune ressource ne peut être en deux endroits au même instant.

| ID | Règle | Formalisation | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| B-01 | Un enseignant ne peut assurer qu'une seule séance à un instant donné. | `∀ p, ∀ s : Σ x[p][g][m][s] ≤ 1` | DURE | L3 | Contrainte Timefold Hard-0 + test `ConstraintVerifier` | ✔ |
| B-02 | Un groupe ne peut suivre qu'une seule séance à un instant donné. | `∀ g, ∀ s : Σ x[g][m][s] ≤ 1` | DURE | L3 | Contrainte Hard-0 + test | ✔ |
| B-03 | Une salle ne peut accueillir qu'un seul groupe à un instant donné. | `∀ r, ∀ s : Σ y[g][r][s] ≤ 1` | DURE | L3 | Contrainte Hard-0 + test | ✔ |
| B-04 | Un groupe dédoublé occupe simultanément ses sous-groupes : séances des sous-groupes sur le même créneau. | `créneau(sg1) = créneau(sg2)` | DURE | L3 | Contrainte Hard-0 + test (modèle dès L1 via A-04) | ✔ |
| B-05 | Les séances mutualisées entre classes (options, langue 2, « barrettes ») sont alignées sur le même créneau. | `∀ barrette b : groupes de b alignés` | DURE | L3 | Contrainte Hard-0 + test ; entité `Barrette` | ⚠ |

## C. Volumes horaires par matière

Égalité stricte, pas un plafond : le solveur réalise un pavage exact de la grille.

| ID | Règle | Formalisation | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| C-01 | Le volume hebdomadaire de chaque matière est respecté exactement pour chaque groupe : ni plus, ni moins. | `Σ x[g][m][s] × durée(s) == volume_hebdo(g,m)` | DURE | L3 | Contrainte Hard-1 + test | ✔ |
| C-02 | Le volume se décompose en séances selon des patterns de découpage autorisés par matière (ex. 4h → 2+2 \| 2+1+1). Un seul pattern retenu par couple. | `patterns(g,m) = [[2,2],[2,1,1],…]` | DURE | L3 | Génération des `Seance` pré-solveur + contrainte | ✔ |
| C-03 | Certaines matières exigent des blocs insécables (TP, arts, EPS). La durée minimale de séance est un attribut de la matière. | `durée_séance_min(m)`, `durée_séance_max(m)` | DURE | L3 | Attributs `Matiere` (L1) + contrainte Hard-1 | ✔ |
| C-04 | Le volume en demi-groupe est distingué du volume en classe entière : il consomme davantage d'heures-professeur. | `volume_prof = vol_normal + vol_dédoublé × nb_sous_groupes` | DURE | L3 | Modèle maquette (L1) + contrainte ; alimente H-02 | — |
| C-05 | Un volume impair incompatible avec la trame est traité par alternance de quinzaine (semaine A/B) ou créneau court dédié. | `3h → 2h (sem. A) + 4h (sem. B)` | DURE | L3 | **Décision « semaines paires/impaires » à trancher avant L1** (numéro de semaine dans `Creneau`) | ✔ |
| C-06 | Une matière peut être partagée entre plusieurs enseignants : répartition du volume contractualisée. | `Σ_p volume(g,m,p) == volume_hebdo(g,m)` | DURE | L3 | Modèle affectation (L1) + contrainte | ✔ |
| C-07 | En co-enseignement, le volume élève reste inchangé mais le besoin enseignant est multiplié par le nombre d'intervenants. | `besoin_prof = k × h` | DURE | L3 | Modèle + contrainte ; alimente H-02 | — |
| C-08 | La somme des volumes d'un groupe ne peut excéder la capacité de sa grille ; marge (slack) de 5 à 10 % requise. | `Σ volume(g,m) ≤ 0.95 × nb_créneaux(g)` | CONTRÔLE | L2 | `bilanGrille` (service faisabilité) — cf. H-01 | ✔ |

## D. Enseignants

Deux régimes : enseignants **mixtes** (partagés avec l'État, EDT public subi) et enseignants **propres** à l'école.

| ID | Règle | Formalisation | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| D-01 | Quota horaire hebdomadaire contractuel jamais dépassé. | `Σ heures_affectées(p) ≤ quota(p)` | DURE | L3 | Contrainte Hard-2 + test | ✔ |
| D-02 | Un enseignant n'intervient que sur ses matières habilitées et niveaux autorisés. | `x = 0 si m ∉ habilitations(p)` | DURE | L3 | Contrainte Hard-2 + test | ✔ |
| D-03 | Enseignant mixte : EDT public importé (PDF, photo, saisie) converti en indisponibilités bloquantes non négociables. | `indispo(p) ← EDT_État(p)` | DURE | L3 (saisie) / L5 (import) | `Indisponibilite` source `ETAT` + contrainte Hard-2 | ✔ |
| D-04 | Tout EDT externe extrait par OCR/IA est validé humainement avant injection. Aucune extraction utilisée telle quelle. | `statut ∈ {brouillon, validé}` ; seul « validé » alimente le solveur | DURE | L5 | Écran de validation à 2 colonnes + statut en base | — |
| D-05 | Temps de trajet réservé avant et après chaque bloc d'activité externe de l'enseignant mixte. | `buffer = 30 à 45 min` | DURE | L3/L5 | Enrichissement des indisponibilités à la normalisation | ✔ |
| D-06 | Enseignant propre : disponible sur toute la grille, sous réserve d'indisponibilités personnelles déclarées et validées. | `indispo(p) ← déclarations validées` | DURE | L3 | `Indisponibilite` source `PERSONNELLE` + workflow de validation | ✔ |
| D-07 | Heures consécutives plafonnées et amplitude journalière bornée. | `max_consécutif(p) : 4 à 5 h` | DURE | L3 | Contrainte Hard-2 + test | ✔ |
| D-08 | Heures d'un vacataire regroupées sur le minimum de journées de présence. | `minimiser nb_jours_présence(p)` | SOUPLE | L4 | Contrainte Soft-2 + test | ✔ |
| D-09 | Heures creuses (trous) dans la journée d'un enseignant minimisées. | `pénalité × Σ trous(p)` | SOUPLE | L4 | Contrainte Soft-2 + test | ✔ |
| D-10 | Préférences horaires déclarées prises en compte, sans jamais primer sur une règle dure. | poids paramétrable | SOUPLE | L4 | Contrainte Soft-2 + test | ✔ |

## E. Salles et ressources matérielles

| ID | Règle | Formalisation | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| E-01 | Capacité de la salle ≥ effectif du groupe accueilli. | `capacité(r) ≥ effectif(g)` | DURE | L3 | Contrainte Hard-2 + test | ✔ |
| E-02 | Les matières à salle spécialisée (labo, informatique, gymnase, atelier) ne peuvent être planifiées ailleurs. | `type_salle_requis(m) ⊆ type(r)` | DURE | L3 | Contrainte Hard-2 + test | ✔ |
| E-03 | Les équipements requis par une matière sont présents dans la salle affectée. | `équipements(m) ⊆ équipements(r)` | DURE | L3 | Contrainte Hard-2 + test | ✔ |
| E-04 | Déplacements inutiles entre salles/bâtiments minimisés pour un même groupe. | `pénalité × changements_bâtiment` | SOUPLE | L4 | Contrainte Soft-2 + test | ✔ |

## F. Équilibrage des séances

La répartition reflète la force (coefficient) et la densité de contenu de chaque matière.

| ID | Règle | Formalisation | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| F-01 | Plafond journalier par matière (interdit de concentrer 4 h de maths le lundi). | `Σ_s∈d x[g][m][s] ≤ max_par_jour(m)` | DURE | L3 | Contrainte + test (niveau hard selon hiérarchie) | ✔ |
| F-02 | Espacement minimal entre deux séances d'une même matière. | `gap_min = ⌊5 / n_séances⌋ ; \|d1−d2\| ≥ gap_min` | SOUPLE | L4 | Contrainte Soft-1 + test | ✔ |
| F-03 | Charge journalière équilibrée d'un jour à l'autre. | `charge_min ≤ Σ ≤ charge_max` | SOUPLE | L4 | Contrainte Soft-1 + test | ✔ |
| F-04 | Alternance des intensités cognitives : pas de cumul de matières lourdes sur une demi-journée. | `Σ poids_cognitif(m) ≤ seuil / demi-journée` | SOUPLE | L4 | Contrainte Soft-1 + test (`Matiere.poidsCognitif`) | ✔ |
| F-05 | Matières à fort coefficient placées prioritairement en début de matinée. | bonus si matinée et coeff élevé | SOUPLE | L4 | Contrainte Soft-1 + test | ✔ |
| F-06 | Régularité hebdomadaire : même matière au même créneau d'une semaine à l'autre. | stabilité inter-semaines | SOUPLE | L4 | Contrainte Soft-1 + test | ✔ |
| F-07 | Deux séances d'une même matière ne sont adjacentes que si la matière autorise les blocs de 2 h. | `adjacence ssi durée_séance_max(m) ≥ 2` | DURE | L3 | Contrainte + test | ✔ |

## G. Rythme scolaire et bien-être des élèves

| ID | Règle | Formalisation | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| G-01 | Pause déjeuner garantie pour chaque groupe et chaque enseignant. | `créneau_déjeuner : bloqué` | DURE | L3 | Grille (A-07) + contrainte + test | ✔ |
| G-02 | Amplitude horaire journalière d'un groupe bornée. | `fin − début ≤ amplitude_max` | DURE | L3 | Contrainte + test | ✔ |
| G-03 | Heures consécutives sans pause plafonnées pour un groupe. | `max_consécutif(g) : 4 h typique` | DURE | L3 | Contrainte + test | ✔ |
| G-04 | Heures creuses dans la journée d'un groupe proscrites (pénalité forte). | `poids élevé × Σ trous(g)` | SOUPLE | L4 | Contrainte Soft-0 + test | ✔ |
| G-05 | EPS ni immédiatement avant le déjeuner ni en fin de journée si installations mutualisées. | `créneaux interdits(m_EPS)` | SOUPLE | L4 | Contrainte Soft-0 + test | ✔ |
| G-06 | Journées des jeunes niveaux allégées par rapport aux niveaux d'examen. | `charge_max(niveau)` | SOUPLE | L4 | Contrainte Soft-0 + test | ✔ |

## H. Faisabilité — contrôles préalables au calcul

Bilans arithmétiques exécutés **systématiquement avant toute génération** (quelques millisecondes) ; évitent un échec opaque du solveur.

| ID | Règle | Formalisation | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| H-01 | Bilan grille : le total des volumes d'un groupe tient dans les créneaux disponibles, avec marge (slack visé 5-10 %). | `Σ volume(g,m) ≤ créneaux(g)` | CONTRÔLE | L2 | `bilanGrille()` + test unitaire | ✔ |
| H-02 | Bilan besoin enseignant : intègre impérativement dédoublements et co-enseignement. | `besoin(m) = Σ_g [vol_normal + vol_dédoublé × nb_sg]` | CONTRÔLE | L2 | `bilanBesoinEnseignant()` + test | ✔ |
| H-03 | Bilan offre/besoin : la somme des quotas des enseignants habilités couvre le besoin ; sinon recrutement requis. | `besoin(m) ≤ Σ quota(p habilité)` | CONTRÔLE | L2 | `bilanOffreBesoin()` + test | ✔ |
| H-04 | Bilan salles spécialisées : les volumes exigeant un type de salle tiennent dans la capacité totale du parc. | `Σ volume(g, m_labo) ≤ nb_labos × nb_créneaux` | CONTRÔLE | L2 | `bilanSallesSpecialisees()` + test | ✔ |
| H-05 | Bilan disponibilité mixte : après déduction de l'EDT de l'État, les mixtes conservent assez de créneaux libres. | `créneaux_libres(p) ≥ heures_école(p)` | CONTRÔLE | L2 | `bilanDisponibiliteMixte()` + test | ✔ |
| H-06 | Tout contrôle en échec produit un message actionnable (« il manque 6 h d'enseignant de SVT »), pas un simple statut. | `diagnostic ≠ INFEASIBLE` | CONTRÔLE | L2 | Format du `FaisabiliteRapport` + test | ✔ |

## I. Relaxation, exceptions et exploitation

| ID | Règle | Formalisation | Type | Lot | Implémentation prévue | Statut |
|---|---|---|---|---|---|---|
| I-01 | Pondérations des règles souples paramétrables par établissement, jamais figées dans le code. | `poids ∈ [0,100]` par règle et par établissement | STRUCT. | L4 | `ConstraintWeightOverrides` + stockage par tenant | ✔ |
| I-02 | En cas d'infaisabilité, le volume horaire peut être relaxé avec tolérance et pénalité lourde, plutôt qu'aucun résultat. | `v − ε ≤ Σ ≤ v + ε`, écart affiché | SOUPLE | L4 | Contrainte de relaxation + affichage écart | — |
| I-03 | L'administrateur peut verrouiller des séances : constantes lors des générations ultérieures. | `séance.verrouillée = true` | DURE | L3 | `@PlanningPin` + endpoint verrouillage + test | ✔ |
| I-04 | Toute modification manuelle est soumise à une vérification de conflits en temps réel sur l'ensemble des règles dures. | validation synchrone au dépôt | DURE | L1 | Grille D&D : validation serveur des règles dures au `PATCH /api/seances/{id}` | ✔ |
| I-05 | Le moteur restitue la meilleure solution trouvée dans un budget de temps fixé, avec son score de qualité. | `budget : 30 s à 10 min` | STRUCT. | L3 | Terminaisons Timefold + persistance du score | ✔ |
| I-06 | En cas d'échec, le système identifie le sous-ensemble de contraintes en conflit et propose des corrections chiffrées. | noyau d'infaisabilité | CONTRÔLE | L3 | `SolutionManager.analyze` + rapport | ✔ |
| I-07 | Une absence d'enseignant déclenche une replanification limitée à la journée, sans reconstruire la semaine. | `périmètre = 1 jour`, reste figé | STRUCT. | L7 | `ProblemChange` (real-time planning) + pinning du reste | — |
| I-08 | Chaque emploi du temps généré est versionné ; retour à une version antérieure possible. | historique + rollback | STRUCT. | L6 (schéma dès L1) | `Seance.version` + table de versions | ✔ |
| I-09 | Une mise à jour de l'EDT public d'un enseignant mixte invalide les séances concernées et déclenche une alerte. | trigger sur `EDT_État` modifié | DURE | L5 | Invalidation + notification | — |

---

## Règle de priorité générale

En cas de conflit entre deux règles, l'ordre d'arbitrage est :

1. **Unicité des ressources** (B) — Hard 0
2. **Volumes horaires exacts** (C) — Hard 1
3. **Indisponibilités des enseignants mixtes** (D, E) — Hard 2
4. **Rythme et bien-être** (G) — Soft 0
5. **Équilibrage pédagogique** (F) — Soft 1
6. **Préférences individuelles** (D-08..D-10, E-04) — Soft 2

**Aucune règle souple ne peut justifier la violation d'une règle dure.** Le `BendableScore(3,3)` de Timefold encode cette hiérarchie : un niveau supérieur n'est jamais compensé par un niveau inférieur.

---

## Note de traçabilité — Lot 0 (actuel)

Le Lot 0 (socle, admin plateforme, comptes, abonnements, paiements manuels) ne couvre **aucune** règle A→I : c'est voulu, ces règles portent sur le moteur d'emplois du temps (L1→L7). Les seules exigences du cahier déjà actives au L0 :

- **A-07 (anticipation)** : la grille horaire sera un paramètre de l'établissement — aucune valeur de grille n'est codée en dur dans le socle.
- **I-01 (anticipation)** : rien dans le socle ne fige de pondération en code.
- La colonne **Statut** de ce document doit être mise à jour à chaque lot ; une règle passe à `✔` uniquement quand sa contrainte/service **et** son test existent.
