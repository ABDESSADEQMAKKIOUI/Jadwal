'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatUnites } from '@/lib/format';
import type {
  Affectation,
  Enseignant,
  Groupe,
  MaquetteLigne,
  Matiere,
  Niveau,
  VolumeOverride,
} from '@/lib/types';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Dialog } from '@/components/ui/dialog';
import { EmptyState } from '@/components/ui/empty-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { ChargementPage } from '@/components/ui/spinner';
import { Table, TBody, Td, Th, THead, Tr } from '@/components/ui/table';

interface LigneFormulaire {
  matiereId: string;
  volumeUnites: string;
  volumeUnitesB: string;
  maxParJourUnites: string;
  dedoublement: string;
  nbSousGroupes: string;
  coEnseignants: string;
  patterns: string;
}

const LIGNE_VIDE: LigneFormulaire = {
  matiereId: '',
  volumeUnites: '8',
  volumeUnitesB: '',
  maxParJourUnites: '',
  dedoublement: 'AUCUN',
  nbSousGroupes: '',
  coEnseignants: '',
  patterns: '',
};

function parserPatterns(texte: string): number[][] {
  return texte
    .split('|')
    .map((bloc) =>
      bloc
        .split(',')
        .map((valeur) => Number(valeur.trim()))
        .filter((nombre) => Number.isFinite(nombre) && nombre > 0),
    )
    .filter((bloc) => bloc.length > 0);
}

function formaterPatterns(patterns: number[][]): string {
  return (patterns ?? []).map((bloc) => bloc.join(',')).join(' | ');
}

function nombreOuNull(valeur: string): number | null {
  const propre = valeur.trim();
  if (propre.length === 0) return null;
  const nombre = Number(propre);
  return Number.isFinite(nombre) ? nombre : null;
}

function aplatirGroupes(groupes: Groupe[]): { id: number; libelle: string }[] {
  const resultat: { id: number; libelle: string }[] = [];
  for (const groupe of groupes) {
    resultat.push({
      id: groupe.id,
      libelle: `${groupe.libelle} (${groupe.niveauLibelle})`,
    });
    for (const sousGroupe of groupe.sousGroupes ?? []) {
      const libelle = sousGroupe.libelle.includes(groupe.libelle)
        ? sousGroupe.libelle
        : `${groupe.libelle} ${sousGroupe.libelle}`;
      resultat.push({ id: sousGroupe.id, libelle });
    }
  }
  return resultat;
}

function MessageErreur({ message }: { message: string }) {
  return (
    <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {message}
    </p>
  );
}

