'use client';

import Link from 'next/link';
import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatDate } from '@/lib/format';
import type { Etablissement } from '@/lib/types';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog } from '@/components/ui/dialog';
import { EmptyState } from '@/components/ui/empty-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ChargementPage } from '@/components/ui/spinner';
import { Table, TBody, Td, Th, THead, Tr } from '@/components/ui/table';

const FORMULAIRE_INITIAL = {
  nom: '',
  code: '',
  ville: '',
  telephone: '',
  email: '',
};

export default function PageEtablissements() {
  const queryClient = useQueryClient();
  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [formulaire, setFormulaire] = useState(FORMULAIRE_INITIAL);

  const { data: etablissements, isLoading, error } = useQuery({
    queryKey: ['admin', 'etablissements'],
    queryFn: () => apiFetch<Etablissement[]>('/admin/etablissements'),
  });

  const creation = useMutation({
    mutationFn: (donnees: typeof FORMULAIRE_INITIAL) =>
      apiFetch<Etablissement>('/admin/etablissements', {
        method: 'POST',
        body: JSON.stringify(donnees),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'etablissements'] });
      setDialogOuvert(false);
      setFormulaire(FORMULAIRE_INITIAL);
    },
  });

  function ouvrirDialog() {
    creation.reset();
    setFormulaire(FORMULAIRE_INITIAL);
    setDialogOuvert(true);
  }

  function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    creation.mutate(formulaire);
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-neutral-900">Établissements</h1>
          <p className="mt-1 text-sm text-neutral-500">
            Écoles clientes de la plateforme JADWAL.
          </p>
        </div>
        <Button onClick={ouvrirDialog}>Nouvel établissement</Button>
      </div>

      {isLoading && <ChargementPage />}

      {error !== null && (
        <p className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </p>
      )}

      {etablissements !== undefined && (
        <div className="mt-6">
          {etablissements.length === 0 ? (
            <EmptyState message="Aucun établissement pour le moment." />
          ) : (
            <Table>
              <THead>
                <Tr>
                  <Th>Nom</Th>
                  <Th>Code</Th>
                  <Th>Ville</Th>
                  <Th>Statut</Th>
                  <Th>Abonnement actif</Th>
                  <Th>Créé le</Th>
                  <Th className="text-right">Actions</Th>
                </Tr>
              </THead>
              <TBody>
                {etablissements.map((etablissement) => (
                  <Tr key={etablissement.id} className="hover:bg-neutral-50">
                    <Td className="font-medium text-neutral-900">
                      {etablissement.nom}
                    </Td>
                    <Td>{etablissement.code}</Td>
                    <Td>{etablissement.ville}</Td>
                    <Td>
                      <Badge statut={etablissement.statut} />
                    </Td>
                    <Td>
                      {etablissement.abonnementActif !== null ? (
                        <span className="inline-flex items-center gap-2">
                          <span>{etablissement.abonnementActif.planNom}</span>
                          <Badge statut={etablissement.abonnementActif.statut} />
                        </span>
                      ) : (
                        <span className="text-neutral-400">Aucun</span>
                      )}
                    </Td>
                    <Td>{formatDate(etablissement.dateCreation)}</Td>
                    <Td className="text-right">
                      <Link
                        href={`/admin/etablissements/${etablissement.id}`}
                        className="text-sm font-medium text-teal-600 hover:text-teal-800"
                      >
                        Détail
                      </Link>
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
        titre="Nouvel établissement"
        onFermer={() => setDialogOuvert(false)}
      >
        <form onSubmit={soumettre} className="space-y-4">
          <div>
            <Label htmlFor="nom">Nom</Label>
            <Input
              id="nom"
              value={formulaire.nom}
              onChange={(e) =>
                setFormulaire({ ...formulaire, nom: e.target.value })
              }
              placeholder="Groupe Scolaire Al Amal"
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="code">Code</Label>
              <Input
                id="code"
                value={formulaire.code}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, code: e.target.value })
                }
                placeholder="AL-AMAL"
                required
              />
            </div>
            <div>
              <Label htmlFor="ville">Ville</Label>
              <Input
                id="ville"
                value={formulaire.ville}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, ville: e.target.value })
                }
                placeholder="Casablanca"
                required
              />
            </div>
          </div>
          <div>
            <Label htmlFor="telephone">Téléphone</Label>
            <Input
              id="telephone"
              value={formulaire.telephone}
              onChange={(e) =>
                setFormulaire({ ...formulaire, telephone: e.target.value })
              }
              placeholder="05 22 00 00 00"
              required
            />
          </div>
          <div>
            <Label htmlFor="emailEtablissement">E-mail</Label>
            <Input
              id="emailEtablissement"
              type="email"
              value={formulaire.email}
              onChange={(e) =>
                setFormulaire({ ...formulaire, email: e.target.value })
              }
              placeholder="contact@ecole.ma"
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
