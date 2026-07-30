import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';

type Contexte = { params: Promise<{ path: string[] }> };

async function proxifier(
  request: NextRequest,
  contexte: Contexte,
  methode: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
): Promise<NextResponse> {
  const { path } = await contexte.params;
  const magasin = await cookies();
  const token = magasin.get('jadwal_token')?.value;

  const url = `${BACKEND_URL}/api/${path.join('/')}${request.nextUrl.search}`;

  /*
   * Un corps qui n'est pas du JSON (import Massar en multipart/form-data) doit
   * être relayé octet pour octet, en conservant son Content-Type : celui-ci
   * porte la frontière des parties, et la réécrire en JSON rendrait le corps
   * illisible pour le backend.
   */
  const typeEntrant = request.headers.get('content-type');
  const typeBinaire =
    typeEntrant !== null && !typeEntrant.includes('application/json') ? typeEntrant : null;

  const entetes: Record<string, string> = {
    'Content-Type': typeBinaire ?? 'application/json',
  };
  if (token) {
    entetes.Authorization = `Bearer ${token}`;
  }

  const init: RequestInit = { method: methode, headers: entetes, cache: 'no-store' };
  if (methode === 'POST' || methode === 'PUT' || methode === 'PATCH') {
    if (typeBinaire !== null) {
      const corps = await request.arrayBuffer();
      if (corps.byteLength > 0) {
        init.body = corps;
      }
    } else {
      const corps = await request.text();
      if (corps.length > 0) {
        init.body = corps;
      }
    }
  }

  let reponse: Response;
  try {
    reponse = await fetch(url, init);
  } catch {
    return NextResponse.json(
      { statut: 502, message: 'Le serveur JADWAL est injoignable.' },
      { status: 502 },
    );
  }

  if (reponse.status === 204) {
    return new NextResponse(null, { status: 204 });
  }

  const texte = await reponse.text();
  if (texte.length === 0) {
    return new NextResponse(null, { status: reponse.status });
  }

  try {
    return NextResponse.json(JSON.parse(texte) as unknown, {
      status: reponse.status,
    });
  } catch {
    return new NextResponse(texte, { status: reponse.status });
  }
}

export async function GET(request: NextRequest, contexte: Contexte) {
  return proxifier(request, contexte, 'GET');
}

export async function POST(request: NextRequest, contexte: Contexte) {
  return proxifier(request, contexte, 'POST');
}

export async function PUT(request: NextRequest, contexte: Contexte) {
  return proxifier(request, contexte, 'PUT');
}

export async function PATCH(request: NextRequest, contexte: Contexte) {
  return proxifier(request, contexte, 'PATCH');
}

export async function DELETE(request: NextRequest, contexte: Contexte) {
  return proxifier(request, contexte, 'DELETE');
}
