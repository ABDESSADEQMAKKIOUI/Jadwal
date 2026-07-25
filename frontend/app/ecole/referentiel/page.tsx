'use client';

import { useEffect, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatUnites } from '@/lib/format';
import type {
  Barrette,
  Grille,
  Groupe,
  Jour,
  Matiere,
  Niveau,
  Salle,
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

const TOUS_LES_JOURS: Jour[] = [
  'LUNDI',
  'MARDI',
  'MERCREDI',
  'JEUDI',
  'VENDREDI',
  'SAMEDI',
  'DIMANCHE',
];

const LIBELLES_JOURS: Record<Jour, string> = {
  LUNDI: 'Lundi',
  MARDI: 'Mardi',
  MERCREDI: 'Mercredi',
  JEUDI: 'Jeudi',
  VENDREDI: 'Vendredi',
  SAMEDI: 'Samedi',
  DIMANCHE: 'Dimanche',
};

function csvVersListe(texte: string): string[] {
  return texte
    .split(',')
    .map((element) => element.trim())
    .filter((element) => element.length > 0);
}

function MessageErreur({ message }: { message: string }) {
  return (
    <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {message}
    </p>
  );
}

function Pastilles({ elements }: { elements: string[] }) {
  if (elements.length === 0) return <span className="text-neutral-400">—</span>;
  return (
    <span className="flex flex-wrap gap-1">
      {elements.map((element) => (
        <span
          key={element}
          className="inline-flex items-center rounded-full bg-neutral-100 px-2 py-0.5 text-xs font-medium text-neutral-600"
        >
          {element}
        </span>
      ))}
    </span>
  );
}

type Onglet =
  | 'grille'
  | 'niveaux'
  | 'groupes'
  | 'matieres'
  | 'salles'
  | 'barrettes';

const ONGLETS: { id: Onglet; libelle: string }[] = [
  { id: 'grille', libelle: 'Grille horaire' },
  { id: 'niveaux', libelle: 'Niveaux' },
  { id: 'groupes', libelle: 'Groupes' },
  { id: 'matieres', libelle: 'Matières' },
  { id: 'salles', libelle: 'Salles' },
  { id: 'barrettes', libelle: 'Barrettes' },
];

export default function PageReferentiel() {
  const [onglet, setOnglet] = useState<Onglet>('grille');

  return (
    <div>
      <h1 className="text-xl font-semibold text-neutral-900">Référentiel</h1>
      <p className="mt-1 text-sm text-neutral-500">
        Grille horaire, niveaux, groupes, matières, salles et barrettes de votre
        établissement. Toutes les durées sont exprimées en unités de 30 minutes.
      </p>

      <div className="mt-6 border-b border-neutral-200">
        <nav className="-mb-px flex flex-wrap gap-1">
          {ONGLETS.map((element) => (
            <button
              key={element.id}
              type="button"
              onClick={() => setOnglet(element.id)}
              className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
                onglet === element.id
                  ? 'border-teal-600 text-teal-700'
                  : 'border-transparent text-neutral-500 hover:border-neutral-300 hover:text-neutral-700'
              }`}
            >
              {element.libelle}
            </button>
          ))}
        </nav>
      </div>

      <div className="mt-6">
        {onglet === 'grille' && <OngletGrille />}
        {onglet === 'niveaux' && <OngletNiveaux />}
        {onglet === 'groupes' && <OngletGroupes />}
        {onglet === 'matieres' && <OngletMatieres />}
        {onglet === 'salles' && <OngletSalles />}
        {onglet === 'barrettes' && <OngletBarrettes />}
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Grille horaire                                                     */
/* ------------------------------------------------------------------ */

function copierGrille(grille: Grille): Grille {
  return {
    ...grille,
    joursActifs: [...grille.joursActifs],
    plagesBloquees: grille.plagesBloquees.map((plage) => ({ ...plage })),
  };
}

function OngletGrille() {
  const queryClient = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ['ecole', 'grille'],
    queryFn: () => apiFetch<Grille>('/ecole/grille'),
  });

  const [formulaire, setFormulaire] = useState<Grille | null>(null);
  const [dialogForcer, setDialogForcer] = useState(false);
  const [messageSucces, setMessageSucces] = useState<string | null>(null);

  useEffect(() => {
    if (data !== undefined) {
      setFormulaire(copierGrille(data));
    }
  }, [data]);

  const enregistrement = useMutation({
    mutationFn: (forcer: boolean) =>
      apiFetch<unknown>(`/ecole/grille?forcer=${forcer ? 'true' : 'false'}`, {
        method: 'PUT',
        body: JSON.stringify(formulaire),
      }),
    onSuccess: (_donnees, forcer) => {
      setDialogForcer(false);
      setMessageSucces(
        forcer
          ? 'Grille enregistrée. Les versions et séances existantes ont été supprimées et les créneaux régénérés.'
          : 'Grille horaire enregistrée.',
      );
      void queryClient.invalidateQueries({ queryKey: ['ecole'] });
    },
    onError: (_erreur, forcer) => {
      if (!forcer) {
        setDialogForcer(true);
      }
    },
  });

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;
  if (formulaire === null) {
    return <EmptyState message="Aucune grille horaire disponible." />;
  }

  function basculerJour(jour: Jour) {
    if (formulaire === null) return;
    const actifs = formulaire.joursActifs.includes(jour)
      ? formulaire.joursActifs.filter((element) => element !== jour)
      : TOUS_LES_JOURS.filter(
          (element) =>
            formulaire.joursActifs.includes(element) || element === jour,
        );
    setFormulaire({ ...formulaire, joursActifs: actifs });
  }

  function modifierPlage(
    index: number,
    champ: 'type' | 'indexDebut' | 'dureeUnites',
    valeur: string,
  ) {
    if (formulaire === null) return;
    const plages = formulaire.plagesBloquees.map((plage, i) => {
      if (i !== index) return plage;
      if (champ === 'type') return { ...plage, type: valeur };
      return { ...plage, [champ]: Number(valeur) };
    });
    setFormulaire({ ...formulaire, plagesBloquees: plages });
  }

  function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setMessageSucces(null);
    enregistrement.mutate(false);
  }

  return (
    <div className="space-y-6">
      <form onSubmit={soumettre} className="space-y-6">
        <Card titre="Structure de la semaine">
          <div className="space-y-5">
            <div>
              <Label>Jours actifs</Label>
              <div className="mt-2 flex flex-wrap gap-4">
                {TOUS_LES_JOURS.map((jour) => (
                  <label
                    key={jour}
                    className="flex items-center gap-2 text-sm text-neutral-700"
                  >
                    <input
                      type="checkbox"
                      checked={formulaire.joursActifs.includes(jour)}
                      onChange={() => basculerJour(jour)}
                      className="h-4 w-4 rounded border-neutral-300 text-teal-600 focus:ring-teal-500"
                    />
                    {LIBELLES_JOURS[jour]}
                  </label>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <Label htmlFor="heureDebut">Heure de début</Label>
                <Input
                  id="heureDebut"
                  type="time"
                  value={formulaire.heureDebut}
                  onChange={(e) =>
                    setFormulaire({ ...formulaire, heureDebut: e.target.value })
                  }
                  required
                />
              </div>
              <div>
                <Label htmlFor="dureeUniteMinutes">Durée d’une unité (min)</Label>
                <Input
                  id="dureeUniteMinutes"
                  type="number"
                  min="5"
                  value={formulaire.dureeUniteMinutes}
                  onChange={(e) =>
                    setFormulaire({
                      ...formulaire,
                      dureeUniteMinutes: Number(e.target.value),
                    })
                  }
                  required
                />
              </div>
              <div>
                <Label htmlFor="unitesParJour">Unités par jour</Label>
                <Input
                  id="unitesParJour"
                  type="number"
                  min="1"
                  value={formulaire.unitesParJour}
                  onChange={(e) =>
                    setFormulaire({
                      ...formulaire,
                      unitesParJour: Number(e.target.value),
                    })
                  }
                  required
                />
                <p className="mt-1 text-xs text-neutral-500">
                  Soit {formatUnites(formulaire.unitesParJour)} par jour.
                </p>
              </div>
              <div>
                <Label htmlFor="amplitudeMaxUnites">Amplitude max (unités)</Label>
                <Input
                  id="amplitudeMaxUnites"
                  type="number"
                  min="1"
                  value={formulaire.amplitudeMaxUnites}
                  onChange={(e) =>
                    setFormulaire({
                      ...formulaire,
                      amplitudeMaxUnites: Number(e.target.value),
                    })
                  }
                  required
                />
                <p className="mt-1 text-xs text-neutral-500">
                  Soit {formatUnites(formulaire.amplitudeMaxUnites)} maximum.
                </p>
              </div>
            </div>
          </div>
        </Card>

        <Card
          titre="Plages bloquées"
          actions={
            <Button
              variante="secondary"
              taille="sm"
              onClick={() =>
                setFormulaire({
                  ...formulaire,
                  plagesBloquees: [
                    ...formulaire.plagesBloquees,
                    { type: 'DEJEUNER', indexDebut: 8, dureeUnites: 2 },
                  ],
                })
              }
            >
              Ajouter une plage
            </Button>
          }
        >
          {formulaire.plagesBloquees.length === 0 ? (
            <EmptyState message="Aucune plage bloquée (déjeuner, pause…)." />
          ) : (
            <div className="space-y-3">
              {formulaire.plagesBloquees.map((plage, index) => (
                <div
                  key={index}
                  className="flex flex-wrap items-end gap-3 rounded-lg border border-neutral-100 bg-neutral-50 p-3"
                >
                  <div className="w-40">
                    <Label htmlFor={`plageType${index}`}>Type</Label>
                    <Input
                      id={`plageType${index}`}
                      value={plage.type}
                      onChange={(e) =>
                        modifierPlage(index, 'type', e.target.value)
                      }
                      placeholder="DEJEUNER"
                      required
                    />
                  </div>
                  <div className="w-32">
                    <Label htmlFor={`plageDebut${index}`}>Index de début</Label>
                    <Input
                      id={`plageDebut${index}`}
                      type="number"
                      min="0"
                      value={plage.indexDebut}
                      onChange={(e) =>
                        modifierPlage(index, 'indexDebut', e.target.value)
                      }
                      required
                    />
                  </div>
                  <div className="w-32">
                    <Label htmlFor={`plageDuree${index}`}>Durée (unités)</Label>
                    <Input
                      id={`plageDuree${index}`}
                      type="number"
                      min="1"
                      value={plage.dureeUnites}
                      onChange={(e) =>
                        modifierPlage(index, 'dureeUnites', e.target.value)
                      }
                      required
                    />
                  </div>
                  <p className="pb-2 text-xs text-neutral-500">
                    {formatUnites(plage.dureeUnites)}
                  </p>
                  <Button
                    variante="ghost"
                    taille="sm"
                    className="ml-auto text-red-600 hover:bg-red-50 hover:text-red-700"
                    onClick={() =>
                      setFormulaire({
                        ...formulaire,
                        plagesBloquees: formulaire.plagesBloquees.filter(
                          (_plage, i) => i !== index,
                        ),
                      })
                    }
                  >
                    Retirer
                  </Button>
                </div>
              ))}
            </div>
          )}
        </Card>

        {messageSucces !== null && (
          <p className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
            {messageSucces}
          </p>
        )}

        <div className="flex justify-end">
          <Button type="submit" disabled={enregistrement.isPending}>
            {enregistrement.isPending
              ? 'Enregistrement…'
              : 'Enregistrer la grille'}
          </Button>
        </div>
      </form>

      <Dialog
        ouvert={dialogForcer}
        titre="Enregistrement impossible"
        onFermer={() => setDialogForcer(false)}
      >
        <div className="space-y-4">
          {enregistrement.error !== null && (
            <MessageErreur message={enregistrement.error.message} />
          )}
          <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            <p className="font-semibold">Action destructive</p>
            <p className="mt-1">
              Forcer l’enregistrement supprimera définitivement toutes les
              versions de planning et toutes les séances existantes, puis
              régénérera les créneaux à partir de la nouvelle grille.
            </p>
          </div>
          <div className="flex justify-end gap-3">
            <Button variante="secondary" onClick={() => setDialogForcer(false)}>
              Annuler
            </Button>
            <Button
              variante="danger"
              disabled={enregistrement.isPending}
              onClick={() => enregistrement.mutate(true)}
            >
              {enregistrement.isPending
                ? 'Enregistrement…'
                : 'Forcer l’enregistrement'}
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Niveaux                                                            */
/* ------------------------------------------------------------------ */

const FORM_NIVEAU_INITIAL = {
  libelle: '',
  cycle: '',
  ordre: '1',
  chargeMaxUnitesJour: '14',
};

function OngletNiveaux() {
  const queryClient = useQueryClient();
  const { data: niveaux, isLoading, error } = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Niveau | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_NIVEAU_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'niveaux'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () => {
      const corps = JSON.stringify({
        libelle: formulaire.libelle,
        cycle: formulaire.cycle,
        ordre: Number(formulaire.ordre),
        chargeMaxUnitesJour: Number(formulaire.chargeMaxUnitesJour),
      });
      return enEdition === null
        ? apiFetch<Niveau>('/ecole/niveaux', { method: 'POST', body: corps })
        : apiFetch<Niveau>(`/ecole/niveaux/${enEdition.id}`, {
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
      apiFetch<unknown>(`/ecole/niveaux/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_NIVEAU_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(niveau: Niveau) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(niveau);
    setFormulaire({
      libelle: niveau.libelle,
      cycle: niveau.cycle,
      ordre: String(niveau.ordre),
      chargeMaxUnitesJour: String(niveau.chargeMaxUnitesJour),
    });
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = [...(niveaux ?? [])].sort((a, b) => a.ordre - b.ordre);

  return (
    <Card
      titre="Niveaux"
      actions={<Button taille="sm" onClick={ouvrirCreation}>Nouveau niveau</Button>}
    >
      {suppression.error !== null && (
        <div className="mb-4">
          <MessageErreur message={suppression.error.message} />
        </div>
      )}
      {liste.length === 0 ? (
        <EmptyState message="Aucun niveau. Créez d’abord vos niveaux (ex. 1AC, 2AC…)." />
      ) : (
        <Table>
          <THead>
            <Tr>
              <Th>Ordre</Th>
              <Th>Libellé</Th>
              <Th>Cycle</Th>
              <Th>Charge max / jour</Th>
              <Th className="text-right">Actions</Th>
            </Tr>
          </THead>
          <TBody>
            {liste.map((niveau) => (
              <Tr key={niveau.id}>
                <Td>{niveau.ordre}</Td>
                <Td className="font-medium text-neutral-900">{niveau.libelle}</Td>
                <Td>{niveau.cycle}</Td>
                <Td>
                  {niveau.chargeMaxUnitesJour} unités (
                  {formatUnites(niveau.chargeMaxUnitesJour)})
                </Td>
                <Td className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variante="secondary"
                      taille="sm"
                      onClick={() => ouvrirEdition(niveau)}
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
                            `Supprimer le niveau « ${niveau.libelle} » ?`,
                          )
                        ) {
                          suppression.mutate(niveau.id);
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

      <Dialog
        ouvert={dialogOuvert}
        titre={enEdition === null ? 'Nouveau niveau' : 'Modifier le niveau'}
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sauvegarde.mutate();
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="libelleNiveau">Libellé</Label>
              <Input
                id="libelleNiveau"
                value={formulaire.libelle}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, libelle: e.target.value })
                }
                placeholder="1AC"
                required
              />
            </div>
            <div>
              <Label htmlFor="cycleNiveau">Cycle</Label>
              <Input
                id="cycleNiveau"
                value={formulaire.cycle}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, cycle: e.target.value })
                }
                placeholder="COLLEGE"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="ordreNiveau">Ordre</Label>
              <Input
                id="ordreNiveau"
                type="number"
                min="1"
                value={formulaire.ordre}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, ordre: e.target.value })
                }
                required
              />
            </div>
            <div>
              <Label htmlFor="chargeNiveau">Charge max / jour (unités)</Label>
              <Input
                id="chargeNiveau"
                type="number"
                min="1"
                value={formulaire.chargeMaxUnitesJour}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    chargeMaxUnitesJour: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-neutral-500">
                Soit {formatUnites(Number(formulaire.chargeMaxUnitesJour) || 0)}{' '}
                par jour.
              </p>
            </div>
          </div>

          {sauvegarde.error !== null && (
            <MessageErreur message={sauvegarde.error.message} />
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
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Groupes                                                            */
/* ------------------------------------------------------------------ */

const FORM_GROUPE_INITIAL = { niveauId: '', libelle: '', effectif: '30' };

function OngletGroupes() {
  const queryClient = useQueryClient();
  const { data: groupes, isLoading, error } = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<Groupe[]>('/ecole/groupes'),
  });
  const { data: niveaux } = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Groupe | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_GROUPE_INITIAL);
  const [dialogDedoubler, setDialogDedoubler] = useState<Groupe | null>(null);
  const [nombreSousGroupes, setNombreSousGroupes] = useState('2');

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'groupes'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () =>
      enEdition === null
        ? apiFetch<Groupe>('/ecole/groupes', {
            method: 'POST',
            body: JSON.stringify({
              niveauId: Number(formulaire.niveauId),
              libelle: formulaire.libelle,
              effectif: Number(formulaire.effectif),
            }),
          })
        : apiFetch<Groupe>(`/ecole/groupes/${enEdition.id}`, {
            method: 'PATCH',
            body: JSON.stringify({
              libelle: formulaire.libelle,
              effectif: Number(formulaire.effectif),
            }),
          }),
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
    },
  });

  const suppression = useMutation({
    mutationFn: (id: number) =>
      apiFetch<unknown>(`/ecole/groupes/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  const dedoublement = useMutation({
    mutationFn: (groupeId: number) =>
      apiFetch<unknown>(`/ecole/groupes/${groupeId}/sous-groupes`, {
        method: 'POST',
        body: JSON.stringify({ nombre: Number(nombreSousGroupes) }),
      }),
    onSuccess: () => {
      invalider();
      setDialogDedoubler(null);
    },
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_GROUPE_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(groupe: Groupe) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(groupe);
    setFormulaire({
      niveauId: String(groupe.niveauId),
      libelle: groupe.libelle,
      effectif: String(groupe.effectif),
    });
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = groupes ?? [];

  function ligneGroupe(groupe: Groupe, sousGroupe: boolean) {
    return (
      <Tr key={groupe.id}>
        <Td className={sousGroupe ? 'pl-10' : 'font-medium text-neutral-900'}>
          {sousGroupe ? `↳ ${groupe.libelle}` : groupe.libelle}
        </Td>
        <Td>{groupe.niveauLibelle}</Td>
        <Td>{groupe.effectif}</Td>
        <Td>{groupe.type}</Td>
        <Td className="text-right">
          <div className="flex justify-end gap-2">
            <Button
              variante="secondary"
              taille="sm"
              onClick={() => ouvrirEdition(groupe)}
            >
              Modifier
            </Button>
            {!sousGroupe && (
              <Button
                variante="secondary"
                taille="sm"
                onClick={() => {
                  dedoublement.reset();
                  setNombreSousGroupes('2');
                  setDialogDedoubler(groupe);
                }}
              >
                Dédoubler
              </Button>
            )}
            <Button
              variante="danger"
              taille="sm"
              disabled={suppression.isPending}
              onClick={() => {
                if (
                  window.confirm(
                    `Supprimer le groupe « ${groupe.libelle} » ?`,
                  )
                ) {
                  suppression.mutate(groupe.id);
                }
              }}
            >
              Supprimer
            </Button>
          </div>
        </Td>
      </Tr>
    );
  }

  return (
    <Card
      titre="Groupes (classes et sous-groupes)"
      actions={<Button taille="sm" onClick={ouvrirCreation}>Nouveau groupe</Button>}
    >
      {suppression.error !== null && (
        <div className="mb-4">
          <MessageErreur message={suppression.error.message} />
        </div>
      )}
      {liste.length === 0 ? (
        <EmptyState message="Aucun groupe. Créez vos classes (ex. 1AC-A)." />
      ) : (
        <Table>
          <THead>
            <Tr>
              <Th>Libellé</Th>
              <Th>Niveau</Th>
              <Th>Effectif</Th>
              <Th>Type</Th>
              <Th className="text-right">Actions</Th>
            </Tr>
          </THead>
          <TBody>
            {liste.map((groupe) => [
              ligneGroupe(groupe, false),
              ...(groupe.sousGroupes ?? []).map((sousGroupe) =>
                ligneGroupe(sousGroupe, true),
              ),
            ])}
          </TBody>
        </Table>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre={enEdition === null ? 'Nouveau groupe' : 'Modifier le groupe'}
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sauvegarde.mutate();
          }}
          className="space-y-4"
        >
          {enEdition === null && (
            <div>
              <Label htmlFor="niveauGroupe">Niveau</Label>
              <Select
                id="niveauGroupe"
                value={formulaire.niveauId}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, niveauId: e.target.value })
                }
                required
              >
                <option value="">— Choisir un niveau —</option>
                {(niveaux ?? []).map((niveau) => (
                  <option key={niveau.id} value={niveau.id}>
                    {niveau.libelle}
                  </option>
                ))}
              </Select>
            </div>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="libelleGroupe">Libellé</Label>
              <Input
                id="libelleGroupe"
                value={formulaire.libelle}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, libelle: e.target.value })
                }
                placeholder="1AC-A"
                required
              />
            </div>
            <div>
              <Label htmlFor="effectifGroupe">Effectif</Label>
              <Input
                id="effectifGroupe"
                type="number"
                min="1"
                value={formulaire.effectif}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, effectif: e.target.value })
                }
                required
              />
            </div>
          </div>

          {sauvegarde.error !== null && (
            <MessageErreur message={sauvegarde.error.message} />
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

      <Dialog
        ouvert={dialogDedoubler !== null}
        titre={`Dédoubler « ${dialogDedoubler?.libelle ?? ''} »`}
        onFermer={() => setDialogDedoubler(null)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (dialogDedoubler !== null) {
              dedoublement.mutate(dialogDedoubler.id);
            }
          }}
          className="space-y-4"
        >
          <p className="text-sm text-neutral-600">
            Des sous-groupes (G1), (G2)… seront créés et l’effectif sera réparti
            équitablement.
          </p>
          <div>
            <Label htmlFor="nombreSousGroupes">Nombre de sous-groupes</Label>
            <Input
              id="nombreSousGroupes"
              type="number"
              min="2"
              max="6"
              value={nombreSousGroupes}
              onChange={(e) => setNombreSousGroupes(e.target.value)}
              required
            />
          </div>

          {dedoublement.error !== null && (
            <MessageErreur message={dedoublement.error.message} />
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variante="secondary" onClick={() => setDialogDedoubler(null)}>
              Annuler
            </Button>
            <Button type="submit" disabled={dedoublement.isPending}>
              {dedoublement.isPending ? 'Création…' : 'Dédoubler'}
            </Button>
          </div>
        </form>
      </Dialog>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Matières                                                           */
/* ------------------------------------------------------------------ */

const FORM_MATIERE_INITIAL = {
  libelle: '',
  code: '',
  coefficient: '1',
  poidsCognitif: '3',
  couleur: '#6366f1',
  typeSalleRequis: '',
  equipementsRequis: '',
  dureeMinUnites: '1',
  dureeMaxUnites: '4',
  eviterAvantDejeuner: false,
  eviterFinJournee: false,
};

function OngletMatieres() {
  const queryClient = useQueryClient();
  const { data: matieres, isLoading, error } = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Matiere | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_MATIERE_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'matieres'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () => {
      const corps = JSON.stringify({
        libelle: formulaire.libelle,
        code: formulaire.code,
        coefficient: Number(formulaire.coefficient),
        poidsCognitif: Number(formulaire.poidsCognitif),
        couleur: formulaire.couleur,
        typeSalleRequis:
          formulaire.typeSalleRequis.trim().length > 0
            ? formulaire.typeSalleRequis.trim()
            : null,
        equipementsRequis: csvVersListe(formulaire.equipementsRequis),
        dureeMinUnites: Number(formulaire.dureeMinUnites),
        dureeMaxUnites: Number(formulaire.dureeMaxUnites),
        eviterAvantDejeuner: formulaire.eviterAvantDejeuner,
        eviterFinJournee: formulaire.eviterFinJournee,
      });
      return enEdition === null
        ? apiFetch<Matiere>('/ecole/matieres', { method: 'POST', body: corps })
        : apiFetch<Matiere>(`/ecole/matieres/${enEdition.id}`, {
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
      apiFetch<unknown>(`/ecole/matieres/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_MATIERE_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(matiere: Matiere) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(matiere);
    setFormulaire({
      libelle: matiere.libelle,
      code: matiere.code,
      coefficient: String(matiere.coefficient),
      poidsCognitif: String(matiere.poidsCognitif),
      couleur: matiere.couleur,
      typeSalleRequis: matiere.typeSalleRequis ?? '',
      equipementsRequis: (matiere.equipementsRequis ?? []).join(', '),
      dureeMinUnites: String(matiere.dureeMinUnites),
      dureeMaxUnites: String(matiere.dureeMaxUnites),
      eviterAvantDejeuner: matiere.eviterAvantDejeuner,
      eviterFinJournee: matiere.eviterFinJournee,
    });
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = matieres ?? [];

  return (
    <Card
      titre="Matières"
      actions={
        <Button taille="sm" onClick={ouvrirCreation}>Nouvelle matière</Button>
      }
    >
      {suppression.error !== null && (
        <div className="mb-4">
          <MessageErreur message={suppression.error.message} />
        </div>
      )}
      {liste.length === 0 ? (
        <EmptyState message="Aucune matière pour le moment." />
      ) : (
        <Table>
          <THead>
            <Tr>
              <Th>Matière</Th>
              <Th>Code</Th>
              <Th>Coef.</Th>
              <Th>Poids cognitif</Th>
              <Th>Salle requise</Th>
              <Th>Équipements</Th>
              <Th>Durées</Th>
              <Th>Contraintes</Th>
              <Th className="text-right">Actions</Th>
            </Tr>
          </THead>
          <TBody>
            {liste.map((matiere) => (
              <Tr key={matiere.id}>
                <Td className="font-medium text-neutral-900">
                  <span className="flex items-center gap-2">
                    <span
                      className="inline-block h-4 w-4 shrink-0 rounded"
                      style={{ backgroundColor: matiere.couleur }}
                      aria-hidden="true"
                    />
                    {matiere.libelle}
                  </span>
                </Td>
                <Td>{matiere.code}</Td>
                <Td>{matiere.coefficient}</Td>
                <Td>{matiere.poidsCognitif}</Td>
                <Td>{matiere.typeSalleRequis ?? '—'}</Td>
                <Td>
                  <Pastilles elements={matiere.equipementsRequis ?? []} />
                </Td>
                <Td className="whitespace-nowrap">
                  {formatUnites(matiere.dureeMinUnites)} –{' '}
                  {formatUnites(matiere.dureeMaxUnites)}
                </Td>
                <Td>
                  <Pastilles
                    elements={[
                      ...(matiere.eviterAvantDejeuner
                        ? ['Éviter avant déjeuner']
                        : []),
                      ...(matiere.eviterFinJournee
                        ? ['Éviter fin de journée']
                        : []),
                    ]}
                  />
                </Td>
                <Td className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variante="secondary"
                      taille="sm"
                      onClick={() => ouvrirEdition(matiere)}
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
                            `Supprimer la matière « ${matiere.libelle} » ?`,
                          )
                        ) {
                          suppression.mutate(matiere.id);
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

      <Dialog
        ouvert={dialogOuvert}
        titre={enEdition === null ? 'Nouvelle matière' : 'Modifier la matière'}
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sauvegarde.mutate();
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="libelleMatiere">Libellé</Label>
              <Input
                id="libelleMatiere"
                value={formulaire.libelle}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, libelle: e.target.value })
                }
                placeholder="Mathématiques"
                required
              />
            </div>
            <div>
              <Label htmlFor="codeMatiere">Code</Label>
              <Input
                id="codeMatiere"
                value={formulaire.code}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, code: e.target.value })
                }
                placeholder="MATH"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <Label htmlFor="coefficientMatiere">Coefficient</Label>
              <Input
                id="coefficientMatiere"
                type="number"
                min="0"
                step="0.5"
                value={formulaire.coefficient}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, coefficient: e.target.value })
                }
                required
              />
            </div>
            <div>
              <Label htmlFor="poidsMatiere">Poids cognitif</Label>
              <Input
                id="poidsMatiere"
                type="number"
                min="1"
                max="5"
                value={formulaire.poidsCognitif}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    poidsCognitif: e.target.value,
                  })
                }
                required
              />
            </div>
            <div>
              <Label htmlFor="couleurMatiere">Couleur</Label>
              <input
                id="couleurMatiere"
                type="color"
                value={formulaire.couleur}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, couleur: e.target.value })
                }
                className="h-9 w-full cursor-pointer rounded-lg border border-neutral-300 bg-white p-1"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="typeSalleMatiere">Type de salle requis</Label>
              <Input
                id="typeSalleMatiere"
                value={formulaire.typeSalleRequis}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    typeSalleRequis: e.target.value,
                  })
                }
                placeholder="LABO (vide = aucune exigence)"
              />
            </div>
            <div>
              <Label htmlFor="equipementsMatiere">
                Équipements requis (séparés par des virgules)
              </Label>
              <Input
                id="equipementsMatiere"
                value={formulaire.equipementsRequis}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    equipementsRequis: e.target.value,
                  })
                }
                placeholder="PROJECTEUR, PAILLASSE"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="dureeMinMatiere">Durée min (unités)</Label>
              <Input
                id="dureeMinMatiere"
                type="number"
                min="1"
                value={formulaire.dureeMinUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    dureeMinUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-neutral-500">
                {formatUnites(Number(formulaire.dureeMinUnites) || 0)}
              </p>
            </div>
            <div>
              <Label htmlFor="dureeMaxMatiere">Durée max (unités)</Label>
              <Input
                id="dureeMaxMatiere"
                type="number"
                min="1"
                value={formulaire.dureeMaxUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    dureeMaxUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-neutral-500">
                {formatUnites(Number(formulaire.dureeMaxUnites) || 0)}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap gap-6">
            <label className="flex items-center gap-2 text-sm text-neutral-700">
              <input
                type="checkbox"
                checked={formulaire.eviterAvantDejeuner}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    eviterAvantDejeuner: e.target.checked,
                  })
                }
                className="h-4 w-4 rounded border-neutral-300 text-teal-600 focus:ring-teal-500"
              />
              Éviter avant le déjeuner
            </label>
            <label className="flex items-center gap-2 text-sm text-neutral-700">
              <input
                type="checkbox"
                checked={formulaire.eviterFinJournee}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    eviterFinJournee: e.target.checked,
                  })
                }
                className="h-4 w-4 rounded border-neutral-300 text-teal-600 focus:ring-teal-500"
              />
              Éviter en fin de journée
            </label>
          </div>

          {sauvegarde.error !== null && (
            <MessageErreur message={sauvegarde.error.message} />
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
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Salles                                                             */
/* ------------------------------------------------------------------ */

const FORM_SALLE_INITIAL = {
  nom: '',
  capacite: '30',
  type: '',
  equipements: '',
  batiment: '',
};

function OngletSalles() {
  const queryClient = useQueryClient();
  const { data: salles, isLoading, error } = useQuery({
    queryKey: ['ecole', 'salles'],
    queryFn: () => apiFetch<Salle[]>('/ecole/salles'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Salle | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_SALLE_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'salles'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () => {
      const corps = JSON.stringify({
        nom: formulaire.nom,
        capacite: Number(formulaire.capacite),
        type: formulaire.type,
        equipements: csvVersListe(formulaire.equipements),
        batiment:
          formulaire.batiment.trim().length > 0
            ? formulaire.batiment.trim()
            : null,
      });
      return enEdition === null
        ? apiFetch<Salle>('/ecole/salles', { method: 'POST', body: corps })
        : apiFetch<Salle>(`/ecole/salles/${enEdition.id}`, {
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
      apiFetch<unknown>(`/ecole/salles/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_SALLE_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(salle: Salle) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(salle);
    setFormulaire({
      nom: salle.nom,
      capacite: String(salle.capacite),
      type: salle.type,
      equipements: (salle.equipements ?? []).join(', '),
      batiment: salle.batiment ?? '',
    });
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = salles ?? [];

  return (
    <Card
      titre="Salles"
      actions={<Button taille="sm" onClick={ouvrirCreation}>Nouvelle salle</Button>}
    >
      {suppression.error !== null && (
        <div className="mb-4">
          <MessageErreur message={suppression.error.message} />
        </div>
      )}
      {liste.length === 0 ? (
        <EmptyState message="Aucune salle pour le moment." />
      ) : (
        <Table>
          <THead>
            <Tr>
              <Th>Nom</Th>
              <Th>Capacité</Th>
              <Th>Type</Th>
              <Th>Équipements</Th>
              <Th>Bâtiment</Th>
              <Th className="text-right">Actions</Th>
            </Tr>
          </THead>
          <TBody>
            {liste.map((salle) => (
              <Tr key={salle.id}>
                <Td className="font-medium text-neutral-900">{salle.nom}</Td>
                <Td>{salle.capacite}</Td>
                <Td>{salle.type}</Td>
                <Td>
                  <Pastilles elements={salle.equipements ?? []} />
                </Td>
                <Td>{salle.batiment ?? '—'}</Td>
                <Td className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variante="secondary"
                      taille="sm"
                      onClick={() => ouvrirEdition(salle)}
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
                            `Supprimer la salle « ${salle.nom} » ?`,
                          )
                        ) {
                          suppression.mutate(salle.id);
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

      <Dialog
        ouvert={dialogOuvert}
        titre={enEdition === null ? 'Nouvelle salle' : 'Modifier la salle'}
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sauvegarde.mutate();
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="nomSalle">Nom</Label>
              <Input
                id="nomSalle"
                value={formulaire.nom}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, nom: e.target.value })
                }
                placeholder="Salle 101"
                required
              />
            </div>
            <div>
              <Label htmlFor="capaciteSalle">Capacité</Label>
              <Input
                id="capaciteSalle"
                type="number"
                min="1"
                value={formulaire.capacite}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, capacite: e.target.value })
                }
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="typeSalle">Type</Label>
              <Input
                id="typeSalle"
                value={formulaire.type}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, type: e.target.value })
                }
                placeholder="STANDARD, LABO, GYMNASE…"
                required
              />
            </div>
            <div>
              <Label htmlFor="batimentSalle">Bâtiment</Label>
              <Input
                id="batimentSalle"
                value={formulaire.batiment}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, batiment: e.target.value })
                }
                placeholder="Bâtiment A (optionnel)"
              />
            </div>
          </div>
          <div>
            <Label htmlFor="equipementsSalle">
              Équipements (séparés par des virgules)
            </Label>
            <Input
              id="equipementsSalle"
              value={formulaire.equipements}
              onChange={(e) =>
                setFormulaire({ ...formulaire, equipements: e.target.value })
              }
              placeholder="PROJECTEUR, TABLEAU_INTERACTIF"
            />
          </div>

          {sauvegarde.error !== null && (
            <MessageErreur message={sauvegarde.error.message} />
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
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Barrettes                                                          */
/* ------------------------------------------------------------------ */

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

function OngletBarrettes() {
  const queryClient = useQueryClient();
  const { data: barrettes, isLoading, error } = useQuery({
    queryKey: ['ecole', 'barrettes'],
    queryFn: () => apiFetch<Barrette[]>('/ecole/barrettes'),
  });
  const { data: matieres } = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });
  const { data: groupes } = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<Groupe[]>('/ecole/groupes'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [libelle, setLibelle] = useState('');
  const [matiereId, setMatiereId] = useState('');
  const [groupeIds, setGroupeIds] = useState<number[]>([]);

  const groupesAplatis = aplatirGroupes(groupes ?? []);
  const libellesGroupes = new Map(
    groupesAplatis.map((groupe) => [groupe.id, groupe.libelle]),
  );

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'barrettes'] });
  }

  const creation = useMutation({
    mutationFn: () =>
      apiFetch<Barrette>('/ecole/barrettes', {
        method: 'POST',
        body: JSON.stringify({
          libelle,
          matiereId: Number(matiereId),
          groupeIds,
        }),
      }),
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
    },
  });

  const suppression = useMutation({
    mutationFn: (id: number) =>
      apiFetch<unknown>(`/ecole/barrettes/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = barrettes ?? [];

  return (
    <Card
      titre="Barrettes (cours alignés)"
      actions={
        <Button
          taille="sm"
          onClick={() => {
            creation.reset();
            suppression.reset();
            setLibelle('');
            setMatiereId('');
            setGroupeIds([]);
            setDialogOuvert(true);
          }}
        >
          Nouvelle barrette
        </Button>
      }
    >
      <p className="mb-4 text-sm text-neutral-500">
        Une barrette aligne une matière sur le même créneau pour plusieurs
        groupes (ex. langues vivantes).
      </p>
      {suppression.error !== null && (
        <div className="mb-4">
          <MessageErreur message={suppression.error.message} />
        </div>
      )}
      {liste.length === 0 ? (
        <EmptyState message="Aucune barrette pour le moment." />
      ) : (
        <Table>
          <THead>
            <Tr>
              <Th>Libellé</Th>
              <Th>Matière</Th>
              <Th>Groupes</Th>
              <Th className="text-right">Actions</Th>
            </Tr>
          </THead>
          <TBody>
            {liste.map((barrette) => (
              <Tr key={barrette.id}>
                <Td className="font-medium text-neutral-900">{barrette.libelle}</Td>
                <Td>{barrette.matiereLibelle}</Td>
                <Td>
                  <Pastilles
                    elements={barrette.groupeIds.map(
                      (id) => libellesGroupes.get(id) ?? `Groupe ${id}`,
                    )}
                  />
                </Td>
                <Td className="text-right">
                  <Button
                    variante="danger"
                    taille="sm"
                    disabled={suppression.isPending}
                    onClick={() => {
                      if (
                        window.confirm(
                          `Supprimer la barrette « ${barrette.libelle} » ?`,
                        )
                      ) {
                        suppression.mutate(barrette.id);
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
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre="Nouvelle barrette"
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
            <Label htmlFor="libelleBarrette">Libellé</Label>
            <Input
              id="libelleBarrette"
              value={libelle}
              onChange={(e) => setLibelle(e.target.value)}
              placeholder="Barrette langues 1AC"
              required
            />
          </div>
          <div>
            <Label htmlFor="matiereBarrette">Matière</Label>
            <Select
              id="matiereBarrette"
              value={matiereId}
              onChange={(e) => setMatiereId(e.target.value)}
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
            <Label>Groupes concernés</Label>
            <div className="mt-1 max-h-52 space-y-1 overflow-y-auto rounded-lg border border-neutral-200 p-3">
              {groupesAplatis.length === 0 && (
                <p className="text-sm text-neutral-500">
                  Aucun groupe disponible.
                </p>
              )}
              {groupesAplatis.map((groupe) => (
                <label
                  key={groupe.id}
                  className="flex items-center gap-2 text-sm text-neutral-700"
                >
                  <input
                    type="checkbox"
                    checked={groupeIds.includes(groupe.id)}
                    onChange={(e) =>
                      setGroupeIds(
                        e.target.checked
                          ? [...groupeIds, groupe.id]
                          : groupeIds.filter((id) => id !== groupe.id),
                      )
                    }
                    className="h-4 w-4 rounded border-neutral-300 text-teal-600 focus:ring-teal-500"
                  />
                  {groupe.libelle}
                </label>
              ))}
            </div>
          </div>

          {creation.error !== null && (
            <MessageErreur message={creation.error.message} />
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variante="secondary" onClick={() => setDialogOuvert(false)}>
              Annuler
            </Button>
            <Button
              type="submit"
              disabled={creation.isPending || groupeIds.length < 2}
            >
              {creation.isPending ? 'Création…' : 'Créer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </Card>
  );
}
