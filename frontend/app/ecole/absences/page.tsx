'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState, type CSSProperties, type ReactNode } from 'react';
import { apiFetch } from '@/lib/api';
import type { Niveau } from '@/lib/types';
import type { GroupeResume } from '@/lib/types-planning';
import type {
  Absence,
  AlerteAbsence,
  DemiJournee,
  EleveFeuille,
  FeuilleAppel,
  LigneAppel,
  PeriodeStatistiques,
  ResultatFeuille,
  SerieAbsences,
  StatistiquesAbsences,
  TypeAbsence,
} from '@/lib/types-absences';
import {
  chevauche,
  JOURS_MAX_STATISTIQUES,
  libelleDemiJournee,
  SEUIL_ALERTE_PAR_DEFAUT,
} from '@/lib/types-absences';
import {
  Alert,
  Badge,
  Button,
  Card,
  EmptyState,
  Input,
  KpiTile,
  Select,
  Tabs,
} from '@/components/ds';
import { ChargementPage, Spinner } from '@/components/ui/spinner';

/* ------------------------------------------------------------------ */
/* Styles de tableau, mêmes valeurs que les autres écrans école        */
/* ------------------------------------------------------------------ */

const TABLEAU: CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
  fontSize: 'var(--text-sm)',
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

const TH_DROITE: CSSProperties = { ...TH, textAlign: 'right' };

const TD: CSSProperties = {
  padding: '0 14px',
  height: 'var(--row-height)',
  borderBottom: '1px solid var(--neutral-100)',
  color: 'var(--text-body)',
  textAlign: 'left',
};

const TD_DROITE: CSSProperties = {
  ...TD,
  textAlign: 'right',
  fontVariantNumeric: 'tabular-nums',
};

const TD_FORT: CSSProperties = {
  ...TD,
  fontWeight: 'var(--weight-medium)',
  color: 'var(--text-strong)',
  whiteSpace: 'nowrap',
};

const TD_MONO: CSSProperties = {
  ...TD,
  fontFamily: 'var(--font-mono)',
  fontSize: 'var(--text-xs)',
  fontVariantNumeric: 'tabular-nums',
  color: 'var(--text-muted)',
  whiteSpace: 'nowrap',
};

const TITRE_CARTE: CSSProperties = {
  margin: 0,
  fontSize: 'var(--type-card-title-size)',
  fontWeight: 'var(--type-card-title-weight)',
  color: 'var(--text-strong)',
};

const CAPTION: CSSProperties = {
  margin: 0,
  fontSize: 'var(--text-sm)',
  color: 'var(--text-muted)',
};

const BARRE_FILTRES: CSSProperties = {
  display: 'flex',
  flexWrap: 'wrap',
  alignItems: 'flex-end',
  gap: 14,
};

/* ------------------------------------------------------------------ */
/* Dates : manipulées en chaînes ISO, jamais via Date                  */
/* ------------------------------------------------------------------ */

/**
 * Les dates par défaut sont calculées **au montage** et non à l'initialisation
 * de l'état : le rendu serveur et le rendu navigateur peuvent tomber de part et
 * d'autre de minuit (fuseaux différents), ce qui produirait un écart
 * d'hydratation et, pire, une feuille d'appel ouverte sur la veille.
 */
function isoDuJour(decalageMois = 0, forcerPremierDuMois = false): string {
  const maintenant = new Date();
  if (decalageMois !== 0) maintenant.setMonth(maintenant.getMonth() + decalageMois);
  if (forcerPremierDuMois) maintenant.setDate(1);
  const mois = String(maintenant.getMonth() + 1).padStart(2, '0');
  const jour = String(maintenant.getDate()).padStart(2, '0');
  return `${maintenant.getFullYear()}-${mois}-${jour}`;
}

/** `2026-09-14` → `14/09/2026`. Sans passer par Date : aucun décalage possible. */
function formatJour(iso: string): string {
  const morceaux = iso.split('-');
  if (morceaux.length !== 3) return iso;
  return `${morceaux[2]}/${morceaux[1]}/${morceaux[0]}`;
}

/** Nombre de jours de la période, bornes incluses. */
function joursEntre(debut: string, fin: string): number {
  const depuis = Date.parse(`${debut}T00:00:00Z`);
  const jusqua = Date.parse(`${fin}T00:00:00Z`);
  if (Number.isNaN(depuis) || Number.isNaN(jusqua)) return 0;
  return Math.floor((jusqua - depuis) / 86_400_000) + 1;
}

/**
 * Chaîne de requête sans paramètre vide. Indispensable : le contrôleur attend
 * des `Long` pour `groupeId` / `niveauId`, et un `?groupeId=` vide échouerait à
 * la conversion côté Spring.
 */
function parametres(entrees: Record<string, string | number | null | undefined>): string {
  const requete = new URLSearchParams();
  for (const [cle, valeur] of Object.entries(entrees)) {
    if (valeur === null || valeur === undefined || valeur === '') continue;
    requete.set(cle, String(valeur));
  }
  const chaine = requete.toString();
  return chaine === '' ? '' : `?${chaine}`;
}

function formatNombre(valeur: number): string {
  return valeur.toLocaleString('fr-FR');
}

/** Taux au dixième, comme le serveur le calcule. */
function formatTaux(taux: number): string {
  return `${taux.toLocaleString('fr-FR', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })} %`;
}

/** Accord au pluriel des libellés de comptage. */
function pluriel(nombre: number, singulier: string, plurielMot?: string): string {
  if (nombre <= 1) return `${formatNombre(nombre)} ${singulier}`;
  return `${formatNombre(nombre)} ${plurielMot ?? `${singulier}s`}`;
}

