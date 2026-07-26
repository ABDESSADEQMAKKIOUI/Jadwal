'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import {
  useEffect,
  useState,
  type CSSProperties,
  type FormEvent,
} from 'react';
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
import {
  Alert,
  Badge,
  Button,
  Card,
  EmptyState,
  Input,
  KpiTile,
  Select,
} from '@/components/ds';
import { Dialog } from '@/components/ui/dialog';
import { ChargementPage } from '@/components/ui/spinner';

const LIBELLES_JOURS: Record<Jour, string> = {
  LUNDI: 'Lundi',
  MARDI: 'Mardi',
  MERCREDI: 'Mercredi',
  JEUDI: 'Jeudi',
  VENDREDI: 'Vendredi',
  SAMEDI: 'Samedi',
  DIMANCHE: 'Dimanche',
};

/* ------------------------------------------------------------------ */
/* Styles de la maquette « JADWAL Ynexis » (écran Fiche enseignant).  */
/* Repris mot pour mot : thMini / thMiniC / tdMini / cellBase.        */
/* ------------------------------------------------------------------ */

const RETOUR: CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 4,
  fontSize: 'var(--text-sm)',
  color: 'var(--text-muted)',
};

const ENTETE_CARTE: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 16,
  padding: '18px 24px',
  borderBottom: '1px solid var(--border-subtle)',
};

const TITRE_CARTE: CSSProperties = {
  margin: 0,
  fontSize: 'var(--type-card-title-size)',
  fontWeight: 'var(--type-card-title-weight)',
  color: 'var(--text-strong)',
};

const CORPS_CARTE: CSSProperties = { padding: '18px 24px 24px' };

const TH_MINI: CSSProperties = {
  width: 56,
  padding: '2px 4px',
  textAlign: 'left',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  color: 'var(--text-muted)',
};

const TH_MINI_C: CSSProperties = {
  padding: '2px 4px',
  textAlign: 'center',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
};

const TD_MINI: CSSProperties = {
  whiteSpace: 'nowrap',
  padding: '0 6px 0 0',
  fontSize: 'var(--text-2xs)',
  fontVariantNumeric: 'tabular-nums',
  color: 'var(--text-subtle)',
};

const CELLULE_BASE: CSSProperties = {
  height: 26,
  minWidth: 72,
  borderRadius: 'var(--radius-xs)',
};

const ENVELOPPE_GRILLE: CSSProperties = {
  overflowX: 'auto',
  userSelect: 'none',
};

const TABLE_MINI: CSSProperties = {
  minWidth: '100%',
  borderCollapse: 'separate',
  borderSpacing: 2,
};

const LEGENDE: CSSProperties = {
  marginTop: 14,
  display: 'flex',
  flexWrap: 'wrap',
  gap: 16,
  fontSize: 'var(--text-xs)',
  color: 'var(--text-muted)',
};

const LEGENDE_ITEM: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 6,
};

function pastilleLegende(fond: string, bordure?: string): CSSProperties {
  return {
    display: 'inline-block',
    height: 12,
    width: 12,
    borderRadius: 'var(--radius-xs)',
    background: fond,
    ...(bordure === undefined ? {} : { border: `1px solid ${bordure}` }),
  };
}

/** Micro-titre de sous-section (idiome des libellés capitales du DS). */
const SOUS_TITRE: CSSProperties = {
  margin: '18px 0 8px',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
};

/** Ligne de liste bordée (idiome « versions » de la maquette). */
const LIGNE_LISTE: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  flexWrap: 'wrap',
  gap: 12,
  border: '1px solid var(--border-subtle)',
  borderRadius: 'var(--radius-sm)',
  padding: '10px 12px',
};