export default function PageMaquettes() {
  const queryClient = useQueryClient();
  const [niveauId, setNiveauId] = useState('');

  const { data: niveaux, isLoading, error } = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });
  const { data: matieres } = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });
  const { data: maquette } = useQuery({
    queryKey: ['ecole', 'maquettes', niveauId],
    queryFn: () =>
      apiFetch<MaquetteLigne[]>(`/ecole/maquettes?niveauId=${niveauId}`),
    enabled: niveauId !== '',
  });

  const [lignes, setLignes] = useState<LigneFormulaire[]>([]);
  const [messageMaquette, setMessageMaquette] = useState<string | null>(null);

  useEffect(() => {
    if (maquette !== undefined) {
      setMessageMaquette(null);
      setLignes(
        maquette.map((ligne) => ({
          matiereId: String(ligne.matiereId),
          volumeUnites: String(ligne.volumeUnites),
          volumeUnitesB:
            ligne.volumeUnitesB === null ? '' : String(ligne.volumeUnitesB),
          maxParJourUnites:
            ligne.maxParJourUnites === null
              ? ''
              : String(ligne.maxParJourUnites),
          dedoublement: ligne.dedoublement,
          nbSousGroupes:
            ligne.nbSousGroupes === null ? '' : String(ligne.nbSousGroupes),
          coEnseignants:
            ligne.coEnseignants === null ? '' : String(ligne.coEnseignants),
          patterns: formaterPatterns(ligne.patterns),
        })),
      );
    }
  }, [maquette]);

  const sauvegardeMaquette = useMutation({
    mutationFn: () =>
      apiFetch<unknown>('/ecole/maquettes', {
        method: 'PUT',
        body: JSON.stringify({
          niveauId: Number(niveauId),
          lignes: lignes.map((ligne) => ({
            matiereId: Number(ligne.matiereId),
            volumeUnites: Number(ligne.volumeUnites),
            volumeUnitesB: nombreOuNull(ligne.volumeUnitesB),
            maxParJourUnites: nombreOuNull(ligne.maxParJourUnites),
            dedoublement: ligne.dedoublement,
            nbSousGroupes: nombreOuNull(ligne.nbSousGroupes),
            coEnseignants: nombreOuNull(ligne.coEnseignants),
            patterns: parserPatterns(ligne.patterns),
          })),
        }),
      }),
    onSuccess: () => {
      setMessageMaquette('Maquette enregistrée.');
      void queryClient.invalidateQueries({
        queryKey: ['ecole', 'maquettes', niveauId],
      });
    },
  });

  function modifierLigne(
    index: number,
    champ: keyof LigneFormulaire,
    valeur: string,
  ) {
    setMessageMaquette(null);
    setLignes((precedentes) =>
      precedentes.map((ligne, i) =>
        i === index ? { ...ligne, [champ]: valeur } : ligne,
      ),
    );
  }

  const totalUnites = lignes.reduce(
    (somme, ligne) => somme + (Number(ligne.volumeUnites) || 0),
    0,
  );

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-neutral-900">Maquettes horaires</h1>
        <p className="mt-1 text-sm text-neutral-500">
          Volumes hebdomadaires par niveau et par matière, affectations des
          enseignants et dérogations de volume par groupe. Unité = 30 minutes.
        </p>
      </div>

      <Card titre="Maquette par niveau">
        <div className="max-w-xs">
          <Label htmlFor="niveauMaquette">Niveau</Label>
          <Select
            id="niveauMaquette"
            value={niveauId}
            onChange={(e) => setNiveauId(e.target.value)}
          >
            <option value="">— Choisir un niveau —</option>
            {(niveaux ?? []).map((niveau) => (
              <option key={niveau.id} value={niveau.id}>
                {niveau.libelle}
              </option>
            ))}
          </Select>
        </div>

        {niveauId !== '' && (
          <div className="mt-5">
            {lignes.length === 0 ? (
              <EmptyState message="Aucune ligne. Ajoutez les matières de ce niveau." />
            ) : (
              <div className="overflow-x-auto rounded-lg border border-neutral-200">
                <table className="w-full min-w-max text-left text-sm">
                  <thead className="bg-neutral-50">
                    <tr>
                      <th className="px-3 py-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Matière
                      </th>
                      <th className="px-3 py-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Volume (unités)
                      </th>
                      <th className="px-3 py-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Vol. semaine B
                      </th>
                      <th className="px-3 py-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Max / jour
                      </th>
                      <th className="px-3 py-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Dédoublement
                      </th>
                      <th className="px-3 py-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Nb sous-grp
                      </th>
                      <th className="px-3 py-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Co-ens.
                      </th>
                      <th className="px-3 py-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Patterns (ex. « 4,4 | 4,2,2 »)
                      </th>
                      <th className="px-3 py-3" />
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 bg-white">
                    {lignes.map((ligne, index) => (
                      <tr key={index}>
                        <td className="px-3 py-2">
                          <Select
                            value={ligne.matiereId}
                            onChange={(e) =>
                              modifierLigne(index, 'matiereId', e.target.value)
                            }
                            className="min-w-40"
                            required
                          >
                            <option value="">— Matière —</option>
                            {(matieres ?? []).map((matiere) => (
                              <option key={matiere.id} value={matiere.id}>
                                {matiere.libelle}
                              </option>
                            ))}
                          </Select>
                        </td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            min="1"
                            value={ligne.volumeUnites}
                            onChange={(e) =>
                              modifierLigne(
                                index,
                                'volumeUnites',
                                e.target.value,
                              )
                            }
                            className="w-20"
                            required
                          />
                          <p className="mt-1 text-xs text-neutral-500">
                            {formatUnites(Number(ligne.volumeUnites) || 0)}
                          </p>
                        </td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            min="0"
                            value={ligne.volumeUnitesB}
                            onChange={(e) =>
                              modifierLigne(
                                index,
                                'volumeUnitesB',
                                e.target.value,
                              )
                            }
                            className="w-20"
                            placeholder="—"
                          />
                        </td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            min="1"
                            value={ligne.maxParJourUnites}
                            onChange={(e) =>
                              modifierLigne(
                                index,
                                'maxParJourUnites',
                                e.target.value,
                              )
                            }
                            className="w-20"
                            placeholder="—"
                          />
                        </td>
                        <td className="px-3 py-2">
                          <Select
                            value={ligne.dedoublement}
                            onChange={(e) =>
                              modifierLigne(
                                index,
                                'dedoublement',
                                e.target.value,
                              )
                            }
                            className="min-w-36"
                          >
                            <option value="AUCUN">Aucun</option>
                            <option value="SOUS_GROUPES">Sous-groupes</option>
                            <option value="CO_ENSEIGNEMENT">
                              Co-enseignement
                            </option>
                          </Select>
                        </td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            min="2"
                            value={ligne.nbSousGroupes}
                            onChange={(e) =>
                              modifierLigne(
                                index,
                                'nbSousGroupes',
                                e.target.value,
                              )
                            }
                            className="w-16"
                            placeholder="—"
                          />
                        </td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            min="2"
                            value={ligne.coEnseignants}
                            onChange={(e) =>
                              modifierLigne(
                                index,
                                'coEnseignants',
                                e.target.value,
                              )
                            }
                            className="w-16"
                            placeholder="—"
                          />
                        </td>
                        <td className="px-3 py-2">
                          <Input
                            value={ligne.patterns}
                            onChange={(e) =>
                              modifierLigne(index, 'patterns', e.target.value)
                            }
                            className="w-40"
                            placeholder="4,4 | 4,2,2"
                          />
                        </td>
                        <td className="px-3 py-2 text-right">
                          <Button
                            variante="ghost"
                            taille="sm"
                            className="text-red-600 hover:bg-red-50 hover:text-red-700"
                            onClick={() =>
                              setLignes((precedentes) =>
                                precedentes.filter((_ligne, i) => i !== index),
                              )
                            }
                          >
                            Retirer
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-4">
                <Button
                  variante="secondary"
                  onClick={() =>
                    setLignes((precedentes) => [...precedentes, { ...LIGNE_VIDE }])
                  }
                >
                  Ajouter une ligne
                </Button>
                <p className="text-sm text-neutral-600">
                  Total : <span className="font-semibold">{totalUnites}</span>{' '}
                  unités ({formatUnites(totalUnites)})
                </p>
              </div>
              <Button
                disabled={
                  sauvegardeMaquette.isPending ||
                  lignes.some((ligne) => ligne.matiereId === '')
                }
                onClick={() => sauvegardeMaquette.mutate()}
              >
                {sauvegardeMaquette.isPending
                  ? 'Enregistrement…'
                  : 'Enregistrer la maquette'}
              </Button>
            </div>

            {sauvegardeMaquette.error !== null && (
              <div className="mt-4">
                <MessageErreur message={sauvegardeMaquette.error.message} />
              </div>
            )}
            {messageMaquette !== null && (
              <p className="mt-4 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
                {messageMaquette}
              </p>
            )}
          </div>
        )}
      </Card>

      <SectionAffectations />
      <SectionOverrides />
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Affectations groupe × matière × enseignant                         */
/* ------------------------------------------------------------------ */

const FORM_AFFECTATION_INITIAL = {
  groupeId: '',
  matiereId: '',
  enseignantId: '',
  volumeUnites: '',
};

function SectionAffectations() {
  const queryClient = useQueryClient();
  const { data: affectations, isLoading, error } = useQuery({
    queryKey: ['ecole', 'affectations'],
    queryFn: () => apiFetch<Affectation[]>('/ecole/affectations'),
  });
  const { data: groupes } = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<Groupe[]>('/ecole/groupes'),
  });
  const { data: matieres } = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });
  const { data: enseignants } = useQuery({
    queryKey: ['ecole', 'enseignants'],
    queryFn: () => apiFetch<Enseignant[]>('/ecole/enseignants'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [formulaire, setFormulaire] = useState(FORM_AFFECTATION_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'affectations'] });
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'enseignants'] });
  }

  const creation = useMutation({
    mutationFn: () =>
      apiFetch<Affectation>('/ecole/affectations', {
        method: 'POST',
        body: JSON.stringify({
          groupeId: Number(formulaire.groupeId),
          matiereId: Number(formulaire.matiereId),
          enseignantId: Number(formulaire.enseignantId),
          volumeUnites: nombreOuNull(formulaire.volumeUnites),
        }),
      }),
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
    },
  });

  const suppression = useMutation({
    mutationFn: (id: number) =>
      apiFetch<unknown>(`/ecole/affectations/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  const groupesAplatis = aplatirGroupes(groupes ?? []);

  return (
    <Card
      titre="Affectations (qui enseigne quoi à qui)"
      actions={
        <Button
          taille="sm"
          onClick={() => {
            creation.reset();
            suppression.reset();
            setFormulaire(FORM_AFFECTATION_INITIAL);
            setDialogOuvert(true);
          }}
        >
          Nouvelle affectation
        </Button>
      }
    >
      {isLoading && <ChargementPage />}
      {error !== null && <MessageErreur message={error.message} />}
      {suppression.error !== null && (
        <div className="mb-4">
          <MessageErreur message={suppression.error.message} />
        </div>
      )}

      {affectations !== undefined &&
        (affectations.length === 0 ? (
          <EmptyState message="Aucune affectation pour le moment." />
        ) : (
          <Table>
            <THead>
              <Tr>
                <Th>Groupe</Th>
                <Th>Matière</Th>
                <Th>Enseignant</Th>
                <Th>Volume</Th>
                <Th className="text-right">Actions</Th>
              </Tr>
            </THead>
            <TBody>
              {affectations.map((affectation) => (
                <Tr key={affectation.id}>
                  <Td className="font-medium text-neutral-900">
                    {affectation.groupeLibelle}
                  </Td>
                  <Td>{affectation.matiereLibelle}</Td>
                  <Td>{affectation.enseignantNom}</Td>
                  <Td>
                    {affectation.volumeUnites === null ? (
                      <span className="text-neutral-500">Hérité de la maquette</span>
                    ) : (
                      formatUnites(affectation.volumeUnites)
                    )}
                  </Td>
                  <Td className="text-right">
                    <Button
                      variante="danger"
                      taille="sm"
                      disabled={suppression.isPending}
                      onClick={() => {
                        if (
                          window.confirm(
                            `Supprimer l’affectation « ${affectation.matiereLibelle} — ${affectation.groupeLibelle} » ?`,
                          )
                        ) {
                          suppression.mutate(affectation.id);
                        }
                      }}
                    >
                      Supprimer
                    </Button>
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        ))}

      <Dialog
        ouvert={dialogOuvert}
        titre="Nouvelle affectation"
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            creation.mutate();
          }}
          className="space-y-4"
        >
          <div>
            <Label htmlFor="groupeAffectation">Groupe</Label>
            <Select
              id="groupeAffectation"
              value={formulaire.groupeId}
              onChange={(e) =>
                setFormulaire({ ...formulaire, groupeId: e.target.value })
              }
              required
            >
              <option value="">— Choisir un groupe —</option>
              {groupesAplatis.map((groupe) => (
                <option key={groupe.id} value={groupe.id}>
                  {groupe.libelle}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="matiereAffectation">Matière</Label>
            <Select
              id="matiereAffectation"
              value={formulaire.matiereId}
              onChange={(e) =>
                setFormulaire({ ...formulaire, matiereId: e.target.value })
              }
              required
            >
              <option value="">— Choisir une matière —</option>
              {(matieres ?? []).map((matiere) => (
                <option key={matiere.id} value={matiere.id}>
                  {matiere.libelle}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="enseignantAffectation">Enseignant</Label>
            <Select
              id="enseignantAffectation"
              value={formulaire.enseignantId}
              onChange={(e) =>
                setFormulaire({ ...formulaire, enseignantId: e.target.value })
              }
              required
            >
              <option value="">— Choisir un enseignant —</option>
              {(enseignants ?? []).map((enseignant) => (
                <option key={enseignant.id} value={enseignant.id}>
                  {enseignant.nomComplet}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="volumeAffectation">
              Volume (unités, optionnel)
            </Label>
            <Input
              id="volumeAffectation"
              type="number"
              min="1"
              value={formulaire.volumeUnites}
              onChange={(e) =>
                setFormulaire({ ...formulaire, volumeUnites: e.target.value })
              }
              placeholder="Vide = volume de la maquette"
            />
            {formulaire.volumeUnites.trim().length > 0 && (
              <p className="mt-1 text-xs text-neutral-500">
                Soit {formatUnites(Number(formulaire.volumeUnites) || 0)}.
              </p>
            )}
          </div>

          {creation.error !== null && (
            <MessageErreur message={creation.error.message} />
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
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Overrides de volume par groupe                                     */
/* ------------------------------------------------------------------ */

const FORM_OVERRIDE_INITIAL = { groupeId: '', matiereId: '', volumeUnites: '' };

function SectionOverrides() {
  const queryClient = useQueryClient();
  const { data: overrides, isLoading, error } = useQuery({
    queryKey: ['ecole', 'volume-overrides'],
    queryFn: () => apiFetch<VolumeOverride[]>('/ecole/volume-overrides'),
  });
  const { data: groupes } = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<Groupe[]>('/ecole/groupes'),
  });
  const { data: matieres } = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [formulaire, setFormulaire] = useState(FORM_OVERRIDE_INITIAL);

  const groupesAplatis = aplatirGroupes(groupes ?? []);
  const libellesGroupes = new Map(
    groupesAplatis.map((groupe) => [groupe.id, groupe.libelle]),
  );
  const libellesMatieres = new Map(
    (matieres ?? []).map((matiere) => [matiere.id, matiere.libelle]),
  );

  function invalider() {
    void queryClient.invalidateQueries({
      queryKey: ['ecole', 'volume-overrides'],
    });
  }

  const creation = useMutation({
    mutationFn: () =>
      apiFetch<VolumeOverride>('/ecole/volume-overrides', {
        method: 'POST',
        body: JSON.stringify({
          groupeId: Number(formulaire.groupeId),
          matiereId: Number(formulaire.matiereId),
          volumeUnites: Number(formulaire.volumeUnites),
        }),
      }),
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
    },
  });

  const suppression = useMutation({
    mutationFn: (override: VolumeOverride) =>
      apiFetch<unknown>(
        `/ecole/volume-overrides?groupeId=${override.groupeId}&matiereId=${override.matiereId}`,
        { method: 'DELETE' },
      ),
    onSuccess: invalider,
  });

  return (
    <Card
      titre="Dérogations de volume par groupe"
      actions={
        <Button
          taille="sm"
          onClick={() => {
            creation.reset();
            suppression.reset();
            setFormulaire(FORM_OVERRIDE_INITIAL);
            setDialogOuvert(true);
          }}
        >
          Nouvelle dérogation
        </Button>
      }
    >
      <p className="mb-4 text-sm text-neutral-500">
        Remplace ponctuellement le volume de la maquette pour un groupe donné
        (ex. classe à option renforcée).
      </p>
      {isLoading && <ChargementPage />}
      {error !== null && <MessageErreur message={error.message} />}
      {suppression.error !== null && (
        <div className="mb-4">
          <MessageErreur message={suppression.error.message} />
        </div>
      )}

      {overrides !== undefined &&
        (overrides.length === 0 ? (
          <EmptyState message="Aucune dérogation de volume." />
        ) : (
          <Table>
            <THead>
              <Tr>
                <Th>Groupe</Th>
                <Th>Matière</Th>
                <Th>Volume</Th>
                <Th className="text-right">Actions</Th>
              </Tr>
            </THead>
            <TBody>
              {overrides.map((override) => (
                <Tr key={override.id}>
                  <Td className="font-medium text-neutral-900">
                    {libellesGroupes.get(override.groupeId) ??
                      `Groupe ${override.groupeId}`}
                  </Td>
                  <Td>
                    {libellesMatieres.get(override.matiereId) ??
                      `Matière ${override.matiereId}`}
                  </Td>
                  <Td>
                    {override.volumeUnites} unités (
                    {formatUnites(override.volumeUnites)})
                  </Td>
                  <Td className="text-right">
                    <Button
                      variante="danger"
                      taille="sm"
                      disabled={suppression.isPending}
                      onClick={() => {
                        if (window.confirm('Supprimer cette dérogation ?')) {
                          suppression.mutate(override);
                        }
                      }}
                    >
                      Supprimer
                    </Button>
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        ))}

      <Dialog
        ouvert={dialogOuvert}
        titre="Nouvelle dérogation de volume"
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            creation.mutate();
          }}
          className="space-y-4"
        >
          <div>
            <Label htmlFor="groupeOverride">Groupe</Label>
            <Select
              id="groupeOverride"
              value={formulaire.groupeId}
              onChange={(e) =>
                setFormulaire({ ...formulaire, groupeId: e.target.value })
              }
              required
            >
              <option value="">— Choisir un groupe —</option>
              {groupesAplatis.map((groupe) => (
                <option key={groupe.id} value={groupe.id}>
                  {groupe.libelle}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="matiereOverride">Matière</Label>
            <Select
              id="matiereOverride"
              value={formulaire.matiereId}
              onChange={(e) =>
                setFormulaire({ ...formulaire, matiereId: e.target.value })
              }
              required
            >
              <option value="">— Choisir une matière —</option>
              {(matieres ?? []).map((matiere) => (
                <option key={matiere.id} value={matiere.id}>
                  {matiere.libelle}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="volumeOverride">Volume (unités)</Label>
            <Input
              id="volumeOverride"
              type="number"
              min="1"
              value={formulaire.volumeUnites}
              onChange={(e) =>
                setFormulaire({ ...formulaire, volumeUnites: e.target.value })
              }
              required
            />
            <p className="mt-1 text-xs text-neutral-500">
              Soit {formatUnites(Number(formulaire.volumeUnites) || 0)}.
            </p>
          </div>

          {creation.error !== null && (
            <MessageErreur message={creation.error.message} />
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
    </Card>
  );
}
