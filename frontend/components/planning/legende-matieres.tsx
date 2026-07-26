'use client';

import { Card } from '@/components/ds';
import type { Seance } from '@/lib/types-planning';
import { formatDureeUnites } from './grille-planning';

/** Couleur de repli identique à celle des blocs de séance. */
const COULEUR_DEFAUT = 'var(--color-primary)';

export interface LigneLegende {
  matiereId: number;
  libelle: string;
  couleur: string;
  volume: string;
}

/**
 * Matières présentes dans les séances affichées, avec leur volume cumulé.
 * Triées par volume décroissant, comme la légende de la maquette.
 */
export function construireLegende(
  seances: Seance[],
  dureeUniteMinutes: number,
): LigneLegende[] {
  const parMatiere = new Map<
    number,
    { libelle: string; couleur: string; unites: number }
  >();
  for (const seance of seances) {
    const existant = parMatiere.get(seance.matiereId);
    if (existant === undefined) {
      parMatiere.set(seance.matiereId, {
        libelle: seance.matiereLibelle,
        couleur:
          seance.couleur !== null && seance.couleur.length > 0
            ? seance.couleur
            : COULEUR_DEFAUT,
        unites: seance.dureeUnites,
      });
    } else {
      existant.unites += seance.dureeUnites;
    }
  }
  return [...parMatiere.entries()]
    .sort(
      (a, b) =>
        b[1].unites - a[1].unites || a[1].libelle.localeCompare(b[1].libelle),
    )
    .map(([matiereId, valeur]) => ({
      matiereId,
      libelle: valeur.libelle,
      couleur: valeur.couleur,
      volume: formatDureeUnites(valeur.unites, dureeUniteMinutes),
    }));
}

/** Carte « Légende des matières » du panneau latéral (maquette). */
export function LegendeMatieres({ lignes }: { lignes: LigneLegende[] }) {
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
          Légende des matières
        </h3>
      </div>
      <div
        style={{
          padding: '14px 20px 18px',
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
        }}
      >
        {lignes.length === 0 ? (
          <p
            style={{
              margin: 0,
              fontSize: 'var(--text-sm)',
              color: 'var(--text-muted)',
            }}
          >
            Aucune séance affichée.
          </p>
        ) : (
          lignes.map((ligne) => (
            <div
              key={ligne.matiereId}
              style={{ display: 'flex', alignItems: 'center', gap: 8 }}
            >
              <span
                style={{
                  display: 'inline-block',
                  width: 10,
                  height: 10,
                  flex: 'none',
                  borderRadius: 3,
                  background: ligne.couleur,
                }}
              />
              <span
                style={{
                  fontSize: 'var(--text-sm)',
                  color: 'var(--text-body)',
                }}
              >
                {ligne.libelle}
              </span>
              <span style={{ flex: 1 }} />
              <span
                style={{
                  fontSize: 'var(--text-xs)',
                  fontVariantNumeric: 'tabular-nums',
                  color: 'var(--text-muted)',
                }}
              >
                {ligne.volume}
              </span>
            </div>
          ))
        )}
      </div>
    </Card>
  );
}
