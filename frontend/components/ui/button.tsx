'use client';

import type { ButtonHTMLAttributes } from 'react';

type Variante = 'primary' | 'secondary' | 'danger' | 'ghost';
type Taille = 'sm' | 'md';

const classesVariante: Record<Variante, string> = {
  primary:
    'bg-indigo-600 text-white hover:bg-indigo-700 focus-visible:ring-indigo-300 disabled:bg-indigo-300',
  secondary:
    'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus-visible:ring-gray-200 disabled:text-gray-400',
  danger:
    'bg-red-600 text-white hover:bg-red-700 focus-visible:ring-red-300 disabled:bg-red-300',
  ghost:
    'text-gray-600 hover:bg-gray-100 hover:text-gray-900 focus-visible:ring-gray-200 disabled:text-gray-400',
};

const classesTaille: Record<Taille, string> = {
  sm: 'px-2.5 py-1.5 text-xs',
  md: 'px-4 py-2 text-sm',
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
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 disabled:cursor-not-allowed ${classesVariante[variante]} ${classesTaille[taille]} ${className}`}
      {...props}
    />
  );
}
