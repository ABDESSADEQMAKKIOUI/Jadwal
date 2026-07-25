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
      className={`rounded-xl border border-gray-200 bg-white shadow-sm ${className}`}
    >
      {(titre !== undefined || actions !== undefined) && (
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 px-5 py-4">
          <h2 className="text-sm font-semibold text-gray-900">{titre}</h2>
          {actions}
        </div>
      )}
      <div className="p-5">{children}</div>
    </section>
  );
}
