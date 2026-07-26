'use client';

import { Badge, Button, Card, EmptyState } from '@/components/ds';
import { Spinner } from '@/components/ui/spinner';
import { formatDate } from '@/lib/format';
import type { PlanningVersion } from '@/lib/types-planning';

/**
 * Panneau latéral « Versions du planning » (maquette).
 * Chaque version non active propose sa restauration (I-08), confirmée par la page.
 */
export function PanneauVersions({
  versions,
  chargement,
  onRestaurer,
}: {
  versions: PlanningVersion[];
  chargement: boolean;
  onRestaurer: (version: PlanningVersion) => void;
}) {
  return (
    <Card padded={false}>
      <div
        style={{
          padding: '16px 20px',
          borderBottom: '1px solid var(--border-subtle)',
        }}
      >
        <h3
          style={{
            margin: 0,
            fontSize: 'var(--text-base)',
            fontWeight: 'var(--weight-semibold)',
            color: 'var(--text-strong)',
          }}
        >
          Versions du planning
        </h3>
      </div>
      <div
        style={{
          padding: '12px 16px 16px',
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
        }}
      >
        {chargement ? (
          <div
            style={{
              display: 'flex',
              justifyContent: 'center',
              padding: '20px 0',
            }}
          >
            <Spinner />
          </div>
        ) : versions.length === 0 ? (
          <EmptyState
            variant="gated"
            title="Aucune version"
            description="Lancez une génération pour créer un planning."
            style={{ padding: '20px 4px' }}
          />
        ) : (
          versions.map((version) => (
            <div
              key={version.id}
              style={{
                border: version.active
                  ? '1px solid var(--color-primary-border)'
                  : '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-sm)',
                background: version.active
                  ? 'var(--surface-selected)'
                  : undefined,
                padding: '12px 14px',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 8,
                }}
              >
                <p
                  style={{
                    margin: 0,
                    fontSize: 'var(--text-base)',
                    fontWeight: 'var(--weight-medium)',
                    color: 'var(--text-strong)',
                  }}
                >
                  {version.libelle}
                </p>
                {version.active && (
                  <Badge tone="success" size="sm">
                    ACTIVE
                  </Badge>
                )}
              </div>
              <p
                style={{
                  margin: '4px 0 0',
                  fontSize: 'var(--text-xs)',
                  color: 'var(--text-muted)',
                  fontVariantNumeric: 'tabular-nums',
                }}
              >
                {version.nbSeances} séances · créée le{' '}
                {formatDate(version.creeeLe)}
              </p>
              {!version.active && (
                <div style={{ marginTop: 10 }}>
                  <Button
                    variant="secondary"
                    size="sm"
                    fullWidth
                    onClick={() => onRestaurer(version)}
                  >
                    Restaurer
                  </Button>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </Card>
  );
}
