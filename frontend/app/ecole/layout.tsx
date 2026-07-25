import { redirect } from 'next/navigation';
import type { ReactNode } from 'react';
import AppShell from '@/components/app-shell';
import { getSession } from '@/lib/session';

export default async function EcoleLayout({
  children,
}: {
  children: ReactNode;
}) {
  const session = await getSession();
  if (!session || session.role !== 'DIRECTEUR') {
    redirect('/login');
  }

  return (
    <AppShell
      nomUtilisateur={session.nomComplet}
      navigation={[
        { href: '/ecole', libelle: 'Tableau de bord' },
        { href: '/ecole/referentiel', libelle: 'Référentiel' },
        { href: '/ecole/enseignants', libelle: 'Enseignants' },
        { href: '/ecole/maquettes', libelle: 'Maquettes' },
        { href: '/ecole/generation', libelle: 'Génération' },
        { href: '/ecole/planning', libelle: 'Planning' },
      ]}
    >
      {children}
    </AppShell>
  );
}
