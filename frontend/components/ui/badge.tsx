const libelles: Record<string, string> = {
  ACTIF: 'Actif',
  SUSPENDU: 'Suspendu',
  EXPIRE: 'Expiré',
  EN_ATTENTE_PAIEMENT: 'En attente de paiement',
  CONFIRME: 'Confirmé',
  EN_ATTENTE: 'En attente',
  REJETE: 'Rejeté',
};

/**
 * Pastilles de statut Ynexis : paires fond/texte sémantiques
 * (vert = abouti, ambre = en attente, rouge = rejeté, gris = inactif).
 */
const couleurs: Record<string, string> = {
  ACTIF: 'bg-ok-bg text-ok-fg',
  CONFIRME: 'bg-ok-bg text-ok-fg',
  EN_ATTENTE_PAIEMENT: 'bg-warn-bg text-warn-fg',
  EN_ATTENTE: 'bg-warn-bg text-warn-fg',
  SUSPENDU: 'bg-bad-bg text-bad-fg',
  REJETE: 'bg-bad-bg text-bad-fg',
  EXPIRE: 'bg-idle-bg text-idle-fg',
};

export function Badge({ statut }: { statut: string }) {
  const couleur = couleurs[statut] ?? 'bg-idle-bg text-idle-fg';
  const libelle = libelles[statut] ?? statut;
  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-[var(--radius-pill)] px-2.5 py-0.5 text-xs font-medium ${couleur}`}
    >
      {libelle}
    </span>
  );
}
