'use client';

import type { ButtonHTMLAttributes } from 'react';

type Variante = 'primary' | 'secondary' | 'danger' | 'ghost';
type Taille = 'sm' | 'md';

/** Teal de marque en action primaire, contour neutre en secondaire (idiome Ynexis). */
const classesVariante: Record<Variante, string> = {
  primary:
    'bg-brand text-on-brand hover:bg-brand-hover active:bg-brand-active disabled:bg-brand-border disabled:text-white',
  secondary:
    'border border-line-default bg-surface-card text-ink-body hover:bg-surface-hover hover:text-ink-strong disabled:border-line-subtle disabled:text-ink-subtle',
  danger:
    'bg-bad-solid text-white hover:bg-[var(--red-600)] active:bg-[var(--red-700)] disabled:bg-bad-bg disabled:text-bad-solid',
  ghost:
    'text-ink-muted hover:bg-surface-hover hover:text-ink-strong disabled:text-ink-subtle',
};

/** Hauteurs de contrôle du design system : 32 px (sm) et 38 px (md). */
const classesTaille: Record<Taille, string> = {
  sm: 'h-[var(--control-height-sm)] gap-1.5 px-3 text-xs',
  md: 'h-[var(--control-height-md)] gap-2 px-4 text-base',
};

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante;
  taille?: Taille;
}

export function Button({
  variante = 'primary',
  taille = 'md',
  className = '',
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={`inline-flex items-center justify-center whitespace-nowrap rounded-sm font-medium transition-colors duration-[var(--duration-fast)] focus-visible:outline-none focus-visible:shadow-[var(--ring)] disabled:cursor-not-allowed ${classesVariante[variante]} ${classesTaille[taille]} ${className}`}
      {...props}
    />
  );
}
