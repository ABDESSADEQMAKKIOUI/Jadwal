'use client';

import type { ComponentPropsWithoutRef, CSSProperties, ReactNode } from 'react';

export interface TextareaProps
  extends Omit<
    ComponentPropsWithoutRef<'textarea'>,
    'style' | 'id' | 'required' | 'maxLength' | 'rows' | 'value'
  > {
  label?: string;
  hint?: ReactNode;
  error?: ReactNode;
  id?: string;
  required?: boolean;
  maxLength?: number;
  showCount?: boolean;
  rows?: number;
  value?: string | number | readonly string[];
  style?: CSSProperties;
  containerStyle?: CSSProperties;
}

/**
 * Multi-line text with optional character counter — built for the agent
 * `prompt` (up to 20k chars) and call summaries. Sets dir="auto" so Arabic
 * / Darija content flows RTL inline.
 */
export function Textarea({
  label,
  hint,
  error,
  id,
  required = false,
  maxLength,
  showCount = false,
  rows = 5,
  value,
  style = {},
  containerStyle = {},
  ...rest
}: TextareaProps) {
  const autoId = id || (label ? 'ta-' + label.toLowerCase().replace(/\s+/g, '-') : undefined);
  const count = typeof value === 'string' ? value.length : 0;
  const over = maxLength != null && count > maxLength;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '6px',
        ...containerStyle,
      }}
    >
      {(label || showCount) && (
        <div
          style={{
            display: 'flex',
            alignItems: 'baseline',
            justifyContent: 'space-between',
            gap: 8,
          }}
        >
          {label && (
            <label
              htmlFor={autoId}
              style={{
                fontSize: 'var(--text-sm)',
                fontWeight: 'var(--weight-medium)',
                color: 'var(--text-strong)',
              }}
            >
              {label}
              {required && (
                <span
                  style={{
                    color: 'var(--status-danger-solid)',
                    marginLeft: 3,
                  }}
                >
                  *
                </span>
              )}
            </label>
          )}
          {showCount && (
            <span
              style={{
                fontSize: 'var(--text-xs)',
                fontVariantNumeric: 'tabular-nums',
                color: over ? 'var(--status-danger-fg)' : 'var(--text-subtle)',
              }}
            >
              {count.toLocaleString('fr-FR')}
              {maxLength != null ? ` / ${maxLength.toLocaleString('fr-FR')}` : ''}
            </span>
          )}
        </div>
      )}
      <textarea
        id={autoId}
        dir="auto"
        rows={rows}
        value={value}
        aria-invalid={!!error || over}
        style={{
          width: '100%',
          padding: '10px 12px',
          fontSize: 'var(--text-base)',
          fontFamily: 'var(--font-sans)',
          lineHeight: 'var(--leading-normal)',
          color: 'var(--text-strong)',
          background: 'var(--surface-card)',
          border: `var(--border-width) solid ${error || over ? 'var(--status-danger-solid)' : 'var(--border-default)'}`,
          borderRadius: 'var(--radius-sm)',
          outline: 'none',
          resize: 'vertical',
          transition:
            'border-color var(--duration-fast) var(--ease-standard), box-shadow var(--duration-fast) var(--ease-standard)',
          ...style,
        }}
        onFocus={(e) => {
          e.currentTarget.style.borderColor =
            error || over ? 'var(--status-danger-solid)' : 'var(--border-focus)';
          e.currentTarget.style.boxShadow = error || over ? 'var(--ring-danger)' : 'var(--ring)';
          rest.onFocus?.(e);
        }}
        onBlur={(e) => {
          e.currentTarget.style.borderColor =
            error || over ? 'var(--status-danger-solid)' : 'var(--border-default)';
          e.currentTarget.style.boxShadow = 'none';
          rest.onBlur?.(e);
        }}
        {...rest}
      />
      {error ? (
        <span
          style={{
            fontSize: 'var(--text-xs)',
            color: 'var(--status-danger-fg)',
          }}
        >
          {error}
        </span>
      ) : hint ? (
        <span
          style={{
            fontSize: 'var(--text-xs)',
            color: 'var(--text-muted)',
          }}
        >
          {hint}
        </span>
      ) : null}
    </div>
  );
}
