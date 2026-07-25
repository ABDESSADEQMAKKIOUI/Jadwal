import { cookies } from 'next/headers';
import { NextRequest } from 'next/server';

/**
 * Proxy SSE spécifique pour le suivi d'une génération.
 * Le catch-all /api/backend/[...path] bufferise la réponse (texte complet) :
 * ce handler plus spécifique prend le pas et relaie le flux tel quel.
 */

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';

export const dynamic = 'force-dynamic';

type Contexte = { params: Promise<{ id: string }> };

export async function GET(request: NextRequest, contexte: Contexte) {
  const { id } = await contexte.params;
  const magasin = await cookies();
  const token = magasin.get('jadwal_token')?.value;

  const entetes: Record<string, string> = { Accept: 'text/event-stream' };
  if (token) {
    entetes.Authorization = `Bearer ${token}`;
  }

  let reponse: Response;
  try {
    reponse = await fetch(
      `${BACKEND_URL}/api/ecole/generations/${encodeURIComponent(id)}/stream`,
      {
        method: 'GET',
        headers: entetes,
        cache: 'no-store',
        signal: request.signal,
      },
    );
  } catch {
    return new Response(
      JSON.stringify({ statut: 502, message: 'Le serveur JADWAL est injoignable.' }),
      { status: 502, headers: { 'Content-Type': 'application/json' } },
    );
  }

  if (!reponse.ok || reponse.body === null) {
    let texte = '';
    try {
      texte = await reponse.text();
    } catch {
      texte = '';
    }
    return new Response(texte.length > 0 ? texte : null, {
      status: reponse.status,
    });
  }

  return new Response(reponse.body, {
    status: 200,
    headers: {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-transform',
      Connection: 'keep-alive',
      'X-Accel-Buffering': 'no',
    },
  });
}
