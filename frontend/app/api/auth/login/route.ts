import { NextResponse } from 'next/server';
import type { Utilisateur } from '@/lib/types';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';

export async function POST(request: Request): Promise<NextResponse> {
  let corps: unknown;
  try {
    corps = await request.json();
  } catch {
    return NextResponse.json(
      { statut: 400, message: 'Requête invalide.' },
      { status: 400 },
    );
  }

  let reponse: Response;
  try {
    reponse = await fetch(`${BACKEND_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(corps),
      cache: 'no-store',
    });
  } catch {
    return NextResponse.json(
      { statut: 502, message: 'Le serveur JADWAL est injoignable.' },
      { status: 502 },
    );
  }

  let donnees: unknown = null;
  try {
    donnees = await reponse.json();
  } catch {
    donnees = null;
  }

  if (!reponse.ok) {
    return NextResponse.json(
      donnees ?? { statut: reponse.status, message: 'Erreur du serveur.' },
      { status: reponse.status },
    );
  }

  const { token, utilisateur } = donnees as {
    token: string;
    utilisateur: Utilisateur;
  };

  const resultat = NextResponse.json({ utilisateur });
  resultat.cookies.set('jadwal_token', token, {
    httpOnly: true,
    sameSite: 'lax',
    path: '/',
    maxAge: 43200,
    secure: false,
  });
  return resultat;
}
