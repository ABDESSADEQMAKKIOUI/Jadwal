'use client';

import type { CSSProperties, ReactNode } from 'react';

export type EmptyStateVariant = 'empty' | 'gated';

export interface EmptyStateProps {
  variant?: EmptyStateVariant;
  icon?: ReactNode;
  title?: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  badge?: ReactNode;
  style?: CSSProperties;
}

/**
 * Empty / gated / placeholder state. `variant="empty"` for "nothing yet"
 * (with a primary action), `variant="gated"` for "not configured" (calm,
 * no error styling). Center it inside a Card or a table body.
 */
export function EmptyState({
  variant = 'empty',
  icon = null,
  title,
  description,
  action = null,
  badge = null,
  style = {},
}: EmptyStateProps) {
  const gated = variant === 'gated';

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        textAlign: 'center',
        gap: '10px',
        padding: '44px 28px',
        ...style,
      }}
    >
      {icon && (
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 48,
            height: 48,
            borderRadius: 'var(--radius-lg)',
            background: gated ? 'var(--neutral-100)' : 'var(--teal-50)',
            color: gated ? 'var(--text-muted)' : 'var(--teal-600)',
            marginBottom: 2,
          }}
        >
          {icon}
        </span>
      )}
      {badge && <div>{badge}</div>}
      {title && (
        <div
          style={{
            fontSize: 'var(--text-md)',
            fontWeight: 'var(--weight-semibold)',
            color: 'var(--text-strong)',
          }}
        >
          {title}
        </div>
      )}
      {description && (
        <div
          style={{
            fontSize: 'var(--text-sm)',
            color: 'var(--text-muted)',
            maxWidth: 380,
            lineHeight: 'var(--leading-normal)',
          }}
        >
          {description}
        </div>
      )}
      {action && (
        <div
          style={{
            marginTop: 6,
          }}
        >
          {action}
        </div>
      )}
    </div>
  );
}
