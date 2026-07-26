'use client';

import { useDraggable } from '@dnd-kit/core';
import { useEffect, useState, type CSSProperties, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import type { Seance, VuePlanning } from '@/lib/types-planning';

/** Couleur de repli quand la matière n'a pas de couleur (maquette : var(--color-primary)). */
const COULEUR_DEFAUT = 'var(--color-primary)';

const LARGEUR_MENU = 184;

/** Icône cadenas de la maquette (séance verrouillée, I-03). */
function IconeCadenas() {
  return (
    <svg
      width="11"
      height="11"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ color: 'var(--text-muted)', flex: 'none' }}
      aria-label="Séance verrouillée"
    >
      <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  );
}

/** Icône « … » de la maquette (ouverture du menu de séance). */
function IconeActions() {
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
      <circle cx="12" cy="12" r="1" />
      <circle cx="19" cy="12" r="1" />
      <circle cx="5" cy="12" r="1" />
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
      role="menuitem"
      onClick={onClick}
      style={{
        display: 'block',
        width: '100%',
        border: 0,
        background: 'transparent',
        padding: '7px 12px',
        textAlign: 'left',
        fontSize: 'var(--text-sm)',
        fontFamily: 'var(--font-sans)',
        color: 'var(--text-body)',
        cursor: 'pointer',
        transition: 'background var(--duration-fast) var(--ease-standard)',
      }}
      onMouseEnter={(evenement) => {
        evenement.currentTarget.style.background = 'var(--surface-hover)';
        evenement.currentTarget.style.color = 'var(--text-strong)';
      }}
      onMouseLeave={(evenement) => {
        evenement.currentTarget.style.background = 'transparent';
        evenement.currentTarget.style.color = 'var(--text-body)';
      }}
    >
      {children}
    </button>
  );
}

