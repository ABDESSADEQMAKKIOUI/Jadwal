import type { ModePaiement } from './types';

const formateurMontant = new Intl.NumberFormat('fr-MA', {
  style: 'currency',
  currency: 'MAD',
  minimumFractionDigits: 2,
});

export function formatMontant(montant: number): string {
  return formateurMontant.format(montant);
}

export function formatDate(date: string): string {
  const valeur = new Date(date);
  if (Number.isNaN(valeur.getTime())) return date;
  return valeur.toLocaleDateString('fr-FR');
}

const libellesMode: Record<ModePaiement, string> = {
  VIREMENT: 'Virement bancaire',
  ESPECES: 'Espèces',
  CHEQUE: 'Chèque',
};

export function libelleMode(mode: ModePaiement): string {
  return libellesMode[mode] ?? mode;
}

/**
 * Formate un volume exprimé en unités de 30 minutes (règle A-05).
 * Exemples : 11 → « 5h30 », 36 → « 18h », 1 → « 0h30 ».
 */
export function formatUnites(unites: number): string {
  if (!Number.isFinite(unites) || unites < 0) return '—';
  const heures = Math.floor(unites / 2);
  const demiHeure = unites % 2 !== 0;
  return demiHeure ? `${heures}h30` : `${heures}h`;
}
