'use client';

import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useState, type CSSProperties, type ReactNode } from 'react';
import type { BadgeTone, KpiDeltaTone } from '@/components/ds';
import { Alert, Badge, Button, Card, EmptyState, KpiTile } from '@/components/ds';
import { ChargementPage } from '@/components/ui/spinner';
import { apiFetch } from '@/lib/api';
import { formatDate, formatMontant, libelleMode } from '@/lib/format';
import type {
  AbonnementEcole,
  DashboardEcole,
  Paiement,
  StatutAbonnement,
  StatutEtablissement,
  StatutPaiement,
} from '@/lib/types';

/* ------------------------------------------------------------------ */
/* Styles de la maquette (repris mot pour mot du document Ynexis).     */
/* ------------------------------------------------------------------ */

const TH: CSSProperties = {
  padding: '10px 14px',
  background: 'var(--neutral-50)',
  borderBottom: '1px solid var(--border-subtle)',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
  whiteSpace: 'nowrap',
  textAlign: 'left',
};

const TH_DROITE: CSSProperties = { ...TH, textAlign: 'right' };

const TD: CSSProperties = {
  padding: '0 14px',
  height: 'var(--row-height)',
  borderBottom: '1px solid var(--neutral-100)',
  color: 'var(--text-body)',
  whiteSpace: 'nowrap',
  textAlign: 'left',
};

const TD_DROITE: CSSProperties = {
  ...TD,
  textAlign: 'right',
  fontVariantNumeric: 'tabular-nums',
};

const TD_MONO: CSSProperties = {
  ...TD,
  fontFamily: 'var(--font-mono)',
  fontSize: 'var(--text-xs)',
  fontVariantNumeric: 'tabular-nums',
  color: 'var(--text-muted)',
};

const ENTETE_CARTE: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 16,
  padding: '18px 24px',
  borderBottom: '1px solid var(--border-subtle)',
};

const TITRE_CARTE: CSSProperties = {
  margin: 0,
  fontSize: 'var(--type-card-title-size)',
  fontWeight: 'var(--type-card-title-weight)',
  color: 'var(--text-strong)',
};

const SURTITRE: CSSProperties = {
  margin: 0,
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
};

/** Intitulé de définition (`<dt>`) : capitales fines, comme la maquette. */
const DT: CSSProperties = {
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
};

const DD: CSSProperties = {
  margin: '4px 0 0',
  fontWeight: 'var(--weight-medium)',
  color: 'var(--text-strong)',
};

const DD_MONO: CSSProperties = {
  ...DD,
  fontFamily: 'var(--font-mono)',
  fontVariantNumeric: 'tabular-nums',
};

/** Cellule « puits » des montants d'abonnement (3 colonnes de la maquette). */
function puits(succes: boolean): CSSProperties {
  return {
    borderRadius: 'var(--radius-sm)',
    background: succes ? 'var(--status-success-bg)' : 'var(--surface-sunken)',
    padding: 12,
  };
}

function puitsLibelle(succes: boolean): CSSProperties {
  return {
    margin: 0,
    fontSize: 'var(--text-2xs)',
    fontWeight: 'var(--weight-semibold)',
    letterSpacing: 'var(--tracking-caps)',
    textTransform: 'uppercase',
    color: succes ? 'var(--status-success-fg)' : 'var(--text-muted)',
  };
}

function puitsValeur(succes: boolean): CSSProperties {
  return {
    margin: '6px 0 0',
    fontSize: 'var(--text-md)',
    fontWeight: 'var(--weight-semibold)',
    fontVariantNumeric: 'tabular-nums',
    color: succes ? 'var(--status-success-fg)' : 'var(--text-strong)',
  };
}

/* ------------------------------------------------------------------ */
/* Statuts JADWAL → tons du design system + libellés de la maquette.   */
/* ------------------------------------------------------------------ */

const TON_ETABLISSEMENT: Record<StatutEtablissement, BadgeTone> = {
  ACTIF: 'active',
  SUSPENDU: 'danger',
};

const LIBELLE_ETABLISSEMENT: Record<StatutEtablissement, string> = {
  ACTIF: 'ACTIF',
  SUSPENDU: 'SUSPENDU',
};

const TON_ABONNEMENT: Record<StatutAbonnement, BadgeTone> = {
  ACTIF: 'active',
  EN_ATTENTE_PAIEMENT: 'warning',
  SUSPENDU: 'danger',
  EXPIRE: 'neutral',
};

const LIBELLE_ABONNEMENT: Record<StatutAbonnement, string> = {
  ACTIF: 'ACTIF',
  EN_ATTENTE_PAIEMENT: 'EN ATTENTE DE PAIEMENT',
  SUSPENDU: 'SUSPENDU',
  EXPIRE: 'EXPIRÉ',
};

