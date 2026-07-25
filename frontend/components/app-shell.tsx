'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import type { ReactNode } from 'react';

export interface ElementNavigation {
  href: string;
  libelle: string;
}

/** Deux initiales au plus, pour la pastille d'identité du pied de barre. */
function initiales(nom: string): string {
  const lettres = nom
    .trim()
    .split(/\s+/)
    .map((mot) => mot[0])
    .filter((c): c is string => Boolean(c) && /\p{L}/u.test(c));
  return lettres.slice(0, 2).join('').toUpperCase() || 'JD';
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
      {/* Chrome sombre : --surface-inverse, largeur --sidebar-width (248 px). */}
      <aside
        className="fixed inset-y-0 left-0 flex w-sidebar flex-col bg-surface-inverse"
        style={{ zIndex: 'var(--z-sidebar)' }}
      >
        <div className="flex h-[var(--topbar-height)] items-center border-b border-white/10 px-5">
          <Link href="/" className="inline-flex items-center gap-2.5">
            <img src="/jadwal.svg" alt="JADWAL" className="h-8 w-auto" />
          </Link>
        </div>
        <nav className="flex-1 space-y-0.5 px-3 py-4">
          {navigation.map((element) => {
            const actif = estActif(element.href);
            return (
              <Link
                key={element.href}
                href={element.href}
                aria-current={actif ? 'page' : undefined}
                className={`relative block rounded-sm px-3 py-2 text-base font-medium transition-colors duration-[var(--duration-fast)] ${
                  actif
                    ? 'bg-white/10 text-ink-on-dark'
                    : 'text-white/70 hover:bg-white/5 hover:text-ink-on-dark'
                }`}
              >
                {/* Repère teal de l'élément actif, à la place du soulignement. */}
                {actif && (
                  <span
                    aria-hidden="true"
                    className="absolute inset-y-1.5 left-0 w-0.5 rounded-[var(--radius-pill)] bg-brand"
                  />
                )}
                {element.libelle}
              </Link>
            );
          })}
        </nav>
        <div className="border-t border-white/10 px-5 py-4">
          <div className="flex items-center gap-2.5">
            {/* Pastille d'initiales : composant Avatar du design system. */}
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-[var(--radius-pill)] bg-brand text-xs font-semibold text-on-brand">
              {initiales(nomUtilisateur)}
            </span>
            <p
              className="truncate text-base font-medium text-ink-on-dark"
              title={nomUtilisateur}
            >
              {nomUtilisateur}
            </p>
          </div>
          <button
            type="button"
            onClick={() => {
              void seDeconnecter();
            }}
            className="mt-3 rounded-sm text-sm text-white/60 transition-colors hover:text-ink-on-dark focus-visible:outline-none focus-visible:shadow-[var(--ring)]"
          >
            Se déconnecter
          </button>
        </div>
      </aside>
      <main className="ml-sidebar min-h-screen p-8">{children}</main>
    </div>
  );
}
