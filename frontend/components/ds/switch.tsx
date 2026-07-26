'use client';

import type { CSSProperties, ReactNode } from 'react';

export interface SwitchProps {
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  label?: string;
  description?: ReactNode;
  disabled?: boolean;
  id?: string;
  style?: CSSProperties;
}

/** Toggle switch for boolean agent/campaign flags (active, recordable, etc). */
export function Switch({
  checked = false,
  onChange,
  label,
  description,
  disabled = false,
  id,
  style = {},
}: SwitchProps) {
  const autoId = id || (label ? 'sw-' + label.toLowerCase().replace(/\s+/g, '-') : undefined);
  const toggle = () => {
    if (!disabled && onChange) onChange(!checked);
  };

  const track = (
    <button
      type="button"
      role="switch"
      id={autoId}
      aria-checked={checked}
      disabled={disabled}
      onClick={toggle}
      style={{
        position: 'relative',
        width: 38,
        height: 22,
        flex: 'none',
        borderRadius: 'var(--radius-pill)',
        border: 'none',
        padding: 0,
        background: checked ? 'var(--color-primary)' : 'var(--neutral-300)',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.55 : 1,
        transition: 'background var(--duration-normal) var(--ease-standard)',
      }}
    >
      <span
        aria-hidden="true"
        style={{
          position: 'absolute',
          top: 2,
          left: checked ? 18 : 2,
          width: 18,
          height: 18,
          borderRadius: '50%',
          background: '#fff',
          boxShadow: 'var(--shadow-sm)',
          transition: 'left var(--duration-normal) var(--ease-standard)',
        }}
      />
    </button>
  );

  if (!label && !description) return track;

  return (
    <label
      htmlFor={autoId}
      style={{
        display: 'flex',
        alignItems: description ? 'flex-start' : 'center',
        gap: '12px',
        cursor: disabled ? 'not-allowed' : 'pointer',
        ...style,
      }}
    >
      {track}
      <span
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
        }}
      >
        {label && (
          <span
            style={{
              fontSize: 'var(--text-base)',
              fontWeight: 'var(--weight-medium)',
              color: 'var(--text-strong)',
            }}
          >
            {label}
          </span>
        )}
        {description && (
          <span
            style={{
              fontSize: 'var(--text-xs)',
              color: 'var(--text-muted)',
            }}
          >
            {description}
          </span>
        )}
      </span>
    </label>
  );
}
