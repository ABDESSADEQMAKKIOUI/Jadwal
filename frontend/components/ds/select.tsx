'use client';

import type { ComponentPropsWithoutRef, CSSProperties, ReactNode } from 'react';

export type SelectSize = 'sm' | 'md' | 'lg';

export type SelectOption = string | { value: string | number; label: ReactNode };

export interface SelectProps
  extends Omit<
    ComponentPropsWithoutRef<'select'>,
    'style' | 'size' | 'id' | 'required' | 'children' | 'placeholder'
  > {
  label?: string;
  hint?: ReactNode;
  error?: ReactNode;
  id?: string;
  required?: boolean;
  options?: readonly SelectOption[];
  placeholder?: string;
  size?: SelectSize;
  style?: CSSProperties;
  containerStyle?: CSSProperties;
  children?: ReactNode;
}

const HEIGHTS: Record<SelectSize, string> = {
  sm: 'var(--control-height-sm)',
  md: 'var(--control-height-md)',
  lg: 'var(--control-height-lg)',
};

/** Native select styled to match Input, with label/hint/error and a chevron. */
export function Select({
  label,
  hint,
  error,
  id,
  required = false,
  options = [],
  placeholder,
  size = 'md',
  style = {},
  containerStyle = {},
  children,
  ...rest
}: SelectProps) {
  const autoId = id || (label ? 'sel-' + label.toLowerCase().replace(/\s+/g, '-') : undefined);
  const h = HEIGHTS[size];

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '6px',
        ...containerStyle,
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
      <div
        style={{
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <select
          id={autoId}
          aria-invalid={!!error}
          style={{
            width: '100%',
            height: h,
            padding: '0 34px 0 12px',
            fontSize: 'var(--text-base)',
            fontFamily: 'var(--font-sans)',
            color: 'var(--text-strong)',
            background: 'var(--surface-card)',
            border: `var(--border-width) solid ${error ? 'var(--status-danger-solid)' : 'var(--border-default)'}`,
            borderRadius: 'var(--radius-sm)',
            outline: 'none',
            appearance: 'none',
            cursor: 'pointer',
            transition:
              'border-color var(--duration-fast) var(--ease-standard), box-shadow var(--duration-fast) var(--ease-standard)',
            ...style,
          }}
          onFocus={(e) => {
            e.currentTarget.style.borderColor = 'var(--border-focus)';
            e.currentTarget.style.boxShadow = 'var(--ring)';
            rest.onFocus?.(e);
          }}
          onBlur={(e) => {
            e.currentTarget.style.borderColor = error
              ? 'var(--status-danger-solid)'
              : 'var(--border-default)';
            e.currentTarget.style.boxShadow = 'none';
            rest.onBlur?.(e);
          }}
          {...rest}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((o) => {
            const val = typeof o === 'string' ? o : o.value;
            const lab = typeof o === 'string' ? o : o.label;
            return (
              <option key={val} value={val}>
                {lab}
              </option>
            );
          })}
          {children}
        </select>
        <span
          aria-hidden="true"
          style={{
            position: 'absolute',
            right: 12,
            pointerEvents: 'none',
            color: 'var(--text-subtle)',
            display: 'inline-flex',
          }}
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="m6 9 6 6 6-6" />
          </svg>
        </span>
      </div>
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
