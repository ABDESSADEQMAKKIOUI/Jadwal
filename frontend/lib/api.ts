'use client';

/**
 * Client HTTP côté navigateur : tout passe par le proxy BFF /api/backend/*.
 * Le navigateur n'appelle JAMAIS le backend directement.
 */

export function extraireMessageErreur(donnees: unknown, defaut: string): string {
  if (donnees !== null && typeof donnees === 'object' && 'message' in donnees) {
    const message = (donnees as { message?: unknown }).message;
    if (typeof message === 'string' && message.length > 0) return message;
  }
  return defaut;
}

export async function apiFetch<T = unknown>(
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
    throw new Error('Impossible de contacter le serveur.');
  }

  if (reponse.status === 401) {
    window.location.href = '/login';
    throw new Error('Session expirée. Redirection vers la page de connexion…');
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
    throw new Error(extraireMessageErreur(donnees, 'Une erreur est survenue.'));
  }

  return donnees as T;
}
