'use client';

import { useEffect, useState, type CSSProperties } from 'react';
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
import { Alert, Button, Card, EmptyState, Input, Select } from '@/components/ds';
import { Dialog } from '@/components/ui/dialog';
import { ChargementPage } from '@/components/ui/spinner';

/* ------------------------------------------------------------------ */
/* Chrome de tableau du design system (idiome de la maquette)          */
/* ------------------------------------------------------------------ */

const TABLEAU: CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
  fontSize: 'var(--text-sm)',
};

const TH: CSSProperties = {
  textAlign: 'left',
  padding: '10px 14px',
  background: 'var(--neutral-50)',
  borderBottom: 'var(--border-width) solid var(--border-subtle)',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
  whiteSpace: 'nowrap',
};

const TH_RIGHT: CSSProperties = { ...TH, textAlign: 'right' };

/** Cellule de lecture : hauteur de rangée du design system (44 px). */
const TD: CSSProperties = {
  padding: '0 14px',
  height: 'var(--row-height)',
  borderBottom: 'var(--border-width) solid var(--neutral-100)',
  color: 'var(--text-body)',
  whiteSpace: 'nowrap',
};

/** Cellule éditable : même gouttière, hauteur libre pour accueillir un contrôle. */
const TD_EDIT: CSSProperties = {
  ...TD,
  height: 'auto',
  padding: '10px 14px',
  verticalAlign: 'top',
};

const TD_RIGHT: CSSProperties = {
  ...TD,
  textAlign: 'right',
  fontVariantNumeric: 'tabular-nums',
};

const TD_STRONG: CSSProperties = {
  ...TD,
  fontWeight: 'var(--weight-medium)',
  color: 'var(--text-strong)',
};

/** Valeur héritée : la maquette la met en italique, pas seulement en pâle. */
const TD_MUTED: CSSProperties = {
  ...TD,
  color: 'var(--text-subtle)',
  fontStyle: 'italic',
};

const TD_ACTION: CSSProperties = { ...TD_EDIT, textAlign: 'right' };

