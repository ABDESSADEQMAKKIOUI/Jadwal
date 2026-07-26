'use client';

import { Fragment } from 'react';
import type { CSSProperties, ReactNode } from 'react';

export interface PaginationProps {
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number | null;
  onPageChange?: (page: number) => void;
  style?: CSSProperties;
}

interface PageButtonOptions {
  onClick?: () => void;
  disabled?: boolean;
  active?: boolean;
}

/**
 * Server-paging footer. Shows the visible range + total and prev/next page
 * controls. `page` is 0-indexed (matches the API). Renders a compact window
 * of page buttons.
 */
export function Pagination({
  page = 0,
  size = 20,
  totalElements = 0,
  totalPages,
  onPageChange,
  style = {},
}: PaginationProps) {
  const pages = totalPages != null ? totalPages : Math.max(1, Math.ceil(totalElements / size));
  const first = totalElements === 0 ? 0 : page * size + 1;
  const last = Math.min((page + 1) * size, totalElements);
  const go = (p: number) => {
    if (p >= 0 && p < pages && p !== page && onPageChange) onPageChange(p);
  };

  // compact window: first, current-1..current+1, last
  const win = new Set([0, pages - 1, page - 1, page, page + 1]);
  const nums = [...win].filter((n) => n >= 0 && n < pages).sort((a, b) => a - b);

  const btn = (content: ReactNode, opts: PageButtonOptions = {}) => (
    <button
      type="button"
      onClick={opts.onClick}
      disabled={opts.disabled}
      aria-current={opts.active ? 'page' : undefined}
      style={{
        minWidth: 30,
        height: 30,
        padding: '0 8px',
        fontSize: 'var(--text-sm)',
        fontVariantNumeric: 'tabular-nums',
        fontWeight: opts.active ? 'var(--weight-semibold)' : 'var(--weight-regular)',
        color: opts.active ? '#fff' : 'var(--text-body)',
        background: opts.active ? 'var(--color-primary)' : 'var(--surface-card)',
        border: `var(--border-width) solid ${opts.active ? 'var(--color-primary)' : 'var(--border-default)'}`,
        borderRadius: 'var(--radius-sm)',
        cursor: opts.disabled ? 'not-allowed' : 'pointer',
        opacity: opts.disabled ? 0.45 : 1,
      }}
    >
      {content}
    </button>
  );

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 'var(--space-4)',
        padding: '12px 4px',
        flexWrap: 'wrap',
        ...style,
      }}
    >
      <span
        style={{
          fontSize: 'var(--text-sm)',
          color: 'var(--text-muted)',
          fontVariantNumeric: 'tabular-nums',
        }}
      >
        {first.toLocaleString('fr-FR')}
        {'–'}
        {last.toLocaleString('fr-FR')} sur {totalElements.toLocaleString('fr-FR')}
      </span>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
        }}
      >
        {btn('‹', {
          onClick: () => go(page - 1),
          disabled: page === 0,
        })}
        {nums.map((n, idx) => {
          const prev: number | undefined = nums[idx - 1];
          const gap = prev != null && n - prev > 1;
          return (
            <Fragment key={n}>
              {gap && (
                <span
                  style={{
                    color: 'var(--text-subtle)',
                    padding: '0 2px',
                  }}
                >
                  {'…'}
                </span>
              )}
              {btn(n + 1, {
                onClick: () => go(n),
                active: n === page,
              })}
            </Fragment>
          );
        })}
        {btn('›', {
          onClick: () => go(page + 1),
          disabled: page >= pages - 1,
        })}
      </div>
    </div>
  );
}
