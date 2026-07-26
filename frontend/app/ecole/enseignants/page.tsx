'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState, type CSSProperties, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatUnites } from '@/lib/format';
import type { Enseignant, TypeEnseignant } from '@/lib/types';
import { Alert, Badge, Button, Card, EmptyState } from '@/components/ds';
import { Dialog } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { ChargementPage } from '@/components/ui/spinner';

/* Styles de tableau de la maquette (constantes TH / TD du document Ynexis). */
const TH_BASE: CSSProperties = {
  padding: '10px 14px',
  background: 'var(--neutral-50)',
  borderBottom: '1px solid var(--border-subtle)',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
  whiteSpace: 'nowrap',
};
const TH: CSSProperties = { ...TH_BASE, textAlign: 'left' };
const TH_RIGHT: CSSProperties = { ...TH_BASE, textAlign: 'right' };

const TD_BASE: CSSProperties = {
  padding: '0 14px',
  height: 'var(--row-height)',
  borderBottom: '1px solid var(--neutral-100)',
  color: 'var(--text-body)',
  whiteSpace: 'nowrap',
};
const TD: CSSProperties = { ...TD_BASE, textAlign: 'left' };
const TD_RIGHT: CSSProperties = {
  ...TD_BASE,
  textAlign: 'right',
  fontVariantNumeric: 'tabular-nums',
};

/* Pastille d'initiales de la colonne « Nom » (style inline de la maquette). */
const AVATAR: CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  width: '30px',
  height: '30px',
  flex: 'none',
  borderRadius: 'var(--radius-pill)',
  background: 'var(--teal-50)',
  color: 'var(--teal-700)',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
};

/* Puce de matière habilitée. */
const PUCE_MATIERE: CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  borderRadius: 'var(--radius-xs)',
  background: 'var(--surface-sunken)',
  padding: '2px 6px',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: '0.03em',
  color: 'var(--text-muted)',
};

