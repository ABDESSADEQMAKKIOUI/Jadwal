'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { apiFetch } from '@/lib/api';
import {
  apiFetchDetaille,
  ErreurApi,
  extraireRapport,
} from '@/lib/api-planning';
import type {
  AnalyseScore,
  FaisabiliteRapport,
  Generation,
  StatutFaisabilite,
  StatutGeneration,
} from '@/lib/types-planning';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Dialog } from '@/components/ui/dialog';
import { EmptyState } from '@/components/ui/empty-state';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { ChargementPage, Spinner } from '@/components/ui/spinner';
import { Table, TBody, Td, Th, THead, Tr } from '@/components/ui/table';

const DUREES = [
  { valeur: 30, libelle: '30 secondes' },
  { valeur: 60, libelle: '1 minute' },
  { valeur: 120, libelle: '2 minutes' },
  { valeur: 300, libelle: '5 minutes' },
  { valeur: 600, libelle: '10 minutes' },
];

const LIBELLES_STATUT: Record<StatutGeneration, string> = {
  EN_COURS: 'En cours',
  TERMINEE: 'Terminée',
  INFAISABLE: 'Infaisable',
  ECHEC: 'Échec',
  ARRETEE: 'Arrêtée',
};

const COULEURS_STATUT: Record<StatutGeneration, string> = {
  EN_COURS: 'bg-indigo-100 text-indigo-800',
  TERMINEE: 'bg-green-100 text-green-800',
  INFAISABLE: 'bg-red-100 text-red-700',
  ECHEC: 'bg-red-100 text-red-700',
  ARRETEE: 'bg-gray-100 text-gray-600',
};