/** Valeur retardée : évite une requête par frappe sur le seuil d'alerte. */
function useDifferee<T>(valeur: T, delai = 400): T {
  const [differee, setDifferee] = useState(valeur);
  useEffect(() => {
    const minuterie = setTimeout(() => setDifferee(valeur), delai);
    return () => clearTimeout(minuterie);
  }, [valeur, delai]);
  return differee;
}

/* ------------------------------------------------------------------ */
/* Options de sélecteurs                                               */
/* ------------------------------------------------------------------ */

interface OptionGroupe {
  id: number;
  libelle: string;
}

/** Aplatit l'arbre des groupes ; les sous-groupes de dédoublement sont indentés. */
function aplatirGroupes(groupes: GroupeResume[], prefixe = ''): OptionGroupe[] {
  const resultat: OptionGroupe[] = [];
  for (const groupe of groupes) {
    resultat.push({ id: groupe.id, libelle: `${prefixe}${groupe.libelle}` });
    if (groupe.sousGroupes.length > 0) {
      resultat.push(...aplatirGroupes(groupe.sousGroupes, `${prefixe}— `));
    }
  }
  return resultat;
}

const OPTIONS_DEMI_JOURNEE: readonly { value: DemiJournee; label: string }[] = [
  { value: 'MATIN', label: libelleDemiJournee('MATIN') },
  { value: 'APRES_MIDI', label: libelleDemiJournee('APRES_MIDI') },
  { value: 'JOURNEE', label: libelleDemiJournee('JOURNEE') },
];

const OPTIONS_PERIODE: readonly { value: PeriodeStatistiques; label: string }[] = [
  { value: 'JOUR', label: 'Par jour' },
  { value: 'SEMAINE', label: 'Par semaine' },
  { value: 'MOIS', label: 'Par mois' },
];

/* ------------------------------------------------------------------ */
/* Boutons segmentés de présence                                       */
/* ------------------------------------------------------------------ */

interface Segment {
  valeur: TypeAbsence | null;
  libelle: string;
  /** Trio de tokens de statut : fond, texte, trait. */
  tokens: readonly [string, string, string];
}

/**
 * Quatre choix et non trois : le serveur connaît aussi `EXCLUSION`. Sans ce
 * segment, ouvrir une feuille où une exclusion a été saisie puis enregistrer la
 * transformerait silencieusement en autre chose.
 */
const SEGMENTS: readonly Segment[] = [
  {
    valeur: null,
    libelle: 'Présent',
    tokens: ['--status-success-bg', '--status-success-fg', '--status-success-solid'],
  },
  {
    valeur: 'ABSENCE',
    libelle: 'Absent',
    tokens: ['--status-danger-bg', '--status-danger-fg', '--status-danger-solid'],
  },
  {
    valeur: 'RETARD',
    libelle: 'Retard',
    tokens: ['--status-warning-bg', '--status-warning-fg', '--status-warning-solid'],
  },
  {
    valeur: 'EXCLUSION',
    libelle: 'Exclusion',
    tokens: ['--status-neutral-bg', '--status-neutral-fg', '--status-neutral-solid'],
  },
];

function SegmentsPresence({
  valeur,
  nom,
  libelleGroupe,
  onChange,
}: {
  valeur: TypeAbsence | null;
  nom: string;
  libelleGroupe: string;
  onChange: (type: TypeAbsence | null) => void;
}) {
  return (
    <div
      role="group"
      aria-label={libelleGroupe}
      style={{
        display: 'inline-flex',
        border: '1px solid var(--border-default)',
        borderRadius: 'var(--radius-sm)',
        background: 'var(--surface-card)',
      }}
    >
      {SEGMENTS.map((segment, index) => {
        const actif = segment.valeur === valeur;
        const [fond, texte, trait] = segment.tokens;
        const premier = index === 0;
        const dernier = index === SEGMENTS.length - 1;
        return (
          <label
            key={segment.libelle}
            title={segment.libelle}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              height: 30,
              padding: '0 11px',
              fontSize: 'var(--text-xs)',
              fontWeight: actif ? 'var(--weight-semibold)' : 'var(--weight-medium)',
              color: actif ? `var(${texte})` : 'var(--text-muted)',
              background: actif ? `var(${fond})` : 'transparent',
              boxShadow: actif ? `inset 0 0 0 1px var(${trait})` : 'none',
              borderLeft: premier ? 'none' : '1px solid var(--border-subtle)',
              borderTopLeftRadius: premier ? 5 : 0,
              borderBottomLeftRadius: premier ? 5 : 0,
              borderTopRightRadius: dernier ? 5 : 0,
              borderBottomRightRadius: dernier ? 5 : 0,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              userSelect: 'none',
              transition: 'background var(--duration-fast) var(--ease-standard)',
            }}
          >
            {/* Vraie radio, masquée : on récupère gratuitement la navigation au
                clavier et la sémantique de choix unique. L'anneau de focus est
                reposé sur l'étiquette, seul élément visible. */}
            <input
              type="radio"
              name={nom}
              checked={actif}
              onChange={() => onChange(segment.valeur)}
              onFocus={(evenement) => {
                const etiquette = evenement.currentTarget.closest('label');
                if (etiquette !== null && evenement.currentTarget.matches(':focus-visible')) {
                  etiquette.style.boxShadow = actif
                    ? `inset 0 0 0 1px var(${trait}), var(--ring)`
                    : 'var(--ring)';
                }
              }}
              onBlur={(evenement) => {
                const etiquette = evenement.currentTarget.closest('label');
                if (etiquette !== null) {
                  etiquette.style.boxShadow = actif ? `inset 0 0 0 1px var(${trait})` : 'none';
                }
              }}
              style={{
                position: 'absolute',
                width: 1,
                height: 1,
                opacity: 0,
                margin: 0,
                pointerEvents: 'none',
              }}
            />
            {segment.libelle}
          </label>
        );
      })}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Onglet 1 — feuille d'appel                                          */
