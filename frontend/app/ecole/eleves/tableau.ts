import type { CSSProperties } from 'react';
import type { BadgeTone } from '@/components/ds';
import type { StatutEleve, StatutLigneImport } from '@/lib/types-scolarite';

/* Styles de tableau de la maquette Ynexis (constantes TH / TD du document),
   partagés par la liste des élèves et la prévisualisation d'import. */
const TH_BASE: CSSProperties = {
  padding: '10px 14px',
  background: 'var(--neutral-50)',
  borderBottom: '1px solid var(--border-subtle)',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
  whiteSpace: 'nowrap',
};
export const TH: CSSProperties = { ...TH_BASE, textAlign: 'left' };
export const TH_RIGHT: CSSProperties = { ...TH_BASE, textAlign: 'right' };

const TD_BASE: CSSProperties = {
  padding: '0 14px',
  height: 'var(--row-height)',
  borderBottom: '1px solid var(--neutral-100)',
  color: 'var(--text-body)',
  whiteSpace: 'nowrap',
};
export const TD: CSSProperties = { ...TD_BASE, textAlign: 'left' };
export const TD_RIGHT: CSSProperties = {
  ...TD_BASE,
  textAlign: 'right',
  fontVariantNumeric: 'tabular-nums',
};

/** Code Massar : chiffres alignés, jamais coupé. */
export const CODE_MASSAR: CSSProperties = {
  fontFamily: 'var(--font-mono)',
  fontSize: 'var(--text-xs)',
  letterSpacing: '0.02em',
  color: 'var(--text-strong)',
};

/** Colonne arabe : la police arabe du design system, sens d'écriture inversé. */
export const TEXTE_ARABE: CSSProperties = {
  fontFamily: 'var(--font-arabic)',
  direction: 'rtl',
  unicodeBidi: 'isolate',
};

export const TABLE: CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
  fontSize: 'var(--text-sm)',
};

/* ------------------------------------------------------------------ */
/* Vocabulaire des statuts                                             */
/* ------------------------------------------------------------------ */

export const LIBELLE_STATUT: Record<StatutEleve, string> = {
  INSCRIT: 'Inscrit',
  PARTI: 'Parti',
  REDOUBLANT: 'Redoublant',
};

export const TON_STATUT: Record<StatutEleve, BadgeTone> = {
  INSCRIT: 'active',
  PARTI: 'neutral',
  REDOUBLANT: 'warning',
};

/** NOUVEAU vert, EXISTANT ambre, ERREUR rouge (prévisualisation d'import). */
export const TON_LIGNE_IMPORT: Record<StatutLigneImport, BadgeTone> = {
  NOUVEAU: 'success',
  EXISTANT: 'warning',
  ERREUR: 'danger',
};

export const LIBELLE_LIGNE_IMPORT: Record<StatutLigneImport, string> = {
  NOUVEAU: 'Nouveau',
  EXISTANT: 'Déjà inscrit',
  ERREUR: 'Erreur',
};

/** Date ISO du backend (`aaaa-mm-jj`) en date française, sans fuseau horaire. */
export function formatDateIso(date: string | null): string {
  if (date === null || date.length === 0) return '—';
  const parties = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  if (parties === null) return date;
  return `${parties[3]}/${parties[2]}/${parties[1]}`;
}
