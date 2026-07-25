const libelles: Record<string, string> = {
  ACTIF: 'Actif',
  SUSPENDU: 'Suspendu',
  EXPIRE: 'Expiré',
  EN_ATTENTE_PAIEMENT: 'En attente de paiement',
  CONFIRME: 'Confirmé',
  EN_ATTENTE: 'En attente',
  REJETE: 'Rejeté',
};

const couleurs: Record<string, string> = {
  ACTIF: 'bg-green-100 text-green-800',
  CONFIRME: 'bg-green-100 text-green-800',
  EN_ATTENTE_PAIEMENT: 'bg-amber-100 text-amber-800',
  EN_ATTENTE: 'bg-amber-100 text-amber-800',
  SUSPENDU: 'bg-red-100 text-red-700',
  REJETE: 'bg-red-100 text-red-700',
  EXPIRE: 'bg-gray-100 text-gray-600',
};

export function Badge({ statut }: { statut: string }) {
  const couleur = couleurs[statut] ?? 'bg-gray-100 text-gray-600';
  const libelle = libelles[statut] ?? statut;
  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium ${couleur}`}
    >
      {libelle}
    </span>
  );
}
