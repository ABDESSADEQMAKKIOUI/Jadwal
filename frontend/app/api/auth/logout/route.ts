import { NextResponse } from 'next/server';

export async function POST(): Promise<NextResponse> {
  const resultat = new NextResponse(null, { status: 204 });
  resultat.cookies.set('jadwal_token', '', {
    httpOnly: true,
    sameSite: 'lax',
    path: '/',
    maxAge: 0,
    secure: false,
  });
  return resultat;
}
