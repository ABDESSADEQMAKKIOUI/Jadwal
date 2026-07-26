'use client';

import type { ComponentPropsWithoutRef, CSSProperties, ReactNode } from 'react';

export type CardShadow = 'none' | 'sm' | 'md';

export interface CardProps extends Omit<ComponentPropsWithoutRef<'div'>, 'style' | 'children'> {
  children?: ReactNode;
  padded?: boolean;
  shadow?: CardShadow;
  style?: CSSProperties;
}

export interface CardHeaderProps {
  title?: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  style?: CSSProperties;
}

export interface CardBodyProps {
  children?: ReactNode;
  style?: CSSProperties;
}

export interface CardFooterProps {
  children?: ReactNode;
  style?: CSSProperties;
}

const SHADOWS: Record<CardShadow, string> = {
  none: 'none',
  sm: 'var(--shadow-sm)',
  md: 'var(--shadow-md)',
};

/**
 * Surface container. White, hairline border, optional soft shadow.
 * Compose with Card.Header / Card.Body / Card.Footer, or just pass children.
 */
function CardRoot({ children, padded = true, shadow = 'sm', style = {}, ...rest }: CardProps) {
  return (
    <div
      style={{
        background: 'var(--surface-card)',
        border: 'var(--border-width) solid var(--border-subtle)',
        borderRadius: 'var(--radius-md)',
        boxShadow: SHADOWS[shadow] || SHADOWS.sm,
        padding: padded ? 'var(--space-6)' : 0,
        ...style,
      }}
      {...rest}
    >
      {children}
    </div>
  );
}

function CardHeader({ title, subtitle, actions, style = {} }: CardHeaderProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: 'var(--space-4)',
        marginBottom: 'var(--space-4)',
        ...style,
      }}
    >
      <div>
        {title && (
          <h3
            style={{
              fontSize: 'var(--type-card-title-size)',
              fontWeight: 'var(--type-card-title-weight)',
              color: 'var(--text-strong)',
              margin: 0,
            }}
          >
            {title}
          </h3>
        )}
        {subtitle && (
          <p
            style={{
              fontSize: 'var(--text-sm)',
              color: 'var(--text-muted)',
              margin: '4px 0 0',
            }}
          >
            {subtitle}
          </p>
        )}
      </div>
      {actions && (
        <div
          style={{
            display: 'flex',
            gap: 'var(--space-2)',
            flex: 'none',
          }}
        >
          {actions}
        </div>
      )}
    </div>
  );
}

function CardBody({ children, style = {} }: CardBodyProps) {
  return <div style={style}>{children}</div>;
}

function CardFooter({ children, style = {} }: CardFooterProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        gap: 'var(--space-2)',
        marginTop: 'var(--space-5)',
        paddingTop: 'var(--space-4)',
        borderTop: 'var(--border-width) solid var(--border-subtle)',
        ...style,
      }}
    >
      {children}
    </div>
  );
}

export const Card = Object.assign(CardRoot, {
  Header: CardHeader,
  Body: CardBody,
  Footer: CardFooter,
});
