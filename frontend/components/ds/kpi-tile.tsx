'use client';

import type { CSSProperties, ReactNode } from 'react';

export type KpiDeltaTone = 'success' | 'danger' | 'neutral';

export interface KpiTileProps {
  label?: ReactNode;
  value?: ReactNode;
  delta?: ReactNode;
  deltaTone?: KpiDeltaTone;
  icon?: ReactNode;
  hint?: ReactNode;
  style?: CSSProperties;
}

const DELTA_COLORS: Record<KpiDeltaTone, string> = {
  success: 'var(--green-600)',
  danger: 'var(--red-600)',
  neutral: 'var(--text-muted)',
};

/**
 * KPI tile for the Overview: uppercase overline label, big tabular number,
 * optional delta and a leading icon chip. Composes inside a Card or grid.
 */
export function KpiTile({
  label,
  value,
  delta = null,
  deltaTone = 'success',
  icon = null,
  hint = null,
  style = {},
}: KpiTileProps) {
  const deltaColor = DELTA_COLORS[deltaTone];

  return (
    <div
      style={{
        background: 'var(--surface-card)',
        border: 'var(--border-width) solid var(--border-subtle)',
        borderRadius: 'var(--radius-md)',
        boxShadow: 'var(--shadow-sm)',
        padding: 'var(--space-5)',
        display: 'flex',
        flexDirection: 'column',
        gap: 'var(--space-2)',
        ...style,
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 'var(--space-3)',
        }}
      >
        <span
          style={{
            fontSize: 'var(--text-2xs)',
            fontWeight: 'var(--weight-semibold)',
            letterSpacing: 'var(--tracking-caps)',
            textTransform: 'uppercase',
            color: 'var(--text-muted)',
          }}
        >
          {label}
        </span>
        {icon && (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 30,
              height: 30,
              borderRadius: 'var(--radius-sm)',
              background: 'var(--teal-50)',
              color: 'var(--teal-600)',
              flex: 'none',
            }}
          >
            {icon}
          </span>
        )}
      </div>
      <span
        style={{
          fontSize: 'var(--type-kpi-size)',
          fontWeight: 'var(--type-kpi-weight)',
          color: 'var(--text-strong)',
          fontVariantNumeric: 'tabular-nums',
          letterSpacing: '-0.02em',
          lineHeight: 1.05,
        }}
      >
        {value}
      </span>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          minHeight: 18,
        }}
      >
        {delta != null && (
          <span
            style={{
              fontSize: 'var(--text-xs)',
              fontWeight: 'var(--weight-semibold)',
              color: deltaColor,
            }}
          >
            {delta}
          </span>
        )}
        {hint && (
          <span
            style={{
              fontSize: 'var(--text-xs)',
              color: 'var(--text-subtle)',
            }}
          >
            {hint}
          </span>
        )}
      </div>
    </div>
  );
}
