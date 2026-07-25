'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import type { ReactNode } from 'react';

export interface ElementNavigation {
  href: string;
  libelle: string;
}

export default function AppShell({
  navigation,
  nomUtilisateur,
  children,
}: {
  navigation: ElementNavigation[];
  nomUtilisateur: string;
  children: ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();

  function estActif(href: string): boolean {
    if (pathname === href) return true;
    const segments = href.split('/').filter((s) => s.length > 0);
    return segments.length > 1 && pathname.startsWith(`${href}/`);
  }

  async function seDeconnecter() {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
      router.push('/login');
      router.refresh();
    }
  }

  return (
    <div className="min-h-screen">
      <aside className="fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-slate-900">
        <div className="flex h-16 items-center border-b border-slate-800 px-5">
          <Link href="/">
            <img src="/jadwal.svg" alt="JADWAL" className="h-9 w-auto" />
          </Link>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {navigation.map((element) => (
            <Link
              key={element.href}
              href={element.href}
              className={`block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                estActif(element.href)
                  ? 'bg-slate-800 text-white underline decoration-indigo-400 decoration-2 underline-offset-4'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              {element.libelle}
            </Link>
          ))}
        </nav>
        <div className="border-t border-slate-800 px-5 py-4">
          <p className="truncate text-sm font-medium text-white" title={nomUtilisateur}>
            {nomUtilisateur}
          </p>
          <button
            type="button"
            onClick={() => {
              void seDeconnecter();
            }}
            className="mt-2 text-sm text-slate-400 transition-colors hover:text-white"
          >
            Se déconnecter
          </button>
        </div>
      </aside>
      <main className="ml-64 min-h-screen p-8">{children}</main>
    </div>
  );
}
