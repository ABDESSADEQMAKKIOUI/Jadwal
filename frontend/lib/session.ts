import { cookies } from 'next/headers';
import type { Role } from './types';

export interface Session {
  id: string;
  email: string;
  role: Role;
  etablissementId: number | null;
  nomComplet: string;
}

interface ChargeUtileJwt {
  sub?: unknown;
  email?: unknown;
  role?: unknown;
  etablissementId?: unknown;
  nomComplet?: unknown;
  exp?: unknown;
}

/**
 * Lit le cookie jadwal_token et décode la charge utile du JWT (base64url).
 * La signature n'est PAS vérifiée ici : la vérification réelle est faite par
 * le backend sur chaque appel proxifié. On gère seulement l'expiration (exp)
 * et les tokens malformés.
 */
export async function getSession(): Promise<Session | null> {
  const magasin = await cookies();
  const token = magasin.get('jadwal_token')?.value;
  if (!token) return null;

  const parties = token.split('.');
  if (parties.length !== 3) return null;

  try {
    const brut = Buffer.from(parties[1], 'base64url').toString('utf8');
    const charge = JSON.parse(brut) as ChargeUtileJwt;

    if (typeof charge.exp === 'number' && charge.exp * 1000 < Date.now()) {
      return null;
    }
    if (typeof charge.sub !== 'string' || typeof charge.email !== 'string') {
      return null;
    }
    if (charge.role !== 'SUPER_ADMIN' && charge.role !== 'DIRECTEUR') {
      return null;
    }

    return {
      id: charge.sub,
      email: charge.email,
      role: charge.role,
      etablissementId:
        typeof charge.etablissementId === 'number' ? charge.etablissementId : null,
      nomComplet: typeof charge.nomComplet === 'string' ? charge.nomComplet : '',
    };
  } catch {
    return null;
  }
}
