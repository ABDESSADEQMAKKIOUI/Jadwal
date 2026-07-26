'use client';

import type { ComponentPropsWithoutRef, CSSProperties, ReactNode } from 'react';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

/** `style` object that also accepts the `--*` custom properties the DS sets inline. */
type StyleWithVars = CSSProperties & { [key: `--${string}`]: string | number | undefined };

export interface ButtonProps
  extends Omit<ComponentPropsWithoutRef<'button'>, 'style' | 'children' | 'type' | 'disabled'> {
  children?: ReactNode;
  variant?: ButtonVariant;
  size?: ButtonSize;
  iconLeft?: ReactNode;
  iconRight?: ReactNode;
  loading?: boolean;
  disabled?: boolean;
  type?: 'button' | 'submit' | 'reset';
  fullWidth?: boolean;
  style?: CSSProperties;
}

interface ButtonVars {
  '--bg': string;
  '--bg-hover': string;
  '--bg-active': string;
  '--fg': string;
  '--bd': string;
}

const HEIGHTS: Record<ButtonSize, string> = {
  sm: 'var(--control-height-sm)',
  md: 'var(--control-height-md)',
  lg: 'var(--control-height-lg)',
};

const PADS: Record<ButtonSize, string> = {
  sm: '0 12px',
  md: '0 16px',
  lg: '0 20px',
};

const FONTS: Record<ButtonSize, string> = {
  sm: 'var(--text-sm)',
  md: 'var(--text-base)',
  lg: 'var(--text-md)',
};

const PALETTES: Record<ButtonVariant, ButtonVars> = {
  primary: {
    '--bg': 'var(--color-primary)',
    '--bg-hover': 'var(--color-primary-hover)',
    '--bg-active': 'var(--color-primary-active)',
    '--fg': 'var(--color-on-primary)',
    '--bd': 'var(--color-primary)',
  },
  secondary: {
    '--bg': 'var(--surface-card)',
    '--bg-hover': 'var(--surface-hover)',
    '--bg-active': 'var(--neutral-200)',
    '--fg': 'var(--text-strong)',
    '--bd': 'var(--border-default)',
  },
  ghost: {
    '--bg': 'transparent',
    '--bg-hover': 'var(--surface-hover)',
    '--bg-active': 'var(--neutral-200)',
    '--fg': 'var(--text-body)',
    '--bd': 'transparent',
  },
  danger: {
    '--bg': 'var(--status-danger-solid)',
    '--bg-hover': 'var(--red-600)',
    '--bg-active': 'var(--red-700)',
    '--fg': '#ffffff',
    '--bd': 'var(--status-danger-solid)',
  },
};

/**
 * Ynexis primary action button.
 * Variants: primary (teal), secondary (outline), ghost, danger.
 * Sizes: sm | md | lg. Supports leading/trailing icons and loading state.
 */
export function Button({
  children,
  variant = 'primary',
  size = 'md',
  iconLeft = null,
  iconRight = null,
  loading = false,
  disabled = false,
  type = 'button',
  fullWidth = false,
  style = {},
  ...rest
}: ButtonProps) {
  const palette: Partial<ButtonVars> = PALETTES[variant] ?? {};
  const isDisabled = disabled || loading;
  const buttonStyle: StyleWithVars = {
    ...palette,
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '7px',
    height: HEIGHTS[size],
    padding: PADS[size],
    width: fullWidth ? '100%' : 'auto',
    fontSize: FONTS[size],
    fontWeight: 'var(--weight-medium)',
    fontFamily: 'var(--font-sans)',
    lineHeight: 1,
    color: 'var(--fg)',
    background: 'var(--bg)',
    border: 'var(--border-width) solid var(--bd)',
    borderRadius: 'var(--radius-sm)',
    cursor: isDisabled ? 'not-allowed' : 'pointer',
    opacity: isDisabled ? 0.55 : 1,
    transition:
      'background var(--duration-fast) var(--ease-standard), box-shadow var(--duration-fast) var(--ease-standard)',
    whiteSpace: 'nowrap',
    ...style,
  };

  return (
    <button
      type={type}
      disabled={isDisabled}
      style={buttonStyle}
      onMouseEnter={(e) => {
        if (!isDisabled) e.currentTarget.style.background = 'var(--bg-hover)';
      }}
      onMouseLeave={(e) => {
        if (!isDisabled) e.currentTarget.style.background = 'var(--bg)';
      }}
      onMouseDown={(e) => {
        if (!isDisabled) e.currentTarget.style.background = 'var(--bg-active)';
      }}
      onMouseUp={(e) => {
        if (!isDisabled) e.currentTarget.style.background = 'var(--bg-hover)';
      }}
      {...rest}
    >
      {loading && <Spinner />}
      {!loading && iconLeft}
      {children}
      {!loading && iconRight}
    </button>
  );
}

function Spinner() {
  return (
    <span
      aria-hidden="true"
      style={{
        width: '14px',
        height: '14px',
        border: '2px solid currentColor',
        borderTopColor: 'transparent',
        borderRadius: '50%',
        display: 'inline-block',
        animation: 'ynx-spin 0.6s linear infinite',
      }}
    />
  );
}

if (typeof document !== 'undefined' && !document.getElementById('ynx-spin-kf')) {
  const s = document.createElement('style');
  s.id = 'ynx-spin-kf';
  s.textContent = '@keyframes ynx-spin{to{transform:rotate(360deg)}}';
  document.head.appendChild(s);
}