/** Deux initiales au plus, comme la fonction `initiales` de la maquette. */
function initiales(nom: string): string {
  return nom
    .split(' ')
    .filter(Boolean)
    .map((partie) => partie[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

/** Barre de charge : teal, puis ambre au-delà de 95 % du quota. */
function styleBarre(charge: number, quota: number): CSSProperties {
  const ratio = quota > 0 ? charge / quota : 0;
  return {
    display: 'block',
    height: '100%',
    width: `${Math.min(100, Math.round(ratio * 100))}%`,
    borderRadius: 'var(--radius-pill)',
    background:
      ratio > 0.95 ? 'var(--status-warning-solid)' : 'var(--color-primary)',
  };
}

const FORM_INITIAL = {
  nomComplet: '',
  email: '',
  type: 'MIXTE' as TypeEnseignant,
  quotaHebdoUnites: '36',
  maxConsecutifUnites: '8',
  amplitudeMaxUnites: '20',
  vacataire: false,
  bufferTrajetUnites: '0',
};

export default function PageEnseignants() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { data: enseignants, isLoading, error } = useQuery({
    queryKey: ['ecole', 'enseignants'],
    queryFn: () => apiFetch<Enseignant[]>('/ecole/enseignants'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Enseignant | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'enseignants'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () => {
      const corps = JSON.stringify({
        nomComplet: formulaire.nomComplet,
        email: formulaire.email,
        type: formulaire.type,
        quotaHebdoUnites: Number(formulaire.quotaHebdoUnites),
        maxConsecutifUnites: Number(formulaire.maxConsecutifUnites),
        amplitudeMaxUnites: Number(formulaire.amplitudeMaxUnites),
        vacataire: formulaire.vacataire,
        bufferTrajetUnites: Number(formulaire.bufferTrajetUnites),
      });
      return enEdition === null
        ? apiFetch<Enseignant>('/ecole/enseignants', {
            method: 'POST',
            body: corps,
          })
        : apiFetch<Enseignant>(`/ecole/enseignants/${enEdition.id}`, {
            method: 'PATCH',
            body: corps,
          });
    },
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
    },
  });

  const suppression = useMutation({
    mutationFn: (id: number) =>
      apiFetch<unknown>(`/ecole/enseignants/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(enseignant: Enseignant) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(enseignant);
    setFormulaire({
      nomComplet: enseignant.nomComplet,
      email: enseignant.email,
      type: enseignant.type,
      quotaHebdoUnites: String(enseignant.quotaHebdoUnites),
      maxConsecutifUnites: String(enseignant.maxConsecutifUnites),
      amplitudeMaxUnites: String(enseignant.amplitudeMaxUnites),
      vacataire: enseignant.vacataire,
      bufferTrajetUnites: String(enseignant.bufferTrajetUnites),
    });
    setDialogOuvert(true);
  }

  function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    sauvegarde.mutate();
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          gap: '16px',
          flexWrap: 'wrap',
        }}
      >
        <p
          style={{
            margin: 0,
            fontSize: 'var(--text-base)',
            color: 'var(--text-muted)',
            maxWidth: '62ch',
          }}
        >
          Corps enseignant, quotas horaires, habilitations et indisponibilités.
        </p>
        <Button variant="primary" size="md" onClick={ouvrirCreation}>
          Nouvel enseignant
        </Button>
      </div>

      {error !== null && (
        <Alert tone="danger" title="Chargement impossible">
          {error.message}
        </Alert>
      )}

      {suppression.error !== null && (
        <Alert tone="danger" title="Suppression impossible">
          {suppression.error.message}
        </Alert>
      )}

      {isLoading && (
        <Card padded={false}>
          <ChargementPage />
        </Card>
      )}

      {enseignants !== undefined && enseignants.length === 0 && (
        <Card padded={false}>
          <EmptyState
            icon={
              <svg
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.75"
                strokeLinecap="round"
                strokeLinejoin="round"
                style={{ flex: 'none', display: 'block' }}
              >
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            }
            title="Aucun enseignant"
            description="Ajoutez le corps enseignant pour renseigner les quotas horaires, les habilitations et les indisponibilités."
            action={<Button onClick={ouvrirCreation}>Nouvel enseignant</Button>}
          />
        </Card>
      )}

      {enseignants !== undefined && enseignants.length > 0 && (
        <Card padded={false} style={{ overflow: 'hidden' }}>
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: 'var(--text-sm)',
            }}
          >
            <thead>
              <tr>
                <th style={TH}>Nom</th>
                <th style={TH}>Type</th>
                <th style={TH_RIGHT}>Quota hebdo</th>
                <th style={TH}>Matières habilitées</th>
                <th style={TH}>Charge affectée</th>
                <th style={TH_RIGHT}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {enseignants.map((enseignant) => {
                const matieres = enseignant.matieres ?? [];
                const fiche = `/ecole/enseignants/${enseignant.id}`;
                return (
                  <tr key={enseignant.id}>
                    <td style={TD}>
                      <span
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '10px',
                        }}
                      >
                        <span style={AVATAR}>
                          {initiales(enseignant.nomComplet)}
                        </span>
                        <span
                          style={{ display: 'flex', flexDirection: 'column' }}
                        >
                          <Link
                            href={fiche}
                            style={{
                              fontWeight: 'var(--weight-medium)',
                              color: 'var(--text-strong)',
                            }}
                          >
                            {enseignant.nomComplet}
                          </Link>
                          <span
                            style={{
                              fontSize: 'var(--text-xs)',
                              color: 'var(--text-muted)',
                            }}
                          >
                            {enseignant.email}
                          </span>
                        </span>
                      </span>
                    </td>
                    <td style={TD}>
                      <span style={{ display: 'inline-flex', gap: '6px' }}>
                        <Badge
                          tone={
                            enseignant.type === 'MIXTE' ? 'active' : 'info'
                          }
                          size="sm"
                        >
                          {enseignant.type}
                        </Badge>
                        {enseignant.vacataire && (
                          <Badge tone="info" size="sm">
                            VACATAIRE
                          </Badge>
                        )}
                      </span>
                    </td>
                    <td style={TD_RIGHT}>
                      {formatUnites(enseignant.quotaHebdoUnites)}
                    </td>
                    <td style={TD}>
                      {matieres.length === 0 ? (
                        <span style={{ color: 'var(--text-subtle)' }}>—</span>
                      ) : (
                        <span
                          style={{
                            display: 'flex',
                            flexWrap: 'wrap',
                            gap: '4px',
                          }}
                        >
                          {matieres.map((matiere) => (
                            <span key={matiere} style={PUCE_MATIERE}>
                              {matiere}
                            </span>
                          ))}
                        </span>
                      )}
                    </td>
                    <td style={TD}>
                      <span
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '10px',
                        }}
                      >
                        <span
                          style={{
                            width: '72px',
                            height: '6px',
                            borderRadius: 'var(--radius-pill)',
                            background: 'var(--surface-sunken)',
                            overflow: 'hidden',
                            flex: 'none',
                          }}
                        >
                          <span
                            style={styleBarre(
                              enseignant.chargeAffectee,
                              enseignant.quotaHebdoUnites,
                            )}
                          />
                        </span>
                        <span
                          style={{
                            fontVariantNumeric: 'tabular-nums',
                            color: 'var(--text-body)',
                          }}
                        >
                          {formatUnites(enseignant.chargeAffectee)} /{' '}
                          {formatUnites(enseignant.quotaHebdoUnites)}
                        </span>
                      </span>
                    </td>
                    <td style={TD_RIGHT}>
                      <span style={{ display: 'inline-flex', gap: '6px' }}>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => router.push(fiche)}
                        >
                          Détail
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => ouvrirEdition(enseignant)}
                        >
                          Modifier
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          style={{ color: 'var(--status-danger-fg)' }}
                          disabled={suppression.isPending}
                          onClick={() => {
                            if (
                              window.confirm(
                                `Supprimer l’enseignant « ${enseignant.nomComplet} » ?`,
                              )
                            ) {
                              suppression.mutate(enseignant.id);
                            }
                          }}
                        >
                          Supprimer
                        </Button>
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Card>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre={
          enEdition === null ? 'Nouvel enseignant' : 'Modifier l’enseignant'
        }
        onFermer={() => setDialogOuvert(false)}
      >
        <form onSubmit={soumettre} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="nomEnseignant">Nom complet</Label>
              <Input
                id="nomEnseignant"
                value={formulaire.nomComplet}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, nomComplet: e.target.value })
                }
                placeholder="Fatima Zahra Bennis"
                required
              />
            </div>
            <div>
              <Label htmlFor="emailEnseignant">E-mail</Label>
              <Input
                id="emailEnseignant"
                type="email"
                value={formulaire.email}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, email: e.target.value })
                }
                placeholder="f.bennis@ecole.ma"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="typeEnseignant">Type</Label>
              <Select
                id="typeEnseignant"
                value={formulaire.type}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    type: e.target.value as TypeEnseignant,
                  })
                }
              >
                <option value="MIXTE">Mixte (plusieurs classes)</option>
                <option value="PROPRE">Propre (classe dédiée)</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="quotaEnseignant">Quota hebdo (unités)</Label>
              <Input
                id="quotaEnseignant"
                type="number"
                min="1"
                value={formulaire.quotaHebdoUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    quotaHebdoUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-ink-muted">
                Soit {formatUnites(Number(formulaire.quotaHebdoUnites) || 0)}{' '}
                par semaine.
              </p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="maxConsecutif">Max consécutif (unités)</Label>
              <Input
                id="maxConsecutif"
                type="number"
                min="1"
                value={formulaire.maxConsecutifUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    maxConsecutifUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-ink-muted">
                {formatUnites(Number(formulaire.maxConsecutifUnites) || 0)}{' '}
                d’affilée maximum.
              </p>
            </div>
            <div>
              <Label htmlFor="amplitudeMax">Amplitude max (unités)</Label>
              <Input
                id="amplitudeMax"
                type="number"
                min="1"
                value={formulaire.amplitudeMaxUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    amplitudeMaxUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-ink-muted">
                {formatUnites(Number(formulaire.amplitudeMaxUnites) || 0)} par
                jour maximum.
              </p>
            </div>
          </div>
          <div className="grid grid-cols-2 items-end gap-4">
            <label className="flex items-center gap-2 pb-2 text-sm text-ink-body">
              <input
                type="checkbox"
                checked={formulaire.vacataire}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, vacataire: e.target.checked })
                }
                className="h-4 w-4 rounded-xs border-line-default text-brand focus-visible:shadow-[var(--ring)]"
              />
              Vacataire (intervient dans plusieurs établissements)
            </label>
            <div>
              <Label htmlFor="bufferTrajet">Buffer trajet (unités)</Label>
              <Input
                id="bufferTrajet"
                type="number"
                min="0"
                value={formulaire.bufferTrajetUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    bufferTrajetUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-ink-muted">
                Temps de trajet réservé pour les vacataires.
              </p>
            </div>
          </div>

          {sauvegarde.error !== null && (
            <Alert tone="danger">{sauvegarde.error.message}</Alert>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" onClick={() => setDialogOuvert(false)}>
              Annuler
            </Button>
            <Button type="submit" loading={sauvegarde.isPending}>
              {sauvegarde.isPending ? 'Enregistrement…' : 'Enregistrer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
