'use client';

import type { ReactNode } from 'react';

import { Badge, type BadgeProps, type BadgeTone } from './badge';

export interface StatusBadgeProps extends Omit<BadgeProps, 'children'> {
  status?: string | null;
  label?: ReactNode;
}

interface StatusMeta {
  tone: BadgeTone;
  label: ReactNode;
  dot?: boolean;
}

const MAP: Record<string, StatusMeta> = {
  // CampaignStatus
  ACTIVE: {
    tone: 'active',
    label: 'Actif',
    dot: true,
  },
  PAUSED: {
    tone: 'neutral',
    label: 'En pause',
  },
  COMPLETED: {
    tone: 'success',
    label: 'Terminé',
  },
  CANCELLED: {
    tone: 'neutral',
    label: 'Annulé',
  },
  // CallStatus
  IN_PROGRESS: {
    tone: 'warning',
    label: 'En cours',
    dot: true,
  },
  COMPLETED_CALL: {
    tone: 'success',
    label: 'Terminé',
  },
  CANCELED: {
    tone: 'neutral',
    label: 'Annulé',
  },
  NO_ANSWER: {
    tone: 'warning',
    label: 'Sans réponse',
  },
  VOICE_BOX: {
    tone: 'neutral',
    label: 'Messagerie',
  },
  UNREACHABLE: {
    tone: 'danger',
    label: 'Injoignable',
  },
  FAILED: {
    tone: 'danger',
    label: 'Échoué',
  },
  TRANSFERRED: {
    tone: 'info',
    label: 'Transféré',
  },
  REJECTED: {
    tone: 'danger',
    label: 'Rejeté',
  },
  FAILOVER: {
    tone: 'danger',
    label: 'Bascule',
  },
  REJECTED_LIMIT_REACHED: {
    tone: 'danger',
    label: 'Limite atteinte',
  },
  REJECTED_MAX_CONCURRENCY: {
    tone: 'danger',
    label: 'Concurrence max',
  },
  // ScheduledCallStatus
  PENDING: {
    tone: 'warning',
    label: 'En attente',
  },
  DONE: {
    tone: 'success',
    label: 'Fait',
  },
  // StageSemantic
  NEW: {
    tone: 'info',
    label: 'Nouveau',
  },
  WON: {
    tone: 'success',
    label: 'Gagné',
    dot: true,
  },
  LOSE: {
    tone: 'danger',
    label: 'Perdu',
  },
  // CRM sync
  SUCCESS: {
    tone: 'success',
    label: 'Succès',
  },
  PARTIAL: {
    tone: 'warning',
    label: 'Partiel',
  },
  // user / generic
  ACTIVE_USER: {
    tone: 'success',
    label: 'Actif',
  },
  INACTIVE: {
    tone: 'neutral',
    label: 'Inactif',
  },
};

/**
 * Maps a backend enum value to the right tone + French label + dot.
 * Covers CampaignStatus, CallStatus, ScheduledCallStatus, StageSemantic,
 * CrmSync status, and user active/inactive. Pass `status` (the raw enum).
 */
export function StatusBadge({ status, label, size = 'md', ...rest }: StatusBadgeProps) {
  const m: StatusMeta = (status ? MAP[status] : undefined) || {
    tone: 'neutral',
    label: label || String(status || '—'),
  };

  return (
    <Badge tone={m.tone} dot={!!m.dot} size={size} {...rest}>
      {label || m.label}
    </Badge>
  );
}
