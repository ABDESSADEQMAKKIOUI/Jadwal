'use client';

import {
  DndContext,
  PointerSensor,
  pointerWithin,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';
import type { CSSProperties } from 'react';
import type {
  CreneauGrille,
  GrilleHoraire,
  Jour,
  PlanningVue,
  Seance,
  VuePlanning,
} from '@/lib/types-planning';
import { SeanceCard } from './seance-card';

export const LIBELLES_JOURS: Record<Jour, string> = {
  LUNDI: 'Lundi',
  MARDI: 'Mardi',
  MERCREDI: 'Mercredi',
  JEUDI: 'Jeudi',
  VENDREDI: 'Vendredi',
  SAMEDI: 'Samedi',
  DIMANCHE: 'Dimanche',
};

const LIBELLES_BLOCAGES: Record<string, string> = {
  DEJEUNER: 'Déjeuner',
  PAUSE: 'Pause',
};

/** Géométrie de la maquette : hauteur d'en-tête, hauteur de ligne, largeur des heures. */
const HAUTEUR_ENTETE = 38;
const HAUTEUR_LIGNE = 44;
const LARGEUR_HEURES = 68;
const LARGEUR_MIN_JOUR = 150;

/** "08:00" + index unités -> "09:30" (unités de dureeUniteMinutes minutes). */
export function formatHeure(
  heureDebut: string,
  index: number,
  dureeUniteMinutes: number,
): string {
  const [heures = 8, minutes = 0] = heureDebut
    .split(':')
    .map((partie) => Number.parseInt(partie, 10));
  const total = heures * 60 + minutes + index * dureeUniteMinutes;
  const h = Math.floor(total / 60) % 24;
  const m = total % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

/** Durée en unités -> "1h30", "45 min", "2h". */
export function formatDureeUnites(
  unites: number,
  dureeUniteMinutes: number,
): string {
  const total = unites * dureeUniteMinutes;
  const h = Math.floor(total / 60);
  const m = total % 60;
  if (h === 0) return `${m} min`;
  if (m === 0) return `${h}h`;
  return `${h}h${String(m).padStart(2, '0')}`;
}

/** Style d'une plage bloquée (déjeuner, pause) — repris de la maquette. */
function styleBlocage(colonne: number, debut: number, duree: number): CSSProperties {
  return {
    gridColumn: colonne,
    gridRow: `${debut + 2} / span ${duree}`,
    zIndex: 1,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'var(--neutral-100)',
    borderRight: '1px solid var(--border-subtle)',
    borderTop: '1px solid var(--border-subtle)',
    borderBottom: '1px solid var(--border-subtle)',
    fontSize: 'var(--text-2xs)',
    fontWeight: 'var(--weight-semibold)',
    letterSpacing: 'var(--tracking-caps)',
    textTransform: 'uppercase',
    color: 'var(--text-subtle)',
  };
}

/** Répartition des séances qui se chevauchent en colonnes parallèles. */
function calculerLanes(
  seances: Seance[],
): Map<number, { lane: number; nbLanes: number }> {
  const resultat = new Map<number, { lane: number; nbLanes: number }>();
  const parJour = new Map<string, Seance[]>();
  for (const seance of seances) {
    const liste = parJour.get(seance.jour) ?? [];
    liste.push(seance);
    parJour.set(seance.jour, liste);
  }
  for (const liste of parJour.values()) {
    liste.sort(
      (a, b) => a.indexDebut - b.indexDebut || b.dureeUnites - a.dureeUnites,
    );
    let cluster: Seance[] = [];
    let finMax = -1;
    const finaliser = () => {
      if (cluster.length === 0) return;
      const finsLanes: number[] = [];
      const attribution = new Map<number, number>();
      for (const seance of cluster) {
        let lane = finsLanes.findIndex((fin) => fin <= seance.indexDebut);
        if (lane === -1) {
          lane = finsLanes.length;
          finsLanes.push(0);
        }
        finsLanes[lane] = seance.indexDebut + seance.dureeUnites;
        attribution.set(seance.id, lane);
      }
      for (const seance of cluster) {
        resultat.set(seance.id, {
          lane: attribution.get(seance.id) ?? 0,
          nbLanes: finsLanes.length,
        });
      }
    };
    for (const seance of liste) {
      if (cluster.length > 0 && seance.indexDebut >= finMax) {
        finaliser();
        cluster = [];
        finMax = -1;
      }
      cluster.push(seance);
      finMax = Math.max(finMax, seance.indexDebut + seance.dureeUnites);
    }
    finaliser();
  }
  return resultat;
}

/** Cellule déposable correspondant à un créneau COURS. */
function CelluleCreneau({
  creneau,
  colonne,
}: {
  creneau: CreneauGrille;
  colonne: number;
}) {
  const { isOver, setNodeRef } = useDroppable({ id: `creneau-${creneau.id}` });
  const duree = Math.max(1, creneau.unitesDisponibles);
  return (
    <div
      ref={setNodeRef}
      style={{
        gridColumn: colonne,
        gridRow: `${creneau.indexDebut + 2} / span ${duree}`,
        zIndex: 1,
        borderRadius: 'var(--radius-xs)',
        background: isOver ? 'var(--surface-selected)' : 'transparent',
        boxShadow: isOver ? 'inset 0 0 0 2px var(--color-primary)' : 'none',
        transition: 'background var(--duration-fast) var(--ease-standard)',
      }}
    />
  );
}

/**
 * Grille hebdomadaire de la maquette : colonnes = jours actifs, lignes = unités
 * de la grille horaire. Gère le glisser-déposer des séances vers les créneaux COURS.
 */
export function GrillePlanning({
  grille,
  planning,
  vue,
  onDeplacer,
  onBasculerVerrou,
  onChangerSalle,
  onChangerEnseignant,
}: {
  grille: GrilleHoraire;
  planning: PlanningVue;
  vue: VuePlanning;
  onDeplacer: (seanceId: number, creneauId: number) => void;
  onBasculerVerrou: (seance: Seance) => void;
  onChangerSalle: (seance: Seance) => void;
  onChangerEnseignant: (seance: Seance) => void;
}) {
  const capteurs = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
  );

  const jours = grille.joursActifs;
  const lanes = calculerLanes(planning.seances);
  const seancesParId = new Map(
    planning.seances.map((seance) => [seance.id, seance]),
  );

  function colonneDuJour(jour: Jour): number {
    return jours.indexOf(jour) + 2;
  }

  function gererFinGlisser(evenement: DragEndEvent) {
    if (evenement.over === null) return;
    const seanceId = Number(String(evenement.active.id).replace('seance-', ''));
    const creneauId = Number(String(evenement.over.id).replace('creneau-', ''));
    if (Number.isNaN(seanceId) || Number.isNaN(creneauId)) return;
    const seance = seancesParId.get(seanceId);
    if (seance !== undefined && seance.creneauId === creneauId) return;
    onDeplacer(seanceId, creneauId);
  }

  const indices = Array.from({ length: grille.unitesParJour }, (_, i) => i);

  /**
   * Plages non enseignables : celles de la grille horaire (toutes colonnes) et les
   * créneaux non COURS renvoyés par la vue. Dédupliquées pour éviter de superposer
   * deux fois le même bandeau.
   */
  const blocages: { cle: string; colonne: number; debut: number; duree: number; libelle: string }[] =
    [];
  /* Clé de position seule : les deux sources donnent des durées différentes
     pour la même plage (le créneau non-COURS de la vue porte 0 unité, la
     grille porte la vraie durée). Dédupliquer sur la durée ne filtrerait
     donc rien et empilerait deux bandeaux sur la même cellule. */
  const positions = new Map<string, number>();
  function ajouterBlocage(
    jour: Jour,
    colonne: number,
    debut: number,
    duree: number,
    type: string,
  ) {
    const dureeNormalisee = Math.max(1, duree);
    const libelle = LIBELLES_BLOCAGES[type] ?? type;
    const cle = `${jour}-${debut}`;
    const rang = positions.get(cle);
    if (rang !== undefined) {
      // Même plage vue deux fois : on garde la plus longue des deux durées.
      const existant = blocages[rang];
      if (existant !== undefined && dureeNormalisee > existant.duree) {
        existant.duree = dureeNormalisee;
        existant.libelle = libelle;
      }
      return;
    }
    positions.set(cle, blocages.length);
    blocages.push({
      cle,
      colonne,
      debut,
      duree: dureeNormalisee,
      libelle,
    });
  }
  for (const creneau of planning.creneaux) {
    if (creneau.type === 'COURS' || !jours.includes(creneau.jour)) continue;
    ajouterBlocage(
      creneau.jour,
      colonneDuJour(creneau.jour),
      creneau.indexDebut,
      creneau.unitesDisponibles,
      creneau.type,
    );
  }
  for (const [index, jour] of jours.entries()) {
    for (const plage of grille.plagesBloquees) {
      ajouterBlocage(
        jour,
        index + 2,
        plage.indexDebut,
        plage.dureeUnites,
        plage.type,
      );
    }
  }

  return (
    <DndContext
      sensors={capteurs}
      collisionDetection={pointerWithin}
      onDragEnd={gererFinGlisser}
    >
      <div
        style={{
          minWidth: 0,
          overflow: 'hidden',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)',
          background: 'var(--surface-card)',
          boxShadow: 'var(--shadow-sm)',
        }}
      >
        <div style={{ overflowX: 'auto' }}>
          <div
            style={{
              display: 'grid',
              minWidth: 'max-content',
              gridTemplateColumns: `${LARGEUR_HEURES}px repeat(${jours.length}, minmax(${LARGEUR_MIN_JOUR}px, 1fr))`,
              gridTemplateRows: `${HAUTEUR_ENTETE}px repeat(${grille.unitesParJour}, ${HAUTEUR_LIGNE}px)`,
            }}
          >
            {/* Coin supérieur gauche */}
            <div
              style={{
                gridColumn: 1,
                gridRow: 1,
                position: 'sticky',
                top: 0,
                zIndex: 3,
                background: 'var(--neutral-50)',
                borderBottom: '1px solid var(--border-subtle)',
                borderRight: '1px solid var(--border-subtle)',
              }}
            />

            {/* En-têtes de jours */}
            {jours.map((jour, index) => (
              <div
                key={jour}
                style={{
                  gridColumn: index + 2,
                  gridRow: 1,
                  position: 'sticky',
                  top: 0,
                  zIndex: 3,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: 'var(--neutral-50)',
                  borderBottom: '1px solid var(--border-subtle)',
                  borderRight: '1px solid var(--border-subtle)',
                  fontSize: 'var(--text-2xs)',
                  fontWeight: 'var(--weight-semibold)',
                  letterSpacing: 'var(--tracking-caps)',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                }}
              >
                {LIBELLES_JOURS[jour]}
              </div>
            ))}

            {/* Colonne des heures */}
            {indices.map((index) => (
              <div
                key={`heure-${index}`}
                style={{
                  gridColumn: 1,
                  gridRow: index + 2,
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'flex-end',
                  background: 'var(--neutral-50)',
                  borderBottom: '1px solid var(--border-subtle)',
                  borderRight: '1px solid var(--border-subtle)',
                  padding: '3px 8px 0',
                  fontSize: 'var(--text-2xs)',
                  fontVariantNumeric: 'tabular-nums',
                  color:
                    index % 2 === 0 ? 'var(--text-muted)' : 'var(--text-subtle)',
                }}
              >
                {formatHeure(grille.heureDebut, index, grille.dureeUniteMinutes)}
              </div>
            ))}

            {/* Fond quadrillé */}
            {jours.map((jour, colonne) =>
              indices.map((index) => (
                <div
                  key={`fond-${jour}-${index}`}
                  style={{
                    gridColumn: colonne + 2,
                    gridRow: index + 2,
                    zIndex: 0,
                    borderRight: '1px solid var(--neutral-100)',
                    borderBottom: `1px solid ${
                      index % 2 === 1 ? 'var(--neutral-100)' : 'transparent'
                    }`,
                  }}
                />
              )),
            )}

            {/* Plages non enseignables (déjeuner, pauses) */}
            {blocages.map((blocage) => (
              <div
                key={`blocage-${blocage.cle}`}
                style={styleBlocage(
                  blocage.colonne,
                  blocage.debut,
                  blocage.duree,
                )}
              >
                {blocage.libelle}
              </div>
            ))}

            {/* Créneaux COURS : cibles de dépôt */}
            {planning.creneaux
              .filter(
                (creneau) =>
                  creneau.type === 'COURS' && jours.includes(creneau.jour),
              )
              .map((creneau) => (
                <CelluleCreneau
                  key={`creneau-${creneau.id}`}
                  creneau={creneau}
                  colonne={colonneDuJour(creneau.jour)}
                />
              ))}

            {/* Séances */}
            {planning.seances
              .filter((seance) => jours.includes(seance.jour))
              .map((seance) => {
                const lane = lanes.get(seance.id) ?? { lane: 0, nbLanes: 1 };
                const largeur = 100 / lane.nbLanes;
                return (
                  <SeanceCard
                    key={seance.id}
                    seance={seance}
                    vue={vue}
                    style={{
                      gridColumn: colonneDuJour(seance.jour),
                      gridRow: `${seance.indexDebut + 2} / span ${seance.dureeUnites}`,
                      ...(lane.nbLanes > 1
                        ? {
                            width: `calc(${largeur}% - 6px)`,
                            marginLeft: `calc(${lane.lane * largeur}% + 3px)`,
                            justifySelf: 'start',
                          }
                        : {}),
                    }}
                    onBasculerVerrou={onBasculerVerrou}
                    onChangerSalle={onChangerSalle}
                    onChangerEnseignant={onChangerEnseignant}
                  />
                );
              })}
          </div>
        </div>
      </div>
    </DndContext>
  );
}
