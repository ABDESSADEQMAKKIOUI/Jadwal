'use client';

import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatMontant } from '@/lib/format';
import type { Plan } from '@/lib/types';
import { Button } from '@/components/ui/button';
import { Dialog } from '@/components/ui/dialog';
import { EmptyState } from '@/components/ui/empty-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ChargementPage } from '@/components/ui/spinner';
import { Table, TBody, Td, Th, THead, Tr } from '@/components/ui/table';

const FORMULAIRE_INITIAL = {
  code: '',
  nom: '',
  prixAnnuel: '',
  description: '',
};

export default function PagePlans() {
  const queryClient = useQueryClient();
  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [formulaire, setFormulaire] = useState(FORMULAIRE_INITIAL);

  const { data: plans, isLoading, error } = useQuery({
    queryKey: ['admin', 'plans'],
    queryFn: () => apiFetch<Plan[]>('/admin/plans'),
  });

  const creation = useMutation({
    mutationFn: () =>
      apiFetch<Plan>('/admin/plans', {
        method: 'POST',
        body: JSON.stringify({
          code: formulaire.code,
          nom: formulaire.nom,
          prixAnnuel: Number(formulaire.prixAnnuel),
          description: formulaire.description,
        }),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] });
      setDialogOuvert(false);
      setFormulaire(FORMULAIRE_INITIAL);
    },
  });

  function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    creation.mutate();
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-neutral-900">Plans</h1>
          <p className="mt-1 text-sm text-neutral-500">
            Offres d’abonnement annuel proposées aux établissements.
          </p>
        </div>
        <Button
          onClick={() => {
            creation.reset();
            setFormulaire(FORMULAIRE_INITIAL);
            setDialogOuvert(true);
          }}
        >
          Nouveau plan
        </Button>
      </div>

      {isLoading && <ChargementPage />}

      {error !== null && (
        <p className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </p>
      )}

      {plans !== undefined && (
        <div className="mt-6">
          {plans.length === 0 ? (
            <EmptyState message="Aucun plan pour le moment." />
          ) : (
            <Table>
              <THead>
                <Tr>
                  <Th>Code</Th>
                  <Th>Nom</Th>
                  <Th>Prix annuel</Th>
                  <Th>Description</Th>
                  <Th>Actif</Th>
                </Tr>
              </THead>
              <TBody>
                {plans.map((plan) => (
                  <Tr key={plan.id}>
                    <Td>{plan.code}</Td>
                    <Td className="font-medium text-neutral-900">{plan.nom}</Td>
                    <Td>{formatMontant(plan.prixAnnuel)}</Td>
                    <Td className="max-w-md">{plan.description}</Td>
                    <Td>{plan.actif ? 'Oui' : 'Non'}</Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
          )}
        </div>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre="Nouveau plan"
        onFermer={() => setDialogOuvert(false)}
      >
        <form onSubmit={soumettre} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="codePlan">Code</Label>
              <Input
                id="codePlan"
                value={formulaire.code}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, code: e.target.value })
                }
                placeholder="STANDARD"
                required
              />
            </div>
            <div>
              <Label htmlFor="nomPlan">Nom</Label>
              <Input
                id="nomPlan"
                value={formulaire.nom}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, nom: e.target.value })
                }
                placeholder="Plan Standard"
                required
              />
            </div>
          </div>
          <div>
            <Label htmlFor="prixAnnuel">Prix annuel (MAD)</Label>
            <Input
              id="prixAnnuel"
              type="number"
              min="0"
              step="0.01"
              value={formulaire.prixAnnuel}
              onChange={(e) =>
                setFormulaire({ ...formulaire, prixAnnuel: e.target.value })
              }
              placeholder="5000.00"
              required
            />
          </div>
          <div>
            <Label htmlFor="description">Description</Label>
            <Input
              id="description"
              value={formulaire.description}
              onChange={(e) =>
                setFormulaire({ ...formulaire, description: e.target.value })
              }
              placeholder="Jusqu’à 30 classes, support par e-mail…"
              required
            />
          </div>

          {creation.error !== null && (
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {creation.error.message}
            </p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variante="secondary" onClick={() => setDialogOuvert(false)}>
              Annuler
            </Button>
            <Button type="submit" disabled={creation.isPending}>
              {creation.isPending ? 'Création…' : 'Créer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