/* ------------------------------------------------------------------ */

interface LigneBrouillon {
  type: TypeAbsence | null;
  justifiee: boolean;
  motif: string;
}

const LIGNE_VIERGE: LigneBrouillon = { type: null, justifiee: false, motif: '' };

function brouillonDepuisEleve(eleve: EleveFeuille): LigneBrouillon {
  const saisie = eleve.absence;
  if (saisie === null) return LIGNE_VIERGE;
  return {
    type: saisie.type,
    justifiee: saisie.justifiee,
    motif: saisie.motif ?? '',
  };
}

function OngletFeuilleAppel({ groupes }: { groupes: OptionGroupe[] }) {
  const clientQuery = useQueryClient();

  const [groupeChoisi, setGroupeChoisi] = useState('');
  const [date, setDate] = useState('');
  const [demiJournee, setDemiJournee] = useState<DemiJournee>('MATIN');
  const [brouillon, setBrouillon] = useState<Record<number, LigneBrouillon>>({});
  const [resultat, setResultat] = useState<ResultatFeuille | null>(null);

  useEffect(() => {
    setDate(isoDuJour());
  }, []);

  // Première classe par défaut : l'appel se fait presque toujours classe par
  // classe, ouvrir l'écran sur un sélecteur vide coûterait un clic à chaque fois.
  useEffect(() => {
    if (groupeChoisi !== '') return;
    const premier = groupes[0];
    if (premier !== undefined) setGroupeChoisi(String(premier.id));
  }, [groupes, groupeChoisi]);

  const groupeId = groupeChoisi === '' ? null : Number(groupeChoisi);
  const pret = groupeId !== null && date !== '';

  const requeteFeuille = useQuery({
    queryKey: ['ecole', 'absences', 'feuille', groupeId, date, demiJournee],
    queryFn: () =>
      apiFetch<FeuilleAppel>(
        `/ecole/absences/feuille${parametres({ groupeId, date, demiJournee })}`,
      ),
    enabled: pret,
  });

  // État complet de la journée : la feuille ne montre que le contexte demandé,
  // or une saisie sur la journée entière sera pourtant remplacée à
  // l'enregistrement. On la signale plutôt que de l'écraser en silence.
  const requeteJournee = useQuery({
    queryKey: ['ecole', 'absences', 'journee', groupeId, date],
    queryFn: () =>
      apiFetch<Absence[]>(`/ecole/absences${parametres({ debut: date, fin: date, groupeId })}`),
    enabled: pret,
  });

  const feuille = requeteFeuille.data;
  const eleves = feuille?.eleves ?? [];

  useEffect(() => {
    if (feuille === undefined) return;
    const initial: Record<number, LigneBrouillon> = {};
    for (const eleve of feuille.eleves) {
      initial[eleve.eleveId] = brouillonDepuisEleve(eleve);
    }
    setBrouillon(initial);
    setResultat(null);
  }, [feuille]);

  function ligne(eleveId: number): LigneBrouillon {
    return brouillon[eleveId] ?? LIGNE_VIERGE;
  }

  function majLigne(eleveId: number, modification: Partial<LigneBrouillon>) {
    setResultat(null);
    setBrouillon((precedent) => {
      const suivante: LigneBrouillon = {
        ...(precedent[eleveId] ?? LIGNE_VIERGE),
        ...modification,
      };
      // Un élève déclaré présent perd justification et motif : ils n'ont plus d'objet.
      if (suivante.type === null) {
        suivante.justifiee = false;
        suivante.motif = '';
      }
      return { ...precedent, [eleveId]: suivante };
    });
  }

  function toutPresent() {
    setResultat(null);
    const remis: Record<number, LigneBrouillon> = {};
    for (const eleve of eleves) remis[eleve.eleveId] = LIGNE_VIERGE;
    setBrouillon(remis);
  }

  const compteurs = useMemo(() => {
    let absents = 0;
    let retards = 0;
    let exclusions = 0;
    for (const eleve of eleves) {
      const type = (brouillon[eleve.eleveId] ?? LIGNE_VIERGE).type;
      if (type === 'ABSENCE') absents += 1;
      else if (type === 'RETARD') retards += 1;
      else if (type === 'EXCLUSION') exclusions += 1;
    }
    return { absents, retards, exclusions, presents: eleves.length - absents - retards - exclusions };
  }, [eleves, brouillon]);

  const modifie = useMemo(
    () =>
      eleves.some((eleve) => {
        const saisie = brouillonDepuisEleve(eleve);
        const courante = brouillon[eleve.eleveId];
        if (courante === undefined) return false;
        return (
          courante.type !== saisie.type ||
          courante.justifiee !== saisie.justifiee ||
          courante.motif !== saisie.motif
        );
      }),
    [eleves, brouillon],
  );

  /** Saisies du jour dont le contexte recouvre celui appelé sans être le même. */
  const saisiesRecouvertes = useMemo(
    () =>
      (requeteJournee.data ?? []).filter(
        (absence) =>
          absence.demiJournee !== demiJournee && chevauche(demiJournee, absence.demiJournee),
      ),
    [requeteJournee.data, demiJournee],
  );

  const enregistrer = useMutation({
    mutationFn: () => {
      const saisies: LigneAppel[] = [];
      for (const eleve of eleves) {
        const courante = ligne(eleve.eleveId);
        if (courante.type === null) continue;
        saisies.push({
          eleveId: eleve.eleveId,
          type: courante.type,
          justifiee: courante.justifiee,
          motif: courante.motif.trim() === '' ? null : courante.motif.trim(),
        });
      }
      return apiFetch<ResultatFeuille>('/ecole/absences/feuille', {
        method: 'POST',
        body: JSON.stringify({
          groupeId,
          date,
          demiJournee,
          seanceId: null,
          saisies,
        }),
      });
    },
    onSuccess: (reponse) => {
      setResultat(reponse);
      void clientQuery.invalidateQueries({ queryKey: ['ecole', 'absences'] });
    },
  });

  const chargement = requeteFeuille.isLoading || (pret && requeteFeuille.isFetching && feuille === undefined);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <Card>
        <div style={{ ...BARRE_FILTRES, justifyContent: 'space-between' }}>
          <div style={BARRE_FILTRES}>
            <Select
              label="Classe"
              value={groupeChoisi}
              placeholder={groupes.length === 0 ? 'Aucune classe' : 'Choisir une classe'}
              options={groupes.map((groupe) => ({ value: groupe.id, label: groupe.libelle }))}
              onChange={(evenement) => setGroupeChoisi(evenement.target.value)}
              containerStyle={{ width: 240 }}
            />
            <Input
              label="Date"
              type="date"
              value={date}
              onChange={(evenement) => setDate(evenement.target.value)}
              containerStyle={{ width: 170 }}
            />
            <Select
              label="Demi-journée"
              value={demiJournee}
              options={OPTIONS_DEMI_JOURNEE}
              onChange={(evenement) => setDemiJournee(evenement.target.value as DemiJournee)}
              containerStyle={{ width: 170 }}
            />
          </div>
          <Button
            variant="ghost"
            size="md"
            disabled={eleves.length === 0}
            onClick={toutPresent}
          >
            Tout marquer présent
          </Button>
        </div>
      </Card>

      {requeteFeuille.error !== null && (
        <Alert tone="danger" title="Feuille d'appel indisponible">
          {requeteFeuille.error.message}
        </Alert>
      )}

      {enregistrer.error !== null && (
        <Alert tone="danger" title="Enregistrement impossible">
          {enregistrer.error.message}
        </Alert>
      )}

      {resultat !== null && !modifie && (
        <Alert tone="success" title="Appel enregistré">
          {[
            pluriel(resultat.crees, 'saisie créée', 'saisies créées'),
            pluriel(resultat.misAJour, 'modifiée', 'modifiées'),
            pluriel(resultat.supprimes, 'effacée (présent)', 'effacées (présents)'),
          ].join(' · ')}
        </Alert>
      )}

      {saisiesRecouvertes.length > 0 && (
        <Alert tone="warning" title="Des saisies d'un autre contexte vont être remplacées">
          {pluriel(saisiesRecouvertes.length, 'élève de cette classe a', 'élèves de cette classe ont')}{' '}
          déjà une saisie du {formatJour(date)} sur un contexte qui recouvre «{' '}
          {libelleDemiJournee(demiJournee).toLowerCase()} » :{' '}
          {saisiesRecouvertes
            .map(
              (absence) =>
                `${absence.eleveNom} (${libelleDemiJournee(absence.demiJournee).toLowerCase()})`,
            )
            .join(', ')}
          . La feuille fait foi : l'enregistrer remplacera ces saisies.
        </Alert>
      )}

      {chargement ? (
        <ChargementPage />
      ) : feuille === undefined ? (
        <Card padded={false}>
          <EmptyState
            variant="gated"
            title="Choisissez une classe"
            description="Sélectionnez une classe, une date et une demi-journée pour ouvrir la feuille d'appel."
          />
        </Card>
      ) : eleves.length === 0 ? (
        <Card padded={false}>
          <EmptyState
            variant="gated"
            title={`Aucun élève dans ${feuille.groupe.libelle}`}
            description="Rattachez des élèves à cette classe depuis l'écran Élèves pour pouvoir faire l'appel."
          />
        </Card>
      ) : (
        <Card padded={false}>
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              alignItems: 'baseline',
              justifyContent: 'space-between',
              gap: 12,
              padding: '18px 24px',
              borderBottom: '1px solid var(--border-subtle)',
            }}
          >
            <div>
              <h3 style={TITRE_CARTE}>
                {feuille.groupe.libelle} · {libelleDemiJournee(feuille.demiJournee)}
              </h3>
              <p style={{ ...CAPTION, marginTop: 4 }}>
                {feuille.groupe.niveauLibelle} · {pluriel(eleves.length, 'élève')} ·{' '}
                {formatJour(feuille.date)}
              </p>
            </div>
            {(requeteJournee.data ?? []).length > 0 && (
              <p style={{ ...CAPTION, fontSize: 'var(--text-xs)' }}>
                Journée du {formatJour(date)} :{' '}
                {OPTIONS_DEMI_JOURNEE.map(
                  (option) =>
                    `${
                      (requeteJournee.data ?? []).filter(
                        (absence) => absence.demiJournee === option.value,
                      ).length
                    } ${option.label.toLowerCase()}`,
                ).join(' · ')}
              </p>
            )}
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table style={TABLEAU}>
              <thead>
                <tr>
                  <th style={TH}>Élève</th>
                  <th style={TH}>Code Massar</th>
                  <th style={TH}>Présence</th>
                  <th style={TH}>Justifiée</th>
                  <th style={TH}>Motif</th>
                </tr>
              </thead>
              <tbody>
                {eleves.map((eleve) => {
                  const courante = ligne(eleve.eleveId);
                  const signale = courante.type !== null;
                  return (
                    <tr key={eleve.eleveId}>
                      <td style={TD_FORT}>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                          {eleve.nom} {eleve.prenom}
                          {eleve.statut !== 'INSCRIT' && (
                            <Badge tone="neutral" size="sm">
                              {eleve.statut === 'PARTI' ? 'Parti' : 'Redoublant'}
                            </Badge>
                          )}
                        </span>
                      </td>
                      <td style={TD_MONO}>{eleve.codeMassar}</td>
                      <td style={{ ...TD, whiteSpace: 'nowrap' }}>
                        <SegmentsPresence
                          valeur={courante.type}
                          nom={`presence-${eleve.eleveId}`}
                          libelleGroupe={`Présence de ${eleve.nom} ${eleve.prenom}`}
                          onChange={(type) => majLigne(eleve.eleveId, { type })}
                        />
                      </td>
                      <td style={{ ...TD, whiteSpace: 'nowrap' }}>
                        <label
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 8,
                            fontSize: 'var(--text-sm)',
                            color: signale ? 'var(--text-body)' : 'var(--text-subtle)',
                            cursor: signale ? 'pointer' : 'default',
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={courante.justifiee}
                            disabled={!signale}
                            onChange={(evenement) =>
                              majLigne(eleve.eleveId, { justifiee: evenement.target.checked })
                            }
                            aria-label={`Absence justifiée pour ${eleve.nom} ${eleve.prenom}`}
                            style={{
                              width: 16,
                              height: 16,
                              accentColor: 'var(--color-primary)',
                              cursor: signale ? 'pointer' : 'default',
                            }}
                          />
                          Justifiée
                        </label>
                      </td>
                      <td style={{ ...TD, minWidth: 220 }}>
                        <Input
                          size="sm"
                          value={courante.motif}
                          maxLength={200}
                          disabled={!signale}
                          placeholder={signale ? 'Motif (facultatif)' : '—'}
                          aria-label={`Motif pour ${eleve.nom} ${eleve.prenom}`}
                          onChange={(evenement) =>
                            majLigne(eleve.eleveId, { motif: evenement.target.value })
                          }
                        />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Barre d'action collante : sur une classe de 40 élèves, le compte
              d'absents et le bouton doivent rester sous les yeux. */}
          <div
            style={{
              position: 'sticky',
              bottom: 0,
              display: 'flex',
              flexWrap: 'wrap',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 16,
              padding: '14px 24px',
              borderTop: '1px solid var(--border-subtle)',
              borderBottomLeftRadius: 'var(--radius-md)',
              borderBottomRightRadius: 'var(--radius-md)',
              background: 'var(--surface-card)',
              boxShadow: '0 -6px 16px -12px rgba(14, 19, 18, 0.35)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
              <Badge tone={compteurs.absents > 0 ? 'danger' : 'success'} dot>
                {pluriel(compteurs.absents, 'absent')}
              </Badge>
              {compteurs.retards > 0 && (
                <Badge tone="warning" dot>
                  {pluriel(compteurs.retards, 'retard')}
                </Badge>
              )}
              {compteurs.exclusions > 0 && (
                <Badge tone="neutral" dot>
                  {pluriel(compteurs.exclusions, 'exclusion')}
                </Badge>
              )}
              <span style={{ ...CAPTION, fontSize: 'var(--text-xs)' }}>
                {pluriel(compteurs.presents, 'élève présent', 'élèves présents')} sur{' '}
                {formatNombre(eleves.length)}
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              {modifie && (
                <span
                  style={{
                    fontSize: 'var(--text-sm)',
                    fontWeight: 'var(--weight-medium)',
                    color: 'var(--status-warning-fg)',
                  }}
                >
                  Modifications non enregistrées
                </span>
              )}
              {requeteFeuille.isFetching && <Spinner className="h-4 w-4" />}
              <Button
                variant="primary"
                size="md"
                loading={enregistrer.isPending}
                disabled={!modifie}
                onClick={() => enregistrer.mutate()}
              >
                Enregistrer l’appel
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Onglet 2 — statistiques                                             */
/* ------------------------------------------------------------------ */

/** Icônes des tuiles KPI, même trait que la barre latérale (24, stroke 1.75). */
const PROPS_SVG = {
  width: 16,
  height: 16,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  style: { display: 'block' },
} as const;

const ICONE_ABSENCES: ReactNode = (
  <svg {...PROPS_SVG}>
    <rect x="3" y="4" width="18" height="18" rx="2" />
    <path d="M16 2v4M8 2v4M3 10h18" />
    <path d="m9.5 14.5 5 5M14.5 14.5l-5 5" />
  </svg>
);

const ICONE_RETARDS: ReactNode = (
  <svg {...PROPS_SVG}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5l3 2" />
  </svg>
);

const ICONE_NON_JUSTIFIEES: ReactNode = (
  <svg {...PROPS_SVG}>
    <path d="M10.3 3.6 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.6a2 2 0 0 0-3.4 0z" />
    <path d="M12 9v4M12 17h.01" />
  </svg>
);

const ICONE_TAUX: ReactNode = (
  <svg {...PROPS_SVG}>
    <path d="M3 3v18h18" />
    <path d="m7 15 4-4 3 3 5-6" />
  </svg>
);

/** Bandeau de légende du graphique, réutilisé par la barre de taux du tableau. */
function Pastille({ couleur, libelle }: { couleur: string; libelle: string }) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        fontSize: 'var(--text-xs)',
        color: 'var(--text-muted)',
      }}
    >
      <span
        aria-hidden="true"
        style={{
          width: 10,
          height: 10,
          borderRadius: 2,
          background: couleur,
          flex: 'none',
        }}
      />
      {libelle}
    </span>
  );
}

const HAUTEUR_GRAPHIQUE = 168;

/**
 * Série temporelle en barres, en CSS seul : deux barres par point (absences,
 * retards) dans un conteneur de hauteur fixe aligné en bas, les hauteurs étant
 * exprimées en pourcentage du maximum de la période.
 */
function GraphiqueSeries({ series }: { series: SerieAbsences[] }) {
  const maximum = Math.max(1, ...series.map((point) => Math.max(point.absences, point.retards)));
  // Un libellé sur N pour que l'axe reste lisible jusqu'à 400 points.
  const pas = Math.max(1, Math.ceil(series.length / 16));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', gap: 14 }}>
        <Pastille couleur="var(--teal-500)" libelle="Absences" />
        <Pastille couleur="var(--amber-500)" libelle="Retards" />
      </div>
      <div style={{ display: 'flex', gap: 10 }}>
        {/* Graduations : maximum, moitié, zéro. */}
        <div
          aria-hidden="true"
          style={{
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            height: HAUTEUR_GRAPHIQUE,
            paddingBottom: 0,
            fontSize: 'var(--text-2xs)',
            color: 'var(--text-subtle)',
            fontVariantNumeric: 'tabular-nums',
            textAlign: 'right',
            flex: 'none',
            minWidth: 26,
          }}
        >
          <span>{formatNombre(maximum)}</span>
          <span>{formatNombre(Math.round(maximum / 2))}</span>
          <span>0</span>
        </div>
        <div style={{ flex: 1, minWidth: 0, overflowX: 'auto' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'flex-end',
              gap: 4,
              height: HAUTEUR_GRAPHIQUE,
              minWidth: '100%',
              borderBottom: '1px solid var(--border-subtle)',
              // Lignes de graduation, sans élément supplémentaire.
              backgroundImage:
                'linear-gradient(to bottom, var(--neutral-100) 1px, transparent 1px), linear-gradient(to bottom, var(--neutral-100) 1px, transparent 1px)',
              backgroundPosition: 'left top, left 50%',
              backgroundSize: '100% 1px',
              backgroundRepeat: 'no-repeat',
            }}
          >
            {series.map((point, index) => (
              <div
                key={point.cle}
                title={`${point.libelle} — ${pluriel(point.absences, 'absence')}, ${pluriel(
                  point.retards,
                  'retard',
                )}`}
                style={{
                  display: 'flex',
                  alignItems: 'flex-end',
                  justifyContent: 'center',
                  gap: 2,
                  height: '100%',
                  flex: `1 0 ${series.length > 40 ? 14 : 22}px`,
                  minWidth: series.length > 40 ? 14 : 22,
                  position: 'relative',
                }}
              >
                <div
                  style={{
                    width: 8,
                    height: `${(point.absences / maximum) * 100}%`,
                    minHeight: point.absences > 0 ? 3 : 0,
                    background: 'var(--teal-500)',
                    borderRadius: '2px 2px 0 0',
                  }}
                />
                <div
                  style={{
                    width: 8,
                    height: `${(point.retards / maximum) * 100}%`,
                    minHeight: point.retards > 0 ? 3 : 0,
                    background: 'var(--amber-500)',
                    borderRadius: '2px 2px 0 0',
                  }}
                />
                <span
                  aria-hidden={index % pas === 0 ? undefined : 'true'}
                  style={{
                    position: 'absolute',
                    top: '100%',
                    marginTop: 6,
                    fontSize: 'var(--text-2xs)',
                    color: 'var(--text-subtle)',
                    whiteSpace: 'nowrap',
                    visibility: index % pas === 0 ? 'visible' : 'hidden',
                  }}
                >
                  {point.libelle}
                </span>
              </div>
            ))}
          </div>
          {/* Gouttière réservée aux libellés positionnés en absolu ci-dessus. */}
          <div style={{ height: 22 }} />
        </div>
      </div>
    </div>
  );
}

/** Barre de taux en ligne dans le tableau par classe. */
function BarreTaux({ taux }: { taux: number }) {
  const largeur = Math.min(100, Math.max(0, taux));
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8, width: 150 }}>
      <span
        aria-hidden="true"
        style={{
          flex: 1,
          height: 6,
          borderRadius: 'var(--radius-pill)',
          background: 'var(--surface-sunken)',
          overflow: 'hidden',
        }}
      >
        <span
          style={{
            display: 'block',
            width: `${largeur}%`,
            height: '100%',
            borderRadius: 'var(--radius-pill)',
            background:
              taux >= 15
                ? 'var(--status-danger-solid)'
                : taux >= 8
                  ? 'var(--status-warning-solid)'
                  : 'var(--teal-500)',
          }}
        />
      </span>
      <span
        style={{
          minWidth: 52,
          textAlign: 'right',
          fontVariantNumeric: 'tabular-nums',
          fontSize: 'var(--text-sm)',
          color: 'var(--text-strong)',
        }}
      >
        {formatTaux(taux)}
      </span>
    </span>
  );
}

