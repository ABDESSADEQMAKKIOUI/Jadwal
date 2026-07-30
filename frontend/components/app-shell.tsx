'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import type { CSSProperties, ReactNode } from 'react';

import './chrome.css';

/** Clés du jeu d'icônes de la barre latérale (SVG recopiés de la maquette). */
export type CleIcone =
  | 'tableauDeBord'
  | 'planning'
  | 'generation'
  | 'referentiel'
  | 'enseignants'
  | 'eleves'
  | 'maquettes'
  | 'ponderations'
  | 'etablissements'
  | 'plans'
  | 'paiements'
  | 'absences'
  | 'defaut';

export interface ElementNavigation {
  href: string;
  libelle: string;
  /** Icône explicite ; à défaut elle est déduite du href (voir ICONE_PAR_HREF). */
  icone?: CleIcone;
}

/* Attributs communs aux icônes de navigation, tels quels depuis la maquette. */
const PROPS_SVG = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  style: { flex: 'none', display: 'block' },
} as const;

const ICONES: Record<CleIcone, ReactNode> = {
  tableauDeBord: (
    <svg {...PROPS_SVG}>
      <rect x="3" y="3" width="7" height="9" rx="1" />
      <rect x="14" y="3" width="7" height="5" rx="1" />
      <rect x="14" y="12" width="7" height="9" rx="1" />
      <rect x="3" y="16" width="7" height="5" rx="1" />
    </svg>
  ),
  planning: (
    <svg {...PROPS_SVG}>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4" />
      <path d="M8 2v4" />
      <path d="M3 10h18" />
    </svg>
  ),
  generation: (
    <svg {...PROPS_SVG}>
      <path d="M12 8V4H8" />
      <rect x="4" y="8" width="16" height="12" rx="2" />
      <path d="M2 14h2" />
      <path d="M20 14h2" />
      <path d="M15 13v2" />
      <path d="M9 13v2" />
    </svg>
  ),
  referentiel: (
    <svg {...PROPS_SVG}>
      <path d="M12 7v14" />
      <path d="M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z" />
    </svg>
  ),
  enseignants: (
    <svg {...PROPS_SVG}>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  ),
  /* Élèves : toque de diplômé, même trait que les icônes de la maquette. */
  eleves: (
    <svg {...PROPS_SVG}>
      <path d="M22 10 12 5 2 10l10 5z" />
      <path d="M6 12v5c0 1.66 2.69 3 6 3s6-1.34 6-3v-5" />
      <path d="M22 10v6" />
    </svg>
  ),
  /* Absences : porte-bloc coché, l'objet même de la feuille d'appel. */
  absences: (
    <svg {...PROPS_SVG}>
      <rect x="8" y="2" width="8" height="4" rx="1" />
      <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
      <path d="m9 14 2 2 4-4" />
    </svg>
  ),
  maquettes: (
    <svg {...PROPS_SVG}>
      <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z" />
      <path d="M14 2v5h5" />
      <path d="M16 13H8" />
      <path d="M16 17H8" />
      <path d="M10 9H8" />
    </svg>
  ),
  ponderations: (
    <svg {...PROPS_SVG}>
      <line x1="4" y1="21" x2="4" y2="14" />
      <line x1="4" y1="10" x2="4" y2="3" />
      <line x1="12" y1="21" x2="12" y2="12" />
      <line x1="12" y1="8" x2="12" y2="3" />
      <line x1="20" y1="21" x2="20" y2="16" />
      <line x1="20" y1="12" x2="20" y2="3" />
      <line x1="2" y1="14" x2="6" y2="14" />
      <line x1="10" y1="8" x2="14" y2="8" />
      <line x1="18" y1="16" x2="22" y2="16" />
    </svg>
  ),
  /* Écrans admin plateforme : absents de la maquette, icônes du même trait (24, stroke 1.75). */
  etablissements: (
    <svg {...PROPS_SVG}>
      <path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18" />
      <path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2" />
      <path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2" />
      <path d="M10 6h4" />
      <path d="M10 10h4" />
      <path d="M10 14h4" />
      <path d="M10 18h4" />
    </svg>
  ),
  plans: (
    <svg {...PROPS_SVG}>
      <path d="M12 2 2 7l10 5 10-5z" />
      <path d="M2 17l10 5 10-5" />
      <path d="M2 12l10 5 10-5" />
    </svg>
  ),
  paiements: (
    <svg {...PROPS_SVG}>
      <rect x="2" y="4" width="20" height="16" rx="2" />
      <path d="M2 10h20" />
      <path d="M6 15h4" />
    </svg>
  ),
  defaut: (
    <svg {...PROPS_SVG}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 8v4" />
      <path d="M12 16h.01" />
    </svg>
  ),
};

