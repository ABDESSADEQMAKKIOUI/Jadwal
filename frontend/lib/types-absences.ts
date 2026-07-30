/**
 * Types du module Vie scolaire — absences (Lot 2).
 * Alignés champ pour champ sur le contrat REST `/api/ecole/absences`.
 *
 * Fichier séparé de `types-scolarite.ts` (élèves) : les deux domaines ont été
 * écrits en parallèle, mieux vaut deux fichiers qu'un conflit.
 *
 * Rappel : ces données décrivent des mineurs. Elles s'affichent à l'écran pour
 * le personnel de l'établissement, elles ne vont jamais dans une trace, une URL
 * ni un journal côté navigateur.
 */

/** Nature de l'évènement saisi sur la feuille d'appel. */
export type TypeAbsence = 'ABSENCE' | 'RETARD' | 'EXCLUSION';

/** Contexte d'appel. `JOURNEE` vaut deux demi-journées dans les statistiques. */
export type DemiJournee = 'MATIN' | 'APRES_MIDI' | 'JOURNEE';

/** Situation administrative de l'élève. */
export type StatutEleve = 'INSCRIT' | 'PARTI' | 'REDOUBLANT';

/** Granularité des séries de statistiques. */
export type PeriodeStatistiques = 'JOUR' | 'SEMAINE' | 'MOIS';

/** Une saisie de vie scolaire telle que la liste de suivi l'affiche. */
export interface Absence {
  id: number;
  eleveId: number;
  eleveNom: string;
  codeMassar: string;
  /** `null` si l'élève n'est rattaché à aucune classe. */
  groupeLibelle: string | null;
  /** Date ISO `aaaa-mm-jj`. */
  dateAbsence: string;
  demiJournee: DemiJournee;
  type: TypeAbsence;
  justifiee: boolean;
  motif: string | null;
}

/** Saisie déjà enregistrée pour un élève sur une feuille d'appel. */
export interface SaisieAbsence {
  id: number;
  type: TypeAbsence;
  /**
   * Contexte réel de la saisie : une absence sur la journée entière peut
   * concerner un élève dont on appelle une demi-journée.
   */
  demiJournee: DemiJournee;
  justifiee: boolean;
  motif: string | null;
}

/** En-tête de groupe d'une feuille d'appel. */
export interface GroupeFeuille {
  id: number;
  libelle: string;
  niveauId: number;
  niveauLibelle: string;
  /** Capacité déclarée du groupe, à ne pas confondre avec le nombre d'élèves rattachés. */
  effectif: number;
}

/** Une ligne de feuille d'appel : l'élève, et sa saisie du contexte si elle existe. */
export interface EleveFeuille {
  eleveId: number;
  nom: string;
  prenom: string;
  codeMassar: string;
  statut: StatutEleve;
  /** `null` quand aucune saisie ne concerne l'élève dans ce contexte. */
  absence: SaisieAbsence | null;
}

/** Feuille d'appel d'un groupe (GET /api/ecole/absences/feuille). */
export interface FeuilleAppel {
  groupe: GroupeFeuille;
  date: string;
  demiJournee: DemiJournee;
  /** Séance appelée, `null` pour un appel de demi-journée. */
  seanceId: number | null;
  eleves: EleveFeuille[];
}

/**
 * Une ligne soumise en lot. `type: null` déclare l'élève **présent** et
 * supprime donc sa saisie antérieure.
 */
export interface LigneAppel {
  eleveId: number;
  type: TypeAbsence | null;
  justifiee: boolean;
  motif: string | null;
}

/**
 * Saisie en lot d'une feuille d'appel (POST /api/ecole/absences/feuille).
 * Aucun `etablissementId` : il vient du JWT, côté serveur.
 */
export interface SaisieFeuilleRequete {
  groupeId: number;
  date: string;
  demiJournee: DemiJournee;
  seanceId: number | null;
  saisies: LigneAppel[];
}

