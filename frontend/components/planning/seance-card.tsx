'use client';

import { useDraggable } from '@dnd-kit/core';
import { useState, type CSSProperties, type ReactNode } from 'react';
import type { Seance, VuePlanning } from '@/lib/types-planning';

const COULEUR_DEFAUT = '#4f46e5';

/** Icône cadenas (séance verrouillée, I-03). */
function IconeCadenas() {
  return (
    <svg
      className="h-3 w-3 shrink-0 text-gray-500"
      viewBox="0 0 20 20"
      fill="currentColor"
      aria-label="Séance verrouillée"
    >
      <path
        fillRule="evenodd"
        d="M10 2a4 4 0 0 0-4 4v2H5a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2h-1V6a4 4 0 0 0-4-4Zm2 6V6a2 2 0 1 0-4 0v2h4Z"
        clipRule="evenodd"
      />
    </svg>
  );
}

function BoutonMenu({
  onClick,
  children,
}: {
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="block w-full px-3 py-1.5 text-left text-xs text-gray-700 hover:bg-gray-50 hover:text-gray-900"
    >
      {children}
    </button>
  );
}

/**
 * Bloc séance positionné dans la grille hebdomadaire.
 * Draggable (désactivé si verrouillée) + menu d'actions (verrou, salle, enseignant).
 */
export function SeanceCard({
  seance,
  vue,
  style,
  onBasculerVerrou,
  onChangerSalle,
  onChangerEnseignant,
}: {
  seance: Seance;
  vue: VuePlanning;
  style: CSSProperties;
  onBasculerVerrou: (seance: Seance) => void;
  onChangerSalle: (seance: Seance) => void;
  onChangerEnseignant: (seance: Seance) => void;
}) {
  const [menuOuvert, setMenuOuvert] = useState(false);
  const { attributes, listeners, setNodeRef, transform, isDragging } =
    useDraggable({
      id: `seance-${seance.id}`,
      disabled: seance.verrouillee,
    });

  const couleur =
    seance.couleur !== null && seance.couleur.length > 0
      ? seance.couleur
      : COULEUR_DEFAUT;

  const lignes: string[] = [];
  if (vue === 'GROUPE') {
    if (seance.enseignantNom !== null) lignes.push(seance.enseignantNom);
    if (seance.salleNom !== null) lignes.push(seance.salleNom);
  } else if (vue === 'ENSEIGNANT') {
    lignes.push(seance.groupeLibelle);
    if (seance.salleNom !== null) lignes.push(seance.salleNom);
  } else {
    lignes.push(seance.groupeLibelle);
    if (seance.enseignantNom !== null) lignes.push(seance.enseignantNom);
  }

  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      onContextMenu={(evenement) => {
        evenement.preventDefault();
        setMenuOuvert((ouvert) => !ouvert);
      }}
      style={{
        ...style,
        borderLeftColor: couleur,
        backgroundColor: `${couleur}1a`,
        transform:
          transform !== null
            ? `translate3d(${transform.x}px, ${transform.y}px, 0)`
            : undefined,
        zIndex: isDragging ? 40 : menuOuvert ? 30 : 10,
      }}
      className={`relative m-0.5 flex min-h-0 flex-col overflow-visible rounded-md border border-gray-200 border-l-4 bg-white p-1.5 text-[11px] leading-tight shadow-sm ${
        seance.verrouillee ? 'cursor-default' : 'cursor-grab active:cursor-grabbing'
      } ${isDragging ? 'opacity-90 shadow-lg ring-2 ring-indigo-300' : ''}`}
    >
      <div className="flex items-start justify-between gap-1">
        <p className="truncate font-semibold text-gray-900">
          {seance.matiereLibelle}
        </p>
        <span className="flex shrink-0 items-center gap-1">
          {seance.semaine !== 'TOUTES' && (
            <span className="rounded bg-indigo-100 px-1 text-[9px] font-bold text-indigo-700">
              {seance.semaine}
            </span>
          )}
          {seance.verrouillee && <IconeCadenas />}
          <button
            type="button"
            aria-label="Actions sur la séance"
            onPointerDown={(evenement) => evenement.stopPropagation()}
            onClick={(evenement) => {
              evenement.stopPropagation();
              setMenuOuvert((ouvert) => !ouvert);
            }}
            className="rounded p-0.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
          >
            <svg className="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
              <path d="M10 6a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Zm0 5.5a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Zm0 5.5a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Z" />
            </svg>
          </button>
        </span>
      </div>
      {lignes.map((ligne, index) => (
        <p key={`${index}-${ligne}`} className="truncate text-gray-600">
          {ligne}
        </p>
      ))}

      {menuOuvert && (
        <div
          onPointerDown={(evenement) => evenement.stopPropagation()}
          onContextMenu={(evenement) => {
            evenement.preventDefault();
            evenement.stopPropagation();
          }}
        >
          <div
            className="fixed inset-0 z-40 cursor-default"
            aria-hidden="true"
            onClick={(evenement) => {
              evenement.stopPropagation();
              setMenuOuvert(false);
            }}
          />
          <div className="absolute right-0 top-6 z-50 w-44 rounded-lg border border-gray-200 bg-white py-1 shadow-lg">
            <BoutonMenu
              onClick={() => {
                setMenuOuvert(false);
                onBasculerVerrou(seance);
              }}
            >
              {seance.verrouillee ? 'Déverrouiller' : 'Verrouiller'}
            </BoutonMenu>
            <BoutonMenu
              onClick={() => {
                setMenuOuvert(false);
                onChangerSalle(seance);
              }}
            >
              Changer de salle
            </BoutonMenu>
            <BoutonMenu
              onClick={() => {
                setMenuOuvert(false);
                onChangerEnseignant(seance);
              }}
            >
              Changer d’enseignant
            </BoutonMenu>
          </div>
        </div>
      )}
    </div>
  );
}
