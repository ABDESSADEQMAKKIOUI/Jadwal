'use client';

import type { ReactNode } from 'react';

export function Dialog({
  ouvert,
  titre,
  onFermer,
  children,
}: {
  ouvert: boolean;
  titre: string;
  onFermer: () => void;
  children: ReactNode;
}) {
  if (!ouvert) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-gray-900/50"
        onClick={onFermer}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={titre}
        className="relative w-full max-w-lg rounded-xl border border-gray-200 bg-white p-6 shadow-xl"
      >
        <div className="mb-5 flex items-start justify-between">
          <h2 className="text-lg font-semibold text-gray-900">{titre}</h2>
          <button
            type="button"
            onClick={onFermer}
            aria-label="Fermer"
            className="rounded-md p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
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
