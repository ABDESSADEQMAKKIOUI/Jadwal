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

const HACHURES: CSSProperties = {
  backgroundImage:
    'repeating-linear-gradient(45deg, #f3f4f6 0px, #f3f4f6 6px, #e5e7eb 6px, #e5e7eb 12px)',
};

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
      }}
      className={`rounded transition-colors ${
        isOver ? 'bg-indigo-100/80 ring-2 ring-inset ring-indigo-400' : ''
      }`}
    />
  );
}

/**
 * Grille hebdomadaire : colonnes = jours actifs, lignes = unités de 30 min.
 * Gère le glisser-déposer des séances vers les créneaux COURS.
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

  return (
    <DndContext
      sensors={capteurs}
      collisionDetection={pointerWithin}
      onDragEnd={gererFinGlisser}
    >
      <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm">
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: `4.5rem repeat(${jours.length}, minmax(9rem, 1fr))`,
            gridTemplateRows: `2.5rem repeat(${grille.unitesParJour}, 2.75rem)`,
          }}
          className="min-w-max"
        >
          {/* En-tête : coin + jours */}
          <div
            style={{ gridColumn: 1, gridRow: 1 }}
            className="border-b border-r border-gray-200 bg-gray-50"
          />
          {jours.map((jour, i) => (
            <div
              key={jour}
              style={{ gridColumn: i + 2, gridRow: 1 }}
              className="flex items-center justify-center border-b border-r border-gray-200 bg-gray-50 text-xs font-semibold uppercase tracking-wide text-gray-600"
            >
              {LIBELLES_JOURS[jour]}
            </div>
          ))}

          {/* Colonne des heures */}
          {indices.map((index) => (
            <div
              key={`heure-${index}`}
              style={{ gridColumn: 1, gridRow: index + 2 }}
              className="flex items-start justify-end border-b border-r border-gray-200 bg-gray-50 px-2 pt-0.5 text-[10px] font-medium text-gray-500"
            >
              {formatHeure(grille.heureDebut, index, grille.dureeUniteMinutes)}
            </div>
          ))}

          {/* Fond quadrillé */}
          {jours.map((jour, i) =>
            indices.map((index) => (
              <div
                key={`fond-${jour}-${index}`}
                style={{ gridColumn: i + 2, gridRow: index + 2, zIndex: 0 }}
                className="border-b border-r border-gray-100"
              />
            )),
          )}

          {/* Plages bloquées de la grille (déjeuner, pauses) */}
          {jours.map((jour, i) =>
            grille.plagesBloquees.map((plage, p) => (
              <div
                key={`plage-${jour}-${p}`}
                style={{
                  ...HACHURES,
                  gridColumn: i + 2,
                  gridRow: `${plage.indexDebut + 2} / span ${plage.dureeUnites}`,
                  zIndex: 1,
                }}
                className="flex items-center justify-center border-b border-r border-gray-200 text-[10px] font-medium uppercase tracking-wide text-gray-500"
              >
                {LIBELLES_BLOCAGES[plage.type] ?? plage.type}
              </div>
            )),
          )}

          {/* Créneaux non COURS renvoyés par la vue (hachurés) */}
          {planning.creneaux
            .filter(
              (creneau) =>
                creneau.type !== 'COURS' && jours.includes(creneau.jour),
            )
            .map((creneau) => (
              <div
                key={`bloque-${creneau.id}`}
                style={{
                  ...HACHURES,
                  gridColumn: colonneDuJour(creneau.jour),
                  gridRow: `${creneau.indexDebut + 2} / span ${Math.max(1, creneau.unitesDisponibles)}`,
                  zIndex: 1,
                }}
                className="border-b border-r border-gray-200"
              />
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
                    width: `calc(${largeur}% - 4px)`,
                    marginLeft: `${lane.lane * largeur}%`,
                    justifySelf: 'start',
                  }}
                  onBasculerVerrou={onBasculerVerrou}
                  onChangerSalle={onChangerSalle}
                  onChangerEnseignant={onChangerEnseignant}
                />
              );
            })}
        </div>
      </div>
    </DndContext>
  );
}
