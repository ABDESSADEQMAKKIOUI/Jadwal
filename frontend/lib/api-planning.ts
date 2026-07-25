'use client';

import { extraireMessageErreur } from './api';
import type { Conflit, FaisabiliteRapport } from './types-planning';

/**
 * Variante d'apiFetch qui conserve le corps JSON des réponses en erreur
 * (nécessaire pour le rapport de faisabilité d'un 422 et les conflits d'un 409).
 */
export class ErreurApi extends Error {
  readonly statut: number;
  readonly corps: unknown;

  constructor(statut: number, message: string, corps: unknown) {
    super(message);
    this.name = 'ErreurApi';
    this.statut = statut;
    this.corps = corps;
  }
}

export async function apiFetchDetaille<T = unknown>(
  chemin: string,
  init?: RequestInit,
): Promise<T> {
  let reponse: Response;
  try {
    reponse = await fetch(`/api/backend${chemin}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...((init?.headers as Record<string, string> | undefined) ?? {}),
      },
    });
  } catch {
    throw new ErreurApi(0, 'Impossible de contacter le serveur.', null);
  }

  if (reponse.status === 401) {
    window.location.href = '/login';
    throw new ErreurApi(
      401,
      'Session expirée. Redirection vers la page de connexion…',
      null,
    );
  }

  let donnees: unknown = null;
  const texte = await reponse.text();
  if (texte.length > 0) {
    try {
      donnees = JSON.parse(texte);
    } catch {
      donnees = null;
    }
  }

  if (!reponse.ok) {
    throw new ErreurApi(
      reponse.status,
      extraireMessageErreur(donnees, 'Une erreur est survenue.'),
      donnees,
    );
  }

  return donnees as T;
}

/** Extrait la liste des conflits d'un corps d'erreur 409 (I-04). */
export function extraireConflits(corps: unknown): Conflit[] {
  if (corps === null || typeof corps !== 'object') return [];
  const conflits = (corps as Record<string, unknown>).conflits;
  if (!Array.isArray(conflits)) return [];
  const resultat: Conflit[] = [];
  for (const element of conflits) {
    if (element !== null && typeof element === 'object') {
      const enregistrement = element as Record<string, unknown>;
      resultat.push({
        regle:
          typeof enregistrement.regle === 'string' ? enregistrement.regle : '',
        message:
          typeof enregistrement.message === 'string'
            ? enregistrement.message
            : '',
      });
    }
  }
  return resultat;
}

/** Extrait le rapport de faisabilité d'un corps d'erreur 422. */
export function extraireRapport(corps: unknown): FaisabiliteRapport | null {
  if (corps === null || typeof corps !== 'object') return null;
  const rapport = (corps as Record<string, unknown>).rapport;
  if (rapport === null || rapport === undefined || typeof rapport !== 'object') {
    return null;
  }
  const enregistrement = rapport as Record<string, unknown>;
  if (!Array.isArray(enregistrement.bilans)) return null;
  return rapport as FaisabiliteRapport;
}
