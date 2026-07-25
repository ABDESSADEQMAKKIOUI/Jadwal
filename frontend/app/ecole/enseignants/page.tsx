'use client';

import Link from 'next/link';
import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatUnites } from '@/lib/format';
import type { Enseignant, TypeEnseignant } from '@/lib/types';
import { Button } from '@/components/ui/button';
import { Dialog } from '@/components/ui/dialog';
import { EmptyState } from '@/components/ui/empty-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { ChargementPage } from '@/components/ui/spinner';
import { Table, TBody, Td, Th, THead, Tr } from '@/components/ui/table';

const FORM_INITIAL = {
  nomComplet: '',
  email: '',
  type: 'MIXTE' as TypeEnseignant,
  quotaHebdoUnites: '36',
  maxConsecutifUnites: '8',
  amplitudeMaxUnites: '20',
  vacataire: false,
  bufferTrajetUnites: '0',
};

function BadgeTypeEnseignant({ type }: { type: TypeEnseignant }) {
  const classes =
    type === 'MIXTE'
      ? 'bg-indigo-100 text-indigo-800'
      : 'bg-purple-100 text-purple-800';
  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium ${classes}`}
    >
      {type === 'MIXTE' ? 'Mixte' : 'Propre'}
    </span>
  );
}

export default function PageEnseignants() {
  const queryClient = useQueryClient();
  const { data: enseignants, isLoading, error } = useQuery({
    queryKey: ['ecole', 'enseignants'],
    queryFn: () => apiFetch<Enseignant[]>('/ecole/enseignants'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Enseignant | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'enseignants'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () => {
      const corps = JSON.stringify({
        nomComplet: formulaire.nomComplet,
        email: formulaire.email,
        type: formulaire.type,
        quotaHebdoUnites: Number(formulaire.quotaHebdoUnites),
        maxConsecutifUnites: Number(formulaire.maxConsecutifUnites),
        amplitudeMaxUnites: Number(formulaire.amplitudeMaxUnites),
        vacataire: formulaire.vacataire,
        bufferTrajetUnites: Number(formulaire.bufferTrajetUnites),
      });
      return enEdition === null
        ? apiFetch<Enseignant>('/ecole/enseignants', {
            method: 'POST',
            body: corps,
          })
        : apiFetch<Enseignant>(`/ecole/enseignants/${enEdition.id}`, {
            method: 'PATCH',
            body: corps,
          });
    },
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
    },
  });

  const suppression = useMutation({
    mutationFn: (id: number) =>
      apiFetch<unknown>(`/ecole/enseignants/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(enseignant: Enseignant) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(enseignant);
    setFormulaire({
      nomComplet: enseignant.nomComplet,
      email: enseignant.email,
      type: enseignant.type,
      quotaHebdoUnites: String(enseignant.quotaHebdoUnites),
      maxConsecutifUnites: String(enseignant.maxConsecutifUnites),
      amplitudeMaxUnites: String(enseignant.amplitudeMaxUnites),
      vacataire: enseignant.vacataire,
      bufferTrajetUnites: String(enseignant.bufferTrajetUnites),
    });
    setDialogOuvert(true);
  }

  function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    sauvegarde.mutate();
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Enseignants</h1>
          <p className="mt-1 text-sm text-gray-500">
            Corps enseignant, quotas horaires, habilitations et
            indisponibilités.
          </p>
        </div>
        <Button onClick={ouvrirCreation}>Nouvel enseignant</Button>
      </div>

      {isLoading && <ChargementPage />}

      {error !== null && (
        <p className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </p>
      )}

      {suppression.error !== null && (
        <p className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {suppression.error.message}
        </p>
      )}

      {enseignants !== undefined && (
        <div className="mt-6">
          {enseignants.length === 0 ? (
            <EmptyState message="Aucun enseignant pour le moment." />
          ) : (
            <Table>
              <THead>
                <Tr>
                  <Th>Nom</Th>
                  <Th>Type</Th>
                  <Th>Quota hebdo</Th>
                  <Th>Vacataire</Th>
                  <Th>Matières habilitées</Th>
                  <Th>Charge affectée</Th>
                  <Th className="text-right">Actions</Th>
                </Tr>
              </THead>
              <TBody>
                {enseignants.map((enseignant) => (
                  <Tr key={enseignant.id}>
                    <Td>
                      <Link
                        href={`/ecole/enseignants/${enseignant.id}`}
                        className="font-medium text-indigo-700 hover:text-indigo-900 hover:underline"
                      >
                        {enseignant.nomComplet}
                      </Link>
                      <p className="text-xs text-gray-500">{enseignant.email}</p>
                    </Td>
                    <Td>
                      <BadgeTypeEnseignant type={enseignant.type} />
                    </Td>
                    <Td className="font-medium text-gray-900">
                      {formatUnites(enseignant.quotaHebdoUnites)}
                    </Td>
                    <Td>{enseignant.vacataire ? 'Oui' : '—'}</Td>
                    <Td>
                      {(enseignant.matieres ?? []).length === 0 ? (
                        <span className="text-gray-400">—</span>
                      ) : (
                        <span className="flex flex-wrap gap-1">
                          {enseignant.matieres.map((matiere) => (
                            <span
                              key={matiere}
                              className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600"
                            >
                              {matiere}
                            </span>
                          ))}
                        </span>
                      )}
                    </Td>
                    <Td>
                      {formatUnites(enseignant.chargeAffectee)}{' '}
                      <span className="text-xs text-gray-500">
                        / {formatUnites(enseignant.quotaHebdoUnites)}
                      </span>
                    </Td>
                    <Td className="text-right">
                      <div className="flex justify-end gap-2">
                        <Link
                          href={`/ecole/enseignants/${enseignant.id}`}
                          className="inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-2.5 py-1.5 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-50"
                        >
                          Détail
                        </Link>
                        <Button
                          variante="secondary"
                          taille="sm"
                          onClick={() => ouvrirEdition(enseignant)}
                        >
                          Modifier
                        </Button>
                        <Button
                          variante="danger"
                          taille="sm"
                          disabled={suppression.isPending}
                          onClick={() => {
                            if (
                              window.confirm(
                                `Supprimer l’enseignant « ${enseignant.nomComplet} » ?`,
                              )
                            ) {
                              suppression.mutate(enseignant.id);
                            }
                          }}
                        >
                          Supprimer
                        </Button>
                      </div>
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
        titre={
          enEdition === null ? 'Nouvel enseignant' : 'Modifier l’enseignant'
        }
        onFermer={() => setDialogOuvert(false)}
      >
        <form onSubmit={soumettre} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="nomEnseignant">Nom complet</Label>
              <Input
                id="nomEnseignant"
                value={formulaire.nomComplet}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, nomComplet: e.target.value })
                }
                placeholder="Fatima Zahra Bennis"
                required
              />
            </div>
            <div>
              <Label htmlFor="emailEnseignant">E-mail</Label>
              <Input
                id="emailEnseignant"
                type="email"
                value={formulaire.email}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, email: e.target.value })
                }
                placeholder="f.bennis@ecole.ma"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="typeEnseignant">Type</Label>
              <Select
                id="typeEnseignant"
                value={formulaire.type}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    type: e.target.value as TypeEnseignant,
                  })
                }
              >
                <option value="MIXTE">Mixte (plusieurs classes)</option>
                <option value="PROPRE">Propre (classe dédiée)</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="quotaEnseignant">Quota hebdo (unités)</Label>
              <Input
                id="quotaEnseignant"
                type="number"
                min="1"
                value={formulaire.quotaHebdoUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    quotaHebdoUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-gray-500">
                Soit {formatUnites(Number(formulaire.quotaHebdoUnites) || 0)}{' '}
                par semaine.
              </p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="maxConsecutif">Max consécutif (unités)</Label>
              <Input
                id="maxConsecutif"
                type="number"
                min="1"
                value={formulaire.maxConsecutifUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    maxConsecutifUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-gray-500">
                {formatUnites(Number(formulaire.maxConsecutifUnites) || 0)}{' '}
                d’affilée maximum.
              </p>
            </div>
            <div>
              <Label htmlFor="amplitudeMax">Amplitude max (unités)</Label>
              <Input
                id="amplitudeMax"
                type="number"
                min="1"
                value={formulaire.amplitudeMaxUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    amplitudeMaxUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-gray-500">
                {formatUnites(Number(formulaire.amplitudeMaxUnites) || 0)} par
                jour maximum.
              </p>
            </div>
          </div>
          <div className="grid grid-cols-2 items-end gap-4">
            <label className="flex items-center gap-2 pb-2 text-sm text-gray-700">
              <input
                type="checkbox"
                checked={formulaire.vacataire}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, vacataire: e.target.checked })
                }
                className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
              />
              Vacataire (intervient dans plusieurs établissements)
            </label>
            <div>
              <Label htmlFor="bufferTrajet">Buffer trajet (unités)</Label>
              <Input
                id="bufferTrajet"
                type="number"
                min="0"
                value={formulaire.bufferTrajetUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    bufferTrajetUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-gray-500">
                Temps de trajet réservé pour les vacataires.
              </p>
            </div>
          </div>

          {sauvegarde.error !== null && (
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {sauvegarde.error.message}
            </p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variante="secondary" onClick={() => setDialogOuvert(false)}>
              Annuler
            </Button>
            <Button type="submit" disabled={sauvegarde.isPending}>
              {sauvegarde.isPending ? 'Enregistrement…' : 'Enregistrer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
