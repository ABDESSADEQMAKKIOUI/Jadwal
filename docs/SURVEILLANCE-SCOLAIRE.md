# Surveillance générale des établissements — cadrage

Extension de JADWAL demandée le 2026-07-26 : passer d'un générateur d'emplois du temps à une
**application de surveillance générale** de l'établissement (absences, listes Massar, examens
locaux, statistiques de réussite, emplois du temps).

---

## 1. Ce qui existe déjà, ce qui manque

| Fonctionnalité demandée | État |
|---|---|
| **Organisation des emplois du temps** | ✔ **livré** — solveur Timefold, grille D&D, export PDF (lots L1→L4 + L6 partiel) |
| Suivi des absences | à construire |
| Export des listes Massar | à construire |
| Organisation des examens locaux | à construire |
| Statistiques de réussite | à construire |

### Le verrou : l'élève n'existe pas

Le modèle actuel ne connaît **pas les élèves**. `Groupe` porte un simple `effectif` (un nombre) ;
il n'y a aucune entité `Eleve`, aucun code Massar, aucune note. Or :

- une **absence** se rattache à un élève nommé ;
- une **liste Massar** est une liste d'élèves ;
- un **bulletin** et donc un **taux de réussite** dérivent de notes par élève ;
- une **convocation d'examen** est nominative.

**Les quatre fonctionnalités demandées reposent donc toutes sur la même brique manquante.** C'est
elle qu'il faut poser d'abord, et bien : elle conditionne le reste.

## 2. Conséquence majeure : des données personnelles de mineurs

C'est le changement de nature le plus important de cette extension, et il ne doit pas être traité
après coup.

Aujourd'hui JADWAL stocke des données professionnelles (enseignants, établissements). Demain il
stockera, pour des **mineurs identifiés** : nom, prénom, date de naissance, code Massar (identifiant
national), **absences** (donc présence physique jour par jour) et **notes** (donc performance
scolaire). Le cahier de charges initial le prévoyait déjà : *« Données personnelles d'élèves et
d'enseignants : déclaration CNDP au titre de la loi 09-08 »* (PROJET.md §11).

Ce que cela impose concrètement, dès la première ligne de code du module :

1. **Isolation par établissement sans exception.** Une fuite inter-établissement n'est plus une
   gêne fonctionnelle, c'est une violation de données personnelles. Tout accès à un élève, une
   absence, une note passe par `findBy…AndEtablissementId` — jamais par `findById` seul.
2. **Moindre privilège par rôle.** Un enseignant n'a pas à voir les notes de toutes les classes ;
   un surveillant n'a pas à voir les bulletins. Les rôles actuels (`SUPER_ADMIN`, `DIRECTEUR`) ne
   suffisent plus : il faut au minimum `VIE_SCOLAIRE` (saisie des absences) et `ENSEIGNANT`.
3. **Aucune donnée personnelle dans les journaux.** Pas de nom, pas de code Massar, pas de note
   dans un `log.info`. Les identifiants techniques suffisent au diagnostic.
4. **Traçabilité des accès aux notes et absences** — qui a consulté ou modifié quoi.
5. **Le super-admin de la plateforme n'a pas à lire les élèves.** Il gère des abonnements. Les
   endpoints élèves restent hors de `/api/admin/**`.
6. **Rétention.** Une durée de conservation doit être décidée (année scolaire + N années) ; les
   exports contiennent des listes nominatives et ne doivent pas être servis en URL publique.

## 3. Découpage proposé

Quatre modules, dans cet ordre — chacun s'appuie sur le précédent.

### Lot A — Élèves et listes Massar (fondation)

Sans lui, rien d'autre n'est possible.

- Entité `Eleve` : établissement, groupe, **code Massar** (unique par établissement), nom, prénom,
  date et lieu de naissance, sexe, statut (inscrit / parti / redoublant), tuteur (nom, téléphone).
