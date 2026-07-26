'use client';

import type { CSSProperties, ReactNode } from 'react';

export interface TabItem {
  id: string;
  label: ReactNode;
  count?: number | null;
  icon?: ReactNode;
}

export interface TabsProps {
  tabs?: readonly TabItem[];
  value?: string;
  onChange?: (id: string) => void;
  style?: CSSProperties;
}

/**
 * Underline tab bar. Controlled: pass `value` + `onChange`. Tabs are
 * { id, label, count?, icon? }. Used for agent/campaign detail sections.
 */
export function Tabs({ tabs = [], value, onChange, style = {} }: TabsProps) {
  return (
    <div
      role="tablist"
      style={{
        display: 'flex',
        gap: '2px',
        borderBottom: 'var(--border-width) solid var(--border-subtle)',
        ...style,
      }}
    >
      {tabs.map((t) => {
        const active = t.id === value;
        return (
          <button
            key={t.id}
            role="tab"
            aria-selected={active}
            onClick={() => onChange && onChange(t.id)}
            style={{
              position: 'relative',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '7px',
              padding: '10px 14px',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              fontSize: 'var(--text-base)',
              fontWeight: active ? 'var(--weight-semibold)' : 'var(--weight-medium)',
              color: active ? 'var(--text-strong)' : 'var(--text-muted)',
              borderBottom: `2px solid ${active ? 'var(--color-primary)' : 'transparent'}`,
              marginBottom: '-1px',
              transition: 'color var(--duration-fast) var(--ease-standard)',
              whiteSpace: 'nowrap',
            }}
            onMouseEnter={(e) => {
              if (!active) e.currentTarget.style.color = 'var(--text-body)';
            }}
            onMouseLeave={(e) => {
              if (!active) e.currentTarget.style.color = 'var(--text-muted)';
            }}
          >
            {t.icon}
            {t.label}
            {t.count != null && (
              <span
                style={{
                  fontSize: 'var(--text-2xs)',
                  fontWeight: 'var(--weight-semibold)',
                  fontVariantNumeric: 'tabular-nums',
                  color: active ? 'var(--teal-700)' : 'var(--text-muted)',
                  background: active ? 'var(--teal-50)' : 'var(--neutral-100)',
                  padding: '1px 7px',
                  borderRadius: 'var(--radius-pill)',
                }}
              >
                {t.count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
