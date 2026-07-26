'use client';

import type { CSSProperties } from 'react';

export type AvatarShape = 'circle' | 'square';

export interface AvatarProps {
  name?: string;
  size?: number;
  shape?: AvatarShape;
  src?: string | null;
  style?: CSSProperties;
}

const TINTS: readonly (readonly [string, string])[] = [
  ['var(--teal-100)', 'var(--teal-700)'],
  ['var(--blue-100)', 'var(--blue-700)'],
  ['var(--amber-100)', 'var(--amber-700)'],
  ['var(--coral-100)', 'var(--coral-700)'],
  ['var(--green-100)', 'var(--green-700)'],
];

/**
 * Initials avatar with deterministic teal-family tint. Square-rounded or circle.
 * Rend une `<img>` native quand `src` est fourni (comme le bundle du DS,
 * volontairement sans `next/image`).
 */
export function Avatar({
  name = '',
  size = 32,
  shape = 'circle',
  src = null,
  style = {},
}: AvatarProps) {
  const initials =
    name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((w) => w[0])
      .join('')
      .toUpperCase() || '?';
  let h = 0;
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0;
  const [bg, fg] = TINTS[h % TINTS.length];
  const radius = shape === 'square' ? 'var(--radius-sm)' : 'var(--radius-pill)';

  if (src) {
    return (
      <img
        src={src}
        alt={name}
        width={size}
        height={size}
        style={{
          width: size,
          height: size,
          borderRadius: radius,
          objectFit: 'cover',
          display: 'block',
          ...style,
        }}
      />
    );
  }

  return (
    <span
      aria-label={name}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: size,
        height: size,
        borderRadius: radius,
        background: bg,
        color: fg,
        fontSize: Math.round(size * 0.4),
        fontWeight: 'var(--weight-semibold)',
        fontFamily: 'var(--font-sans)',
        flex: 'none',
        userSelect: 'none',
        ...style,
      }}
    >
      {initials}
    </span>
  );
}
