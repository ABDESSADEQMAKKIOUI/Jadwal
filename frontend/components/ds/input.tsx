'use client';

import type { ComponentPropsWithoutRef, CSSProperties, ReactNode } from 'react';

export type InputSize = 'sm' | 'md' | 'lg';

export interface InputProps
  extends Omit<ComponentPropsWithoutRef<'input'>, 'style' | 'size' | 'id' | 'required'> {
  label?: string;
  hint?: ReactNode;
  error?: ReactNode;
  id?: string;
  required?: boolean;
  iconLeft?: ReactNode;
  size?: InputSize;
  style?: CSSProperties;
  containerStyle?: CSSProperties;
}

const HEIGHTS: Record<InputSize, string> = {
  sm: 'var(--control-height-sm)',
  md: 'var(--control-height-md)',
  lg: 'var(--control-height-lg)',
};

/**
 * Text input with label, optional hint, and ProblemDetail-style error.
 * Renders the API `errors[field]` message inline + red ring when `error` set.
 */
export function Input({
  label,
  hint,
  error,
  id,
  required = false,
  iconLeft = null,
  size = 'md',
  style = {},
  containerStyle = {},
  ...rest
}: InputProps) {
  const autoId = id || (label ? 'in-' + label.toLowerCase().replace(/\s+/g, '-') : undefined);
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
        {iconLeft && (
          <span
            style={{
              position: 'absolute',
              left: 11,
              display: 'inline-flex',
              color: 'var(--text-subtle)',
              pointerEvents: 'none',
            }}
          >
            {iconLeft}
          </span>
        )}
        <input
          id={autoId}
          aria-invalid={!!error}
          style={{
            width: '100%',
            height: h,
            padding: iconLeft ? '0 12px 0 34px' : '0 12px',
            fontSize: 'var(--text-base)',
            fontFamily: 'var(--font-sans)',
            color: 'var(--text-strong)',
            background: 'var(--surface-card)',
            border: `var(--border-width) solid ${error ? 'var(--status-danger-solid)' : 'var(--border-default)'}`,
            borderRadius: 'var(--radius-sm)',
            outline: 'none',
            transition:
              'border-color var(--duration-fast) var(--ease-standard), box-shadow var(--duration-fast) var(--ease-standard)',
            ...style,
          }}
          onFocus={(e) => {
            e.currentTarget.style.borderColor = error
              ? 'var(--status-danger-solid)'
              : 'var(--border-focus)';
            e.currentTarget.style.boxShadow = error ? 'var(--ring-danger)' : 'var(--ring)';
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
        />
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