function BadgeGeneration({ statut }: { statut: StatutGeneration }) {
  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium ${COULEURS_STATUT[statut] ?? 'bg-gray-100 text-gray-600'}`}
    >
      {LIBELLES_STATUT[statut] ?? statut}
    </span>
  );
}

function PastilleFaisabilite({ statut }: { statut: StatutFaisabilite }) {
  const couleur =
    statut === 'OK'
      ? 'bg-green-500'
      : statut === 'AVERTISSEMENT'
        ? 'bg-amber-500'
        : 'bg-red-500';
  return (
    <span
      className={`mt-1 inline-block h-2.5 w-2.5 shrink-0 rounded-full ${couleur}`}
      aria-label={statut}
    />
  );
}

/** Extrait le nombre de conflits durs d'un score type "-3hard/-120soft". */
function conflitsDurs(score: string | null): number | null {
  if (score === null) return null;
  const correspondance = /(-?\d+)hard/.exec(score);
  if (correspondance === null) return null;
  return Math.abs(Number(correspondance[1]));
}

function formatSecondes(secondes: number): string {
  if (secondes < 60) return `${secondes} s`;
  const minutes = Math.floor(secondes / 60);
  const reste = secondes % 60;
  return reste === 0 ? `${minutes} min` : `${minutes} min ${reste} s`;
}

function formatDateHeure(valeur: string): string {
  const date = new Date(valeur);
  if (Number.isNaN(date.getTime())) return valeur;
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

interface Progression {
  score: string | null;
  tempsEcouleSecondes: number | null;
}

export default function PageGeneration() {
  const clientQuery = useQueryClient();

  const [rapport, setRapport] = useState<FaisabiliteRapport | null>(null);
  const [dureeMax, setDureeMax] = useState(120);
  const [suiviId, setSuiviId] = useState<number | null>(null);
  const [progression, setProgression] = useState<Progression | null>(null);
  const [resultatFinal, setResultatFinal] = useState<{
    id: number;
    statut: StatutGeneration;
    score: string | null;
  } | null>(null);
  const [analyseId, setAnalyseId] = useState<number | null>(null);
  const initialise = useRef(false);

  const requeteGenerations = useQuery({
    queryKey: ['ecole', 'generations'],
    queryFn: () => apiFetch<Generation[]>('/ecole/generations'),
  });

  // Reprend le suivi si une génération est déjà en cours au chargement.
  useEffect(() => {
    if (initialise.current) return;
    if (requeteGenerations.data === undefined) return;
    initialise.current = true;
    const enCours = requeteGenerations.data.find(
      (generation) => generation.statut === 'EN_COURS',
    );
    if (enCours !== undefined) {
      setSuiviId(enCours.id);
    }
  }, [requeteGenerations.data]);

  function invaliderListes() {
    void clientQuery.invalidateQueries({ queryKey: ['ecole', 'generations'] });
    void clientQuery.invalidateQueries({
      queryKey: ['ecole', 'plannings', 'versions'],
    });
    void clientQuery.invalidateQueries({ queryKey: ['ecole', 'planning'] });
  }

  // Suivi SSE de la génération en cours.
  useEffect(() => {
    if (suiviId === null) return;
    const source = new EventSource(
      `/api/backend/ecole/generations/${suiviId}/stream`,
    );
    let terminee = false;

    const finaliser = (statut: StatutGeneration, score: string | null) => {
      if (terminee) return;
      terminee = true;
      source.close();
      setResultatFinal({ id: suiviId, statut, score });
      setSuiviId(null);
      setProgression(null);
      invaliderListes();
    };

    const traiter = (evenement: MessageEvent) => {
      let donnees: {
        score?: string | null;
        tempsEcouleSecondes?: number;
        statut?: StatutGeneration;
      };
      try {
        donnees = JSON.parse(evenement.data as string) as typeof donnees;
      } catch {
        return;
      }
      setProgression({
        score: donnees.score ?? null,
        tempsEcouleSecondes: donnees.tempsEcouleSecondes ?? null,
      });
      if (donnees.statut !== undefined && donnees.statut !== 'EN_COURS') {
        finaliser(donnees.statut, donnees.score ?? null);
      }
    };

    source.addEventListener('progression', traiter);
    source.addEventListener('statut', traiter);
    source.addEventListener('final', traiter);
    source.onmessage = traiter;
    source.onerror = () => {
      // Le flux peut se couper à la fin : on vérifie le statut réel.
      void apiFetch<Generation>(`/ecole/generations/${suiviId}`)
        .then((generation) => {
          if (generation.statut !== 'EN_COURS') {
            finaliser(generation.statut, generation.score);
          }
        })
        .catch(() => {
          if (!terminee) {
            terminee = true;
            source.close();
            setSuiviId(null);
            setProgression(null);
            invaliderListes();
          }
        });
    };

    return () => {
      source.close();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [suiviId]);

  const verifierFaisabilite = useMutation({
    mutationFn: () =>
      apiFetch<FaisabiliteRapport>('/ecole/faisabilite', { method: 'POST' }),
    onSuccess: (donnees) => setRapport(donnees),
  });

  const lancerGeneration = useMutation({
    mutationFn: () =>
      apiFetchDetaille<Generation>('/ecole/generations', {
        method: 'POST',
        body: JSON.stringify({ dureeMaxSecondes: dureeMax }),
      }),
    onSuccess: (generation) => {
      setResultatFinal(null);
      setProgression(null);
      setSuiviId(generation.id);
      invaliderListes();
    },
    onError: (erreur: Error) => {
      if (erreur instanceof ErreurApi && erreur.statut === 422) {
        const rapportErreur = extraireRapport(erreur.corps);
        if (rapportErreur !== null) {
          setRapport(rapportErreur);
        }
      }
    },
  });

  const arreterGeneration = useMutation({
    mutationFn: (id: number) =>
      apiFetch(`/ecole/generations/${id}/stop`, { method: 'POST' }),
  });

  const requeteAnalyse = useQuery({
    queryKey: ['ecole', 'generation-analyse', analyseId],
    queryFn: () =>
      apiFetch<AnalyseScore>(`/ecole/generations/${analyseId}/analyse`),
    enabled: analyseId !== null,
  });

  const generations = requeteGenerations.data ?? [];
  const faisabiliteEnEchec = rapport !== null && rapport.global === 'ECHEC';
  const nbDurs = conflitsDurs(progression?.score ?? null);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Génération</h1>
        <p className="mt-1 text-sm text-gray-500">
          Vérifiez la faisabilité puis lancez la génération automatique de
          l’emploi du temps.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card
          titre="Faisabilité"
          actions={
            <Button
              taille="sm"
              variante="secondary"
              disabled={verifierFaisabilite.isPending}
              onClick={() => verifierFaisabilite.mutate()}
            >
              {verifierFaisabilite.isPending
                ? 'Vérification…'
                : 'Vérifier la faisabilité'}
            </Button>
          }
        >
          {verifierFaisabilite.isPending ? (
            <ChargementPage />
          ) : verifierFaisabilite.error !== null ? (
            <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {verifierFaisabilite.error.message}
            </p>
          ) : rapport === null ? (
            <EmptyState message="Lancez une vérification pour contrôler les données avant génération." />
          ) : (
            <div className="space-y-3">
              <div
                className={`rounded-lg px-4 py-2.5 text-sm font-medium ${
                  rapport.global === 'OK'
                    ? 'bg-green-50 text-green-800'
                    : rapport.global === 'AVERTISSEMENT'
                      ? 'bg-amber-50 text-amber-800'
                      : 'bg-red-50 text-red-800'
                }`}
              >
                {rapport.global === 'OK'
                  ? 'Toutes les vérifications sont passées.'
                  : rapport.global === 'AVERTISSEMENT'
                    ? 'Des avertissements ont été détectés : la génération reste possible.'
                    : 'Faisabilité en échec : corrigez les points ci-dessous avant de lancer une génération.'}
              </div>
              <ul className="space-y-2">
                {rapport.bilans.map((bilan) => (
                  <li
                    key={bilan.code}
                    className="flex items-start gap-3 rounded-lg border border-gray-100 px-3 py-2"
                  >
                    <PastilleFaisabilite statut={bilan.statut} />
                    <div className="min-w-0">
                      <p className="text-sm text-gray-900">
                        <span className="mr-2 font-mono text-xs font-semibold text-gray-500">
                          {bilan.code}
                        </span>
                        {bilan.libelle}
                      </p>
                      <p className="mt-0.5 text-sm font-medium text-gray-700">
                        {bilan.message}
                      </p>
                      {bilan.details !== null && bilan.details.length > 0 && (
                        <p className="mt-0.5 text-xs text-gray-500">
                          {bilan.details}
                        </p>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </Card>

        <div className="space-y-6">
          <Card titre="Lancer une génération">
            {suiviId === null ? (
              <div className="space-y-4">
                <div>
                  <Label htmlFor="duree-max">Durée maximale de calcul</Label>
                  <Select
                    id="duree-max"
                    value={dureeMax}
                    onChange={(evenement) =>
                      setDureeMax(Number(evenement.target.value))
                    }
                  >
                    {DUREES.map((duree) => (
                      <option key={duree.valeur} value={duree.valeur}>
                        {duree.libelle}
                      </option>
                    ))}
                  </Select>
                </div>
                {faisabiliteEnEchec && (
                  <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-700">
                    La dernière vérification de faisabilité est en échec. Le
                    lancement est bloqué tant que les erreurs ne sont pas
                    corrigées.
                  </p>
                )}
                {lancerGeneration.error !== null && (
                  <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-700">
                    {lancerGeneration.error.message}
                    {lancerGeneration.error instanceof ErreurApi &&
                      lancerGeneration.error.statut === 422 &&
                      ' Le rapport de faisabilité a été mis à jour ci-contre.'}
                  </p>
                )}
                <Button
                  disabled={faisabiliteEnEchec || lancerGeneration.isPending}
                  onClick={() => lancerGeneration.mutate()}
                >
                  {lancerGeneration.isPending
                    ? 'Lancement…'
                    : 'Lancer la génération'}
                </Button>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <Spinner />
                  <div>
                    <p className="text-sm font-semibold text-gray-900">
                      Génération en cours…
                    </p>
                    <p className="text-xs text-gray-500">
                      Temps écoulé :{' '}
                      {progression?.tempsEcouleSecondes !== null &&
                      progression?.tempsEcouleSecondes !== undefined
                        ? formatSecondes(progression.tempsEcouleSecondes)
                        : '—'}{' '}
                      / {formatSecondes(dureeMax)} max
                    </p>
                  </div>
                </div>
                <dl className="grid grid-cols-2 gap-3 rounded-lg bg-gray-50 p-4 text-sm">
                  <div>
                    <dt className="text-gray-500">Score</dt>
                    <dd className="mt-0.5 font-mono font-medium text-gray-900">
                      {progression?.score ?? '—'}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-gray-500">Conflits durs</dt>
                    <dd
                      className={`mt-0.5 font-medium ${
                        nbDurs !== null && nbDurs > 0
                          ? 'text-red-700'
                          : 'text-green-700'
                      }`}
                    >
                      {nbDurs !== null ? nbDurs : '—'}
                    </dd>
                  </div>
                </dl>
                <Button
                  variante="danger"
                  disabled={arreterGeneration.isPending}
                  onClick={() => arreterGeneration.mutate(suiviId)}
                >
                  {arreterGeneration.isPending ? 'Arrêt…' : 'Arrêter'}
                </Button>
              </div>
            )}

            {resultatFinal !== null && suiviId === null && (
              <div
                className={`mt-4 rounded-lg px-4 py-3 text-sm ${
                  resultatFinal.statut === 'TERMINEE'
                    ? 'border border-green-200 bg-green-50 text-green-800'
                    : resultatFinal.statut === 'ARRETEE'
                      ? 'border border-gray-200 bg-gray-50 text-gray-700'
                      : 'border border-red-200 bg-red-50 text-red-700'
                }`}
              >
                {resultatFinal.statut === 'TERMINEE' && (
                  <>
                    <p className="font-semibold">Génération terminée.</p>
                    <p className="mt-1">
                      Score final :{' '}
                      <span className="font-mono">
                        {resultatFinal.score ?? '—'}
                      </span>
                      {' · '}
                      <Link
                        href="/ecole/planning"
                        className="font-medium underline"
                      >
                        Voir le planning
                      </Link>
                    </p>
                  </>
                )}
                {resultatFinal.statut === 'INFAISABLE' && (
                  <>
                    <p className="font-semibold">
                      Génération infaisable : des contraintes dures restent
                      violées.
                    </p>
                    <button
                      type="button"
                      onClick={() => setAnalyseId(resultatFinal.id)}
                      className="mt-1 font-medium underline"
                    >
                      Voir l’analyse des contraintes
                    </button>
                  </>
                )}
                {resultatFinal.statut === 'ECHEC' && (
                  <p className="font-semibold">
                    La génération a échoué. Réessayez ou contactez le support.
                  </p>
                )}
                {resultatFinal.statut === 'ARRETEE' && (
                  <p className="font-semibold">
                    Génération arrêtée. La meilleure solution trouvée a été
                    conservée.
                  </p>
                )}
              </div>
            )}
          </Card>

          <Card titre="Pondérations">
            <p className="text-sm text-gray-600">
              Ajustez l’importance des règles souples (rythme, équilibrage,
              préférences) prises en compte par le moteur.
            </p>
            <Link
              href="/ecole/ponderations"
              className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-indigo-600 hover:text-indigo-800"
            >
              Régler les pondérations
              <svg
                className="h-4 w-4"
                viewBox="0 0 20 20"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
              >
                <path
                  d="M7 5l5 5-5 5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </Link>
          </Card>
        </div>
      </div>

      <Card titre="Historique des générations">
        {requeteGenerations.isLoading ? (
          <ChargementPage />
        ) : requeteGenerations.error !== null ? (
          <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {requeteGenerations.error.message}
          </p>
        ) : generations.length === 0 ? (
          <EmptyState message="Aucune génération pour le moment." />
        ) : (
          <Table>
            <THead>
              <Tr>
                <Th>Date</Th>
                <Th>Durée max</Th>
                <Th>Statut</Th>
                <Th>Score</Th>
                <Th className="text-right">Analyse</Th>
              </Tr>
            </THead>
            <TBody>
              {generations.map((generation) => (
                <Tr key={generation.id}>
                  <Td>{formatDateHeure(generation.lanceeLe)}</Td>
                  <Td>{formatSecondes(generation.dureeMaxSecondes)}</Td>
                  <Td>
                    <BadgeGeneration statut={generation.statut} />
                  </Td>
                  <Td className="font-mono text-xs">
                    {generation.score ?? '—'}
                  </Td>
                  <Td className="text-right">
                    <Button
                      taille="sm"
                      variante="ghost"
                      disabled={generation.statut === 'EN_COURS'}
                      onClick={() => setAnalyseId(generation.id)}
                    >
                      Analyse
                    </Button>
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        )}
      </Card>

      <Dialog
        ouvert={analyseId !== null}
        titre="Analyse du score"
        onFermer={() => setAnalyseId(null)}
      >
        {requeteAnalyse.isLoading ? (
          <ChargementPage />
        ) : requeteAnalyse.error !== null ? (
          <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {requeteAnalyse.error.message}
          </p>
        ) : requeteAnalyse.data === undefined ? (
          <EmptyState message="Analyse indisponible." />
        ) : (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Score global :{' '}
              <span className="font-mono font-medium text-gray-900">
                {requeteAnalyse.data.score ?? '—'}
              </span>
            </p>
            {requeteAnalyse.data.contraintes.length === 0 ? (
              <EmptyState message="Aucune contrainte pénalisée." />
            ) : (
              <div className="max-h-80 overflow-y-auto">
                <Table>
                  <THead>
                    <Tr>
                      <Th>Règle</Th>
                      <Th>Violations</Th>
                      <Th>Description</Th>
                    </Tr>
                  </THead>
                  <TBody>
                    {requeteAnalyse.data.contraintes.map((contrainte) => (
                      <Tr key={contrainte.regle}>
                        <Td>{contrainte.regle}</Td>
                        <Td>{contrainte.nombreViolations}</Td>
                        <Td className="text-sm">{contrainte.libelle}</Td>
                      </Tr>
                    ))}
                  </TBody>
                </Table>
              </div>
            )}
          </div>
        )}
      </Dialog>
    </div>
  );
}