/** Puce de couleur de matière : carré arrondi 10 × 10, rayon 3 (helper `puce`). */
const PUCE_MATIERE: CSSProperties = {
  display: 'inline-block',
  width: 10,
  height: 10,
  borderRadius: 3,
  flex: 'none',
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

const ZONE_TABLEAU: CSSProperties = { overflowX: 'auto' };

const ZONE_ALERTE: CSSProperties = { padding: '14px 24px 0' };

/** Rail de la mini-barre de répartition (maquette : 120 px × 6 px). */
function railBarre(largeur: number): CSSProperties {
  return {
    display: 'block',
    width: largeur,
    height: 6,
    borderRadius: 'var(--radius-pill)',
    background: 'var(--surface-sunken)',
    overflow: 'hidden',
    flex: 'none',
  };
}

function remplissageBarre(pourcentage: number, couleur: string): CSSProperties {
  return {
    display: 'block',
    height: '100%',
    width: `${pourcentage}%`,
    borderRadius: 'var(--radius-pill)',
    background: couleur,
  };
}

/* ------------------------------------------------------------------ */
/* Formulaire de maquette                                              */
/* ------------------------------------------------------------------ */

interface LigneFormulaire {
  /** Identité stable côté client : sert de clé React, jamais envoyée à l'API. */
  cle: string;
  matiereId: string;
  volumeUnites: string;
  volumeUnitesB: string;
  maxParJourUnites: string;
  dedoublement: string;
  nbSousGroupes: string;
  coEnseignants: string;
  patterns: string;
}

const LIGNE_VIDE: Omit<LigneFormulaire, 'cle'> = {
  matiereId: '',
  volumeUnites: '8',
  volumeUnitesB: '',
  maxParJourUnites: '',
  dedoublement: 'AUCUN',
  nbSousGroupes: '',
  coEnseignants: '',
  patterns: '',
};

/**
 * Clé de rangée : `crypto.randomUUID` n'est exposé qu'en contexte sécurisé,
 * d'où le repli sur un compteur de session (unicité suffisante pour React).
 */
let compteurLignes = 0;
function nouvelleCle(): string {
  compteurLignes += 1;
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `ligne-${compteurLignes}`;
}

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

/* ------------------------------------------------------------------ */
/* Écran                                                               */
/* ------------------------------------------------------------------ */

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
  const { data: maquette, isFetching: chargementMaquette } = useQuery({
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
          cle: nouvelleCle(),
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

  function ajouterLigne() {
    setMessageMaquette(null);
    setLignes((precedentes) => [
      ...precedentes,
      { ...LIGNE_VIDE, cle: nouvelleCle() },
    ]);
  }

  const totalUnites = lignes.reduce(
    (somme, ligne) => somme + (Number(ligne.volumeUnites) || 0),
    0,
  );
  /* La maquette normalise la barre de répartition sur la matière la plus
     lourde (`maxVol`), pas sur le total : sans cela toutes les barres sont
     quasi vides. `totalUnites` reste le compteur d'en-tête de carte. */
  const volumeMax = lignes.reduce(
    (maximum, ligne) => Math.max(maximum, Number(ligne.volumeUnites) || 0),
    0,
  );

  const couleursMatieres = new Map(
    (matieres ?? []).map((matiere) => [String(matiere.id), matiere.couleur]),
  );
  const niveauChoisi = (niveaux ?? []).find(
    (niveau) => String(niveau.id) === niveauId,
  );

  if (isLoading) return <ChargementPage />;
  if (error !== null)
    return <Alert tone="danger">{error.message}</Alert>;

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
          Volumes hebdomadaires par niveau et par matière, affectations et
          dérogations. Unité = 30 minutes.
        </p>
        <div style={{ width: 220 }}>
          <Select
            id="niveau-maquette"
            label="Niveau"
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
      </div>

      <Card padded={false} style={{ overflow: 'hidden' }}>
        <div style={ENTETE_CARTE}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
            <h3 style={TITRE_CARTE}>
              {niveauChoisi === undefined
                ? 'Maquette'
                : `Maquette ${niveauChoisi.libelle}`}
            </h3>
            <span
              style={{
                fontSize: 'var(--text-sm)',
                color: 'var(--text-muted)',
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              {totalUnites} unités · {formatUnites(totalUnites)} / semaine
            </span>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <Button
              variant="secondary"
              size="sm"
              disabled={niveauId === ''}
              onClick={ajouterLigne}
            >
              Ajouter une ligne
            </Button>
            <Button
              variant="primary"
              size="sm"
              loading={sauvegardeMaquette.isPending}
              disabled={
                niveauId === '' || lignes.some((ligne) => ligne.matiereId === '')
              }
              onClick={() => sauvegardeMaquette.mutate()}
            >
              {sauvegardeMaquette.isPending ? 'Enregistrement…' : 'Enregistrer'}
            </Button>
          </div>
        </div>

        {(sauvegardeMaquette.error !== null || messageMaquette !== null) && (
          <div style={ZONE_ALERTE}>
            {sauvegardeMaquette.error !== null && (
              <Alert tone="danger">{sauvegardeMaquette.error.message}</Alert>
            )}
            {messageMaquette !== null && (
              <Alert tone="success">{messageMaquette}</Alert>
            )}
          </div>
        )}

        {niveauId === '' ? (
          <EmptyState
            variant="gated"
            title="Choisissez un niveau"
            description="La maquette horaire se règle niveau par niveau : sélectionnez-en un pour afficher ses volumes par matière."
          />
        ) : chargementMaquette && lignes.length === 0 ? (
          <ChargementPage />
        ) : lignes.length === 0 ? (
          <EmptyState
            title="Aucune ligne de maquette"
            description="Ajoutez les matières enseignées à ce niveau, puis leur volume hebdomadaire en unités de 30 minutes."
            action={
              <Button variant="primary" size="sm" onClick={ajouterLigne}>
                Ajouter une ligne
              </Button>
            }
          />
        ) : (
          <div style={ZONE_TABLEAU}>
            <table style={TABLEAU}>
              <thead>
                <tr>
                  <th style={TH}>Matière</th>
                  <th style={TH_RIGHT}>Volume</th>
                  <th style={TH_RIGHT}>Sem. B</th>
                  <th style={TH_RIGHT}>Max / jour</th>
                  <th style={TH}>Dédoublement</th>
                  <th style={TH}>Patterns</th>
                  <th style={TH}>Répartition</th>
                  <th style={TH_RIGHT} />
                </tr>
              </thead>
              <tbody>
                {lignes.map((ligne, index) => {
                  const unites = Number(ligne.volumeUnites) || 0;
                  const part =
                    volumeMax > 0
                      ? Math.min(100, Math.round((unites / volumeMax) * 100))
                      : 0;
                  const couleur =
                    couleursMatieres.get(ligne.matiereId) ??
                    'var(--color-primary)';

                  return (
                    <tr key={ligne.cle}>
                      <td style={TD_EDIT}>
                        <span
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                          }}
                        >
                          <span
                            aria-hidden="true"
                            style={{ ...PUCE_MATIERE, background: couleur }}
                          />
                          <Select
                            size="sm"
                            aria-label="Matière"
                            value={ligne.matiereId}
                            onChange={(e) =>
                              modifierLigne(index, 'matiereId', e.target.value)
                            }
                            containerStyle={{ minWidth: 168 }}
                          >
                            <option value="">— Matière —</option>
                            {(matieres ?? []).map((matiere) => (
                              <option key={matiere.id} value={matiere.id}>
                                {matiere.libelle}
                              </option>
                            ))}
                          </Select>
                        </span>
                      </td>
                      <td style={TD_EDIT}>
                        <Input
                          size="sm"
                          type="number"
                          min="1"
                          aria-label="Volume en unités"
                          value={ligne.volumeUnites}
                          onChange={(e) =>
                            modifierLigne(index, 'volumeUnites', e.target.value)
                          }
                          hint={formatUnites(unites)}
                          containerStyle={{ width: 76 }}
                          style={{ textAlign: 'right' }}
                        />
                      </td>
                      <td style={TD_EDIT}>
                        <Input
                          size="sm"
                          type="number"
                          min="0"
                          placeholder="—"
                          aria-label="Volume de la semaine B"
                          value={ligne.volumeUnitesB}
                          onChange={(e) =>
                            modifierLigne(index, 'volumeUnitesB', e.target.value)
                          }
                          containerStyle={{ width: 76 }}
                          style={{ textAlign: 'right' }}
                        />
                      </td>
                      <td style={TD_EDIT}>
                        <Input
                          size="sm"
                          type="number"
                          min="1"
                          placeholder="—"
                          aria-label="Maximum par jour, en unités"
                          value={ligne.maxParJourUnites}
                          onChange={(e) =>
                            modifierLigne(
                              index,
                              'maxParJourUnites',
                              e.target.value,
                            )
                          }
                          containerStyle={{ width: 76 }}
                          style={{ textAlign: 'right' }}
                        />
                      </td>
                      <td style={TD_EDIT}>
                        <span
                          style={{ display: 'flex', alignItems: 'center', gap: 8 }}
                        >
                          <Select
                            size="sm"
                            aria-label="Dédoublement"
                            value={ligne.dedoublement}
                            onChange={(e) =>
                              modifierLigne(
                                index,
                                'dedoublement',
                                e.target.value,
                              )
                            }
                            containerStyle={{ minWidth: 148 }}
                          >
                            <option value="AUCUN">Aucun</option>
                            <option value="SOUS_GROUPES">Sous-groupes</option>
                            <option value="CO_ENSEIGNEMENT">
                              Co-enseignement
                            </option>
                          </Select>
                          {ligne.dedoublement === 'SOUS_GROUPES' && (
                            <Input
                              size="sm"
                              type="number"
                              min="2"
                              placeholder="Nb"
                              title="Nombre de sous-groupes"
                              aria-label="Nombre de sous-groupes"
                              value={ligne.nbSousGroupes}
                              onChange={(e) =>
                                modifierLigne(
                                  index,
                                  'nbSousGroupes',
                                  e.target.value,
                                )
                              }
                              containerStyle={{ width: 68 }}
                              style={{ textAlign: 'right' }}
                            />
                          )}
                          {ligne.dedoublement === 'CO_ENSEIGNEMENT' && (
                            <Input
                              size="sm"
                              type="number"
                              min="2"
                              placeholder="Nb"
                              title="Nombre de co-enseignants"
                              aria-label="Nombre de co-enseignants"
                              value={ligne.coEnseignants}
                              onChange={(e) =>
                                modifierLigne(
                                  index,
                                  'coEnseignants',
                                  e.target.value,
                                )
                              }
                              containerStyle={{ width: 68 }}
                              style={{ textAlign: 'right' }}
                            />
                          )}
                        </span>
                      </td>
                      <td style={TD_EDIT}>
                        <Input
                          size="sm"
                          placeholder="4,4 | 4,2,2"
                          aria-label="Patterns de séances"
                          value={ligne.patterns}
                          onChange={(e) =>
                            modifierLigne(index, 'patterns', e.target.value)
                          }
                          containerStyle={{ width: 152 }}
                          style={{ fontFamily: 'var(--font-mono)' }}
                        />
                      </td>
                      <td style={TD_EDIT}>
                        <span
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                            height: 'var(--control-height-sm)',
                          }}
                          title={`${formatUnites(unites)} — ${part} % de la matière la plus lourde`}
                        >
                          <span style={railBarre(120)}>
                            <span style={remplissageBarre(part, couleur)} />
                          </span>
                        </span>
                      </td>
                      <td
                        style={TD_ACTION}
                      >
                        <Button
                          variant="ghost"
                          size="sm"
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
                  );
                })}
              </tbody>
            </table>
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
  const formulaireIncomplet =
    formulaire.groupeId === '' ||
    formulaire.matiereId === '' ||
    formulaire.enseignantId === '';

  return (
    <Card padded={false} style={{ overflow: 'hidden' }}>
      <div style={ENTETE_CARTE}>
        <h3 style={TITRE_CARTE}>Affectations · qui enseigne quoi à qui</h3>
        <Button
          variant="primary"
          size="sm"
          onClick={() => {
            creation.reset();
            suppression.reset();
            setFormulaire(FORM_AFFECTATION_INITIAL);
            setDialogOuvert(true);
          }}
        >
          Nouvelle affectation
        </Button>
      </div>

      {(error !== null || suppression.error !== null) && (
        <div style={ZONE_ALERTE}>
          {error !== null && <Alert tone="danger">{error.message}</Alert>}
          {suppression.error !== null && (
            <Alert tone="danger">{suppression.error.message}</Alert>
          )}
        </div>
      )}

      {isLoading && <ChargementPage />}

      {affectations !== undefined &&
        (affectations.length === 0 ? (
          <EmptyState
            title="Aucune affectation"
            description="Une affectation relie un groupe, une matière et un enseignant. Sans elles, la génération n'a rien à placer."
          />
        ) : (
          <div style={ZONE_TABLEAU}>
            <table style={TABLEAU}>
              <thead>
                <tr>
                  <th style={TH}>Groupe</th>
                  <th style={TH}>Matière</th>
                  <th style={TH}>Enseignant</th>
                  <th style={TH}>Volume</th>
                  <th style={TH_RIGHT}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {affectations.map((affectation) => {
                  return (
                    <tr key={affectation.id}>
                      <td style={TD_STRONG}>
                        {affectation.groupeLibelle}
                      </td>
                      <td style={TD}>
                        {affectation.matiereLibelle}
                      </td>
                      <td style={TD}>
                        {affectation.enseignantNom}
                      </td>
                      {affectation.volumeUnites === null ? (
                        <td style={TD_MUTED}>
                          Hérité de la maquette
                        </td>
                      ) : (
                        <td
                          style={{
                            ...TD,
                            fontVariantNumeric: 'tabular-nums',
                          }}
                        >
                          {formatUnites(affectation.volumeUnites)}
                        </td>
                      )}
                      <td style={TD_RIGHT}>
                        <Button
                          variant="ghost"
                          size="sm"
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
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ))}

      <Dialog
        ouvert={dialogOuvert}
        titre="Nouvelle affectation"
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (formulaireIncomplet) return;
            creation.mutate();
          }}
          style={{ display: 'flex', flexDirection: 'column', gap: 16 }}
        >
          <Select
            id="affectation-groupe"
            label="Groupe"
            required
            value={formulaire.groupeId}
            onChange={(e) =>
              setFormulaire({ ...formulaire, groupeId: e.target.value })
            }
          >
            <option value="">— Choisir un groupe —</option>
            {groupesAplatis.map((groupe) => (
              <option key={groupe.id} value={groupe.id}>
                {groupe.libelle}
              </option>
            ))}
          </Select>
          <Select
            id="affectation-matiere"
            label="Matière"
            required
            value={formulaire.matiereId}
            onChange={(e) =>
              setFormulaire({ ...formulaire, matiereId: e.target.value })
            }
          >
            <option value="">— Choisir une matière —</option>
            {(matieres ?? []).map((matiere) => (
              <option key={matiere.id} value={matiere.id}>
                {matiere.libelle}
              </option>
            ))}
          </Select>
          <Select
            id="affectation-enseignant"
            label="Enseignant"
            required
            value={formulaire.enseignantId}
            onChange={(e) =>
              setFormulaire({ ...formulaire, enseignantId: e.target.value })
            }
          >
            <option value="">— Choisir un enseignant —</option>
            {(enseignants ?? []).map((enseignant) => (
              <option key={enseignant.id} value={enseignant.id}>
                {enseignant.nomComplet}
              </option>
            ))}
          </Select>
          <Input
            id="affectation-volume"
            label="Volume (unités, optionnel)"
            type="number"
            min="1"
            value={formulaire.volumeUnites}
            onChange={(e) =>
              setFormulaire({ ...formulaire, volumeUnites: e.target.value })
            }
            placeholder="Vide = volume de la maquette"
            hint={
              formulaire.volumeUnites.trim().length > 0
                ? `Soit ${formatUnites(Number(formulaire.volumeUnites) || 0)}.`
                : undefined
            }
          />

          {creation.error !== null && (
            <Alert tone="danger">{creation.error.message}</Alert>
          )}

          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              gap: 12,
              paddingTop: 4,
            }}
          >
            <Button variant="secondary" onClick={() => setDialogOuvert(false)}>
              Annuler
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={creation.isPending}
              disabled={formulaireIncomplet}
            >
              {creation.isPending ? 'Création…' : 'Créer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Dérogations de volume par groupe                                   */
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

  const formulaireIncomplet =
    formulaire.groupeId === '' ||
    formulaire.matiereId === '' ||
    formulaire.volumeUnites.trim().length === 0;

  return (
    <Card padded={false} style={{ overflow: 'hidden' }}>
      <div style={{ ...ENTETE_CARTE, alignItems: 'flex-start' }}>
        <div>
          <h3 style={TITRE_CARTE}>Dérogations de volume par groupe</h3>
          <p
            style={{
              margin: '4px 0 0',
              fontSize: 'var(--text-sm)',
              color: 'var(--text-muted)',
            }}
          >
            Remplace ponctuellement le volume de la maquette pour un groupe donné
            (ex. classe à option renforcée).
          </p>
        </div>
        <Button
          variant="primary"
          size="sm"
          onClick={() => {
            creation.reset();
            suppression.reset();
            setFormulaire(FORM_OVERRIDE_INITIAL);
            setDialogOuvert(true);
          }}
        >
          Nouvelle dérogation
        </Button>
      </div>

      {(error !== null || suppression.error !== null) && (
        <div style={ZONE_ALERTE}>
          {error !== null && <Alert tone="danger">{error.message}</Alert>}
          {suppression.error !== null && (
            <Alert tone="danger">{suppression.error.message}</Alert>
          )}
        </div>
      )}

      {isLoading && <ChargementPage />}

      {overrides !== undefined &&
        (overrides.length === 0 ? (
          <EmptyState
            variant="gated"
            title="Aucune dérogation de volume"
            description="Tous les groupes suivent le volume de la maquette de leur niveau."
          />
        ) : (
          <div style={ZONE_TABLEAU}>
            <table style={TABLEAU}>
              <thead>
                <tr>
                  <th style={TH}>Groupe</th>
                  <th style={TH}>Matière</th>
                  <th style={TH_RIGHT}>Volume</th>
                  <th style={TH_RIGHT}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {overrides.map((override) => {
                  return (
                    <tr key={override.id}>
                      <td style={TD_STRONG}>
                        {libellesGroupes.get(override.groupeId) ??
                          `Groupe ${override.groupeId}`}
                      </td>
                      <td style={TD}>
                        {libellesMatieres.get(override.matiereId) ??
                          `Matière ${override.matiereId}`}
                      </td>
                      <td style={TD_RIGHT}>
                        {override.volumeUnites} unités ·{' '}
                        {formatUnites(override.volumeUnites)}
                      </td>
                      <td style={TD_RIGHT}>
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={suppression.isPending}
                          onClick={() => {
                            if (window.confirm('Supprimer cette dérogation ?')) {
                              suppression.mutate(override);
                            }
                          }}
                        >
                          Supprimer
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ))}

      <Dialog
        ouvert={dialogOuvert}
        titre="Nouvelle dérogation de volume"
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (formulaireIncomplet) return;
            creation.mutate();
          }}
          style={{ display: 'flex', flexDirection: 'column', gap: 16 }}
        >
          <Select
            id="derogation-groupe"
            label="Groupe"
            required
            value={formulaire.groupeId}
            onChange={(e) =>
              setFormulaire({ ...formulaire, groupeId: e.target.value })
            }
          >
            <option value="">— Choisir un groupe —</option>
            {groupesAplatis.map((groupe) => (
              <option key={groupe.id} value={groupe.id}>
                {groupe.libelle}
              </option>
            ))}
          </Select>
          <Select
            id="derogation-matiere"
            label="Matière"
            required
            value={formulaire.matiereId}
            onChange={(e) =>
              setFormulaire({ ...formulaire, matiereId: e.target.value })
            }
          >
            <option value="">— Choisir une matière —</option>
            {(matieres ?? []).map((matiere) => (
              <option key={matiere.id} value={matiere.id}>
                {matiere.libelle}
              </option>
            ))}
          </Select>
          <Input
            id="derogation-volume"
            label="Volume (unités)"
            required
            type="number"
            min="1"
            value={formulaire.volumeUnites}
            onChange={(e) =>
              setFormulaire({ ...formulaire, volumeUnites: e.target.value })
            }
            hint={`Soit ${formatUnites(Number(formulaire.volumeUnites) || 0)}.`}
          />

          {creation.error !== null && (
            <Alert tone="danger">{creation.error.message}</Alert>
          )}

          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              gap: 12,
              paddingTop: 4,
            }}
          >
            <Button variant="secondary" onClick={() => setDialogOuvert(false)}>
              Annuler
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={creation.isPending}
              disabled={formulaireIncomplet}
            >
              {creation.isPending ? 'Création…' : 'Créer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </Card>
  );
}