/**
 * Bloc séance positionné dans la grille hebdomadaire (rendu de la maquette).
 * Draggable (désactivé si verrouillée) + menu d'actions (verrou, salle, enseignant).
 *
 * Le bloc garde le `overflow:hidden` de la maquette : le menu est donc rendu
 * dans un portail en position fixe, ce qui l'empêche d'être rogné par le bloc
 * ou par le défilement horizontal de la grille.
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
  const [menu, setMenu] = useState<{ gauche: number; haut: number } | null>(
    null,
  );
  const { attributes, listeners, setNodeRef, transform, isDragging } =
    useDraggable({
      id: `seance-${seance.id}`,
      disabled: seance.verrouillee,
    });

  // Le menu est positionné en coordonnées écran : on le referme si la page bouge.
  useEffect(() => {
    if (menu === null) return;
    const fermer = () => setMenu(null);
    window.addEventListener('scroll', fermer, true);
    window.addEventListener('resize', fermer);
    return () => {
      window.removeEventListener('scroll', fermer, true);
      window.removeEventListener('resize', fermer);
    };
  }, [menu]);

  function ouvrirMenu(gaucheSouhaitee: number, hautSouhaite: number) {
    const gauche = Math.max(
      8,
      Math.min(gaucheSouhaitee, window.innerWidth - LARGEUR_MENU - 8),
    );
    const haut = Math.max(8, Math.min(hautSouhaite, window.innerHeight - 148));
    setMenu({ gauche, haut });
  }

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
        if (menu !== null) {
          setMenu(null);
          return;
        }
        ouvrirMenu(evenement.clientX, evenement.clientY);
      }}
      style={{
        // Apparence de la maquette (s.style), la position en grille vient du parent.
        position: 'relative',
        margin: 3,
        display: 'flex',
        minHeight: 0,
        flexDirection: 'column',
        gap: 1,
        overflow: 'hidden',
        borderRadius: 'var(--radius-sm)',
        border: '1px solid var(--border-subtle)',
        borderLeft: `3px solid ${couleur}`,
        background: 'var(--surface-card)',
        boxShadow: 'var(--shadow-xs)',
        padding: '6px 8px',
        lineHeight: 1.3,
        cursor: seance.verrouillee ? 'default' : 'grab',
        ...style,
        zIndex: isDragging ? 40 : 10,
        transform:
          transform !== null
            ? `translate3d(${transform.x}px, ${transform.y}px, 0)`
            : undefined,
        ...(isDragging
          ? {
              boxShadow: 'var(--shadow-lg)',
              opacity: 0.92,
              cursor: 'grabbing',
            }
          : {}),
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 4,
        }}
      >
        <p
          style={{
            margin: 0,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            fontSize: 'var(--text-xs)',
            fontWeight: 'var(--weight-semibold)',
            color: 'var(--text-strong)',
          }}
        >
          {seance.matiereLibelle}
        </p>
        <span
          style={{
            display: 'flex',
            flex: 'none',
            alignItems: 'center',
            gap: 4,
          }}
        >
          {seance.semaine !== 'TOUTES' && (
            <span
              style={{
                borderRadius: 'var(--radius-xs)',
                background: 'var(--surface-card)',
                border: '1px solid var(--border-subtle)',
                padding: '0 4px',
                fontSize: 9,
                fontWeight: 'var(--weight-bold)',
                color: 'var(--text-muted)',
              }}
            >
              {seance.semaine}
            </span>
          )}
          {seance.verrouillee && <IconeCadenas />}
          <button
            type="button"
            aria-label="Actions sur la séance"
            aria-haspopup="menu"
            aria-expanded={menu !== null}
            onPointerDown={(evenement) => evenement.stopPropagation()}
            onClick={(evenement) => {
              evenement.stopPropagation();
              if (menu !== null) {
                setMenu(null);
                return;
              }
              const rectangle = evenement.currentTarget.getBoundingClientRect();
              ouvrirMenu(rectangle.right - LARGEUR_MENU, rectangle.bottom + 6);
            }}
            style={{
              border: 0,
              background: 'transparent',
              padding: 0,
              color: 'var(--text-subtle)',
              cursor: 'pointer',
              lineHeight: 0,
            }}
          >
            <IconeActions />
          </button>
        </span>
      </div>
      {lignes.map((ligne, index) => (
        <p
          key={`${index}-${ligne}`}
          style={{
            margin: 0,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            fontSize: 'var(--text-2xs)',
            color: 'var(--text-muted)',
          }}
        >
          {ligne}
        </p>
      ))}

      {menu !== null &&
        createPortal(
          <div
            onPointerDown={(evenement) => evenement.stopPropagation()}
            onContextMenu={(evenement) => {
              evenement.preventDefault();
              evenement.stopPropagation();
            }}
          >
            <div
              aria-hidden="true"
              onClick={() => setMenu(null)}
              style={{
                position: 'fixed',
                inset: 0,
                zIndex: 'var(--z-dropdown)',
                cursor: 'default',
              }}
            />
            <div
              role="menu"
              aria-label={`Actions sur la séance ${seance.matiereLibelle}`}
              style={{
                position: 'fixed',
                left: menu.gauche,
                top: menu.haut,
                width: LARGEUR_MENU,
                zIndex: 'calc(var(--z-dropdown) + 1)',
                background: 'var(--surface-card)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-sm)',
                boxShadow: 'var(--shadow-lg)',
                padding: '4px 0',
                overflow: 'hidden',
              }}
            >
              <BoutonMenu
                onClick={() => {
                  setMenu(null);
                  onBasculerVerrou(seance);
                }}
              >
                {seance.verrouillee ? 'Déverrouiller' : 'Verrouiller'}
              </BoutonMenu>
              <BoutonMenu
                onClick={() => {
                  setMenu(null);
                  onChangerSalle(seance);
                }}
              >
                Changer de salle
              </BoutonMenu>
              <BoutonMenu
                onClick={() => {
                  setMenu(null);
                  onChangerEnseignant(seance);
                }}
              >
                Changer d’enseignant
              </BoutonMenu>
            </div>
          </div>,
          document.body,
        )}
    </div>
  );
}