const TON_PAIEMENT: Record<StatutPaiement, BadgeTone> = {
  CONFIRME: 'success',
  EN_ATTENTE: 'warning',
  REJETE: 'danger',
};

const LIBELLE_PAIEMENT: Record<StatutPaiement, string> = {
  CONFIRME: 'CONFIRMÉ',
  EN_ATTENTE: 'EN ATTENTE',
  REJETE: 'REJETÉ',
};

/* ------------------------------------------------------------------ */
/* Tuiles de KPI : uniquement des chiffres calculés à partir du        */
/* payload réel de GET /api/ecole/dashboard (aucune valeur inventée).  */
/* ------------------------------------------------------------------ */

/** Montant compact des tuiles de KPI (« 16 000 MAD »), comme la maquette. */
const formateurEntier = new Intl.NumberFormat('fr-FR', {
  maximumFractionDigits: 0,
});

function montantCompact(montant: number): string {
  return `${formateurEntier.format(montant)} MAD`;
}

/** Nombre de jours entiers restants avant la date de fin (peut être négatif). */
function joursRestants(dateFin: string): number | null {
  const fin = new Date(dateFin);
  if (Number.isNaN(fin.getTime())) return null;
  const aujourdhui = new Date();
  const jour = 24 * 60 * 60 * 1000;
  return Math.ceil((fin.getTime() - aujourdhui.getTime()) / jour);
}

interface Tuile {
  id: string;
  label: string;
  value: ReactNode;
  delta: ReactNode;
  deltaTone: KpiDeltaTone;
  hint: ReactNode;
}

function construireTuiles(
  abonnement: AbonnementEcole | null,
  paiements: Paiement[],
): Tuile[] {
  const confirmes = paiements.filter((p) => p.statut === 'CONFIRME');
  const enAttente = paiements.filter((p) => p.statut === 'EN_ATTENTE');

  const tuilePaiements: Tuile = {
    id: 'paiements',
    label: 'Paiements enregistrés',
    value: String(paiements.length),
    delta: `${confirmes.length} confirmé${confirmes.length > 1 ? 's' : ''}`,
    deltaTone: confirmes.length > 0 ? 'success' : 'neutral',
    hint:
      enAttente.length > 0
        ? `${enAttente.length} en attente`
        : 'aucun en attente',
  };

  if (abonnement === null) {
    if (paiements.length === 0) return [];
    const encaisse = confirmes.reduce((total, p) => total + p.montant, 0);
    return [
      tuilePaiements,
      {
        id: 'encaisse',
        label: 'Total encaissé',
        value: montantCompact(encaisse),
        delta: 'paiements confirmés',
        deltaTone: 'success',
        hint: 'tous abonnements',
      },
    ];
  }

  const pourcentage =
    abonnement.prixAnnuel > 0
      ? Math.round((abonnement.totalPaye / abonnement.prixAnnuel) * 100)
      : 0;
  const solde = abonnement.resteAPayer <= 0;
  const jours = joursRestants(abonnement.dateFin);

  return [
    {
      id: 'paye',
      label: 'Total payé',
      value: montantCompact(abonnement.totalPaye),
      delta: `${pourcentage} % du prix annuel`,
      deltaTone: 'success',
      hint: `sur ${montantCompact(abonnement.prixAnnuel)}`,
    },
    {
      id: 'reste',
      label: 'Reste à payer',
      value: montantCompact(abonnement.resteAPayer),
      delta: solde ? 'soldé' : 'à régler',
      deltaTone: solde ? 'success' : 'danger',
      hint: 'virement · chèque · espèces',
    },
    {
      id: 'echeance',
      label: 'Fin d’abonnement',
      value: jours === null ? '—' : jours >= 0 ? `${jours} j` : 'Échu',
      delta:
        jours === null
          ? null
          : jours >= 0
            ? 'restants'
            : 'échéance dépassée',
      deltaTone: jours !== null && jours < 0 ? 'danger' : 'neutral',
      hint: `jusqu’au ${formatDate(abonnement.dateFin)}`,
    },
    tuilePaiements,
  ];
}

/** « 2025–2026 » : période réelle de l'abonnement, jamais une année devinée. */
function periodeAbonnement(abonnement: AbonnementEcole | null): string | null {
  if (abonnement === null) return null;
  const debut = new Date(abonnement.dateDebut);
  const fin = new Date(abonnement.dateFin);
  if (Number.isNaN(debut.getTime()) || Number.isNaN(fin.getTime())) return null;
  const anneeDebut = debut.getFullYear();
  const anneeFin = fin.getFullYear();
  return anneeDebut === anneeFin
    ? String(anneeDebut)
    : `${anneeDebut}–${anneeFin}`;
}

