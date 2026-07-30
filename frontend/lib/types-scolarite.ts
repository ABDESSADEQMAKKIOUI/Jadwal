/**
 * Types du module Vie scolaire (élèves, import Massar, exports, absences).
 * Alignés sur le contrat REST `/api/ecole/eleves` et `/api/ecole/absences`,
 * champs additifs du backend compris.
 *
 * Ces données décrivent des mineurs nommés : aucun champ n'est ajouté ici sans
 * exister dans le contrat, et aucun identifiant d'établissement n'y figure —
 * l'établissement vient du jeton, côté serveur, jamais du client.
 */

/* ------------------------------------------------------------------ */
/* Élèves                                                              */
/* ------------------------------------------------------------------ */

export type StatutEleve = 'INSCRIT' | 'PARTI' | 'REDOUBLANT';

export type SexeEleve = 'M' | 'F';

/** ELEVE_JSON du contrat. Les champs facultatifs de la fiche sont nullables. */
export interface Eleve {
  id: number;
  codeMassar: string;
  nom: string;
  prenom: string;
  nomAr: string | null;
  prenomAr: string | null;
  /** Date ISO `aaaa-mm-jj`. */
  dateNaissance: string | null;
  lieuNaissance: string | null;
  sexe: SexeEleve | null;
  statut: StatutEleve;
  groupeId: number | null;
  groupeLibelle: string | null;
  niveauLibelle: string | null;
  tuteurNom: string | null;
  tuteurTelephone: string | null;
}

/** Enveloppe de pagination du contrat : `{contenu, total, page, taille}`. */
export interface PageReponse<T> {
  contenu: T[];
  total: number;
  page: number;
  taille: number;
}

/**
 * Corps de création d'un élève (POST). `codeMassar`, `nom` et `prenom` sont
 * obligatoires ; les autres champs sont omis quand ils ne sont pas saisis.
 */
export interface CreationEleve {
  codeMassar: string;
  nom: string;
  prenom: string;
  nomAr?: string;
  prenomAr?: string;
  dateNaissance?: string;
  lieuNaissance?: string;
  sexe?: SexeEleve | '';
  statut?: StatutEleve;
  tuteurNom?: string;
  tuteurTelephone?: string;
  groupeId?: number | null;
}

/**
 * Corps de modification partielle (PATCH). Sémantique du backend : champ absent
 * ou nul = inchangé, chaîne vide = champ facultatif effacé. Comme
 * `groupeId: null` signifie « inchangé », le retrait de classe se demande
 * explicitement avec `retirerDuGroupe`. La date de naissance n'est pas effaçable.
 */
export interface ModificationEleve {
  codeMassar?: string;
  nom?: string;
  prenom?: string;
  nomAr?: string;
  prenomAr?: string;
  dateNaissance?: string;
  lieuNaissance?: string;
  sexe?: SexeEleve | '';
  statut?: StatutEleve;
  tuteurNom?: string;
  tuteurTelephone?: string;
  groupeId?: number;
  retirerDuGroupe?: boolean;
}

/* ------------------------------------------------------------------ */
/* Import Massar (analyse puis validation humaine)                     */
/* ------------------------------------------------------------------ */

export type StatutLigneImport = 'NOUVEAU' | 'EXISTANT' | 'ERREUR';

/**
 * Fiche telle que l'analyse d'un fichier Massar l'a comprise : tout champ absent
 * du fichier reste nul, `id` n'est renseigné que pour une ligne rapprochée d'un
 * élève déjà inscrit — et le serveur ne s'y fie jamais, il refait le
 * rapprochement sur le code Massar.
 */
export type EleveImporte = { [Champ in keyof Eleve]: Eleve[Champ] | null };

export interface LigneImportEleve {
  /** Rang de la ligne dans le fichier ; l'en-tête est la ligne 1. */
  numero: number;
  donnees: EleveImporte;
  statut: StatutLigneImport;
  messages: string[];
}

export interface ResumeImportEleves {
  total: number;
  nouveaux: number;
  existants: number;
  erreurs: number;
}

/** Rapport d'analyse : produit SANS aucune écriture en base. */
export interface AnalyseImportEleves {
  /** Champs reconnus dans l'en-tête, dans l'ordre du fichier. */
  colonnesDetectees: string[];
  /** En-têtes non reconnus, restitués tels quels. */
  colonnesIgnorees: string[];
  /** Séparateur détecté (« ; » ou « , »). */
  separateur: string;
  lignes: LigneImportEleve[];
  resume: ResumeImportEleves;
}

