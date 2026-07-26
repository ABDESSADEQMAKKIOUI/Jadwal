'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import { apiFetch } from '@/lib/api';
import { apiFetchDetaille, ErreurApi, extraireRapport } from '@/lib/api-planning';
import type {
  AnalyseScore,
  FaisabiliteRapport,
  Generation,
  StatutFaisabilite,
  StatutGeneration,
} from '@/lib/types-planning';
import type { BadgeTone } from '@/components/ds';
import { Alert, Badge, Button, Card, EmptyState, Select } from '@/components/ds';
import { Dialog } from '@/components/ui/dialog';
import { ChargementPage } from '@/components/ui/spinner';

const DUREES = [
  { valeur: 30, libelle: '30 secondes' },
  { valeur: 60, libelle: '1 minute' },
  { valeur: 120, libelle: '2 minutes' },
  { valeur: 300, libelle: '5 minutes' },
  { valeur: 600, libelle: '10 minutes' },
];

const BADGE_STATUT: Record<StatutGeneration, { tone: BadgeTone; libelle: string }> = {
  EN_COURS: { tone: 'active', libelle: 'EN COURS' },
  TERMINEE: { tone: 'success', libelle: 'TERMINÉE' },
  INFAISABLE: { tone: 'danger', libelle: 'INFAISABLE' },
  ECHEC: { tone: 'danger', libelle: 'ÉCHEC' },
  ARRETEE: { tone: 'neutral', libelle: 'ARRÊTÉE' },
};

const PASTILLE_FAISABILITE: Record<StatutFaisabilite, string> = {
  OK: 'var(--status-success-solid)',
  AVERTISSEMENT: 'var(--status-warning-solid)',
  ECHEC: 'var(--status-danger-solid)',
};

const ALERTE_FAISABILITE: Record<
  StatutFaisabilite,
  { tone: 'success' | 'warning' | 'danger'; message: string }
> = {
  OK: { tone: 'success', message: 'Toutes les vérifications sont passées.' },
  AVERTISSEMENT: {
    tone: 'warning',
    message: 'Des avertissements ont été détectés : la génération reste possible.',
  },
  ECHEC: {
    tone: 'danger',
    message:
      'Faisabilité en échec : corrigez les points ci-dessous avant de lancer une génération.',
  },
};

/* Styles repris mot pour mot de la maquette « JADWAL Ynexis ». */

const TITRE_CARTE: CSSProperties = {
  margin: 0,
  fontSize: 'var(--type-card-title-size)',
  fontWeight: 'var(--type-card-title-weight)',
  color: 'var(--text-strong)',
};

const ENTETE_CARTE: CSSProperties = {
  padding: '18px 24px',
  borderBottom: '1px solid var(--border-subtle)',
};

const ETIQUETTE_TUILE: CSSProperties = {
  margin: 0,
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
};

const TH: CSSProperties = {
  padding: '10px 14px',
  background: 'var(--neutral-50)',
  borderBottom: '1px solid var(--border-subtle)',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
  whiteSpace: 'nowrap',
  textAlign: 'left',
};

const TH_RIGHT: CSSProperties = { ...TH, textAlign: 'right' };

const TD: CSSProperties = {
  padding: '0 14px',
  height: 'var(--row-height)',
  borderBottom: '1px solid var(--neutral-100)',
  color: 'var(--text-body)',
  whiteSpace: 'nowrap',
  textAlign: 'left',
};

const TD_RIGHT: CSSProperties = {
  ...TD,
  textAlign: 'right',
  fontVariantNumeric: 'tabular-nums',
};

const TD_MONO: CSSProperties = {
  ...TD,
  fontFamily: 'var(--font-mono)',
  fontSize: 'var(--text-xs)',
  fontVariantNumeric: 'tabular-nums',
  color: 'var(--text-muted)',
};

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

/** Pastille de statut d'un bilan de faisabilité (H-01…H-06). */
function PastilleFaisabilite({ statut }: { statut: StatutFaisabilite }) {
  return (
    <span
      aria-label={statut}
      style={{
        marginTop: '5px',
        display: 'inline-block',
        width: '8px',
        height: '8px',
        flex: 'none',
        borderRadius: 'var(--radius-pill)',
        background: PASTILLE_FAISABILITE[statut],
      }}
    />
  );
}