/* ------------------------------------------------------------------ */

/** Nombre de paiements affichés tant que l'historique n'est pas déplié. */
const PAIEMENTS_REPLIES = 5;

export default function PageTableauDeBordEcole() {
  const router = useRouter();
  const [historiqueDeplie, setHistoriqueDeplie] = useState(false);
  const { data, isLoading, error } = useQuery({
    queryKey: ['ecole', 'dashboard'],
    queryFn: () => apiFetch<DashboardEcole>('/ecole/dashboard'),
  });

  if (isLoading) {
    return <ChargementPage />;
  }

  if (error !== null) {
    return (
      <Alert tone="danger" title="Le tableau de bord n’a pas pu être chargé">
        {error.message}
      </Alert>
    );
  }

  if (data === undefined) {
    return (
      <Card padded={false}>
        <EmptyState
          variant="gated"
          title="Aucune donnée disponible."
          description="Le serveur JADWAL n’a renvoyé aucune information pour cet établissement."
        />
      </Card>
    );
  }

  const { etablissement, abonnement, paiements } = data;
  const tuiles = construireTuiles(abonnement, paiements);
  const periode = periodeAbonnement(abonnement);
  const historiqueRepliable = paiements.length > PAIEMENTS_REPLIES;
  const paiementsAffiches =
    historiqueRepliable && !historiqueDeplie
      ? paiements.slice(0, PAIEMENTS_REPLIES)
      : paiements;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <div>
          <p style={SURTITRE}>
            {periode === null
              ? etablissement.nom
              : `${etablissement.nom} · ${periode}`}
          </p>
          <p
            style={{
              margin: '6px 0 0',
              fontSize: 'var(--text-base)',
              color: 'var(--text-muted)',
            }}
          >
            Vue d’ensemble de l’établissement, de l’abonnement et des paiements.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {/* Le bouton navigue : il annonce donc une navigation, pas un
              téléchargement. Les exports PDF sont sur l'écran Planning. */}
          <Button
            variant="secondary"
            size="md"
            title="Consulter les emplois du temps et les exporter en PDF"
            onClick={() => router.push('/ecole/planning')}
          >
            Ouvrir le planning
          </Button>
          <Button
            variant="primary"
            size="md"
            onClick={() => router.push('/ecole/generation')}
          >
            Lancer une génération
          </Button>
        </div>
      </div>

      {tuiles.length > 0 && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: `repeat(${tuiles.length}, minmax(0, 1fr))`,
            gap: 16,
          }}
        >
          {tuiles.map((tuile) => (
            <KpiTile
              key={tuile.id}
              label={tuile.label}
              value={tuile.value}
              delta={tuile.delta}
              deltaTone={tuile.deltaTone}
              hint={tuile.hint}
            />
          ))}
        </div>
      )}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: 16,
          alignItems: 'start',
        }}
      >
        <Card padded={false}>
          <div style={ENTETE_CARTE}>
            <h3 style={TITRE_CARTE}>Mon établissement</h3>
            <Badge tone={TON_ETABLISSEMENT[etablissement.statut]} dot>
              {LIBELLE_ETABLISSEMENT[etablissement.statut]}
            </Badge>
          </div>
          <div style={{ padding: '20px 24px 24px' }}>
            <p
              style={{
                margin: 0,
                fontSize: 'var(--text-md)',
                fontWeight: 'var(--weight-semibold)',
                color: 'var(--text-strong)',
              }}
            >
              {etablissement.nom}
            </p>
            <p
              style={{
                margin: '4px 0 0',
                fontFamily: 'var(--font-mono)',
                fontSize: 'var(--text-xs)',
                color: 'var(--text-muted)',
              }}
            >
              {etablissement.code}
            </p>
            <dl
              style={{
                margin: '20px 0 0',
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: 16,
                fontSize: 'var(--text-base)',
              }}
            >
              <div>
                <dt style={DT}>Ville</dt>
                <dd style={DD}>{etablissement.ville}</dd>
              </div>
              <div>
                <dt style={DT}>Téléphone</dt>
                <dd style={DD_MONO}>{etablissement.telephone}</dd>
              </div>
              <div style={{ gridColumn: 'span 2' }}>
                <dt style={DT}>E-mail</dt>
                <dd style={DD}>{etablissement.email}</dd>
              </div>
            </dl>
          </div>
        </Card>

        <Card padded={false}>
          <div style={ENTETE_CARTE}>
            <h3 style={TITRE_CARTE}>Mon abonnement</h3>
            {abonnement !== null && (
              <Badge tone={TON_ABONNEMENT[abonnement.statut]} dot>
                {LIBELLE_ABONNEMENT[abonnement.statut]}
              </Badge>
            )}
          </div>
          <div style={{ padding: '20px 24px 24px' }}>
            {abonnement === null ? (
              <EmptyState
                variant="gated"
                title="Aucun abonnement pour le moment."
                description="Veuillez contacter l’administration JADWAL pour souscrire un abonnement et activer votre espace."
                style={{ padding: '12px 0 20px' }}
              />
            ) : (
              <>
                <p
                  style={{
                    margin: 0,
                    fontSize: 'var(--text-md)',
                    fontWeight: 'var(--weight-semibold)',
                    color: 'var(--text-strong)',
                  }}
                >
                  {abonnement.planNom}
                </p>
                <p
                  style={{
                    margin: '4px 0 0',
                    fontSize: 'var(--text-sm)',
                    color: 'var(--text-muted)',
                  }}
                >
                  Du {formatDate(abonnement.dateDebut)} au{' '}
                  {formatDate(abonnement.dateFin)}
                </p>
                <div
                  style={{
                    marginTop: 18,
                    display: 'grid',
                    gridTemplateColumns: 'repeat(3,1fr)',
                    gap: 12,
                  }}
                >
                  <div style={puits(false)}>
                    <p style={puitsLibelle(false)}>Prix annuel</p>
                    <p style={puitsValeur(false)}>
                      {formatMontant(abonnement.prixAnnuel)}
                    </p>
                  </div>
                  <div style={puits(true)}>
                    <p style={puitsLibelle(true)}>Total payé</p>
                    <p style={puitsValeur(true)}>
                      {formatMontant(abonnement.totalPaye)}
                    </p>
                  </div>
                  <div style={puits(false)}>
                    <p style={puitsLibelle(false)}>Reste à payer</p>
                    <p style={puitsValeur(false)}>
                      {formatMontant(abonnement.resteAPayer)}
                    </p>
                  </div>
                </div>
              </>
            )}
            <div style={{ marginTop: 16 }}>
              <Alert tone="neutral">
                Paiement par virement bancaire, chèque ou espèces auprès de
                l’administration JADWAL. Aucun paiement en ligne.
              </Alert>
            </div>
          </div>
        </Card>
      </div>

      <Card padded={false} style={{ overflow: 'hidden' }}>
        <div style={ENTETE_CARTE}>
          <h3 style={TITRE_CARTE}>Historique des paiements</h3>
          {/* La maquette place ici un contrôle « Tout voir » de hauteur
              var(--control-height-sm). Il ne s'affiche que s'il y a
              réellement quelque chose à déplier ; sinon le compteur en
              tient la hauteur. */}
          {historiqueRepliable ? (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setHistoriqueDeplie((precedent) => !precedent)}
            >
              {historiqueDeplie
                ? 'Réduire'
                : `Tout voir (${paiements.length})`}
            </Button>
          ) : (
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                minHeight: 'var(--control-height-sm)',
                fontSize: 'var(--text-xs)',
                color: 'var(--text-muted)',
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              {paiements.length} paiement{paiements.length > 1 ? 's' : ''}
            </span>
          )}
        </div>
        {paiements.length === 0 ? (
          <EmptyState
            variant="empty"
            title="Aucun paiement enregistré pour le moment."
            description="Les règlements transmis à l’administration JADWAL apparaîtront ici dès leur saisie."
          />
        ) : (
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: 'var(--text-sm)',
            }}
          >
            <thead>
              <tr>
                <th style={TH}>Date</th>
                <th style={TH}>Plan</th>
                <th style={TH_DROITE}>Montant</th>
                <th style={TH}>Mode</th>
                <th style={TH}>Référence</th>
                <th style={TH}>Statut</th>
              </tr>
            </thead>
            <tbody>
              {paiementsAffiches.map((paiement) => (
                <tr key={paiement.id}>
                  <td style={TD}>{formatDate(paiement.datePaiement)}</td>
                  <td style={TD}>{paiement.planNom}</td>
                  <td style={TD_DROITE}>{formatMontant(paiement.montant)}</td>
                  <td style={TD}>{libelleMode(paiement.mode)}</td>
                  <td style={TD_MONO}>{paiement.reference ?? '—'}</td>
                  <td style={TD}>
                    <Badge tone={TON_PAIEMENT[paiement.statut]} dot size="sm">
                      {LIBELLE_PAIEMENT[paiement.statut]}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
