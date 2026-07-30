import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';

/**
 * Proxy binaire pour l'export de la liste des élèves (CSV ou XLSX).
 *
 * Le proxy générique /api/backend/[...path] réencode les réponses en JSON : il
 * ne peut pas relayer un classeur ni un CSV avec BOM. Cette route dédiée
 * transmet les octets tels quels, avec le nom de fichier proposé par le backend.
 *
 * Aucun établissement n'est transmis : le backend le déduit du jeton. Seuls les
 * paramètres du contrat sont relayés, et rien du contenu exporté n'est
 * journalisé — ce sont des données personnelles de mineurs.
 */

/** Paramètres du contrat, relayés tels quels. Tout le reste est ignoré. */
const PARAMETRES_RELAYES = [
  'champs',
  'recherche',
  'groupeId',
  'niveauId',
  'statut',
] as const;

const CHEMINS = {
  csv: 'export.csv',
  xlsx: 'export.xlsx',
} as const;

const TYPES_PAR_DEFAUT = {
  csv: 'text/csv; charset=utf-8',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
} as const;

export async function GET(request: NextRequest) {
  const magasin = await cookies();
  const token = magasin.get('jadwal_token')?.value;
  if (!token) {
    return NextResponse.json({ statut: 401, message: 'Session expirée.' }, { status: 401 });
  }

  const demande = request.nextUrl.searchParams;
  const format = demande.get('format') === 'xlsx' ? 'xlsx' : 'csv';

  const parametres = new URLSearchParams();
  for (const cle of PARAMETRES_RELAYES) {
    const valeur = demande.get(cle);
    if (valeur !== null && valeur.length > 0) {
      parametres.set(cle, valeur);
    }
  }

  const requete = parametres.toString();
  const url =
    `${BACKEND_URL}/api/ecole/eleves/${CHEMINS[format]}` +
    (requete.length > 0 ? `?${requete}` : '');

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
    // Le backend renvoie une erreur JSON (403 module absent, 400 champ inconnu…).
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

  const fichier = await reponse.arrayBuffer();
  return new NextResponse(fichier, {
    status: 200,
    headers: {
      'Content-Type': reponse.headers.get('content-type') ?? TYPES_PAR_DEFAUT[format],
      'Content-Disposition':
        reponse.headers.get('content-disposition') ??
        `attachment; filename="eleves.${format}"`,
      'Cache-Control': 'no-store',
    },
  });
}
