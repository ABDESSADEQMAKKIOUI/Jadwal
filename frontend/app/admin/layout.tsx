import { redirect } from 'next/navigation';
import type { ReactNode } from 'react';
import AppShell from '@/components/app-shell';
import { getSession } from '@/lib/session';

export default async function AdminLayout({
  children,
}: {
  children: ReactNode;
}) {
  const session = await getSession();
  if (!session || session.role !== 'SUPER_ADMIN') {
    redirect('/login');
  }

  return (
    <AppShell
      nomUtilisateur={session.nomComplet || session.email}
      roleUtilisateur="SUPER ADMIN"
      navigation={[
        { href: '/admin', libelle: 'Tableau de bord' },
        { href: '/admin/etablissements', libelle: 'Établissements' },
        { href: '/admin/plans', libelle: 'Plans' },
        { href: '/admin/paiements', libelle: 'Paiements' },
      ]}
    >
      {children}
    </AppShell>
  );
}
