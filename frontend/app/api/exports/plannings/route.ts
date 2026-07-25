import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';

/**
 * Proxy binaire pour le PDF des emplois du temps.
 *
 * Le proxy générique /api/backend/[...path] réencode les réponses en JSON : il ne peut pas
 * relayer un PDF. Cette route dédiée transmet le flux tel quel, avec le nom de fichier
 * proposé par le backend.
 */
export async function GET(request: NextRequest) {
  const magasin = await cookies();
  const token = magasin.get('jadwal_token')?.value;
  if (!token) {
    return NextResponse.json({ statut: 401, message: 'Session expirée.' }, { status: 401 });
  }

  const url = `${BACKEND_URL}/api/ecole/exports/plannings.pdf${request.nextUrl.search}`;

  let reponse: Response;
  try {
    reponse = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
      cache: 'no-store',
    });
  } catch {
    return NextResponse.json(
      { statut: 502, message: 'Le serveur JADWAL est injoignable.' },
      { status: 502 },
    );
  }

  if (!reponse.ok) {
    // Le backend renvoie une erreur JSON (409 sans planning actif, 404, 403…).
    const texte = await reponse.text();
    try {
      return NextResponse.json(JSON.parse(texte) as unknown, { status: reponse.status });
    } catch {
      return NextResponse.json(
        { statut: reponse.status, message: "L'export a échoué." },
        { status: reponse.status },
      );
    }
  }

  const pdf = await reponse.arrayBuffer();
  return new NextResponse(pdf, {
    status: 200,
    headers: {
      'Content-Type': 'application/pdf',
      'Content-Disposition':
        reponse.headers.get('content-disposition') ?? 'attachment; filename="emplois-du-temps.pdf"',
      'Cache-Control': 'no-store',
    },
  });
}
