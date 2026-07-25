'use client';

import { useParams } from 'next/navigation';
import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatDate, formatMontant, libelleMode } from '@/lib/format';
import type { EtablissementDetail, Plan } from '@/lib/types';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Dialog } from '@/components/ui/dialog';
import { EmptyState } from '@/components/ui/empty-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { ChargementPage } from '@/components/ui/spinner';
import { Table, TBody, Td, Th, THead, Tr } from '@/components/ui/table';

function dateDuJour(): string {
  return new Date().toISOString().slice(0, 10);
}

function dateDansUnAn(): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() + 1);
  return date.toISOString().slice(0, 10);
}

const FORM_COMPTE_INITIAL = { email: '', nomComplet: '', motDePasse: '' };

export default function PageDetailEtablissement() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const queryClient = useQueryClient();

  const [dialogCompte, setDialogCompte] = useState(false);
  const [formCompte, setFormCompte] = useState(FORM_COMPTE_INITIAL);

  const [dialogAbonnement, setDialogAbonnement] = useState(false);
  const [formAbonnement, setFormAbonnement] = useState({
    planId: '',
    dateDebut: dateDuJour(),
    dateFin: dateDansUnAn(),
  });

  const cleDetail = ['admin', 'etablissement', id];

  const { data: etablissement, isLoading, error } = useQuery({
    queryKey: cleDetail,
    queryFn: () => apiFetch<EtablissementDetail>(`/admin/etablissements/${id}`),
  });

  const { data: plans } = useQuery({
    queryKey: ['admin', 'plans'],
    queryFn: () => apiFetch<Plan[]>('/admin/plans'),
    enabled: dialogAbonnement,
  });

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: cleDetail });
    void queryClient.invalidateQueries({ queryKey: ['admin', 'etablissements'] });
  }

  const changementStatut = useMutation({
    mutationFn: (statut: 'ACTIF' | 'SUSPENDU') =>
      apiFetch<EtablissementDetail>(`/admin/etablissements/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ statut }),
      }),
    onSuccess: invalider,
  });

  const creationCompte = useMutation({
    mutationFn: (donnees: typeof FORM_COMPTE_INITIAL) =>
      apiFetch(`/admin/etablissements/${id}/comptes`, {
        method: 'POST',
        body: JSON.stringify(donnees),
      }),
    onSuccess: () => {
      invalider();
      setDialogCompte(false);
      setFormCompte(FORM_COMPTE_INITIAL);
    },
  });

  const creationAbonnement = useMutation({
    mutationFn: () =>
      apiFetch('/admin/abonnements', {
        method: 'POST',
        body: JSON.stringify({
          etablissementId: Number(id),
          planId: Number(formAbonnement.planId),
          dateDebut: formAbonnement.dateDebut,
          dateFin: formAbonnement.dateFin,
        }),
      }),
    onSuccess: () => {
      invalider();
      setDialogAbonnement(false);
      setFormAbonnement({
        planId: '',
        dateDebut: dateDuJour(),
        dateFin: dateDansUnAn(),
      });
    },
  });

  function soumettreCompte(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    creationCompte.mutate(formCompte);
  }

  function soumettreAbonnement(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    creationAbonnement.mutate();
  }

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

  if (etablissement === undefined) {
    return <EmptyState message="Établissement introuvable." />;
  }

  const estActif = etablissement.statut === 'ACTIF';

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-semibold text-gray-900">
              {etablissement.nom}
            </h1>
            <Badge statut={etablissement.statut} />
          </div>
          <p className="mt-1 text-sm text-gray-500">
            Code {etablissement.code} — {etablissement.ville}
          </p>
        </div>
        <Button
          variante={estActif ? 'danger' : 'primary'}
          onClick={() =>
            changementStatut.mutate(estActif ? 'SUSPENDU' : 'ACTIF')
          }
          disabled={changementStatut.isPending}
        >
          {changementStatut.isPending
            ? 'Mise à jour…'
            : estActif
              ? 'Suspendre'
              : 'Réactiver'}
        </Button>
      </div>

      {changementStatut.error !== null && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {changementStatut.error.message}
        </p>
      )}

      <Card titre="Informations">
        <dl className="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <dt className="text-gray-500">Ville</dt>
            <dd className="mt-1 font-medium text-gray-900">
              {etablissement.ville}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Téléphone</dt>
            <dd className="mt-1 font-medium text-gray-900">
              {etablissement.telephone}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">E-mail</dt>
            <dd className="mt-1 font-medium text-gray-900">
              {etablissement.email}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Code</dt>
            <dd className="mt-1 font-medium text-gray-900">
              {etablissement.code}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Créé le</dt>
            <dd className="mt-1 font-medium text-gray-900">
              {formatDate(etablissement.dateCreation)}
            </dd>
          </div>
        </dl>
      </Card>

      <Card
        titre="Comptes"
        actions={
          <Button
            taille="sm"
            variante="secondary"
            onClick={() => {
              creationCompte.reset();
              setFormCompte(FORM_COMPTE_INITIAL);
              setDialogCompte(true);
            }}
          >
            Créer un compte directeur
          </Button>
        }
      >
        {etablissement.comptes.length === 0 ? (
          <EmptyState message="Aucun compte pour cet établissement." />
        ) : (
          <Table>
            <THead>
              <Tr>
                <Th>Nom complet</Th>
                <Th>E-mail</Th>
                <Th>Rôle</Th>
                <Th>Actif</Th>
              </Tr>
            </THead>
            <TBody>
              {etablissement.comptes.map((compte) => (
                <Tr key={compte.id}>
                  <Td className="font-medium text-gray-900">
                    {compte.nomComplet}
                  </Td>
                  <Td>{compte.email}</Td>
                  <Td>
                    {compte.role === 'DIRECTEUR' ? 'Directeur' : 'Super admin'}
                  </Td>
                  <Td>{compte.actif ? 'Oui' : 'Non'}</Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        )}
      </Card>

      <Card
        titre="Abonnements"
        actions={
          <Button
            taille="sm"
            variante="secondary"
            onClick={() => {
              creationAbonnement.reset();
              setFormAbonnement({
                planId: '',
                dateDebut: dateDuJour(),
                dateFin: dateDansUnAn(),
              });
              setDialogAbonnement(true);
            }}
          >
            Nouvel abonnement
          </Button>
        }
      >
        {etablissement.abonnements.length === 0 ? (
          <EmptyState message="Aucun abonnement pour cet établissement." />
        ) : (
          <Table>
            <THead>
              <Tr>
                <Th>Plan</Th>
                <Th>Prix annuel</Th>
                <Th>Période</Th>
                <Th>Total payé</Th>
                <Th>Statut</Th>
              </Tr>
            </THead>
            <TBody>
              {etablissement.abonnements.map((abonnement) => (
                <Tr key={abonnement.id}>
                  <Td className="font-medium text-gray-900">
                    {abonnement.planNom}
                  </Td>
                  <Td>{formatMontant(abonnement.prixAnnuel)}</Td>
                  <Td>
                    {formatDate(abonnement.dateDebut)} →{' '}
                    {formatDate(abonnement.dateFin)}
                  </Td>
                  <Td>{formatMontant(abonnement.totalPaye)}</Td>
                  <Td>
                    <Badge statut={abonnement.statut} />
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        )}
      </Card>

      <Card titre="Paiements">
        {etablissement.paiements.length === 0 ? (
          <EmptyState message="Aucun paiement enregistré." />
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
              {etablissement.paiements.map((paiement) => (
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

      <Dialog
        ouvert={dialogCompte}
        titre="Créer un compte directeur"
        onFermer={() => setDialogCompte(false)}
      >
        <form onSubmit={soumettreCompte} className="space-y-4">
          <div>
            <Label htmlFor="nomComplet">Nom complet</Label>
            <Input
              id="nomComplet"
              value={formCompte.nomComplet}
              onChange={(e) =>
                setFormCompte({ ...formCompte, nomComplet: e.target.value })
              }
              placeholder="Ahmed Bennani"
              required
            />
          </div>
          <div>
            <Label htmlFor="emailCompte">Adresse e-mail</Label>
            <Input
              id="emailCompte"
              type="email"
              value={formCompte.email}
              onChange={(e) =>
                setFormCompte({ ...formCompte, email: e.target.value })
              }
              placeholder="directeur@ecole.ma"
              required
            />
          </div>
          <div>
            <Label htmlFor="motDePasseCompte">Mot de passe</Label>
            <Input
              id="motDePasseCompte"
              type="password"
              value={formCompte.motDePasse}
              onChange={(e) =>
                setFormCompte({ ...formCompte, motDePasse: e.target.value })
              }
              minLength={8}
              required
            />
          </div>

          {creationCompte.error !== null && (
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {creationCompte.error.message}
            </p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variante="secondary" onClick={() => setDialogCompte(false)}>
              Annuler
            </Button>
            <Button type="submit" disabled={creationCompte.isPending}>
              {creationCompte.isPending ? 'Création…' : 'Créer'}
            </Button>
          </div>
        </form>
      </Dialog>

      <Dialog
        ouvert={dialogAbonnement}
        titre="Nouvel abonnement"
        onFermer={() => setDialogAbonnement(false)}
      >
        <form onSubmit={soumettreAbonnement} className="space-y-4">
          <div>
            <Label htmlFor="plan">Plan</Label>
            <Select
              id="plan"
              value={formAbonnement.planId}
              onChange={(e) =>
                setFormAbonnement({ ...formAbonnement, planId: e.target.value })
              }
              required
            >
              <option value="">— Sélectionner un plan —</option>
              {(plans ?? []).map((plan) => (
                <option key={plan.id} value={String(plan.id)}>
                  {plan.nom} — {formatMontant(plan.prixAnnuel)} / an
                </option>
              ))}
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="dateDebut">Date de début</Label>
              <Input
                id="dateDebut"
                type="date"
                value={formAbonnement.dateDebut}
                onChange={(e) =>
                  setFormAbonnement({
                    ...formAbonnement,
                    dateDebut: e.target.value,
                  })
                }
                required
              />
            </div>
            <div>
              <Label htmlFor="dateFin">Date de fin</Label>
              <Input
                id="dateFin"
                type="date"
                value={formAbonnement.dateFin}
                onChange={(e) =>
                  setFormAbonnement({
                    ...formAbonnement,
                    dateFin: e.target.value,
                  })
                }
                required
              />
            </div>
          </div>

          {creationAbonnement.error !== null && (
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {creationAbonnement.error.message}
            </p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button
              variante="secondary"
              onClick={() => setDialogAbonnement(false)}
            >
              Annuler
            </Button>
            <Button type="submit" disabled={creationAbonnement.isPending}>
              {creationAbonnement.isPending ? 'Création…' : 'Créer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
