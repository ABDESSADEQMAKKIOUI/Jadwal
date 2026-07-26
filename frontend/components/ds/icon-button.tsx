'use client';

import type { ComponentPropsWithoutRef, CSSProperties, ReactNode } from 'react';

export type IconButtonVariant = 'ghost' | 'outline' | 'solid';
export type IconButtonSize = 'sm' | 'md' | 'lg';

export interface IconButtonProps
  extends Omit<ComponentPropsWithoutRef<'button'>, 'style' | 'children' | 'disabled'> {
  children?: ReactNode;
  label?: string;
  variant?: IconButtonVariant;
  size?: IconButtonSize;
  disabled?: boolean;
  style?: CSSProperties;
}

interface IconButtonPalette {
  bg: string;
  hover: string;
  fg: string;
  bd: string;
}

const DIMS: Record<IconButtonSize, number> = {
  sm: 28,
  md: 34,
  lg: 40,
};

const PALETTES: Record<IconButtonVariant, IconButtonPalette> = {
  ghost: {
    bg: 'transparent',
    hover: 'var(--surface-hover)',
    fg: 'var(--text-muted)',
    bd: 'transparent',
  },
  outline: {
    bg: 'var(--surface-card)',
    hover: 'var(--surface-hover)',
    fg: 'var(--text-body)',
    bd: 'var(--border-default)',
  },
  solid: {
    bg: 'var(--color-primary)',
    hover: 'var(--color-primary-hover)',
    fg: '#fff',
    bd: 'var(--color-primary)',
  },
};

/**
 * Square icon-only button for table rows, toolbars, and dense chrome.
 * Pass a Lucide (or any) icon as children. Always provide `label` for a11y.
 */
export function IconButton({
  children,
  label,
  variant = 'ghost',
  size = 'md',
  disabled = false,
  style = {},
  ...rest
}: IconButtonProps) {
  const dims = DIMS[size];
  const palette = PALETTES[variant];

  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      disabled={disabled}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: `${dims}px`,
        height: `${dims}px`,
        color: palette.fg,
        background: palette.bg,
        border: `var(--border-width) solid ${palette.bd}`,
        borderRadius: 'var(--radius-sm)',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.5 : 1,
        transition:
          'background var(--duration-fast) var(--ease-standard), color var(--duration-fast) var(--ease-standard)',
        ...style,
      }}
      onMouseEnter={(e) => {
        if (!disabled) {
          e.currentTarget.style.background = palette.hover;
          if (variant === 'ghost') e.currentTarget.style.color = 'var(--text-strong)';
        }
      }}
      onMouseLeave={(e) => {
        if (!disabled) {
          e.currentTarget.style.background = palette.bg;
          e.currentTarget.style.color = palette.fg;
        }
      }}
      {...rest}
    >
      {children}
    </button>
  );
}
