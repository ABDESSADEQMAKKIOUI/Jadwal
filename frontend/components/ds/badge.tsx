'use client';

import type { ComponentPropsWithoutRef, CSSProperties, ReactNode } from 'react';

export type BadgeTone = 'success' | 'danger' | 'warning' | 'info' | 'active' | 'neutral';
export type BadgeSize = 'sm' | 'md';

export interface BadgeProps extends Omit<ComponentPropsWithoutRef<'span'>, 'style' | 'children'> {
  children?: ReactNode;
  tone?: BadgeTone;
  dot?: boolean;
  solid?: boolean;
  size?: BadgeSize;
  style?: CSSProperties;
}

const TONES: Record<BadgeTone, readonly [string, string, string]> = {
  success: ['--status-success-bg', '--status-success-fg', '--status-success-solid'],
  danger: ['--status-danger-bg', '--status-danger-fg', '--status-danger-solid'],
  warning: ['--status-warning-bg', '--status-warning-fg', '--status-warning-solid'],
  info: ['--status-info-bg', '--status-info-fg', '--status-info-solid'],
  active: ['--status-active-bg', '--status-active-fg', '--status-active-solid'],
  neutral: ['--status-neutral-bg', '--status-neutral-fg', '--status-neutral-solid'],
};

/**
 * Status pill. `tone` selects the semantic color pair; `dot` shows a
 * leading status dot; `solid` fills with the solid color for emphasis.
 */
export function Badge({
  children,
  tone = 'neutral',
  dot = false,
  solid = false,
  size = 'md',
  style = {},
  ...rest
}: BadgeProps) {
  const pairs = TONES[tone] || TONES.neutral;
  const [bg, fg, sol] = pairs;
  const pad = size === 'sm' ? '2px 8px' : '3px 10px';
  const fs = size === 'sm' ? 'var(--text-2xs)' : 'var(--text-xs)';

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        padding: pad,
        fontSize: fs,
        fontWeight: 'var(--weight-semibold)',
        letterSpacing: '0.02em',
        lineHeight: 1.4,
        color: solid ? '#fff' : `var(${fg})`,
        background: solid ? `var(${sol})` : `var(${bg})`,
        borderRadius: 'var(--radius-pill)',
        whiteSpace: 'nowrap',
        ...style,
      }}
      {...rest}
    >
      {dot && (
        <span
          aria-hidden="true"
          style={{
            width: '7px',
            height: '7px',
            borderRadius: '50%',
            background: solid ? 'rgba(255,255,255,0.9)' : `var(${sol})`,
            flex: 'none',
          }}
        />
      )}
      {children}
    </span>
  );
}
