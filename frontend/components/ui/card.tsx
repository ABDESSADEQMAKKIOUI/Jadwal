import type { ReactNode } from 'react';

export function Card({
  titre,
  actions,
  children,
  className = '',
}: {
  titre?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={`rounded-md border border-line-subtle bg-surface-card shadow-[var(--shadow-sm)] ${className}`}
    >
      {(titre !== undefined || actions !== undefined) && (
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line-subtle px-5 py-4">
          {/* Titre de carte : rôle typographique --type-card-title du design system. */}
          <h2 className="text-lg font-semibold text-ink-strong">{titre}</h2>
          {actions}
        </div>
      )}
      <div className="p-5">{children}</div>
    </section>
  );
}
