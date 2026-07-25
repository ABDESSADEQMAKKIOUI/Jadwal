'use client';

import { useQuery } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatDate, formatMontant, libelleMode } from '@/lib/format';
import type { DashboardEcole } from '@/lib/types';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { ChargementPage } from '@/components/ui/spinner';
import { Table, TBody, Td, Th, THead, Tr } from '@/components/ui/table';

export default function PageTableauDeBordEcole() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['ecole', 'dashboard'],
    queryFn: () => apiFetch<DashboardEcole>('/ecole/dashboard'),
  });

  if (isLoading) {
    return <ChargementPage />;
  }

  if (error !== null) {
    return (
      <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {error.message}
      </p>
    );
  }

  if (data === undefined) {
    return <EmptyState message="Aucune donnée disponible." />;
  }

  const { etablissement, abonnement, paiements } = data;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Tableau de bord</h1>
        <p className="mt-1 text-sm text-gray-500">
          Bienvenue sur l’espace de votre établissement.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card titre="Mon établissement">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-base font-semibold text-gray-900">
                {etablissement.nom}
              </p>
              <p className="mt-1 text-sm text-gray-500">
                Code {etablissement.code}
              </p>
            </div>
            <Badge statut={etablissement.statut} />
          </div>
          <dl className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-gray-500">Ville</dt>
              <dd className="mt-0.5 font-medium text-gray-900">
                {etablissement.ville}
              </dd>
            </div>
            <div>
              <dt className="text-gray-500">Téléphone</dt>
              <dd className="mt-0.5 font-medium text-gray-900">
                {etablissement.telephone}
              </dd>
            </div>
            <div className="sm:col-span-2">
              <dt className="text-gray-500">E-mail</dt>
              <dd className="mt-0.5 font-medium text-gray-900">
                {etablissement.email}
              </dd>
            </div>
          </dl>
        </Card>

        <Card titre="Mon abonnement">
          {abonnement === null ? (
            <div className="py-4 text-sm text-gray-600">
              <p className="font-medium text-gray-900">
                Aucun abonnement pour le moment.
              </p>
              <p className="mt-2">
                Veuillez contacter l’administration JADWAL pour souscrire un
                abonnement et activer votre espace.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-base font-semibold text-gray-900">
                    {abonnement.planNom}
                  </p>
                  <p className="mt-1 text-sm text-gray-500">
                    Du {formatDate(abonnement.dateDebut)} au{' '}
                    {formatDate(abonnement.dateFin)}
                  </p>
                </div>
                <Badge statut={abonnement.statut} />
              </div>
              <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-3">
                <div>
                  <dt className="text-gray-500">Prix annuel</dt>
                  <dd className="mt-0.5 font-medium text-gray-900">
                    {formatMontant(abonnement.prixAnnuel)}
                  </dd>
                </div>
                <div>
                  <dt className="text-gray-500">Total payé</dt>
                  <dd className="mt-0.5 font-medium text-green-700">
                    {formatMontant(abonnement.totalPaye)}
                  </dd>
                </div>
                <div>
                  <dt className="text-gray-500">Reste à payer</dt>
                  <dd className="mt-0.5 font-medium text-gray-900">
                    {formatMontant(abonnement.resteAPayer)}
                  </dd>
                </div>
              </dl>
              <div className="rounded-lg border border-indigo-100 bg-indigo-50 px-4 py-3 text-sm text-indigo-900">
                Paiement par virement bancaire, chèque ou espèces auprès de
                l’administration JADWAL. Aucun paiement en ligne.
              </div>
            </div>
          )}
        </Card>
      </div>

      <Card titre="Historique des paiements">
        {paiements.length === 0 ? (
          <EmptyState message="Aucun paiement enregistré pour le moment." />
        ) : (
          <Table>
            <THead>
              <Tr>
                <Th>Date</Th>
                <Th>Plan</Th>
                <Th>Montant</Th>
                <Th>Mode</Th>
                <Th>Référence</Th>
                <Th>Statut</Th>
              </Tr>
            </THead>
            <TBody>
              {paiements.map((paiement) => (
                <Tr key={paiement.id}>
                  <Td>{formatDate(paiement.datePaiement)}</Td>
                  <Td>{paiement.planNom}</Td>
                  <Td className="font-medium text-gray-900">
                    {formatMontant(paiement.montant)}
                  </Td>
                  <Td>{libelleMode(paiement.mode)}</Td>
                  <Td>{paiement.reference ?? '—'}</Td>
                  <Td>
                    <Badge statut={paiement.statut} />
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        )}
      </Card>
    </div>
  );
}
