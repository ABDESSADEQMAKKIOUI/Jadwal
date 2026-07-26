'use client';

import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatDate, formatMontant, libelleMode } from '@/lib/format';
import type { Abonnement, ModePaiement, Paiement } from '@/lib/types';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog } from '@/components/ui/dialog';
import { EmptyState } from '@/components/ui/empty-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { ChargementPage } from '@/components/ui/spinner';
import { Table, TBody, Td, Th, THead, Tr } from '@/components/ui/table';

const LIBELLES_STATUT_ABONNEMENT: Record<string, string> = {
  EN_ATTENTE_PAIEMENT: 'En attente de paiement',
  ACTIF: 'Actif',
  SUSPENDU: 'Suspendu',
  EXPIRE: 'Expiré',
};

function dateDuJour(): string {
  return new Date().toISOString().slice(0, 10);
}

function formulaireInitial() {
  return {
    abonnementId: '',
    montant: '',
    mode: 'VIREMENT' as ModePaiement,
    reference: '',
    datePaiement: dateDuJour(),
    note: '',
  };
}

export default function PagePaiements() {
  const queryClient = useQueryClient();
  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [formulaire, setFormulaire] = useState(formulaireInitial);

  const { data: paiements, isLoading, error } = useQuery({
    queryKey: ['admin', 'paiements'],
    queryFn: () => apiFetch<Paiement[]>('/admin/paiements'),
  });

  const { data: abonnements } = useQuery({
    queryKey: ['admin', 'abonnements'],
    queryFn: () => apiFetch<Abonnement[]>('/admin/abonnements'),
    enabled: dialogOuvert,
  });

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'paiements'] });
    void queryClient.invalidateQueries({ queryKey: ['admin', 'abonnements'] });
    void queryClient.invalidateQueries({ queryKey: ['admin', 'etablissements'] });
    void queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
  }

  const creation = useMutation({
    mutationFn: () =>
      apiFetch<Paiement>('/admin/paiements', {
        method: 'POST',
        body: JSON.stringify({
          abonnementId: Number(formulaire.abonnementId),
          montant: Number(formulaire.montant),
          mode: formulaire.mode,
          reference:
            formulaire.reference.trim().length > 0
              ? formulaire.reference.trim()
              : null,
          datePaiement: formulaire.datePaiement,
          note: formulaire.note.trim().length > 0 ? formulaire.note.trim() : null,
        }),
      }),
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
      setFormulaire(formulaireInitial());
    },
  });

  const changementStatut = useMutation({
    mutationFn: ({
      id,
      statut,
    }: {
      id: number;
      statut: 'CONFIRME' | 'REJETE';
    }) =>
      apiFetch<Paiement>(`/admin/paiements/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ statut }),
      }),
    onSuccess: invalider,
  });

  function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    creation.mutate();
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Le titre de page est rendu par la barre supérieure d'AppShell. */}
        <p className="text-sm text-neutral-500">
          Paiements manuels (virement bancaire, espèces, chèque) enregistrés par
          l’administration.
        </p>
        <Button
          onClick={() => {
            creation.reset();
            setFormulaire(formulaireInitial());
            setDialogOuvert(true);
          }}
        >
          Enregistrer un paiement
        </Button>
      </div>

      {isLoading && <ChargementPage />}

      {error !== null && (
        <p className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </p>
      )}

      {changementStatut.error !== null && (
        <p className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {changementStatut.error.message}
        </p>
      )}

      {paiements !== undefined && (
        <div className="mt-6">
          {paiements.length === 0 ? (
            <EmptyState message="Aucun paiement enregistré pour le moment." />
          ) : (
            <Table>
              <THead>
                <Tr>
                  <Th>Établissement</Th>
                  <Th>Plan</Th>
                  <Th>Montant</Th>
                  <Th>Mode</Th>
                  <Th>Référence</Th>
                  <Th>Date</Th>
                  <Th>Statut</Th>
                  <Th className="text-right">Actions</Th>
                </Tr>
              </THead>
              <TBody>
                {paiements.map((paiement) => (
                  <Tr key={paiement.id} className="hover:bg-neutral-50">
                    <Td className="font-medium text-neutral-900">
                      {paiement.etablissementNom}
                    </Td>
                    <Td>{paiement.planNom}</Td>
                    <Td>{formatMontant(paiement.montant)}</Td>
                    <Td>{libelleMode(paiement.mode)}</Td>
                    <Td>{paiement.reference ?? '—'}</Td>
                    <Td>{formatDate(paiement.datePaiement)}</Td>
                    <Td>
                      <Badge statut={paiement.statut} />
                    </Td>
                    <Td className="text-right">
                      <span className="inline-flex gap-2">
                        {paiement.statut !== 'CONFIRME' && (
                          <Button
                            taille="sm"
                            variante="secondary"
                            disabled={changementStatut.isPending}
                            onClick={() =>
                              changementStatut.mutate({
                                id: paiement.id,
                                statut: 'CONFIRME',
                              })
                            }
                          >
                            Confirmer
                          </Button>
                        )}
                        {paiement.statut !== 'REJETE' && (
                          <Button
                            taille="sm"
                            variante="danger"
                            disabled={changementStatut.isPending}
                            onClick={() =>
                              changementStatut.mutate({
                                id: paiement.id,
                                statut: 'REJETE',
                              })
                            }
                          >
                            Rejeter
                          </Button>
                        )}
                      </span>
                    </Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
          )}
        </div>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre="Enregistrer un paiement"
        onFermer={() => setDialogOuvert(false)}
      >
        <form onSubmit={soumettre} className="space-y-4">
          <div>
            <Label htmlFor="abonnement">Abonnement</Label>
            <Select
              id="abonnement"
              value={formulaire.abonnementId}
              onChange={(e) =>
                setFormulaire({ ...formulaire, abonnementId: e.target.value })
              }
              required
            >
              <option value="">— Sélectionner un abonnement —</option>
              {(abonnements ?? []).map((abonnement) => (
                <option key={abonnement.id} value={String(abonnement.id)}>
                  {abonnement.etablissementNom} — {abonnement.planNom} (
                  {LIBELLES_STATUT_ABONNEMENT[abonnement.statut] ??
                    abonnement.statut}
                  )
                </option>
              ))}
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="montant">Montant (MAD)</Label>
              <Input
                id="montant"
                type="number"
                min="0"
                step="0.01"
                value={formulaire.montant}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, montant: e.target.value })
                }
                placeholder="5000.00"
                required
              />
            </div>
            <div>
              <Label htmlFor="mode">Mode de paiement</Label>
              <Select
                id="mode"
                value={formulaire.mode}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    mode: e.target.value as ModePaiement,
                  })
                }
                required
              >
                <option value="VIREMENT">Virement bancaire</option>
                <option value="ESPECES">Espèces</option>
                <option value="CHEQUE">Chèque</option>
              </Select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="reference">Référence (optionnel)</Label>
              <Input
                id="reference"
                value={formulaire.reference}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, reference: e.target.value })
                }
                placeholder="VIR-2026-0001"
              />
            </div>
            <div>
              <Label htmlFor="datePaiement">Date du paiement</Label>
              <Input
                id="datePaiement"
                type="date"
                value={formulaire.datePaiement}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, datePaiement: e.target.value })
                }
                required
              />
            </div>
          </div>
          <div>
            <Label htmlFor="note">Note (optionnel)</Label>
            <Input
              id="note"
              value={formulaire.note}
              onChange={(e) =>
                setFormulaire({ ...formulaire, note: e.target.value })
              }
              placeholder="Reçu remis en main propre…"
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
              {creation.isPending ? 'Enregistrement…' : 'Enregistrer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