/** Effet d'une saisie en lot. Rejouer la même feuille est idempotent. */
export interface ResultatFeuille {
  crees: number;
  misAJour: number;
  /** Saisies effacées : l'élève a été déclaré présent. */
  supprimes: number;
}

/** Correction d'une saisie existante (PATCH /api/ecole/absences/{id}). */
export interface ModificationAbsenceRequete {
  justifiee?: boolean | null;
  type?: TypeAbsence | null;
  /** Chaîne vide = motif effacé ; absent ou `null` = motif inchangé. */
  motif?: string | null;
}

/** Cumuls d'absentéisme d'une période. */
export interface TotauxAbsences {
  absences: number;
  retards: number;
  exclusions: number;
  justifiees: number;
  nonJustifiees: number;
  /** Pourcentage arrondi au dixième. */
  tauxAbsenteisme: number;
  /** Dénominateur du taux : élèves INSCRIT du périmètre. */
  elevesInscrits: number;
  /** Jours de la période hors dimanche. */
  joursOuvrables: number;
}

/** Un point de la série temporelle d'absentéisme. */
export interface SerieAbsences {
  /** `aaaa-mm-jj` pour un jour ou une semaine (son lundi), `aaaa-mm` pour un mois. */
  cle: string;
  libelle: string;
  absences: number;
  retards: number;
}

/** Absentéisme d'une classe sur la période. */
export interface AbsencesParGroupe {
  groupeId: number;
  groupeLibelle: string;
  /** Élèves INSCRIT rattachés au groupe, dénominateur du taux. */
  effectif: number;
  absences: number;
  tauxAbsenteisme: number;
}

/**
 * Statistiques d'absentéisme (GET /api/ecole/absences/statistiques).
 * `debut` et `fin` sont renvoyés parce que le serveur les complète quand la
 * requête les omet (mois en cours).
 */
export interface StatistiquesAbsences {
  periode: PeriodeStatistiques;
  debut: string;
  fin: string;
  totaux: TotauxAbsences;
  /** Un point par regroupement, dans l'ordre chronologique, trous compris. */
  series: SerieAbsences[];
  /** Classes du périmètre, triées par libellé. */
  parGroupe: AbsencesParGroupe[];
}

/** Élève dont les absences non justifiées atteignent le seuil sur la période. */
export interface AlerteAbsence {
  eleveId: number;
  eleveNom: string;
  codeMassar: string;
  groupeId: number | null;
  groupeLibelle: string | null;
  nonJustifiees: number;
}

/** Seuil d'alerte retenu par le serveur quand la requête n'en fournit pas. */
export const SEUIL_ALERTE_PAR_DEFAUT = 4;

/** Nombre de jours maximum accepté par l'endpoint de statistiques. */
export const JOURS_MAX_STATISTIQUES = 400;

const LIBELLES_DEMI_JOURNEE: Record<DemiJournee, string> = {
  MATIN: 'Matin',
  APRES_MIDI: 'Après-midi',
  JOURNEE: 'Journée entière',
};

export function libelleDemiJournee(demiJournee: DemiJournee): string {
  return LIBELLES_DEMI_JOURNEE[demiJournee] ?? demiJournee;
}

const LIBELLES_TYPE: Record<TypeAbsence, string> = {
  ABSENCE: 'Absence',
  RETARD: 'Retard',
  EXCLUSION: 'Exclusion',
};

export function libelleTypeAbsence(type: TypeAbsence): string {
  return LIBELLES_TYPE[type] ?? type;
}

/**
 * Deux contextes qui se recouvrent ne peuvent coexister pour un même élève et
 * une même date : enregistrer l'un remplace l'autre. Miroir exact de
 * `DemiJournee.chevauche` côté serveur — c'est ce qui permet à l'écran de
 * prévenir avant d'écraser une saisie faite sur la journée entière.
 */
export function chevauche(a: DemiJournee, b: DemiJournee): boolean {
  return a === 'JOURNEE' || b === 'JOURNEE' || a === b;
}