/** Résultat de l'écriture d'un import validé par l'utilisateur. */
export interface ValidationImportEleves {
  crees: number;
  misAJour: number;
  ignores: number;
}

/* ------------------------------------------------------------------ */
/* Exports tableur                                                     */
/* ------------------------------------------------------------------ */

export type FormatExportEleves = 'csv' | 'xlsx';

/** Codes de colonnes acceptés par `?champs=` (ordre demandé = ordre des colonnes). */
export type ChampExportEleve =
  | 'codeMassar'
  | 'nom'
  | 'prenom'
  | 'nomAr'
  | 'prenomAr'
  | 'dateNaissance'
  | 'lieuNaissance'
  | 'sexe'
  | 'statut'
  | 'groupeLibelle'
  | 'niveauLibelle'
  | 'tuteurNom'
  | 'tuteurTelephone';

/* ------------------------------------------------------------------ */
/* Absences                                                            */
/* ------------------------------------------------------------------ */

export type TypeAbsence = 'ABSENCE' | 'RETARD' | 'EXCLUSION';

export type DemiJournee = 'MATIN' | 'APRES_MIDI' | 'JOURNEE';

export type PeriodeStatistiques = 'JOUR' | 'SEMAINE' | 'MOIS';

/** Une saisie de vie scolaire telle que l'écran de suivi l'affiche. */
export interface Absence {
  id: number;
  eleveId: number;
  /** « Nom Prénom », prêt à afficher. */
  eleveNom: string;
  codeMassar: string;
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
  /** Contexte réel de la saisie : une absence JOURNEE apparaît sur une feuille de demi-journée. */
  demiJournee: DemiJournee;
  justifiee: boolean;
  motif: string | null;
}

/** En-tête de groupe d'une feuille d'appel (`effectif` = capacité déclarée). */
export interface GroupeFeuille {
  id: number;
  libelle: string;
  niveauId: number | null;
  niveauLibelle: string | null;
  effectif: number;
}

/** Une ligne de feuille d'appel ; `absence: null` signifie présent. */
export interface EleveFeuille {
  eleveId: number;
  nom: string;
  prenom: string;
  codeMassar: string;
  statut: StatutEleve;
  absence: SaisieAbsence | null;
}

export interface FeuilleAppel {
  groupe: GroupeFeuille;
  date: string;
  demiJournee: DemiJournee;
  seanceId: number | null;
  eleves: EleveFeuille[];
}

/** Une ligne soumise depuis la feuille d'appel ; `type: null` = présent. */
export interface LigneAppel {
  eleveId: number;
  type: TypeAbsence | null;
  justifiee?: boolean;
  motif?: string;
}

export interface SaisieFeuille {
  groupeId: number;
  date: string;
  demiJournee?: DemiJournee;
  seanceId?: number | null;
  saisies: LigneAppel[];
}

export interface ResultatFeuille {
  crees: number;
  misAJour: number;
  supprimes: number;
}

export interface TotauxAbsences {
  absences: number;
  retards: number;
  exclusions: number;
  justifiees: number;
  nonJustifiees: number;
  /** Pourcentage arrondi au dixième. */
  tauxAbsenteisme: number;
  elevesInscrits: number;
  joursOuvrables: number;
}

export interface SerieAbsences {
  /** `aaaa-mm-jj` pour un jour ou une semaine (son lundi), `aaaa-mm` pour un mois. */
  cle: string;
  libelle: string;
  absences: number;
  retards: number;
}

export interface AbsencesParGroupe {
  groupeId: number;
  groupeLibelle: string;
  effectif: number;
  absences: number;
  tauxAbsenteisme: number;
}

export interface StatistiquesAbsences {
  periode: PeriodeStatistiques;
  /** Complétés par le serveur quand la requête les omet (mois en cours). */
  debut: string;
  fin: string;
  totaux: TotauxAbsences;
  series: SerieAbsences[];
  parGroupe: AbsencesParGroupe[];
}

export interface AlerteAbsence {
  eleveId: number;
  eleveNom: string;
  codeMassar: string;
  groupeId: number | null;
  groupeLibelle: string | null;
  nonJustifiees: number;
}
