'use client';

import type { InputHTMLAttributes } from 'react';

export function Input({
  className = '',
  ...props
}: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={`h-[var(--control-height-md)] w-full rounded-sm border border-line-default bg-surface-card px-3 text-base text-ink-strong transition-colors duration-[var(--duration-fast)] placeholder:text-ink-subtle focus:border-brand focus:outline-none focus:shadow-[var(--ring)] disabled:bg-surface-sunken disabled:text-ink-muted ${className}`}
      {...props}
    />
  );
}