function OngletStatistiques({
  groupes,
  niveaux,
}: {
  groupes: OptionGroupe[];
  niveaux: Niveau[];
}) {
  const [periode, setPeriode] = useState<PeriodeStatistiques>('JOUR');
  const [debut, setDebut] = useState('');
  const [fin, setFin] = useState('');
  const [groupeChoisi, setGroupeChoisi] = useState('');
  const [niveauChoisi, setNiveauChoisi] = useState('');
  const [seuilSaisi, setSeuilSaisi] = useState(String(SEUIL_ALERTE_PAR_DEFAUT));

  useEffect(() => {
    setDebut(isoDuJour(0, true));
    setFin(isoDuJour());
  }, []);

  const seuil = useDifferee(seuilSaisi);
  const seuilNombre = Number.parseInt(seuil, 10);
  const seuilValide = Number.isFinite(seuilNombre) && seuilNombre >= 1;

  const bornesPretes = debut !== '' && fin !== '';
  const ordreInverse = bornesPretes && debut > fin;
  const nombreDeJours = bornesPretes ? joursEntre(debut, fin) : 0;
  const periodeTropLongue = nombreDeJours > JOURS_MAX_STATISTIQUES;
  const periodeValide = bornesPretes && !ordreInverse && !periodeTropLongue;

  const groupeId = groupeChoisi === '' ? null : Number(groupeChoisi);
  const niveauId = niveauChoisi === '' ? null : Number(niveauChoisi);

  const requeteStatistiques = useQuery({
    queryKey: ['ecole', 'absences', 'statistiques', periode, debut, fin, groupeId, niveauId],
    queryFn: () =>
      apiFetch<StatistiquesAbsences>(
        `/ecole/absences/statistiques${parametres({
          periode,
          debut,
          fin,
          groupeId,
          niveauId,
        })}`,
      ),
    enabled: periodeValide,
  });

  const requeteAlertes = useQuery({
    queryKey: ['ecole', 'absences', 'alertes', debut, fin, seuilValide ? seuilNombre : null],
    queryFn: () =>
      apiFetch<AlerteAbsence[]>(
        `/ecole/absences/alertes${parametres({
          debut,
          fin,
          seuil: seuilValide ? seuilNombre : null,
        })}`,
      ),
    enabled: periodeValide,
  });

  const statistiques = requeteStatistiques.data;
  const totaux = statistiques?.totaux;
  const alertes = requeteAlertes.data ?? [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <Card>
        <div style={BARRE_FILTRES}>
          <Select
            label="Granularité"
            value={periode}
            options={OPTIONS_PERIODE}
            onChange={(evenement) => setPeriode(evenement.target.value as PeriodeStatistiques)}
            containerStyle={{ width: 170 }}
          />
          <Input
            label="Du"
            type="date"
            value={debut}
            onChange={(evenement) => setDebut(evenement.target.value)}
            containerStyle={{ width: 170 }}
          />
          <Input
            label="Au"
            type="date"
            value={fin}
            onChange={(evenement) => setFin(evenement.target.value)}
            containerStyle={{ width: 170 }}
          />
          <Select
            label="Classe"
            value={groupeChoisi}
            options={[
              { value: '', label: 'Toutes les classes' },
              ...groupes.map((groupe) => ({ value: groupe.id, label: groupe.libelle })),
            ]}
            onChange={(evenement) => setGroupeChoisi(evenement.target.value)}
            containerStyle={{ width: 220 }}
          />
          <Select
            label="Niveau"
            value={niveauChoisi}
            options={[
              { value: '', label: 'Tous les niveaux' },
              ...niveaux.map((niveau) => ({ value: niveau.id, label: niveau.libelle })),
            ]}
            onChange={(evenement) => setNiveauChoisi(evenement.target.value)}
            containerStyle={{ width: 200 }}
          />
        </div>
      </Card>

      {ordreInverse && (
        <Alert tone="warning" title="Période inversée">
          La date de fin précède la date de début.
        </Alert>
      )}

      {periodeTropLongue && (
        <Alert tone="warning" title="Période trop longue">
          Les statistiques sont bornées à {formatNombre(JOURS_MAX_STATISTIQUES)} jours ; la période
          demandée en couvre {formatNombre(nombreDeJours)}.
        </Alert>
      )}

      {requeteStatistiques.error !== null && (
        <Alert tone="danger" title="Statistiques indisponibles">
          {requeteStatistiques.error.message}
        </Alert>
      )}

      {requeteStatistiques.isLoading && periodeValide && <ChargementPage />}

      {statistiques !== undefined && totaux !== undefined && (
        <>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
              gap: 16,
            }}
          >
            <KpiTile
              label="Absences"
              value={formatNombre(totaux.absences)}
              icon={ICONE_ABSENCES}
              hint={`dont ${pluriel(totaux.justifiees, 'justifiée', 'justifiées')}`}
            />
            <KpiTile
              label="Retards"
              value={formatNombre(totaux.retards)}
              icon={ICONE_RETARDS}
              hint={
                totaux.exclusions > 0
                  ? `${pluriel(totaux.exclusions, 'exclusion')} par ailleurs`
                  : 'hors calcul du taux'
              }
            />
            <KpiTile
              label="Non justifiées"
              value={formatNombre(totaux.nonJustifiees)}
              icon={ICONE_NON_JUSTIFIEES}
              hint={`${pluriel(alertes.length, 'élève au-delà du seuil', 'élèves au-delà du seuil')}`}
            />
            <KpiTile
              label="Taux d’absentéisme"
              value={formatTaux(totaux.tauxAbsenteisme)}
              icon={ICONE_TAUX}
              hint={`${pluriel(totaux.elevesInscrits, 'élève inscrit', 'élèves inscrits')} · ${pluriel(
                totaux.joursOuvrables,
                'jour ouvrable',
                'jours ouvrables',
              )}`}
            />
          </div>

          <Card>
            <Card.Header
              title="Évolution"
              subtitle={`Du ${formatJour(statistiques.debut)} au ${formatJour(statistiques.fin)} · ${
                OPTIONS_PERIODE.find((option) => option.value === statistiques.periode)?.label ??
                statistiques.periode
              }`}
            />
            {statistiques.series.length === 0 ? (
              <EmptyState
                variant="gated"
                title="Aucun point à afficher"
                description="La période sélectionnée ne contient aucun regroupement."
              />
            ) : (
              <GraphiqueSeries series={statistiques.series} />
            )}
          </Card>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))',
              gap: 16,
              alignItems: 'start',
            }}
          >
            <Card padded={false}>
              <div style={{ padding: '18px 24px', borderBottom: '1px solid var(--border-subtle)' }}>
                <h3 style={TITRE_CARTE}>Par classe</h3>
                <p style={{ ...CAPTION, marginTop: 4 }}>
                  Taux calculé sur les élèves inscrits rattachés à la classe. Une classe sans élève
                  ni saisie n’apparaît pas.
                </p>
              </div>
              {statistiques.parGroupe.length === 0 ? (
                <EmptyState
                  variant="gated"
                  title="Aucune classe dans le périmètre"
                  description="Aucune classe du filtre ne compte d’élève inscrit ni de saisie sur la période."
                />
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={TABLEAU}>
                    <thead>
                      <tr>
                        <th style={TH}>Classe</th>
                        <th style={TH_DROITE}>Effectif</th>
                        <th style={TH_DROITE}>Absences</th>
                        <th style={TH_DROITE}>Taux</th>
                      </tr>
                    </thead>
                    <tbody>
                      {statistiques.parGroupe.map((groupe) => (
                        <tr key={groupe.groupeId}>
                          <td style={TD_FORT}>{groupe.groupeLibelle}</td>
                          <td style={TD_DROITE}>{formatNombre(groupe.effectif)}</td>
                          <td style={TD_DROITE}>{formatNombre(groupe.absences)}</td>
                          <td style={{ ...TD, textAlign: 'right' }}>
                            <BarreTaux taux={groupe.tauxAbsenteisme} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>

            <Card padded={false}>
              <div
                style={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  alignItems: 'flex-end',
                  justifyContent: 'space-between',
                  gap: 12,
                  padding: '18px 24px',
                  borderBottom: '1px solid var(--border-subtle)',
                }}
              >
                <div>
                  <h3 style={TITRE_CARTE}>Alertes d’absentéisme</h3>
                  <p style={{ ...CAPTION, marginTop: 4 }}>
                    Élèves atteignant le seuil d’absences non justifiées sur la période. Ce décompte
                    porte sur tout l’établissement, les filtres classe et niveau ne s’y appliquent
                    pas.
                  </p>
                </div>
                <Input
                  label="Seuil"
                  type="number"
                  min={1}
                  max={999}
                  value={seuilSaisi}
                  error={seuilSaisi !== '' && !seuilValide ? 'Au moins 1' : undefined}
                  onChange={(evenement) => setSeuilSaisi(evenement.target.value)}
                  containerStyle={{ width: 110 }}
                />
              </div>
              {requeteAlertes.error !== null ? (
                <div style={{ padding: '18px 24px' }}>
                  <Alert tone="danger" title="Alertes indisponibles">
                    {requeteAlertes.error.message}
                  </Alert>
                </div>
              ) : alertes.length === 0 ? (
                <EmptyState
                  variant="empty"
                  title="Aucune alerte"
                  description={`Aucun élève n’atteint ${
                    seuilValide ? seuilNombre : SEUIL_ALERTE_PAR_DEFAUT
                  } absences non justifiées sur la période.`}
                />
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={TABLEAU}>
                    <thead>
                      <tr>
                        <th style={TH}>Élève</th>
                        <th style={TH}>Code Massar</th>
                        <th style={TH}>Classe</th>
                        <th style={TH_DROITE}>Non justifiées</th>
                      </tr>
                    </thead>
                    <tbody>
                      {alertes.map((alerte) => (
                        <tr key={alerte.eleveId}>
                          <td style={TD_FORT}>{alerte.eleveNom}</td>
                          <td style={TD_MONO}>{alerte.codeMassar}</td>
                          <td style={TD}>{alerte.groupeLibelle ?? '—'}</td>
                          <td style={{ ...TD_DROITE, whiteSpace: 'nowrap' }}>
                            <Badge tone="danger">{formatNombre(alerte.nonJustifiees)}</Badge>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>
          </div>
        </>
      )}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Écran                                                               */
/* ------------------------------------------------------------------ */

const ONGLETS = [
  { id: 'appel', label: 'Feuille d’appel' },
  { id: 'statistiques', label: 'Statistiques' },
] as const;

export default function PageAbsences() {
  const [onglet, setOnglet] = useState<'appel' | 'statistiques'>('appel');

  const requeteGroupes = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<GroupeResume[]>('/ecole/groupes'),
  });

  const requeteNiveaux = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });

  const groupes = useMemo(() => aplatirGroupes(requeteGroupes.data ?? []), [requeteGroupes.data]);
  const niveaux = requeteNiveaux.data ?? [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <p style={{ ...CAPTION, maxWidth: '62ch' }}>
        Faites l’appel classe par classe, puis suivez l’absentéisme de l’établissement.
      </p>

      <Tabs
        tabs={ONGLETS}
        value={onglet}
        onChange={(identifiant) => setOnglet(identifiant === 'statistiques' ? 'statistiques' : 'appel')}
      />

      {requeteGroupes.error !== null && (
        <Alert tone="danger" title="Classes indisponibles">
          {requeteGroupes.error.message}
        </Alert>
      )}

      {onglet === 'appel' ? (
        <OngletFeuilleAppel groupes={groupes} />
      ) : (
        <OngletStatistiques groupes={groupes} niveaux={niveaux} />
      )}
    </div>
  );
}