/** Résumé du rapport affiché en pastille dans l'en-tête de la carte Faisabilité. */
function badgeFaisabilite(
  rapport: FaisabiliteRapport,
): { tone: BadgeTone; libelle: string } {
  const echecs = rapport.bilans.filter((bilan) => bilan.statut === 'ECHEC').length;
  const avertissements = rapport.bilans.filter(
    (bilan) => bilan.statut === 'AVERTISSEMENT',
  ).length;
  if (rapport.global === 'ECHEC') {
    return {
      tone: 'danger',
      libelle: `${echecs} ${echecs > 1 ? 'ERREURS' : 'ERREUR'}`,
    };
  }
  if (rapport.global === 'AVERTISSEMENT') {
    return {
      tone: 'warning',
      libelle: `${avertissements} ${avertissements > 1 ? 'AVERTISSEMENTS' : 'AVERTISSEMENT'}`,
    };
  }
  return {
    tone: 'success',
    libelle: `${rapport.bilans.length} ${rapport.bilans.length > 1 ? 'CONTRÔLES OK' : 'CONTRÔLE OK'}`,
  };
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
  const enCours = suiviId !== null;

  // Durée de référence de la génération suivie (peut différer du sélecteur
  // après un rechargement de page qui reprend un suivi en cours).
  const generationSuivie =
    generations.find((generation) => generation.id === suiviId) ?? null;
  const dureeSuivie = generationSuivie?.dureeMaxSecondes ?? dureeMax;
  const secondesEcoulees = progression?.tempsEcouleSecondes ?? null;
  const pourcentage =
    secondesEcoulees === null
      ? 0
      : Math.min(100, Math.round((secondesEcoulees / dureeSuivie) * 100));

  // Tuile « Conflits durs » : verte à 0, rouge dès qu'une contrainte dure est violée.
  const tuileConflits =
    nbDurs === null
      ? { bg: 'var(--surface-sunken)', fg: 'var(--text-muted)' }
      : nbDurs > 0
        ? { bg: 'var(--status-danger-bg)', fg: 'var(--status-danger-fg)' }
        : { bg: 'var(--status-success-bg)', fg: 'var(--status-success-fg)' };

  const badge = rapport === null ? null : badgeFaisabilite(rapport);
  const alerteGlobale = rapport === null ? null : ALERTE_FAISABILITE[rapport.global];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <p
        style={{
          margin: 0,
          fontSize: 'var(--text-base)',
          color: 'var(--text-muted)',
          maxWidth: '62ch',
        }}
      >
        Vérifiez la faisabilité puis lancez la génération automatique de l’emploi du
        temps.
      </p>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 400px',
          gap: '16px',
          alignItems: 'start',
        }}
      >
        {/* ---------- Faisabilité (H-01…H-06) ---------- */}
        <Card padded={false}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '16px',
              padding: '18px 24px',
              borderBottom: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <h3 style={TITRE_CARTE}>Faisabilité</h3>
              {badge !== null && (
                <Badge tone={badge.tone} dot size="sm">
                  {badge.libelle}
                </Badge>
              )}
            </div>
            <Button
              variant="secondary"
              size="sm"
              loading={verifierFaisabilite.isPending}
              onClick={() => verifierFaisabilite.mutate()}
            >
              {rapport === null ? 'Vérifier la faisabilité' : 'Revérifier'}
            </Button>
          </div>
          <div style={{ padding: '16px 24px 24px' }}>
            {verifierFaisabilite.isPending ? (
              <ChargementPage />
            ) : verifierFaisabilite.error !== null ? (
              <Alert tone="danger" title="La vérification a échoué">
                {verifierFaisabilite.error.message}
              </Alert>
            ) : rapport === null || alerteGlobale === null ? (
              <EmptyState
                variant="gated"
                title="Faisabilité non vérifiée"
                description="Lancez une vérification pour contrôler les données avant génération."
              />
            ) : (
              <>
                <Alert tone={alerteGlobale.tone}>{alerteGlobale.message}</Alert>
                <ul
                  style={{
                    margin: '16px 0 0',
                    padding: 0,
                    listStyle: 'none',
                    display: 'flex',
                    flexDirection: 'column',
                  }}
                >
                  {rapport.bilans.map((bilan) => (
                    <li
                      key={bilan.code}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '12px',
                        padding: '14px 0',
                        borderTop: '1px solid var(--neutral-100)',
                      }}
                    >
                      <PastilleFaisabilite statut={bilan.statut} />
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
                              marginRight: '8px',
                              fontFamily: 'var(--font-mono)',
                              fontSize: 'var(--text-xs)',
                              fontWeight: 'var(--weight-semibold)',
                              color: 'var(--text-subtle)',
                            }}
                          >
                            {bilan.code}
                          </span>
                          {bilan.libelle}
                        </p>
                        <p
                          style={{
                            margin: '4px 0 0',
                            fontSize: 'var(--text-sm)',
                            color: 'var(--text-body)',
                          }}
                        >
                          {bilan.message}
                        </p>
                        {bilan.details !== null && bilan.details.length > 0 && (
                          <p
                            style={{
                              margin: '2px 0 0',
                              fontSize: 'var(--text-xs)',
                              color: 'var(--text-muted)',
                            }}
                          >
                            {bilan.details}
                          </p>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </div>
        </Card>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* ---------- Lancement / suivi en direct ---------- */}
          <Card padded={false}>
            <div style={ENTETE_CARTE}>
              <h3 style={TITRE_CARTE}>
                {enCours ? 'Génération en cours' : 'Lancer une génération'}
              </h3>
            </div>
            <div style={{ padding: '20px 24px 24px' }}>
              {enCours && suiviId !== null ? (
                <>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span
                      aria-hidden="true"
                      style={{
                        display: 'inline-block',
                        width: '8px',
                        height: '8px',
                        borderRadius: 'var(--radius-pill)',
                        background: 'var(--status-active-solid)',
                      }}
                    />
                    <span
                      style={{
                        fontSize: 'var(--text-base)',
                        fontWeight: 'var(--weight-medium)',
                        color: 'var(--text-strong)',
                      }}
                    >
                      Recherche de solution…
                    </span>
                  </div>
                  <div
                    style={{
                      marginTop: '14px',
                      height: '6px',
                      borderRadius: 'var(--radius-pill)',
                      background: 'var(--surface-sunken)',
                      overflow: 'hidden',
                    }}
                  >
                    <div
                      style={{
                        display: 'block',
                        height: '100%',
                        width: `${pourcentage}%`,
                        borderRadius: 'var(--radius-pill)',
                        background: 'var(--color-primary)',
                      }}
                    />
                  </div>
                  <p
                    style={{
                      margin: '8px 0 0',
                      fontSize: 'var(--text-xs)',
                      color: 'var(--text-muted)',
                      fontVariantNumeric: 'tabular-nums',
                    }}
                  >
                    {secondesEcoulees === null
                      ? 'Démarrage…'
                      : `${formatSecondes(secondesEcoulees)} écoulées`}{' '}
                    · {formatSecondes(dureeSuivie)} max
                  </p>

                  <div
                    style={{
                      marginTop: '18px',
                      display: 'grid',
                      gridTemplateColumns: '1fr 1fr',
                      gap: '10px',
                    }}
                  >
                    <div
                      style={{
                        borderRadius: 'var(--radius-sm)',
                        background: 'var(--surface-sunken)',
                        padding: '12px',
                      }}
                    >
                      <p style={ETIQUETTE_TUILE}>Score</p>
                      <p
                        style={{
                          margin: '6px 0 0',
                          fontFamily: 'var(--font-mono)',
                          fontSize: 'var(--text-base)',
                          fontWeight: 'var(--weight-semibold)',
                          color: 'var(--text-strong)',
                        }}
                      >
                        {progression?.score ?? '—'}
                      </p>
                    </div>
                    <div
                      style={{
                        borderRadius: 'var(--radius-sm)',
                        background: tuileConflits.bg,
                        padding: '12px',
                      }}
                    >
                      <p style={{ ...ETIQUETTE_TUILE, color: tuileConflits.fg }}>
                        Conflits durs
                      </p>
                      <p
                        style={{
                          margin: '6px 0 0',
                          fontSize: 'var(--text-base)',
                          fontWeight: 'var(--weight-semibold)',
                          fontVariantNumeric: 'tabular-nums',
                          color: tuileConflits.fg,
                        }}
                      >
                        {nbDurs ?? '—'}
                      </p>
                    </div>
                  </div>
                  <div style={{ marginTop: '16px' }}>
                    <Button
                      variant="danger"
                      size="md"
                      fullWidth
                      loading={arreterGeneration.isPending}
                      onClick={() => arreterGeneration.mutate(suiviId)}
                    >
                      Arrêter la génération
                    </Button>
                  </div>
                </>
              ) : (
                <div
                  style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}
                >
                  <Select
                    label="Durée maximale de calcul"
                    id="duree-max"
                    value={dureeMax}
                    onChange={(evenement) =>
                      setDureeMax(Number(evenement.target.value))
                    }
                    options={DUREES.map((duree) => ({
                      value: duree.valeur,
                      label: duree.libelle,
                    }))}
                  />
                  {faisabiliteEnEchec && (
                    <Alert tone="danger" title="Lancement bloqué">
                      La dernière vérification de faisabilité est en échec. Le
                      lancement est bloqué tant que les erreurs ne sont pas
                      corrigées.
                    </Alert>
                  )}
                  {lancerGeneration.error !== null && (
                    <Alert tone="danger" title="Lancement impossible">
                      {lancerGeneration.error.message}
                      {lancerGeneration.error instanceof ErreurApi &&
                        lancerGeneration.error.statut === 422 &&
                        ' Le rapport de faisabilité a été mis à jour ci-contre.'}
                    </Alert>
                  )}
                  <Button
                    size="md"
                    fullWidth
                    disabled={faisabiliteEnEchec}
                    loading={lancerGeneration.isPending}
                    onClick={() => lancerGeneration.mutate()}
                  >
                    Lancer la génération
                  </Button>
                </div>
              )}

              {resultatFinal !== null && !enCours && (
                <div style={{ marginTop: '16px' }}>
                  {resultatFinal.statut === 'TERMINEE' && (
                    <Alert
                      tone="success"
                      title="Génération terminée."
                      onClose={() => setResultatFinal(null)}
                    >
                      Score final :{' '}
                      <span style={{ fontFamily: 'var(--font-mono)' }}>
                        {resultatFinal.score ?? '—'}
                      </span>
                      {' · '}
                      <Link
                        href="/ecole/planning"
                        style={{
                          color: 'inherit',
                          fontWeight: 'var(--weight-medium)',
                          textDecoration: 'underline',
                        }}
                      >
                        Voir le planning
                      </Link>
                    </Alert>
                  )}
                  {resultatFinal.statut === 'INFAISABLE' && (
                    <Alert
                      tone="danger"
                      title="Génération infaisable : des contraintes dures restent violées."
                      onClose={() => setResultatFinal(null)}
                    >
                      <button
                        type="button"
                        onClick={() => setAnalyseId(resultatFinal.id)}
                        style={{
                          background: 'none',
                          border: 0,
                          padding: 0,
                          color: 'inherit',
                          font: 'inherit',
                          fontWeight: 'var(--weight-medium)',
                          textDecoration: 'underline',
                          cursor: 'pointer',
                        }}
                      >
                        Voir l’analyse des contraintes
                      </button>
                    </Alert>
                  )}
                  {resultatFinal.statut === 'ECHEC' && (
                    <Alert
                      tone="danger"
                      title="La génération a échoué."
                      onClose={() => setResultatFinal(null)}
                    >
                      Réessayez ou contactez le support.
                    </Alert>
                  )}
                  {resultatFinal.statut === 'ARRETEE' && (
                    <Alert
                      tone="neutral"
                      title="Génération arrêtée."
                      onClose={() => setResultatFinal(null)}
                    >
                      La meilleure solution trouvée a été conservée.
                    </Alert>
                  )}
                </div>
              )}
            </div>
          </Card>

          {/* ---------- Pondérations ---------- */}
          <Card padded={false}>
            <div style={{ padding: '18px 24px' }}>
              <h3 style={TITRE_CARTE}>Pondérations</h3>
              <p
                style={{
                  margin: '8px 0 0',
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-muted)',
                }}
              >
                Ajustez l’importance des règles souples (rythme, équilibrage,
                préférences) prises en compte par le moteur.
              </p>
              <Link
                href="/ecole/ponderations"
                style={{
                  marginTop: '12px',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  fontSize: 'var(--text-base)',
                  fontWeight: 'var(--weight-medium)',
                  color: 'var(--text-link)',
                }}
              >
                Régler les pondérations
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="m9 18 6-6-6-6" />
                </svg>
              </Link>
            </div>
          </Card>
        </div>
      </div>

      {/* ---------- Historique des générations ---------- */}
      <Card padded={false} style={{ overflow: 'hidden' }}>
        <div style={ENTETE_CARTE}>
          <h3 style={TITRE_CARTE}>Historique des générations</h3>
        </div>
        {requeteGenerations.isLoading ? (
          <ChargementPage />
        ) : requeteGenerations.error !== null ? (
          <div style={{ padding: '16px 24px 24px' }}>
            <Alert tone="danger" title="Historique indisponible">
              {requeteGenerations.error.message}
            </Alert>
          </div>
        ) : generations.length === 0 ? (
          <EmptyState
            title="Aucune génération pour le moment"
            description="Vérifiez la faisabilité puis lancez une première génération : l’historique se remplira ici."
          />
        ) : (
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: 'var(--text-sm)',
            }}
          >
            <thead>
              <tr>
                <th style={TH}>Date</th>
                <th style={TH}>Durée max</th>
                <th style={TH}>Statut</th>
                <th style={TH}>Score</th>
                <th style={TH_RIGHT}>Analyse</th>
              </tr>
            </thead>
            <tbody>
              {generations.map((generation) => (
                <tr key={generation.id}>
                  <td style={TD}>{formatDateHeure(generation.lanceeLe)}</td>
                  <td style={TD}>{formatSecondes(generation.dureeMaxSecondes)}</td>
                  <td style={TD}>
                    <Badge tone={BADGE_STATUT[generation.statut].tone} dot size="sm">
                      {BADGE_STATUT[generation.statut].libelle}
                    </Badge>
                  </td>
                  <td style={TD_MONO}>{generation.score ?? '—'}</td>
                  <td style={TD_RIGHT}>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={generation.statut === 'EN_COURS'}
                      onClick={() => setAnalyseId(generation.id)}
                    >
                      Analyse
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {/* ---------- Analyse du score par règle ---------- */}
      <Dialog
        ouvert={analyseId !== null}
        titre="Analyse du score"
        onFermer={() => setAnalyseId(null)}
      >
        {requeteAnalyse.isLoading ? (
          <ChargementPage />
        ) : requeteAnalyse.error !== null ? (
          <Alert tone="danger" title="Analyse indisponible">
            {requeteAnalyse.error.message}
          </Alert>
        ) : requeteAnalyse.data === undefined ? (
          <EmptyState variant="gated" title="Analyse indisponible" />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <p
              style={{
                margin: 0,
                fontSize: 'var(--text-sm)',
                color: 'var(--text-muted)',
              }}
            >
              Score global :{' '}
              <span
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontWeight: 'var(--weight-semibold)',
                  color: 'var(--text-strong)',
                }}
              >
                {requeteAnalyse.data.score ?? '—'}
              </span>
            </p>
            {requeteAnalyse.data.contraintes.length === 0 ? (
              <EmptyState
                variant="gated"
                title="Aucune contrainte pénalisée"
                description="Le moteur n’a relevé aucune pénalité sur cette génération."
              />
            ) : (
              <div style={{ maxHeight: '320px', overflowY: 'auto' }}>
                <table
                  style={{
                    width: '100%',
                    borderCollapse: 'collapse',
                    fontSize: 'var(--text-sm)',
                  }}
                >
                  <thead>
                    <tr>
                      <th style={TH}>Règle</th>
                      <th style={TH_RIGHT}>Violations</th>
                      <th style={TH}>Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    {requeteAnalyse.data.contraintes.map((contrainte) => (
                      <tr key={contrainte.regle}>
                        <td style={TD_MONO}>{contrainte.regle}</td>
                        <td style={TD_RIGHT}>{contrainte.nombreViolations}</td>
                        <td style={{ ...TD, whiteSpace: 'normal' }}>
                          {contrainte.libelle}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </Dialog>
    </div>
  );
}