/** Icône par défaut d'une entrée, pour que les layouts n'aient rien à déclarer. */
const ICONE_PAR_HREF: Record<string, CleIcone> = {
  '/ecole': 'tableauDeBord',
  '/ecole/planning': 'planning',
  '/ecole/generation': 'generation',
  '/ecole/referentiel': 'referentiel',
  '/ecole/enseignants': 'enseignants',
  '/ecole/eleves': 'eleves',
  '/ecole/absences': 'absences',
  '/ecole/maquettes': 'maquettes',
  '/ecole/ponderations': 'ponderations',
  '/admin': 'tableauDeBord',
  '/admin/etablissements': 'etablissements',
  '/admin/plans': 'plans',
  '/admin/paiements': 'paiements',
};

/* Chrome de la maquette : SidebarItem (mêmes valeurs, tokens à la place des rgba teal). */
const NAV_BASE: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '11px',
  width: '100%',
  padding: '9px 12px',
  border: 'none',
  borderRadius: 'var(--radius-sm)',
  textAlign: 'left',
  position: 'relative',
  textDecoration: 'none',
  fontSize: 'var(--text-base)',
  cursor: 'pointer',
  transition: 'background var(--duration-fast), color var(--duration-fast)',
};
/** rgba(71,163,152,0.20) de la maquette, exprimé sur le token teal-500. */
const NAV_FOND_ACTIF = 'color-mix(in srgb, var(--teal-500) 20%, transparent)';
const NAV_FOND_SURVOL = 'rgba(255,255,255,0.06)';
const NAV_OMBRE_ACTIVE = 'inset 2px 0 0 var(--teal-400)';

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
  roleUtilisateur,
  titre,
  children,
}: {
  navigation: ElementNavigation[];
  nomUtilisateur: string;
  /** Seconde ligne du pied de barre : rôle de l'utilisateur ou nom de l'établissement. */
  roleUtilisateur?: string;
  /** Surcharge du titre de la barre supérieure ; par défaut celui de l'entrée active. */
  titre?: string;
  children: ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();

  function estActif(href: string): boolean {
    if (pathname === href) return true;
    const segments = href.split('/').filter((s) => s.length > 0);
    return segments.length > 1 && pathname.startsWith(`${href}/`);
  }

  // Titre de page = libellé de l'entrée active ; un sous-chemin (/ecole/enseignants/12)
  // reprend donc le libellé de son parent. On garde l'entrée la plus spécifique.
  const entreeActive = navigation
    .filter((element) => estActif(element.href))
    .sort((a, b) => b.href.length - a.href.length)[0];
  const titrePage = titre ?? entreeActive?.libelle ?? 'JADWAL';

  async function seDeconnecter() {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
      router.push('/login');
      router.refresh();
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'var(--surface-app)',
        color: 'var(--text-body)',
        fontFamily: 'var(--font-sans)',
      }}
    >
      <aside
        style={{
          position: 'fixed',
          top: 0,
          bottom: 0,
          left: 0,
          zIndex: 'var(--z-sidebar)',
          display: 'flex',
          width: 'var(--sidebar-width)',
          flexDirection: 'column',
          background: 'var(--surface-inverse)',
          borderRight: '1px solid rgba(255,255,255,0.06)',
          padding: '16px 12px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '4px 8px 18px' }}>
          {/* Le logo reste cliquable (retour à l'accueil), comme avant la refonte.
              Symbole en SVG + mot en HTML : un SVG chargé via <img> est un document
              isolé qui n'hérite pas de la police Inter de la page. Composer le mot
              en HTML garantit la vraie Inter à la bonne graisse. */}
          <Link
            href="/"
            aria-label="JADWAL — accueil"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '9px', textDecoration: 'none' }}
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/jadwal-mark-light.svg" alt="" style={{ height: '24px', width: '24px' }} />
            <span
              style={{
                fontSize: 'var(--text-md)',
                fontWeight: 'var(--weight-semibold)',
                letterSpacing: '0.14em',
                color: 'var(--text-on-dark)',
                lineHeight: 1,
              }}
            >
              JADWAL
            </span>
          </Link>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: '2px', flex: 1, overflowY: 'auto' }}>
          {navigation.map((element) => {
            const actif = estActif(element.href);
            const cleIcone = element.icone ?? ICONE_PAR_HREF[element.href] ?? 'defaut';
            return (
              <Link
                key={element.href}
                href={element.href}
                aria-current={actif ? 'page' : undefined}
                style={{
                  ...NAV_BASE,
                  background: actif ? NAV_FOND_ACTIF : 'transparent',
                  boxShadow: actif ? NAV_OMBRE_ACTIVE : 'none',
                  color: actif ? 'var(--text-on-dark)' : 'var(--text-on-dark-muted)',
                  fontWeight: actif ? 600 : 500,
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = actif ? NAV_FOND_ACTIF : NAV_FOND_SURVOL;
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = actif ? NAV_FOND_ACTIF : 'transparent';
                }}
                // Le box-shadow inline masquerait l'anneau de :focus-visible de base.css :
                // on le repose à la main pour garder la navigation au clavier visible.
                onFocus={(e) => {
                  if (!e.currentTarget.matches(':focus-visible')) return;
                  e.currentTarget.style.boxShadow = actif
                    ? `${NAV_OMBRE_ACTIVE}, var(--ring)`
                    : 'var(--ring)';
                }}
                onBlur={(e) => {
                  e.currentTarget.style.boxShadow = actif ? NAV_OMBRE_ACTIVE : 'none';
                }}
              >
                <span
                  style={{
                    display: 'inline-flex',
                    color: actif ? 'var(--teal-300)' : 'inherit',
                  }}
                >
                  {ICONES[cleIcone]}
                </span>
                {element.libelle}
              </Link>
            );
          })}
        </nav>

        <div
          style={{
            borderTop: '1px solid rgba(255,255,255,0.08)',
            paddingTop: '12px',
            marginTop: '8px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '4px 8px' }}>
            <span
              style={{
                width: '32px',
                height: '32px',
                borderRadius: 'var(--radius-pill)',
                background: 'var(--teal-500)',
                color: 'var(--text-on-dark)',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '13px',
                fontWeight: 600,
                flex: 'none',
              }}
            >
              {initiales(nomUtilisateur)}
            </span>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div
                title={nomUtilisateur}
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-on-dark)',
                  fontWeight: 600,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {nomUtilisateur}
              </div>
              {roleUtilisateur && (
                <div
                  style={{
                    fontSize: 'var(--text-2xs)',
                    color: 'var(--text-on-dark-muted)',
                    letterSpacing: '0.04em',
                  }}
                >
                  {roleUtilisateur}
                </div>
              )}
            </div>
            <button
              type="button"
              aria-label="Se déconnecter"
              title="Se déconnecter"
              onClick={() => {
                void seDeconnecter();
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = 'var(--text-on-dark)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'var(--text-on-dark-muted)';
              }}
              style={{
                border: 0,
                background: 'transparent',
                padding: '4px',
                color: 'var(--text-on-dark-muted)',
                cursor: 'pointer',
                lineHeight: 0,
                borderRadius: 'var(--radius-sm)',
                flex: 'none',
              }}
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={1.75}
                strokeLinecap="round"
                strokeLinejoin="round"
                style={{ display: 'block' }}
              >
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <path d="M16 17l5-5-5-5" />
                <path d="M21 12H9" />
              </svg>
            </button>
          </div>
        </div>
      </aside>

      <div style={{ marginLeft: 'var(--sidebar-width)', minHeight: '100vh' }}>
        <header
          style={{
            position: 'sticky',
            top: 0,
            zIndex: 'var(--z-sticky)',
            display: 'flex',
            height: 'var(--topbar-height)',
            alignItems: 'center',
            gap: '16px',
            borderBottom: '1px solid var(--border-subtle)',
            // rgba(247,249,249,0.85) de la maquette = --surface-app à 85 %.
            background: 'color-mix(in srgb, var(--surface-app) 85%, transparent)',
            backdropFilter: 'blur(8px)',
            padding: '0 28px',
          }}
        >
          <h1
            style={{
              margin: 0,
              fontSize: 'var(--text-xl)',
              fontWeight: 600,
              letterSpacing: '-0.01em',
              color: 'var(--text-strong)',
            }}
          >
            {titrePage}
          </h1>
          <div style={{ flex: 1 }} />
          {/* Recherche et notifications : repères visuels de la maquette, sans backend
              derrière. Tant qu'ils ne sont pas branchés, les deux contrôles sont
              rendus inertes (ni saisie, ni focus, ni pastille de non-lus) pour ne
              rien promettre à l'utilisateur. */}
          <div style={{ position: 'relative', width: '260px' }}>
            <span
              style={{
                position: 'absolute',
                left: '11px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-subtle)',
                display: 'inline-flex',
                pointerEvents: 'none',
              }}
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={1.75}
                strokeLinecap="round"
                strokeLinejoin="round"
                style={{ display: 'block' }}
              >
                <circle cx="11" cy="11" r="8" />
                <path d="m21 21-4.3-4.3" />
              </svg>
            </span>
            {/* Pas de type="search" : la maquette rend un champ nu, sans croix de purge.
                disabled + aria-hidden : décor tant que la recherche globale n'existe
                pas. `disabled` (et non `readOnly`) retire l'élément de l'ordre de
                tabulation ET le rend non focusable au clic : aria-hidden est alors
                légitime (règle axe aria-hidden-focus), là où un champ readOnly masqué
                mais focusable enverrait le focus clavier « nulle part ».
                Même traitement que le bouton Notifications ci-dessous.
                opacity:1 annule le grisage natif des champs désactivés pour
                conserver le rendu de la maquette. On ne force PAS
                -webkit-text-fill-color : cela écraserait aussi la couleur du
                placeholder, seul texte visible ici. */}
            <input
              disabled
              aria-hidden="true"
              title="Recherche globale à venir"
              placeholder="Rechercher…"
              style={{
                width: '100%',
                height: '36px',
                padding: '0 12px 0 34px',
                fontSize: 'var(--text-sm)',
                background: 'var(--surface-card)',
                border: '1px solid var(--border-default)',
                borderRadius: 'var(--radius-sm)',
                outline: 'none',
                color: 'var(--text-strong)',
                opacity: 1,
                fontFamily: 'inherit',
                cursor: 'default',
              }}
            />
          </div>
          <button
            type="button"
            disabled
            aria-label="Notifications"
            title="Notifications à venir"
            style={{
              width: '36px',
              height: '36px',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: 'var(--surface-card)',
              border: '1px solid var(--border-default)',
              borderRadius: 'var(--radius-sm)',
              color: 'var(--text-muted)',
              cursor: 'default',
              position: 'relative',
            }}
          >
            <svg
              width="17"
              height="17"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth={1.75}
              strokeLinecap="round"
              strokeLinejoin="round"
              style={{ display: 'block' }}
            >
              <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
              <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
            </svg>
            {/* Pastille de la maquette. Décor : aucune source de notifications
                ne l'alimente encore — d'où aria-hidden, pour qu'un lecteur
                d'écran n'annonce pas des messages inexistants. */}
            <span
              aria-hidden="true"
              style={{
                position: 'absolute',
                top: '7px',
                right: '8px',
                width: '7px',
                height: '7px',
                borderRadius: '50%',
                background: 'var(--coral-500)',
                border: '1.5px solid var(--surface-card)',
              }}
            />
          </button>
        </header>

        <main style={{ padding: '28px' }}>
          <div style={{ maxWidth: 'var(--content-max)', margin: '0 auto' }}>{children}</div>
        </main>
      </div>
    </div>
  );
}
