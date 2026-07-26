'use client';

import type { CSSProperties, ReactNode } from 'react';

export type AlertTone = 'success' | 'danger' | 'warning' | 'info' | 'neutral';

export interface AlertProps {
  tone?: AlertTone;
  title?: ReactNode;
  children?: ReactNode;
  icon?: ReactNode;
  onClose?: () => void;
  style?: CSSProperties;
}

const TONES: Record<AlertTone, readonly [string, string, string]> = {
  success: ['--status-success-bg', '--status-success-fg', '--green-500'],
  danger: ['--status-danger-bg', '--status-danger-fg', '--red-500'],
  warning: ['--status-warning-bg', '--status-warning-fg', '--amber-500'],
  info: ['--status-info-bg', '--status-info-fg', '--blue-500'],
  neutral: ['--status-neutral-bg', '--status-neutral-fg', '--neutral-400'],
};

/**
 * Inline alert / banner for API feedback. Tone maps to status colors.
 * Use for ProblemDetail.detail messages, rate-limit (429), and gated notices.
 */
export function Alert({ tone = 'info', title, children, icon = null, onClose, style = {} }: AlertProps) {
  const map = TONES[tone];
  const [bg, fg, accent] = map;

  return (
    <div
      role="status"
      style={{
        display: 'flex',
        gap: '10px',
        alignItems: 'flex-start',
        padding: '12px 14px',
        background: `var(${bg})`,
        border: `var(--border-width) solid color-mix(in srgb, var(${accent}) 28%, transparent)`,
        borderRadius: 'var(--radius-md)',
        ...style,
      }}
    >
      {icon && (
        <span
          style={{
            color: `var(${accent})`,
            display: 'inline-flex',
            flex: 'none',
            marginTop: 1,
          }}
        >
          {icon}
        </span>
      )}
      <div
        style={{
          flex: 1,
          minWidth: 0,
        }}
      >
        {title && (
          <div
            style={{
              fontSize: 'var(--text-sm)',
              fontWeight: 'var(--weight-semibold)',
              color: `var(${fg})`,
              marginBottom: children ? 2 : 0,
            }}
          >
            {title}
          </div>
        )}
        {children && (
          <div
            style={{
              fontSize: 'var(--text-sm)',
              color: `var(${fg})`,
              opacity: 0.92,
            }}
          >
            {children}
          </div>
        )}
      </div>
      {onClose && (
        <button
          type="button"
          aria-label="Fermer"
          onClick={onClose}
          style={{
            background: 'none',
            border: 'none',
            color: `var(${fg})`,
            cursor: 'pointer',
            padding: 2,
            opacity: 0.7,
            display: 'inline-flex',
            flex: 'none',
          }}
        >
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          >
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      )}
    </div>
  );
}
