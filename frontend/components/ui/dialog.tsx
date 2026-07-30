'use client';

import type { ReactNode } from 'react';

/**
 * Largeurs disponibles. `md` est la valeur historique (32 rem) ; `lg` et `xl`
 * servent aux dialogues qui contiennent un tableau (prévisualisation d'import).
 */
const LARGEURS = {
  md: 'max-w-lg',
  lg: 'max-w-3xl',
  xl: 'max-w-5xl',
} as const;

export function Dialog({
  ouvert,
  titre,
  taille = 'md',
  onFermer,
  children,
}: {
  ouvert: boolean;
  titre: string;
  taille?: keyof typeof LARGEURS;
  onFermer: () => void;
  children: ReactNode;
}) {
  if (!ouvert) return null;

  return (
    <div
      className="fixed inset-0 flex items-center justify-center p-4"
      style={{ zIndex: 'var(--z-modal)' }}
    >
      {/* Voile de modale : --surface-overlay du design system. */}
      <div
        className="absolute inset-0 bg-[var(--surface-overlay)]"
        onClick={onFermer}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={titre}
        className={`relative max-h-[92vh] w-full overflow-y-auto rounded-lg border border-line-subtle bg-surface-card p-6 shadow-[var(--shadow-lg)] ${LARGEURS[taille]}`}
      >
        <div className="mb-5 flex items-start justify-between gap-4">
          <h2 className="text-lg font-semibold text-ink-strong">{titre}</h2>
          <button
            type="button"
            onClick={onFermer}
            aria-label="Fermer"
            className="-mr-1 -mt-1 rounded-sm p-1 text-ink-subtle transition-colors hover:bg-surface-hover hover:text-ink-body focus-visible:outline-none focus-visible:shadow-[var(--ring)]"
          >
            <svg
              className="h-5 w-5"
              viewBox="0 0 20 20"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
            >
              <path d="M5 5l10 10M15 5L5 15" strokeLinecap="round" />
            </svg>
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
