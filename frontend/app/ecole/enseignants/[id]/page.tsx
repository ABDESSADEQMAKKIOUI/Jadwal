'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useEffect, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatUnites } from '@/lib/format';
import type {
  Enseignant,
  Grille,
  Habilitation,
  Indisponibilite,
  Jour,
  Matiere,
  Niveau,
  PreferenceHoraire,
  Semaine,
  TypePreference,
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

const LIBELLES_JOURS: Record<Jour, string> = {
  LUNDI: 'Lundi',
  MARDI: 'Mardi',
  MERCREDI: 'Mercredi',
  JEUDI: 'Jeudi',
  VENDREDI: 'Vendredi',
  SAMEDI: 'Samedi',
  DIMANCHE: 'Dimanche',
};

function libelleHeure(grille: Grille, index: number): string {
  const morceaux = grille.heureDebut.split(':');
  const heures = Number(morceaux[0]);
  const minutes = Number(morceaux[1]);
  const total =
    (Number.isFinite(heures) ? heures : 8) * 60 +
    (Number.isFinite(minutes) ? minutes : 0) +
    index * grille.dureeUniteMinutes;
  const h = Math.floor(total / 60) % 24;
  const m = total % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

function estPlageBloquee(grille: Grille, index: number): boolean {
  return grille.plagesBloquees.some(
    (plage) =>
      index >= plage.indexDebut && index < plage.indexDebut + plage.dureeUnites,
  );
}

function BadgeStatutIndispo({ statut }: { statut: string }) {
  const valide = statut === 'VALIDE';
  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium ${
        valide ? 'bg-green-100 text-green-800' : 'bg-amber-100 text-amber-800'
      }`}
    >
      {valide ? 'Validée' : 'Brouillon'}
    </span>
  );
}

const FORM_PLAGE_INITIAL = {
  jour: 'LUNDI' as Jour,
  indexDebut: '0',
  dureeUnites: '2',
  source: 'DIRECTEUR',
  semaine: 'TOUTES' as Semaine,
  motif: '',
};

export default function PageDetailEnseignant() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const queryClient = useQueryClient();

  const { data: enseignants, isLoading, error } = useQuery({
    queryKey: ['ecole', 'enseignants'],
    queryFn: () => apiFetch<Enseignant[]>('/ecole/enseignants'),
  });
  const { data: grille } = useQuery({
    queryKey: ['ecole', 'grille'],
    queryFn: () => apiFetch<Grille>('/ecole/grille'),
  });
  const { data: matieres } = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });
  const { data: niveaux } = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });
  const cleHabilitations = ['ecole', 'enseignant', id, 'habilitations'];
  const { data: habilitationsServeur } = useQuery({
    queryKey: cleHabilitations,
    queryFn: () =>
      apiFetch<Habilitation[]>(`/ecole/enseignants/${id}/habilitations`),
  });
  const cleIndispos = ['ecole', 'enseignant', id, 'indisponibilites'];
  const { data: indisponibilites } = useQuery({
    queryKey: cleIndispos,
    queryFn: () =>
      apiFetch<Indisponibilite[]>(`/ecole/enseignants/${id}/indisponibilites`),
  });
  const clePreferences = ['ecole', 'enseignant', id, 'preferences'];
  const { data: preferencesServeur } = useQuery({
    queryKey: clePreferences,
    queryFn: () =>
      apiFetch<PreferenceHoraire[]>(`/ecole/enseignants/${id}/preferences`),
  });

  /* ----------------------- Habilitations ------------------------- */

  const [habilitations, setHabilitations] = useState<Record<number, number[]>>(
    {},
  );

  useEffect(() => {
    if (habilitationsServeur !== undefined) {
      setHabilitations(
        Object.fromEntries(
          habilitationsServeur.map((h) => [h.matiereId, h.niveauIds]),
        ),
      );
    }
  }, [habilitationsServeur]);

  const sauvegardeHabilitations = useMutation({
    mutationFn: () =>
      apiFetch<unknown>(`/ecole/enseignants/${id}/habilitations`, {
        method: 'PUT',
        body: JSON.stringify(
          Object.entries(habilitations).map(([matiereId, niveauIds]) => ({
            matiereId: Number(matiereId),
            niveauIds,
          })),
        ),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: cleHabilitations });
      void queryClient.invalidateQueries({
        queryKey: ['ecole', 'enseignants'],
      });
    },
  });

  function basculerMatiere(matiereId: number) {
    setHabilitations((precedent) => {
      const suivant = { ...precedent };
      if (matiereId in suivant) {
        delete suivant[matiereId];
      } else {
        suivant[matiereId] = [];
      }
      return suivant;
    });
  }

  function basculerNiveau(matiereId: number, niveauId: number) {
    setHabilitations((precedent) => {
      const actuels = precedent[matiereId] ?? [];
      return {
        ...precedent,
        [matiereId]: actuels.includes(niveauId)
          ? actuels.filter((n) => n !== niveauId)
          : [...actuels, niveauId],
      };
    });
  }

  /* ---------------------- Indisponibilités ----------------------- */

  const [selection, setSelection] = useState<{
    jour: Jour;
    debut: number;
    fin: number;
  } | null>(null);
  const [dialogPlage, setDialogPlage] = useState(false);
  const [formPlage, setFormPlage] = useState(FORM_PLAGE_INITIAL);

  function invaliderIndispos() {
    void queryClient.invalidateQueries({ queryKey: cleIndispos });
  }

  const creationIndispo = useMutation({
    mutationFn: (donnees: {
      jour: Jour;
      indexDebut: number;
      dureeUnites: number;
      source: string;
      semaine: Semaine;
      motif: string | null;
    }) =>
      apiFetch<Indisponibilite>(`/ecole/enseignants/${id}/indisponibilites`, {
        method: 'POST',
        body: JSON.stringify(donnees),
      }),
    onSuccess: () => {
      invaliderIndispos();
      setDialogPlage(false);
    },
  });

  const suppressionIndispo = useMutation({
    mutationFn: (indispoId: number) =>
      apiFetch<unknown>(
        `/ecole/enseignants/${id}/indisponibilites/${indispoId}`,
        { method: 'DELETE' },
      ),
    onSuccess: invaliderIndispos,
  });

  const validationIndispo = useMutation({
    mutationFn: (indispoId: number) =>
      apiFetch<Indisponibilite>(
        `/ecole/enseignants/${id}/indisponibilites/${indispoId}`,
        { method: 'PATCH', body: JSON.stringify({ statut: 'VALIDE' }) },
      ),
    onSuccess: invaliderIndispos,
  });

  function indispoPourCellule(
    jour: Jour,
    index: number,
  ): Indisponibilite | undefined {
    return (indisponibilites ?? []).find(
      (indispo) =>
        indispo.jour === jour &&
        index >= indispo.indexDebut &&
        index < indispo.indexDebut + indispo.dureeUnites,
    );
  }

  function terminerSelection() {
    if (selection === null) return;
    const debut = Math.min(selection.debut, selection.fin);
    const fin = Math.max(selection.debut, selection.fin);
    creationIndispo.mutate({
      jour: selection.jour,
      indexDebut: debut,
      dureeUnites: fin - debut + 1,
      source: 'DIRECTEUR',
      semaine: 'TOUTES',
      motif: null,
    });
    setSelection(null);
  }

  function cliquerCelluleOccupee(indispo: Indisponibilite) {
    if (
      indispo.dureeUnites <= 1 ||
      window.confirm(
        `Supprimer cette indisponibilité de ${formatUnites(indispo.dureeUnites)} (${LIBELLES_JOURS[indispo.jour]}) ?`,
      )
    ) {
      suppressionIndispo.mutate(indispo.id);
    }
  }

  /* ------------------------- Préférences ------------------------- */

  const [preferences, setPreferences] = useState<Record<string, TypePreference>>(
    {},
  );
  const [messagePreferences, setMessagePreferences] = useState<string | null>(
    null,
  );

  useEffect(() => {
    if (preferencesServeur !== undefined) {
      const cellules: Record<string, TypePreference> = {};
      for (const plage of preferencesServeur) {
        for (let i = 0; i < plage.dureeUnites; i++) {
          cellules[`${plage.jour}:${plage.indexDebut + i}`] = plage.type;
        }
      }
      setPreferences(cellules);
    }
  }, [preferencesServeur]);

  const sauvegardePreferences = useMutation({
    mutationFn: (plages: PreferenceHoraire[]) =>
      apiFetch<unknown>(`/ecole/enseignants/${id}/preferences`, {
        method: 'PUT',
        body: JSON.stringify(plages),
      }),
    onSuccess: () => {
      setMessagePreferences('Préférences enregistrées.');
      void queryClient.invalidateQueries({ queryKey: clePreferences });
    },
  });

  function cyclerPreference(jour: Jour, index: number) {
    setMessagePreferences(null);
    setPreferences((precedent) => {
      const cle = `${jour}:${index}`;
      const suivant = { ...precedent };
      const actuel = suivant[cle];
      if (actuel === undefined) {
        suivant[cle] = 'EVITER';
      } else if (actuel === 'EVITER') {
        suivant[cle] = 'PREFERER';
      } else {
        delete suivant[cle];
      }
      return suivant;
    });
  }

  function enregistrerPreferences() {
    if (grille === undefined) return;
    const plages: PreferenceHoraire[] = [];
    for (const jour of grille.joursActifs) {
      let index = 0;
      while (index < grille.unitesParJour) {
        const type = preferences[`${jour}:${index}`];
        if (type === undefined) {
          index++;
          continue;
        }
        let fin = index + 1;
        while (
          fin < grille.unitesParJour &&
          preferences[`${jour}:${fin}`] === type
        ) {
          fin++;
        }
        plages.push({ jour, indexDebut: index, dureeUnites: fin - index, type });
        index = fin;
      }
    }
    sauvegardePreferences.mutate(plages);
  }

  /* --------------------------- Rendu ----------------------------- */

  if (isLoading) return <ChargementPage />;
  if (error !== null) {
    return (
      <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {error.message}
      </p>
    );
  }

  const enseignant = (enseignants ?? []).find(
    (element) => String(element.id) === id,
  );
  if (enseignant === undefined) {
    return <EmptyState message="Enseignant introuvable." />;
  }

  const indices =
    grille !== undefined
      ? Array.from({ length: grille.unitesParJour }, (_valeur, i) => i)
      : [];

  return (
    <div className="space-y-6">
      <div>
        <Link
          href="/ecole/enseignants"
          className="text-sm text-indigo-600 hover:text-indigo-800 hover:underline"
        >
          ← Retour aux enseignants
        </Link>
        <h1 className="mt-2 text-xl font-semibold text-gray-900">
          {enseignant.nomComplet}
        </h1>
        <p className="mt-1 text-sm text-gray-500">{enseignant.email}</p>
      </div>

      <Card titre="Fiche enseignant">
        <dl className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
          <div>
            <dt className="text-gray-500">Type</dt>
            <dd className="mt-0.5 font-medium text-gray-900">
              {enseignant.type === 'MIXTE' ? 'Mixte' : 'Propre'}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Quota hebdomadaire</dt>
            <dd className="mt-0.5 font-medium text-gray-900">
              {formatUnites(enseignant.quotaHebdoUnites)}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Charge affectée</dt>
            <dd className="mt-0.5 font-medium text-gray-900">
              {formatUnites(enseignant.chargeAffectee)}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Vacataire</dt>
            <dd className="mt-0.5 font-medium text-gray-900">
              {enseignant.vacataire
                ? `Oui (buffer ${formatUnites(enseignant.bufferTrajetUnites)})`
                : 'Non'}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Max consécutif</dt>
            <dd className="mt-0.5 font-medium text-gray-900">
              {formatUnites(enseignant.maxConsecutifUnites)}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Amplitude max / jour</dt>
            <dd className="mt-0.5 font-medium text-gray-900">
              {formatUnites(enseignant.amplitudeMaxUnites)}
            </dd>
          </div>
        </dl>
      </Card>

      <Card
        titre="Habilitations (matières × niveaux)"
        actions={
          <Button
            taille="sm"
            disabled={sauvegardeHabilitations.isPending}
            onClick={() => sauvegardeHabilitations.mutate()}
          >
            {sauvegardeHabilitations.isPending
              ? 'Enregistrement…'
              : 'Enregistrer les habilitations'}
          </Button>
        }
      >
        {(matieres ?? []).length === 0 ? (
          <EmptyState message="Créez d’abord des matières dans le référentiel." />
        ) : (
          <div className="space-y-3">
            {(matieres ?? []).map((matiere) => {
              const habilite = matiere.id in habilitations;
              return (
                <div
                  key={matiere.id}
                  className={`rounded-lg border p-3 ${
                    habilite
                      ? 'border-indigo-200 bg-indigo-50/50'
                      : 'border-gray-100'
                  }`}
                >
                  <label className="flex items-center gap-2 text-sm font-medium text-gray-900">
                    <input
                      type="checkbox"
                      checked={habilite}
                      onChange={() => basculerMatiere(matiere.id)}
                      className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                    />
                    <span
                      className="inline-block h-3 w-3 rounded"
                      style={{ backgroundColor: matiere.couleur }}
                      aria-hidden="true"
                    />
                    {matiere.libelle}
                  </label>
                  {habilite && (
                    <div className="mt-2 flex flex-wrap gap-4 pl-6">
                      {(niveaux ?? []).length === 0 && (
                        <p className="text-xs text-gray-500">
                          Aucun niveau défini.
                        </p>
                      )}
                      {(niveaux ?? []).map((niveau) => (
                        <label
                          key={niveau.id}
                          className="flex items-center gap-1.5 text-sm text-gray-700"
                        >
                          <input
                            type="checkbox"
                            checked={(
                              habilitations[matiere.id] ?? []
                            ).includes(niveau.id)}
                            onChange={() =>
                              basculerNiveau(matiere.id, niveau.id)
                            }
                            className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                          />
                          {niveau.libelle}
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
        {sauvegardeHabilitations.error !== null && (
          <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {sauvegardeHabilitations.error.message}
          </p>
        )}
        {sauvegardeHabilitations.isSuccess && (
          <p className="mt-4 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
            Habilitations enregistrées.
          </p>
        )}
      </Card>

      <Card
        titre="Indisponibilités hebdomadaires"
        actions={
          <Button
            taille="sm"
            variante="secondary"
            onClick={() => {
              creationIndispo.reset();
              setFormPlage(FORM_PLAGE_INITIAL);
              setDialogPlage(true);
            }}
          >
            Ajouter une plage
          </Button>
        }
      >
        <div className="mb-4 rounded-lg border border-indigo-100 bg-indigo-50 px-4 py-3 text-sm text-indigo-900">
          Règle D-04 : seules les indisponibilités au statut « Validée » sont
          prises en compte par le moteur de génération. Pensez à valider les
          brouillons ci-dessous.
        </div>

        {grille === undefined ? (
          <EmptyState message="Configurez d’abord la grille horaire dans le référentiel." />
        ) : (
          <>
            <p className="mb-3 text-sm text-gray-500">
              Cliquez sur une cellule pour créer ou supprimer une
              indisponibilité, ou faites un cliquer-glisser vertical pour créer
              une plage.
            </p>
            <div
              className="select-none overflow-x-auto"
              onMouseUp={terminerSelection}
              onMouseLeave={() => setSelection(null)}
            >
              <table className="min-w-full border-separate border-spacing-0.5">
                <thead>
                  <tr>
                    <th className="w-16 px-1 py-1 text-left text-xs font-semibold text-gray-500">
                      Heure
                    </th>
                    {grille.joursActifs.map((jour) => (
                      <th
                        key={jour}
                        className="px-1 py-1 text-center text-xs font-semibold text-gray-500"
                      >
                        {LIBELLES_JOURS[jour]}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {indices.map((index) => {
                    const bloquee = estPlageBloquee(grille, index);
                    return (
                      <tr key={index}>
                        <td className="whitespace-nowrap px-1 py-0.5 text-xs text-gray-500">
                          {libelleHeure(grille, index)}
                        </td>
                        {grille.joursActifs.map((jour) => {
                          if (bloquee) {
                            return (
                              <td
                                key={jour}
                                className="h-7 min-w-20 rounded bg-gray-200"
                                title="Plage bloquée (déjeuner, pause…)"
                              />
                            );
                          }
                          const indispo = indispoPourCellule(jour, index);
                          const enSelection =
                            selection !== null &&
                            selection.jour === jour &&
                            index >=
                              Math.min(selection.debut, selection.fin) &&
                            index <= Math.max(selection.debut, selection.fin);
                          let classe =
                            'bg-gray-50 hover:bg-indigo-100 cursor-pointer';
                          if (indispo !== undefined) {
                            classe =
                              indispo.statut === 'VALIDE'
                                ? 'bg-red-400 hover:bg-red-500 cursor-pointer'
                                : 'bg-amber-300 hover:bg-amber-400 cursor-pointer';
                          } else if (enSelection) {
                            classe = 'bg-indigo-300 cursor-pointer';
                          }
                          return (
                            <td
                              key={jour}
                              className={`h-7 min-w-20 rounded text-center text-[10px] font-medium text-white ${classe}`}
                              title={
                                indispo !== undefined
                                  ? `${indispo.statut === 'VALIDE' ? 'Validée' : 'Brouillon'}${indispo.motif !== null && indispo.motif.length > 0 ? ` — ${indispo.motif}` : ''}`
                                  : 'Disponible'
                              }
                              onMouseDown={(e) => {
                                e.preventDefault();
                                if (indispo === undefined) {
                                  setSelection({
                                    jour,
                                    debut: index,
                                    fin: index,
                                  });
                                } else {
                                  cliquerCelluleOccupee(indispo);
                                }
                              }}
                              onMouseEnter={() => {
                                if (
                                  selection !== null &&
                                  selection.jour === jour
                                ) {
                                  setSelection({ ...selection, fin: index });
                                }
                              }}
                            >
                              {indispo !== undefined &&
                              indispo.semaine !== 'TOUTES'
                                ? indispo.semaine
                                : ''}
                            </td>
                          );
                        })}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className="mt-3 flex flex-wrap gap-4 text-xs text-gray-600">
              <span className="flex items-center gap-1.5">
                <span className="inline-block h-3 w-3 rounded bg-amber-300" />
                Brouillon
              </span>
              <span className="flex items-center gap-1.5">
                <span className="inline-block h-3 w-3 rounded bg-red-400" />
                Validée
              </span>
              <span className="flex items-center gap-1.5">
                <span className="inline-block h-3 w-3 rounded bg-gray-200" />
                Plage bloquée
              </span>
            </div>
          </>
        )}

        {creationIndispo.error !== null && !dialogPlage && (
          <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {creationIndispo.error.message}
          </p>
        )}
        {suppressionIndispo.error !== null && (
          <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {suppressionIndispo.error.message}
          </p>
        )}

        {(indisponibilites ?? []).length > 0 && (
          <div className="mt-5">
            <Table>
              <THead>
                <Tr>
                  <Th>Jour</Th>
                  <Th>Plage</Th>
                  <Th>Durée</Th>
                  <Th>Semaine</Th>
                  <Th>Source</Th>
                  <Th>Motif</Th>
                  <Th>Statut</Th>
                  <Th className="text-right">Actions</Th>
                </Tr>
              </THead>
              <TBody>
                {(indisponibilites ?? []).map((indispo) => (
                  <Tr key={indispo.id}>
                    <Td className="font-medium text-gray-900">
                      {LIBELLES_JOURS[indispo.jour]}
                    </Td>
                    <Td>
                      {grille !== undefined
                        ? `${libelleHeure(grille, indispo.indexDebut)} – ${libelleHeure(grille, indispo.indexDebut + indispo.dureeUnites)}`
                        : `Unité ${indispo.indexDebut}`}
                    </Td>
                    <Td>{formatUnites(indispo.dureeUnites)}</Td>
                    <Td>
                      {indispo.semaine === 'TOUTES'
                        ? 'Toutes'
                        : `Semaine ${indispo.semaine}`}
                    </Td>
                    <Td>{indispo.source}</Td>
                    <Td>{indispo.motif ?? '—'}</Td>
                    <Td>
                      <BadgeStatutIndispo statut={indispo.statut} />
                    </Td>
                    <Td className="text-right">
                      <div className="flex justify-end gap-2">
                        {indispo.statut !== 'VALIDE' && (
                          <Button
                            taille="sm"
                            disabled={validationIndispo.isPending}
                            onClick={() => validationIndispo.mutate(indispo.id)}
                          >
                            Valider
                          </Button>
                        )}
                        <Button
                          variante="danger"
                          taille="sm"
                          disabled={suppressionIndispo.isPending}
                          onClick={() => suppressionIndispo.mutate(indispo.id)}
                        >
                          Supprimer
                        </Button>
                      </div>
                    </Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
          </div>
        )}
      </Card>

      <Card
        titre="Préférences horaires"
        actions={
          <Button
            taille="sm"
            disabled={sauvegardePreferences.isPending || grille === undefined}
            onClick={enregistrerPreferences}
          >
            {sauvegardePreferences.isPending
              ? 'Enregistrement…'
              : 'Enregistrer les préférences'}
          </Button>
        }
      >
        {grille === undefined ? (
          <EmptyState message="Configurez d’abord la grille horaire dans le référentiel." />
        ) : (
          <>
            <p className="mb-3 text-sm text-gray-500">
              Cliquez sur une cellule pour alterner : neutre → à éviter →
              préféré. Enregistrez ensuite vos modifications.
            </p>
            <div className="select-none overflow-x-auto">
              <table className="min-w-full border-separate border-spacing-0.5">
                <thead>
                  <tr>
                    <th className="w-16 px-1 py-1 text-left text-xs font-semibold text-gray-500">
                      Heure
                    </th>
                    {grille.joursActifs.map((jour) => (
                      <th
                        key={jour}
                        className="px-1 py-1 text-center text-xs font-semibold text-gray-500"
                      >
                        {LIBELLES_JOURS[jour]}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {indices.map((index) => {
                    const bloquee = estPlageBloquee(grille, index);
                    return (
                      <tr key={index}>
                        <td className="whitespace-nowrap px-1 py-0.5 text-xs text-gray-500">
                          {libelleHeure(grille, index)}
                        </td>
                        {grille.joursActifs.map((jour) => {
                          if (bloquee) {
                            return (
                              <td
                                key={jour}
                                className="h-7 min-w-20 rounded bg-gray-200"
                                title="Plage bloquée"
                              />
                            );
                          }
                          const type = preferences[`${jour}:${index}`];
                          const classe =
                            type === 'EVITER'
                              ? 'bg-red-300 hover:bg-red-400'
                              : type === 'PREFERER'
                                ? 'bg-green-300 hover:bg-green-400'
                                : 'bg-gray-50 hover:bg-indigo-100';
                          return (
                            <td
                              key={jour}
                              className={`h-7 min-w-20 cursor-pointer rounded ${classe}`}
                              title={
                                type === 'EVITER'
                                  ? 'À éviter'
                                  : type === 'PREFERER'
                                    ? 'Préféré'
                                    : 'Neutre'
                              }
                              onClick={() => cyclerPreference(jour, index)}
                            />
                          );
                        })}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className="mt-3 flex flex-wrap gap-4 text-xs text-gray-600">
              <span className="flex items-center gap-1.5">
                <span className="inline-block h-3 w-3 rounded bg-red-300" />À
                éviter
              </span>
              <span className="flex items-center gap-1.5">
                <span className="inline-block h-3 w-3 rounded bg-green-300" />
                Préféré
              </span>
              <span className="flex items-center gap-1.5">
                <span className="inline-block h-3 w-3 rounded bg-gray-200" />
                Plage bloquée
              </span>
            </div>
          </>
        )}
        {sauvegardePreferences.error !== null && (
          <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {sauvegardePreferences.error.message}
          </p>
        )}
        {messagePreferences !== null && (
          <p className="mt-4 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
            {messagePreferences}
          </p>
        )}
      </Card>

      <Dialog
        ouvert={dialogPlage}
        titre="Ajouter une plage d’indisponibilité"
        onFermer={() => setDialogPlage(false)}
      >
        <form
          onSubmit={(evenement: FormEvent<HTMLFormElement>) => {
            evenement.preventDefault();
            creationIndispo.mutate({
              jour: formPlage.jour,
              indexDebut: Number(formPlage.indexDebut),
              dureeUnites: Number(formPlage.dureeUnites),
              source: formPlage.source,
              semaine: formPlage.semaine,
              motif:
                formPlage.motif.trim().length > 0
                  ? formPlage.motif.trim()
                  : null,
            });
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-3 gap-4">
            <div>
              <Label htmlFor="jourPlage">Jour</Label>
              <Select
                id="jourPlage"
                value={formPlage.jour}
                onChange={(e) =>
                  setFormPlage({ ...formPlage, jour: e.target.value as Jour })
                }
              >
                {(grille?.joursActifs ?? []).map((jour) => (
                  <option key={jour} value={jour}>
                    {LIBELLES_JOURS[jour]}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label htmlFor="debutPlage">Index de début</Label>
              <Input
                id="debutPlage"
                type="number"
                min="0"
                value={formPlage.indexDebut}
                onChange={(e) =>
                  setFormPlage({ ...formPlage, indexDebut: e.target.value })
                }
                required
              />
              {grille !== undefined && (
                <p className="mt-1 text-xs text-gray-500">
                  {libelleHeure(grille, Number(formPlage.indexDebut) || 0)}
                </p>
              )}
            </div>
            <div>
              <Label htmlFor="dureePlage">Durée (unités)</Label>
              <Input
                id="dureePlage"
                type="number"
                min="1"
                value={formPlage.dureeUnites}
                onChange={(e) =>
                  setFormPlage({ ...formPlage, dureeUnites: e.target.value })
                }
                required
              />
              <p className="mt-1 text-xs text-gray-500">
                {formatUnites(Number(formPlage.dureeUnites) || 0)}
              </p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="sourcePlage">Source</Label>
              <Select
                id="sourcePlage"
                value={formPlage.source}
                onChange={(e) =>
                  setFormPlage({ ...formPlage, source: e.target.value })
                }
              >
                <option value="DIRECTEUR">Directeur</option>
                <option value="ENSEIGNANT">Enseignant</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="semainePlage">Semaine</Label>
              <Select
                id="semainePlage"
                value={formPlage.semaine}
                onChange={(e) =>
                  setFormPlage({
                    ...formPlage,
                    semaine: e.target.value as Semaine,
                  })
                }
              >
                <option value="TOUTES">Toutes les semaines</option>
                <option value="A">Semaine A</option>
                <option value="B">Semaine B</option>
              </Select>
            </div>
          </div>
          <div>
            <Label htmlFor="motifPlage">Motif (optionnel)</Label>
            <Input
              id="motifPlage"
              value={formPlage.motif}
              onChange={(e) =>
                setFormPlage({ ...formPlage, motif: e.target.value })
              }
              placeholder="Cours à la faculté, engagement personnel…"
            />
          </div>

          {creationIndispo.error !== null && (
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {creationIndispo.error.message}
            </p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variante="secondary" onClick={() => setDialogPlage(false)}>
              Annuler
            </Button>
            <Button type="submit" disabled={creationIndispo.isPending}>
              {creationIndispo.isPending ? 'Création…' : 'Ajouter'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