- Import CSV d'une liste Massar (l'établissement reçoit ses listes du ministère) avec
  **prévisualisation et validation avant écriture** — même principe que la règle `D-04` du cahier :
  aucune donnée importée n'entre sans un clic humain.
- Export **CSV et Excel**, avec **choix des colonnes** et filtres (niveau, classe, statut).
- Écran de gestion : recherche, filtres, affectation d'un élève à une classe, effectif recalculé
  automatiquement sur `Groupe`.

### Lot B — Absences

- Entité `Absence` : élève, date, séance ou demi-journée, type (absence / retard / exclusion),
  justifiée ou non, motif, qui a saisi.
- **Saisie rapide par classe** : une classe, une date, la liste des élèves, cases à cocher — c'est
  l'écran le plus utilisé de toute l'application, il doit tenir en un écran et quelques clics.
- Statistiques **jour / semaine / mois**, filtres par classe, niveau, élève, matière, enseignant.
- Alertes : seuil d'absences non justifiées atteint (l'exigence « alertes et notifications »).
- Rattachement aux séances de l'emploi du temps déjà généré : l'absence est datée **et** située
  dans la grille, ce qui permet « absences par matière » et « par enseignant ».

### Lot C — Examens locaux

- `SessionExamen` : libellé, niveaux concernés (**CE6, 3AC, 1BAC, 2BAC**), période.
- `EpreuveExamen` : matière, date, heure, durée, niveaux.
- Répartition : affectation des **salles** et des **surveillants** (réutilise `Salle` et
  `Enseignant`), placement des élèves par salle, avec contrôle de capacité.
- **Convocations PDF** nominatives, élèves et surveillants — réutilise le module `jadwal-export`
  déjà en place pour les emplois du temps.
- Les contraintes d'unicité de ressources sont les mêmes que pour l'emploi du temps (une salle, un
  surveillant, un créneau) : la logique de détection de conflits est réutilisable.

### Lot D — Notes et statistiques de réussite

- `Bulletin` / `Note` : élève, matière, période (semestre / trimestre), note, coefficient.
- Calcul des **moyennes** et des **taux de réussite** (seuil paramétrable, 10/20 par défaut).
- Statistiques par classe, par niveau, par matière ; **comparaison entre classes et niveaux**.
- Export des résultats.

## 4. Positionnement produit — une question à trancher

L'intitulé demandé est « Application de Surveillance Générale pour les Établissements Scolaires ».
JADWAL devient donc un **outil de gestion scolaire** dont l'emploi du temps n'est qu'un module.

Deux options :

1. **Un seul produit élargi** — JADWAL absorbe les modules. La navigation gagne quatre entrées
   (Élèves, Absences, Examens, Résultats). Simple, cohérent, un seul abonnement.
2. **Des modules activables par abonnement** — le plan `ESSENTIEL` ne contient que l'emploi du
   temps, le plan `PREMIUM` ouvre absences / examens / résultats. Cela valorise commercialement
   l'extension et limite la surface exposée aux établissements qui n'en ont pas besoin.

L'option 2 se greffe naturellement sur le modèle d'abonnement déjà en place (`PlanAbonnement`), et
la contrainte de moindre privilège la rend souhaitable. **À confirmer avant le lot A**, car elle
change le contrôle d'accès de tous les endpoints suivants.

## 5. Rôles à ajouter

Le modèle actuel n'a que `SUPER_ADMIN` et `DIRECTEUR`. Le cahier initial en prévoyait davantage
(PROJET.md §2). Cette extension rend nécessaires au minimum :

| Rôle | Périmètre |
|---|---|
| `DIRECTEUR` | tout l'établissement |
| `VIE_SCOLAIRE` | saisie et suivi des absences, convocations — **pas** les notes |
| `ENSEIGNANT` | ses classes : saisie des notes, consultation de ses absences |
| `ELEVE` / `PARENT` (plus tard) | consultation en lecture seule de son propre dossier |

Les deux derniers supposent un portail et une gestion de comptes élèves : hors de cette extension.
