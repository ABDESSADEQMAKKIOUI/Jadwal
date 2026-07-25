'use client';

import type { SelectHTMLAttributes } from 'react';

export function Select({
  className = '',
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={`h-[var(--control-height-md)] w-full rounded-sm border border-line-default bg-surface-card px-3 text-base text-ink-strong transition-colors duration-[var(--duration-fast)] focus:border-brand focus:outline-none focus:shadow-[var(--ring)] disabled:bg-surface-sunken disabled:text-ink-muted ${className}`}
      {...props}
    >
      {children}
    </select>
  );
}
