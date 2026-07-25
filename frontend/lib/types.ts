export type Role = 'SUPER_ADMIN' | 'DIRECTEUR';

export type StatutEtablissement = 'ACTIF' | 'SUSPENDU';

export type StatutAbonnement = 'EN_ATTENTE_PAIEMENT' | 'ACTIF' | 'SUSPENDU' | 'EXPIRE';

export type StatutPaiement = 'CONFIRME' | 'EN_ATTENTE' | 'REJETE';

export type ModePaiement = 'VIREMENT' | 'ESPECES' | 'CHEQUE';

export interface Utilisateur {
  id: number;
  email: string;
  nomComplet: string;
  role: Role;
  etablissementId: number | null;
  etablissementNom: string | null;
}

export interface AbonnementActifResume {
  id: number;
  planNom: string;
  dateDebut: string;
  dateFin: string;
  statut: StatutAbonnement;
}

export interface Etablissement {
  id: number;
  nom: string;
  code: string;
  ville: string;
  telephone: string;
  email: string;
  statut: StatutEtablissement;
  dateCreation: string;
  abonnementActif: AbonnementActifResume | null;
}

export interface CompteUtilisateur {
  id: number;
  email: string;
  nomComplet: string;
  role: Role;
  actif: boolean;
}

export interface AbonnementEtablissement {
  id: number;
  planId: number;
  planNom: string;
  prixAnnuel: number;
  dateDebut: string;
  dateFin: string;
  statut: StatutAbonnement;
  totalPaye: number;
}

export interface Paiement {
  id: number;
  abonnementId: number;
  etablissementId: number;
  etablissementNom: string;
  planNom: string;
  montant: number;
  mode: ModePaiement;
  reference: string | null;
  datePaiement: string;
  statut: StatutPaiement;
  note: string | null;
  dateCreation: string;
}

export interface EtablissementDetail {
  id: number;
  nom: string;
  code: string;
  ville: string;
  telephone: string;
  email: string;
  statut: StatutEtablissement;
  dateCreation: string;
  comptes: CompteUtilisateur[];
  abonnements: AbonnementEtablissement[];
  paiements: Paiement[];
}

export interface Plan {
  id: number;
  code: string;
  nom: string;
  prixAnnuel: number;
  description: string;
  actif: boolean;
}

export interface Abonnement {
  id: number;
  etablissementId: number;
  etablissementNom: string;
  planId: number;
  planNom: string;
  prixAnnuel: number;
  dateDebut: string;
  dateFin: string;
  statut: StatutAbonnement;
  totalPaye: number;
}

export interface DashboardAdmin {
  nbEtablissements: number;
  nbAbonnementsActifs: number;
  nbPaiementsEnAttente: number;
  totalEncaisseAnnee: number;
}

export interface EtablissementEcole {
  id: number;
  nom: string;
  code: string;
  ville: string;
  telephone: string;
  email: string;
  statut: StatutEtablissement;
}

export interface AbonnementEcole {
  id: number;
  planNom: string;
  prixAnnuel: number;
  dateDebut: string;
  dateFin: string;
  statut: StatutAbonnement;
  totalPaye: number;
  resteAPayer: number;
}

export interface DashboardEcole {
  etablissement: EtablissementEcole;
  abonnement: AbonnementEcole | null;
  paiements: Paiement[];
}

/* ------------------------------------------------------------------ */
/* Référentiel école (Lot 1) — unités = tranches de 30 min (A-05).    */
/* ------------------------------------------------------------------ */

export type Jour =
  | 'LUNDI'
  | 'MARDI'
  | 'MERCREDI'
  | 'JEUDI'
  | 'VENDREDI'
  | 'SAMEDI'
  | 'DIMANCHE';

export type Semaine = 'A' | 'B' | 'TOUTES';

export interface PlageBloquee {
  type: string;
  indexDebut: number;
  dureeUnites: number;
}

export interface Grille {
  joursActifs: Jour[];
  heureDebut: string;
  dureeUniteMinutes: number;
  unitesParJour: number;
  plagesBloquees: PlageBloquee[];
  amplitudeMaxUnites: number;
}

export interface Niveau {
  id: number;
  libelle: string;
  cycle: string;
  ordre: number;
  chargeMaxUnitesJour: number;
}

export interface Groupe {
  id: number;
  libelle: string;
  effectif: number;
  niveauId: number;
  niveauLibelle: string;
  parentId: number | null;
  type: string;
  sousGroupes: Groupe[];
}

export interface Matiere {
  id: number;
  libelle: string;
  code: string;
  coefficient: number;
  poidsCognitif: number;
  couleur: string;
  typeSalleRequis: string | null;
  equipementsRequis: string[];
  dureeMinUnites: number;
  dureeMaxUnites: number;
  eviterAvantDejeuner: boolean;
  eviterFinJournee: boolean;
}

export interface Salle {
  id: number;
  nom: string;
  capacite: number;
  type: string;
  equipements: string[];
  batiment: string | null;
}

export interface Barrette {
  id: number;
  libelle: string;
  matiereId: number;
  matiereLibelle: string;
  groupeIds: number[];
}

export type TypeEnseignant = 'MIXTE' | 'PROPRE';

export interface Enseignant {
  id: number;
  nomComplet: string;
  email: string;
  type: TypeEnseignant;
  quotaHebdoUnites: number;
  maxConsecutifUnites: number;
  amplitudeMaxUnites: number;
  vacataire: boolean;
  bufferTrajetUnites: number;
  matieres: string[];
  chargeAffectee: number;
}

export interface Habilitation {
  matiereId: number;
  niveauIds: number[];
}

export type StatutIndisponibilite = 'BROUILLON' | 'VALIDE';

export interface Indisponibilite {
  id: number;
  source: string;
  jour: Jour;
  indexDebut: number;
  dureeUnites: number;
  semaine: Semaine;
  statut: StatutIndisponibilite;
  motif: string | null;
}

export type TypePreference = 'EVITER' | 'PREFERER';

export interface PreferenceHoraire {
  jour: Jour;
  indexDebut: number;
  dureeUnites: number;
  type: TypePreference;
}

export interface MaquetteLigne {
  id: number;
  niveauId: number;
  matiereId: number;
  matiereLibelle: string;
  volumeUnites: number;
  volumeUnitesB: number | null;
  maxParJourUnites: number | null;
  dedoublement: string;
  nbSousGroupes: number | null;
  coEnseignants: number | null;
  patterns: number[][];
}

export interface VolumeOverride {
  id: number;
  groupeId: number;
  matiereId: number;
  volumeUnites: number;
}

export interface Affectation {
  id: number;
  groupeId: number;
  groupeLibelle: string;
  matiereId: number;
  matiereLibelle: string;
  enseignantId: number;
  enseignantNom: string;
  volumeUnites: number | null;
}

export interface Ponderation {
  regle: string;
  libelle: string;
  famille: string;
  poids: number;
}
