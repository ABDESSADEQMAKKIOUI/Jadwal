/**
 * Types du module Génération & Planning (Lot 1).
 * Alignés sur le contrat REST /api/ecole/*.
 */

export type Jour =
  | 'LUNDI'
  | 'MARDI'
  | 'MERCREDI'
  | 'JEUDI'
  | 'VENDREDI'
  | 'SAMEDI'
  | 'DIMANCHE';

export type Semaine = 'A' | 'B' | 'TOUTES';

export type TypeCreneau = 'COURS' | 'DEJEUNER' | 'PAUSE';

export type VuePlanning = 'GROUPE' | 'ENSEIGNANT' | 'SALLE';

/** Séance planifiée (SEANCE_JSON du contrat). */
export interface Seance {
  id: number;
  groupeId: number;
  groupeLibelle: string;
  matiereId: number;
  matiereLibelle: string;
  couleur: string | null;
  enseignantId: number | null;
  enseignantNom: string | null;
  salleId: number | null;
  salleNom: string | null;
  creneauId: number;
  jour: Jour;
  indexDebut: number;
  dureeUnites: number;
  semaine: Semaine;
  verrouillee: boolean;
}

/** Créneau de la grille renvoyé par les vues planning. */
export interface CreneauGrille {
  id: number;
  jour: Jour;
  indexDebut: number;
  type: TypeCreneau;
  unitesDisponibles: number;
}

/** Réponse des vues planning (groupe / enseignant / salle). */
export interface PlanningVue {
  creneaux: CreneauGrille[];
  seances: Seance[];
}

/** Plage bloquée de la grille horaire (déjeuner, pause…). */
export interface PlageBloquee {
  type: TypeCreneau | string;
  indexDebut: number;
  dureeUnites: number;
}

/** Grille horaire de l'établissement (GET /api/ecole/grille). */
export interface GrilleHoraire {
  joursActifs: Jour[];
  heureDebut: string;
  dureeUniteMinutes: number;
  unitesParJour: number;
  plagesBloquees: PlageBloquee[];
  amplitudeMaxUnites: number;
}

export type StatutFaisabilite = 'OK' | 'AVERTISSEMENT' | 'ECHEC';

/** Bilan unitaire du rapport de faisabilité. */
export interface BilanFaisabilite {
  code: string;
  libelle: string;
  statut: StatutFaisabilite;
  message: string;
  details: string | null;
}

/** Rapport de faisabilité (POST /api/ecole/faisabilite). */
export interface FaisabiliteRapport {
  global: StatutFaisabilite;
  bilans: BilanFaisabilite[];
}

export type StatutGeneration =
  | 'EN_COURS'
  | 'TERMINEE'
  | 'INFAISABLE'
  | 'ECHEC'
  | 'ARRETEE';

/** Génération d'emploi du temps. */
export interface Generation {
  id: number;
  statut: StatutGeneration;
  score: string | null;
  dureeMaxSecondes: number;
  lanceeLe: string;
  termineeLe: string | null;
  versionId: number | null;
}

/** Événement SSE "progression" d'une génération. */
export interface ProgressionGeneration {
  score: string | null;
  tempsEcouleSecondes: number;
  statut: StatutGeneration;
}

/** Ligne d'analyse par contrainte (GET /generations/{id}/analyse). */
export interface ContrainteAnalyse {
  regle: string;
  libelle: string;
  nombreViolations: number;
}

/** Analyse du score d'une génération. */
export interface AnalyseScore {
  score: string | null;
  contraintes: ContrainteAnalyse[];
}

/** Version d'un planning (I-08). */
export interface PlanningVersion {
  id: number;
  libelle: string;
  active: boolean;
  creeeLe: string;
  nbSeances: number;
}

/** Conflit renvoyé lors d'un déplacement de séance refusé (409, I-04). */
export interface Conflit {
  regle: string;
  message: string;
}

/** Pondération d'une règle souple (GET/PUT /api/ecole/ponderations). */
export interface Ponderation {
  regle: string;
  libelle: string;
  famille: string;
  poids: number;
}

/** Groupe (résumé, pour les sélecteurs). */
export interface GroupeResume {
  id: number;
  libelle: string;
  effectif: number;
  niveauId: number;
  niveauLibelle: string;
  parentId: number | null;
  type: string;
  sousGroupes: GroupeResume[];
}

/** Enseignant (résumé, pour les sélecteurs). */
export interface EnseignantResume {
  id: number;
  nomComplet: string;
}

/** Salle (résumé, pour les sélecteurs et dialogs). */
export interface SalleResume {
  id: number;
  nom: string;
  capacite: number;
  type: string;
  batiment: string | null;
}
