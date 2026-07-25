'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { apiFetch } from '@/lib/api';
import type { Ponderation } from '@/lib/types-planning';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { ChargementPage } from '@/components/ui/spinner';

const LIBELLES_FAMILLES: Record<string, string> = {
  RYTHME: 'Rythme (G-*)',
  EQUILIBRAGE: 'Équilibrage (F-*)',
  PREFERENCES: 'Préférences (D-* / E-*)',
};

const ORDRE_FAMILLES = ['RYTHME', 'EQUILIBRAGE', 'PREFERENCES'];

function libelleFamille(famille: string): string {
  return LIBELLES_FAMILLES[famille] ?? famille;
}

export default function PagePonderations() {
  const clientQuery = useQueryClient();
  const [poidsParRegle, setPoidsParRegle] = useState<Record<string, number>>(
    {},
  );
  const [enregistre, setEnregistre] = useState(false);

  const requetePonderations = useQuery({
    queryKey: ['ecole', 'ponderations'],
    queryFn: () => apiFetch<Ponderation[]>('/ecole/ponderations'),
  });

  useEffect(() => {
    if (requetePonderations.data === undefined) return;
    const valeurs: Record<string, number> = {};
    for (const ponderation of requetePonderations.data) {
      valeurs[ponderation.regle] = ponderation.poids;
    }
    setPoidsParRegle(valeurs);
  }, [requetePonderations.data]);

  const enregistrer = useMutation({
    mutationFn: () =>
      apiFetch('/ecole/ponderations', {
        method: 'PUT',
        body: JSON.stringify(
          Object.entries(poidsParRegle).map(([regle, poids]) => ({
            regle,
            poids,
          })),
        ),
      }),
    onSuccess: () => {
      setEnregistre(true);
      void clientQuery.invalidateQueries({
        queryKey: ['ecole', 'ponderations'],
      });
    },
  });

  if (requetePonderations.isLoading) {
    return <ChargementPage />;
  }

  if (requetePonderations.error !== null) {
    return (
      <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {requetePonderations.error.message}
      </p>
    );
  }

  const ponderations = requetePonderations.data ?? [];

  const familles = Array.from(
    new Set(ponderations.map((ponderation) => ponderation.famille)),
  ).sort((a, b) => {
    const indexA = ORDRE_FAMILLES.indexOf(a);
    const indexB = ORDRE_FAMILLES.indexOf(b);
    if (indexA !== -1 && indexB !== -1) return indexA - indexB;
    if (indexA !== -1) return -1;
    if (indexB !== -1) return 1;
    return a.localeCompare(b, 'fr');
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Pondérations</h1>
          <p className="mt-1 text-sm text-gray-500">
            Réglez l’importance de chaque règle souple pour le moteur de
            génération.
          </p>
        </div>
        <Link
          href="/ecole/generation"
          className="text-sm font-medium text-indigo-600 hover:text-indigo-800"
        >
          ← Retour à la génération
        </Link>
      </div>

      <p className="rounded-lg border border-indigo-100 bg-indigo-50 px-4 py-3 text-sm text-indigo-900">
        Les règles dures (conflits d’enseignants, de salles, de groupes…) ne
        sont pas paramétrables : elles sont toujours strictement respectées.
      </p>

      {ponderations.length === 0 ? (
        <EmptyState message="Aucune règle souple disponible." />
      ) : (
        <>
          {familles.map((famille) => (
            <Card key={famille} titre={libelleFamille(famille)}>
              <ul className="divide-y divide-gray-100">
                {ponderations
                  .filter((ponderation) => ponderation.famille === famille)
                  .map((ponderation) => {
                    const valeur =
                      poidsParRegle[ponderation.regle] ?? ponderation.poids;
                    return (
                      <li
                        key={ponderation.regle}
                        className="flex flex-wrap items-center gap-4 py-3"
                      >
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-gray-900">
                            <span className="mr-2 font-mono text-xs font-semibold text-gray-500">
                              {ponderation.regle}
                            </span>
                            {ponderation.libelle}
                          </p>
                        </div>
                        <div className="flex w-full items-center gap-3 sm:w-72">
                          <input
                            type="range"
                            min={0}
                            max={100}
                            step={1}
                            value={valeur}
                            aria-label={`Poids de la règle ${ponderation.regle}`}
                            onChange={(evenement) => {
                              setEnregistre(false);
                              setPoidsParRegle((precedent) => ({
                                ...precedent,
                                [ponderation.regle]: Number(
                                  evenement.target.value,
                                ),
                              }));
                            }}
                            className="h-2 w-full cursor-pointer accent-indigo-600"
                          />
                          <span className="w-10 shrink-0 text-right text-sm font-semibold tabular-nums text-gray-900">
                            {valeur}
                          </span>
                        </div>
                      </li>
                    );
                  })}
              </ul>
            </Card>
          ))}

          <div className="flex items-center gap-3">
            <Button
              disabled={enregistrer.isPending}
              onClick={() => enregistrer.mutate()}
            >
              {enregistrer.isPending
                ? 'Enregistrement…'
                : 'Enregistrer les pondérations'}
            </Button>
            {enregistre && (
              <span className="text-sm font-medium text-green-700">
                Pondérations enregistrées.
              </span>
            )}
            {enregistrer.error !== null && (
              <span className="text-sm text-red-700">
                {enregistrer.error.message}
              </span>
            )}
          </div>
        </>
      )}
    </div>
  );
}