const PUCE_MATIERE: CSSProperties = {
  display: 'inline-block',
  width: 10,
  height: 10,
  flex: 'none',
  borderRadius: 3,
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

/** Deux initiales au plus — même calcul que la maquette. */
function initiales(nom: string): string {
  return nom
    .split(' ')
    .filter(Boolean)
    .map((partie) => partie[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
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
  const [survolIndispo, setSurvolIndispo] = useState<string | null>(null);
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
  const [survolPref, setSurvolPref] = useState<string | null>(null);
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
      <Alert tone="danger" title="Chargement impossible">
        {error.message}
      </Alert>
    );
  }

  const enseignant = (enseignants ?? []).find(
    (element) => String(element.id) === id,
  );
  if (enseignant === undefined) {
    return (
      <Card>
        <EmptyState
          variant="gated"
          title="Enseignant introuvable"
          description="Cet enseignant n’existe pas ou ne fait pas partie de votre établissement."
          action={
            <Link href="/ecole/enseignants" style={RETOUR}>
              <FlecheRetour />
              Retour aux enseignants
            </Link>
          }
        />
      </Card>
    );
  }

  const indices =
    grille !== undefined
      ? Array.from({ length: grille.unitesParJour }, (_valeur, i) => i)
      : [];

  const quota = enseignant.quotaHebdoUnites;
  const pourcentageCharge =
    quota > 0 ? Math.round((enseignant.chargeAffectee / quota) * 100) : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* En-tête de l'écran de détail : rendu DANS le contenu (maquette). */}
      <div>
        <Link href="/ecole/enseignants" style={RETOUR}>
          <FlecheRetour />
          Retour aux enseignants
        </Link>
        <div
          style={{
            marginTop: 12,
            display: 'flex',
            alignItems: 'center',
            gap: 14,
          }}
        >
          <span
            aria-hidden="true"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 48,
              height: 48,
              flex: 'none',
              borderRadius: 'var(--radius-pill)',
              background: 'var(--teal-100)',
              color: 'var(--teal-700)',
              fontSize: 'var(--text-md)',
              fontWeight: 'var(--weight-semibold)',
            }}
          >
            {initiales(enseignant.nomComplet)}
          </span>
          <div style={{ minWidth: 0 }}>
            <h2
              style={{
                margin: 0,
                fontSize: 'var(--type-page-title-size)',
                fontWeight: 'var(--type-page-title-weight)',
                letterSpacing: 'var(--tracking-tight)',
                color: 'var(--text-strong)',
              }}
            >
              {enseignant.nomComplet}
            </h2>
            <p
              style={{
                margin: '4px 0 0',
                fontSize: 'var(--text-base)',
                color: 'var(--text-muted)',
              }}
            >
              {enseignant.email}
              {enseignant.matieres.length > 0
                ? ` · ${enseignant.matieres.join(', ')}`
                : ''}
            </p>
          </div>
          {/* Type et statut vacataire : mêmes pastilles que la liste. */}
          <span
            style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
          >
            <Badge
              tone={enseignant.type === 'MIXTE' ? 'active' : 'info'}
              size="sm"
            >
              {enseignant.type}
            </Badge>
            {enseignant.vacataire && (
              <>
                <Badge tone="info" size="sm">
                  VACATAIRE
                </Badge>
                <span
                  style={{
                    fontSize: 'var(--text-xs)',
                    color: 'var(--text-subtle)',
                  }}
                >
                  buffer trajet {formatUnites(enseignant.bufferTrajetUnites)}
                </span>
              </>
            )}
          </span>
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
          gap: 16,
        }}
      >
        <KpiTile
          label="Quota hebdomadaire"
          value={formatUnites(quota)}
          hint={`${quota} unités`}
        />
        <KpiTile
          label="Charge affectée"
          value={formatUnites(enseignant.chargeAffectee)}
          delta={pourcentageCharge === null ? null : `${pourcentageCharge} %`}
          deltaTone={
            pourcentageCharge !== null && pourcentageCharge > 100
              ? 'danger'
              : 'success'
          }
          hint={
            pourcentageCharge === null
              ? `${enseignant.chargeAffectee} unités`
              : 'du quota'
          }
        />
        <KpiTile
          label="Max consécutif"
          value={formatUnites(enseignant.maxConsecutifUnites)}
          hint={`${enseignant.maxConsecutifUnites} unités`}
        />
        <KpiTile
          label="Amplitude max / jour"
          value={
            enseignant.amplitudeMaxUnites === null
              ? '—'
              : formatUnites(enseignant.amplitudeMaxUnites)
          }
          hint={
            enseignant.amplitudeMaxUnites === null
              ? 'non plafonnée'
              : `${enseignant.amplitudeMaxUnites} unités`
          }
        />
      </div>

      {/* Habilitations : bloc propre à l'application, dans l'idiome du DS. */}
      <Card padded={false}>
        <div style={ENTETE_CARTE}>
          <h3 style={TITRE_CARTE}>Habilitations (matières × niveaux)</h3>
          <Button
            variant="primary"
            size="sm"
            loading={sauvegardeHabilitations.isPending}
            onClick={() => sauvegardeHabilitations.mutate()}
          >
            Enregistrer
          </Button>
        </div>
        <div style={CORPS_CARTE}>
          {(matieres ?? []).length === 0 ? (
            <EmptyState
              variant="gated"
              title="Aucune matière"
              description="Créez d’abord des matières dans le référentiel."
            />
          ) : (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                gap: 12,
                alignItems: 'start',
              }}
            >
              {(matieres ?? []).map((matiere) => {
                const habilite = matiere.id in habilitations;
                return (
                  <div
                    key={matiere.id}
                    style={{
                      border: `1px solid ${habilite ? 'var(--color-primary-border)' : 'var(--border-subtle)'}`,
                      borderRadius: 'var(--radius-sm)',
                      background: habilite
                        ? 'var(--surface-selected)'
                        : 'var(--surface-card)',
                      padding: '12px 14px',
                    }}
                  >
                    <label
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        fontSize: 'var(--text-base)',
                        fontWeight: 'var(--weight-medium)',
                        color: 'var(--text-strong)',
                        cursor: 'pointer',
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={habilite}
                        onChange={() => basculerMatiere(matiere.id)}
                        style={{
                          width: 15,
                          height: 15,
                          flex: 'none',
                          accentColor: 'var(--color-primary)',
                          cursor: 'pointer',
                        }}
                      />
                      <span
                        aria-hidden="true"
                        style={{
                          ...PUCE_MATIERE,
                          background: matiere.couleur,
                        }}
                      />
                      {matiere.libelle}
                    </label>
                    {habilite && (
                      <div
                        style={{
                          marginTop: 10,
                          paddingLeft: 23,
                          display: 'flex',
                          flexWrap: 'wrap',
                          gap: '8px 16px',
                        }}
                      >
                        {(niveaux ?? []).length === 0 && (
                          <span
                            style={{
                              fontSize: 'var(--text-xs)',
                              color: 'var(--text-muted)',
                            }}
                          >
                            Aucun niveau défini.
                          </span>
                        )}
                        {(niveaux ?? []).map((niveau) => (
                          <label
                            key={niveau.id}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: 6,
                              fontSize: 'var(--text-sm)',
                              color: 'var(--text-body)',
                              cursor: 'pointer',
                            }}
                          >
                            <input
                              type="checkbox"
                              checked={(
                                habilitations[matiere.id] ?? []
                              ).includes(niveau.id)}
                              onChange={() =>
                                basculerNiveau(matiere.id, niveau.id)
                              }
                              style={{
                                width: 14,
                                height: 14,
                                flex: 'none',
                                accentColor: 'var(--color-primary)',
                                cursor: 'pointer',
                              }}
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
            <Alert tone="danger" style={{ marginTop: 16 }}>
              {sauvegardeHabilitations.error.message}
            </Alert>
          )}
          {sauvegardeHabilitations.isSuccess && (
            <Alert tone="success" style={{ marginTop: 16 }}>
              Habilitations enregistrées.
            </Alert>
          )}
        </div>
      </Card>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
          gap: 16,
          alignItems: 'start',
        }}
      >
        {/* ------------------- Indisponibilités -------------------- */}
        <Card padded={false}>
          <div style={ENTETE_CARTE}>
            <h3 style={TITRE_CARTE}>Indisponibilités hebdomadaires</h3>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                creationIndispo.reset();
                setFormPlage(FORM_PLAGE_INITIAL);
                setDialogPlage(true);
              }}
            >
              Ajouter une plage
            </Button>
          </div>
          <div style={CORPS_CARTE}>
            <Alert tone="info">
              Règle D-04 : seules les indisponibilités au statut « Validée »
              sont prises en compte par le moteur. Pensez à valider les
              brouillons.
            </Alert>

            {grille === undefined ? (
              <EmptyState
                variant="gated"
                title="Grille horaire non configurée"
                description="Configurez d’abord la grille horaire dans le référentiel."
              />
            ) : (
              <>
                <p
                  style={{
                    margin: '14px 0 10px',
                    fontSize: 'var(--text-sm)',
                    color: 'var(--text-muted)',
                  }}
                >
                  Cliquez sur une cellule pour créer ou supprimer une
                  indisponibilité, ou faites un cliquer-glisser vertical pour
                  créer une plage.
                </p>
                <div
                  style={ENVELOPPE_GRILLE}
                  onMouseUp={terminerSelection}
                  onMouseLeave={() => {
                    setSelection(null);
                    setSurvolIndispo(null);
                  }}
                >
                  <table style={TABLE_MINI}>
                    <thead>
                      <tr>
                        <th style={TH_MINI}>Heure</th>
                        {grille.joursActifs.map((jour) => (
                          <th key={jour} style={TH_MINI_C}>
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
                            <td style={TD_MINI}>
                              {libelleHeure(grille, index)}
                            </td>
                            {grille.joursActifs.map((jour) => {
                              if (bloquee) {
                                return (
                                  <td
                                    key={jour}
                                    title="Plage bloquée (déjeuner, pause…)"
                                    style={{
                                      ...CELLULE_BASE,
                                      background: 'var(--neutral-200)',
                                      cursor: 'default',
                                    }}
                                  />
                                );
                              }
                              const cle = `${jour}:${index}`;
                              const indispo = indispoPourCellule(jour, index);
                              const enSelection =
                                selection !== null &&
                                selection.jour === jour &&
                                index >=
                                  Math.min(selection.debut, selection.fin) &&
                                index <=
                                  Math.max(selection.debut, selection.fin);
                              let fond = 'var(--neutral-50)';
                              if (indispo !== undefined) {
                                fond =
                                  indispo.statut === 'VALIDE'
                                    ? 'var(--status-danger-solid)'
                                    : 'var(--status-warning-solid)';
                              } else if (enSelection) {
                                fond = 'var(--teal-300)';
                              } else if (survolIndispo === cle) {
                                fond = 'var(--teal-100)';
                              }
                              return (
                                <td
                                  key={jour}
                                  title={
                                    indispo !== undefined
                                      ? `${indispo.statut === 'VALIDE' ? 'Validée' : 'Brouillon'}${indispo.motif !== null && indispo.motif.length > 0 ? ` — ${indispo.motif}` : ''}`
                                      : 'Disponible'
                                  }
                                  style={{
                                    ...CELLULE_BASE,
                                    background: fond,
                                    cursor: 'pointer',
                                    textAlign: 'center',
                                    fontSize: 'var(--text-2xs)',
                                    fontWeight: 'var(--weight-semibold)',
                                    color: 'var(--neutral-0)',
                                  }}
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
                                    setSurvolIndispo(cle);
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
                <div style={LEGENDE}>
                  <span style={LEGENDE_ITEM}>
                    <span
                      style={pastilleLegende('var(--status-warning-solid)')}
                    />
                    Brouillon
                  </span>
                  <span style={LEGENDE_ITEM}>
                    <span
                      style={pastilleLegende('var(--status-danger-solid)')}
                    />
                    Validée
                  </span>
                  <span style={LEGENDE_ITEM}>
                    <span style={pastilleLegende('var(--neutral-200)')} />
                    Plage bloquée
                  </span>
                </div>
              </>
            )}

            {creationIndispo.error !== null && !dialogPlage && (
              <Alert tone="danger" style={{ marginTop: 16 }}>
                {creationIndispo.error.message}
              </Alert>
            )}
            {suppressionIndispo.error !== null && (
              <Alert tone="danger" style={{ marginTop: 16 }}>
                {suppressionIndispo.error.message}
              </Alert>
            )}
            {validationIndispo.error !== null && (
              <Alert tone="danger" style={{ marginTop: 16 }}>
                {validationIndispo.error.message}
              </Alert>
            )}

            {/* Liste des plages : porte la validation D-04 (PATCH VALIDE). */}
            {(indisponibilites ?? []).length > 0 && (
              <>
                <div style={SOUS_TITRE}>Plages déclarées</div>
                <div
                  style={{ display: 'flex', flexDirection: 'column', gap: 8 }}
                >
                  {(indisponibilites ?? []).map((indispo) => (
                    <div key={indispo.id} style={LIGNE_LISTE}>
                      <div style={{ minWidth: 0 }}>
                        <p
                          style={{
                            margin: 0,
                            fontSize: 'var(--text-sm)',
                            fontWeight: 'var(--weight-medium)',
                            color: 'var(--text-strong)',
                          }}
                        >
                          {LIBELLES_JOURS[indispo.jour]} ·{' '}
                          {grille !== undefined
                            ? `${libelleHeure(grille, indispo.indexDebut)} – ${libelleHeure(grille, indispo.indexDebut + indispo.dureeUnites)}`
                            : `unité ${indispo.indexDebut}`}
                        </p>
                        <p
                          style={{
                            margin: '2px 0 0',
                            fontSize: 'var(--text-xs)',
                            color: 'var(--text-muted)',
                          }}
                        >
                          {formatUnites(indispo.dureeUnites)} ·{' '}
                          {indispo.semaine === 'TOUTES'
                            ? 'toutes les semaines'
                            : `semaine ${indispo.semaine}`}{' '}
                          · {indispo.source}
                          {indispo.motif !== null && indispo.motif.length > 0
                            ? ` · ${indispo.motif}`
                            : ''}
                        </p>
                      </div>
                      <div
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 6,
                          flex: 'none',
                        }}
                      >
                        <Badge
                          tone={
                            indispo.statut === 'VALIDE' ? 'success' : 'warning'
                          }
                          size="sm"
                        >
                          {indispo.statut === 'VALIDE'
                            ? 'VALIDÉE'
                            : 'BROUILLON'}
                        </Badge>
                        {indispo.statut !== 'VALIDE' && (
                          <Button
                            size="sm"
                            disabled={validationIndispo.isPending}
                            onClick={() => validationIndispo.mutate(indispo.id)}
                          >
                            Valider
                          </Button>
                        )}
                        <Button
                          variant="danger"
                          size="sm"
                          disabled={suppressionIndispo.isPending}
                          onClick={() => suppressionIndispo.mutate(indispo.id)}
                        >
                          Supprimer
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </Card>

        {/* --------------------- Préférences ----------------------- */}
        <Card padded={false}>
          <div style={ENTETE_CARTE}>
            <h3 style={TITRE_CARTE}>Préférences horaires</h3>
            <Button
              variant="primary"
              size="sm"
              disabled={grille === undefined}
              loading={sauvegardePreferences.isPending}
              onClick={enregistrerPreferences}
            >
              Enregistrer
            </Button>
          </div>
          <div style={CORPS_CARTE}>
            {grille === undefined ? (
              <EmptyState
                variant="gated"
                title="Grille horaire non configurée"
                description="Configurez d’abord la grille horaire dans le référentiel."
              />
            ) : (
              <>
                <p
                  style={{
                    margin: '0 0 10px',
                    fontSize: 'var(--text-sm)',
                    color: 'var(--text-muted)',
                  }}
                >
                  Cliquez sur une cellule pour alterner : neutre → à éviter →
                  préféré. Enregistrez ensuite vos modifications.
                </p>
                <div
                  style={ENVELOPPE_GRILLE}
                  onMouseLeave={() => setSurvolPref(null)}
                >
                  <table style={TABLE_MINI}>
                    <thead>
                      <tr>
                        <th style={TH_MINI}>Heure</th>
                        {grille.joursActifs.map((jour) => (
                          <th key={jour} style={TH_MINI_C}>
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
                            <td style={TD_MINI}>
                              {libelleHeure(grille, index)}
                            </td>
                            {grille.joursActifs.map((jour) => {
                              if (bloquee) {
                                return (
                                  <td
                                    key={jour}
                                    title="Plage bloquée"
                                    style={{
                                      ...CELLULE_BASE,
                                      background: 'var(--neutral-200)',
                                      cursor: 'default',
                                    }}
                                  />
                                );
                              }
                              const cle = `${jour}:${index}`;
                              const type = preferences[cle];
                              let fond = 'var(--neutral-50)';
                              if (type === 'EVITER') {
                                // Même valeur que la pastille de légende
                                // ci-dessous, comme dans la maquette.
                                fond = 'var(--status-danger-bg)';
                              } else if (type === 'PREFERER') {
                                fond = 'var(--teal-200)';
                              } else if (survolPref === cle) {
                                fond = 'var(--teal-100)';
                              }
                              return (
                                <td
                                  key={jour}
                                  title={
                                    type === 'EVITER'
                                      ? 'À éviter'
                                      : type === 'PREFERER'
                                        ? 'Préféré'
                                        : 'Neutre'
                                  }
                                  style={{
                                    ...CELLULE_BASE,
                                    background: fond,
                                    cursor: 'pointer',
                                  }}
                                  onMouseEnter={() => setSurvolPref(cle)}
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
                <div style={LEGENDE}>
                  <span style={LEGENDE_ITEM}>
                    <span
                      style={pastilleLegende(
                        'var(--status-danger-bg)',
                        'var(--red-100)',
                      )}
                    />
                    À éviter
                  </span>
                  <span style={LEGENDE_ITEM}>
                    <span style={pastilleLegende('var(--teal-200)')} />
                    Préféré
                  </span>
                  <span style={LEGENDE_ITEM}>
                    <span style={pastilleLegende('var(--neutral-200)')} />
                    Plage bloquée
                  </span>
                </div>
              </>
            )}
            {sauvegardePreferences.error !== null && (
              <Alert tone="danger" style={{ marginTop: 16 }}>
                {sauvegardePreferences.error.message}
              </Alert>
            )}
            {messagePreferences !== null && (
              <Alert tone="success" style={{ marginTop: 16 }}>
                {messagePreferences}
              </Alert>
            )}
          </div>
        </Card>
      </div>

      <Dialog
        ouvert={dialogPlage}
        titre="Ajouter une plage d’indisponibilité"
        onFermer={() => setDialogPlage(false)}
      >
        <FormulairePlage
          formPlage={formPlage}
          setFormPlage={setFormPlage}
          grille={grille}
          enCours={creationIndispo.isPending}
          erreur={
            creationIndispo.error !== null ? creationIndispo.error.message : null
          }
          onAnnuler={() => setDialogPlage(false)}
          onValider={() =>
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
            })
          }
        />
      </Dialog>
    </div>
  );
}

/** Flèche du lien « Retour aux enseignants » — SVG de la maquette. */
function FlecheRetour() {
  return (
    <svg
      width="14"
      height="14"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ display: 'block' }}
    >
      <path d="m12 19-7-7 7-7" />
      <path d="M19 12H5" />
    </svg>
  );
}

function FormulairePlage({
  formPlage,
  setFormPlage,
  grille,
  enCours,
  erreur,
  onAnnuler,
  onValider,
}: {
  formPlage: typeof FORM_PLAGE_INITIAL;
  setFormPlage: (valeur: typeof FORM_PLAGE_INITIAL) => void;
  grille: Grille | undefined;
  enCours: boolean;
  erreur: string | null;
  onAnnuler: () => void;
  onValider: () => void;
}) {
  const debut = Number(formPlage.indexDebut);
  const duree = Number(formPlage.dureeUnites);
  const valide =
    formPlage.indexDebut.trim().length > 0 &&
    Number.isInteger(debut) &&
    debut >= 0 &&
    formPlage.dureeUnites.trim().length > 0 &&
    Number.isInteger(duree) &&
    duree >= 1;

  return (
    <form
      onSubmit={(evenement: FormEvent<HTMLFormElement>) => {
        evenement.preventDefault();
        if (!valide) return;
        onValider();
      }}
      style={{ display: 'flex', flexDirection: 'column', gap: 16 }}
    >
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
          gap: 16,
        }}
      >
        <Select
          id="jourPlage"
          label="Jour"
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
        <Input
          id="debutPlage"
          label="Index de début"
          type="number"
          min="0"
          value={formPlage.indexDebut}
          onChange={(e) =>
            setFormPlage({ ...formPlage, indexDebut: e.target.value })
          }
          hint={
            grille !== undefined
              ? libelleHeure(grille, Number.isFinite(debut) ? debut : 0)
              : undefined
          }
        />
        <Input
          id="dureePlage"
          label="Durée (unités)"
          type="number"
          min="1"
          value={formPlage.dureeUnites}
          onChange={(e) =>
            setFormPlage({ ...formPlage, dureeUnites: e.target.value })
          }
          hint={formatUnites(Number.isFinite(duree) ? duree : 0)}
        />
      </div>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
          gap: 16,
        }}
      >
        <Select
          id="sourcePlage"
          label="Source"
          value={formPlage.source}
          onChange={(e) => setFormPlage({ ...formPlage, source: e.target.value })}
        >
          <option value="DIRECTEUR">Directeur</option>
          <option value="ENSEIGNANT">Enseignant</option>
        </Select>
        <Select
          id="semainePlage"
          label="Semaine"
          value={formPlage.semaine}
          onChange={(e) =>
            setFormPlage({ ...formPlage, semaine: e.target.value as Semaine })
          }
        >
          <option value="TOUTES">Toutes les semaines</option>
          <option value="A">Semaine A</option>
          <option value="B">Semaine B</option>
        </Select>
      </div>
      <Input
        id="motifPlage"
        label="Motif (optionnel)"
        value={formPlage.motif}
        onChange={(e) => setFormPlage({ ...formPlage, motif: e.target.value })}
        placeholder="Cours à la faculté, engagement personnel…"
      />

      {erreur !== null && <Alert tone="danger">{erreur}</Alert>}

      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          gap: 12,
          paddingTop: 4,
        }}
      >
        <Button variant="secondary" onClick={onAnnuler}>
          Annuler
        </Button>
        <Button type="submit" disabled={!valide} loading={enCours}>
          {enCours ? 'Création…' : 'Ajouter'}
        </Button>
      </div>
    </form>
  );
}
