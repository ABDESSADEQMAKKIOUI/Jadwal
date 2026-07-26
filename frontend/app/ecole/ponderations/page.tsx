'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState, type CSSProperties } from 'react';
import { apiFetch } from '@/lib/api';
import type { Ponderation } from '@/lib/types-planning';
import { Alert, Button, Card, EmptyState } from '@/components/ds';
import { ChargementPage } from '@/components/ui/spinner';

const LIBELLES_FAMILLES: Record<string, string> = {
  RYTHME: 'Rythme · G-*',
  EQUILIBRAGE: 'Équilibrage · F-*',
  PREFERENCES: 'Préférences · D-* / E-*',
};

const ORDRE_FAMILLES = ['RYTHME', 'EQUILIBRAGE', 'PREFERENCES'];

function libelleFamille(famille: string): string {
  return LIBELLES_FAMILLES[famille] ?? famille;
}

/** Pastille de valeur : teal dès 70, neutre en dessous (règle de la maquette). */
function stylePastille(poids: number): CSSProperties {
  const fort = poids >= 70;
  return {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 38,
    borderRadius: 'var(--radius-xs)',
    padding: '3px 8px',
    fontSize: 'var(--text-sm)',
    fontWeight: 'var(--weight-semibold)',
    fontVariantNumeric: 'tabular-nums',
    background: fort ? 'var(--surface-selected)' : 'var(--surface-sunken)',
    color: fort ? 'var(--teal-700)' : 'var(--text-body)',
  };
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
      <Alert tone="danger" title="Chargement impossible">
        {requetePonderations.error.message}
      </Alert>
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
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <p
          style={{
            margin: 0,
            fontSize: 'var(--text-base)',
            color: 'var(--text-muted)',
            maxWidth: '62ch',
          }}
        >
          Réglez l’importance de chaque règle souple pour le moteur de
          génération.
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {enregistre && !enregistrer.isPending && (
            <span
              style={{
                fontSize: 'var(--text-sm)',
                fontWeight: 'var(--weight-medium)',
                color: 'var(--status-success-fg)',
              }}
            >
              Pondérations enregistrées.
            </span>
          )}
          <Button
            variant="primary"
            size="md"
            loading={enregistrer.isPending}
            disabled={ponderations.length === 0}
            onClick={() => enregistrer.mutate()}
          >
            Enregistrer les pondérations
          </Button>
        </div>
      </div>

      {enregistrer.error !== null && (
        <Alert tone="danger" title="Enregistrement impossible">
          {enregistrer.error.message}
        </Alert>
      )}

      <Alert tone="neutral">
        Les règles dures (conflits d’enseignants, de salles, de groupes…) ne
        sont pas paramétrables : elles sont toujours strictement respectées.
      </Alert>

      {ponderations.length === 0 ? (
        <Card padded={false}>
          <EmptyState
            variant="gated"
            title="Aucune règle souple"
            description="Aucune règle souple n’est disponible pour cet établissement."
          />
        </Card>
      ) : (
        familles.map((famille) => (
          <Card key={famille} padded={false}>
            <div
              style={{
                padding: '18px 24px',
                borderBottom: '1px solid var(--border-subtle)',
              }}
            >
              <h3
                style={{
                  margin: 0,
                  fontSize: 'var(--type-card-title-size)',
                  fontWeight: 'var(--type-card-title-weight)',
                  color: 'var(--text-strong)',
                }}
              >
                {libelleFamille(famille)}
              </h3>
            </div>
            <div style={{ padding: '6px 24px 18px' }}>
              {ponderations
                .filter((ponderation) => ponderation.famille === famille)
                .map((ponderation) => {
                  const valeur =
                    poidsParRegle[ponderation.regle] ?? ponderation.poids;
                  return (
                    <div
                      key={ponderation.regle}
                      style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        alignItems: 'center',
                        gap: 20,
                        padding: '14px 0',
                        borderBottom: '1px solid var(--neutral-100)',
                      }}
                    >
                      <div style={{ minWidth: 0, flex: 1 }}>
                        <p
                          style={{
                            margin: 0,
                            fontSize: 'var(--text-base)',
                            color: 'var(--text-strong)',
                          }}
                        >
                          <span
                            style={{
                              marginRight: 8,
                              fontFamily: 'var(--font-mono)',
                              fontSize: 'var(--text-xs)',
                              fontWeight: 'var(--weight-semibold)',
                              color: 'var(--text-subtle)',
                            }}
                          >
                            {ponderation.regle}
                          </span>
                          {ponderation.libelle}
                        </p>
                      </div>
                      <div
                        style={{
                          display: 'flex',
                          width: 300,
                          maxWidth: '100%',
                          alignItems: 'center',
                          gap: 14,
                        }}
                      >
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
                          style={{
                            flex: 1,
                            minWidth: 0,
                            height: 4,
                            cursor: 'pointer',
                            accentColor: 'var(--color-primary)',
                          }}
                        />
                        <span style={stylePastille(valeur)}>{valeur}</span>
                      </div>
                    </div>
                  );
                })}
            </div>
          </Card>
        ))
      )}
    </div>
  );
}
