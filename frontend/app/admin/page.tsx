'use client';

import { useQuery } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatMontant } from '@/lib/format';
import type { DashboardAdmin } from '@/lib/types';
import { ChargementPage } from '@/components/ui/spinner';

function CarteStatistique({
  libelle,
  valeur,
}: {
  libelle: string;
  valeur: string;
}) {
  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm">
      <p className="text-sm text-neutral-500">{libelle}</p>
      <p className="mt-2 text-2xl font-semibold text-neutral-900">{valeur}</p>
    </div>
  );
}

export default function PageTableauDeBordAdmin() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: () => apiFetch<DashboardAdmin>('/admin/dashboard'),
  });

  return (
    <div>
      <h1 className="text-xl font-semibold text-neutral-900">Tableau de bord</h1>
      <p className="mt-1 text-sm text-neutral-500">
        Vue d’ensemble de la plateforme JADWAL.
      </p>

      {isLoading && <ChargementPage />}

      {error !== null && (
        <p className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </p>
      )}

      {data !== undefined && (
        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <CarteStatistique
            libelle="Établissements"
            valeur={String(data.nbEtablissements)}
          />
          <CarteStatistique
            libelle="Abonnements actifs"
            valeur={String(data.nbAbonnementsActifs)}
          />
          <CarteStatistique
            libelle="Paiements en attente"
            valeur={String(data.nbPaiementsEnAttente)}
          />
          <CarteStatistique
            libelle="Encaissé cette année"
            valeur={formatMontant(data.totalEncaisseAnnee)}
          />
        </div>
      )}
    </div>
  );
}
