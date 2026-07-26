'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  apiFetchDetaille,
  ErreurApi,
  extraireConflits,
} from '@/lib/api-planning';
import { apiFetch } from '@/lib/api';
import type {
  Conflit,
  EnseignantResume,
  GrilleHoraire,
  GroupeResume,
  PlanningVersion,
  PlanningVue,
  SalleResume,
  Seance,
  Semaine,
  VuePlanning,
} from '@/lib/types-planning';
import { GrillePlanning } from '@/components/planning/grille-planning';
import {
  construireLegende,
  LegendeMatieres,
} from '@/components/planning/legende-matieres';
import { PanneauVersions } from '@/components/planning/panneau-versions';
import { Alert, Button, Card, EmptyState, Select } from '@/components/ds';
import { Dialog } from '@/components/ui/dialog';
import { ChargementPage, Spinner } from '@/components/ui/spinner';

interface OptionEntite {
  id: number;
  libelle: string;
}

function aplatirGroupes(
  groupes: GroupeResume[],
  prefixe = '',
): OptionEntite[] {
  const resultat: OptionEntite[] = [];
  for (const groupe of groupes) {
    resultat.push({ id: groupe.id, libelle: `${prefixe}${groupe.libelle}` });
    if (groupe.sousGroupes.length > 0) {
      resultat.push(...aplatirGroupes(groupe.sousGroupes, `${prefixe}— `));
    }
  }
  return resultat;
}

export default function PagePlanning() {
  const clientQuery = useQueryClient();

  const [vue, setVue] = useState<VuePlanning>('GROUPE');
  const [entiteId, setEntiteId] = useState<number | null>(null);
  const [versionId, setVersionId] = useState<number | null>(null);
  const [semaine, setSemaine] = useState<Semaine>('TOUTES');
  const [conflits, setConflits] = useState<Conflit[] | null>(null);
  const [seancePourSalle, setSeancePourSalle] = useState<Seance | null>(null);
  const [seancePourEnseignant, setSeancePourEnseignant] =
    useState<Seance | null>(null);
  const [salleChoisie, setSalleChoisie] = useState<number | null>(null);
  const [enseignantChoisi, setEnseignantChoisi] = useState<number | null>(null);
  const [versionARestaurer, setVersionARestaurer] =
    useState<PlanningVersion | null>(null);
  const [exportEnCours, setExportEnCours] = useState(false);
  const [erreurExport, setErreurExport] = useState<string | null>(null);

  const requeteGrille = useQuery({
    queryKey: ['ecole', 'grille'],
    queryFn: () => apiFetch<GrilleHoraire>('/ecole/grille'),
  });

  const requeteVersions = useQuery({
    queryKey: ['ecole', 'plannings', 'versions'],
    queryFn: () => apiFetch<PlanningVersion[]>('/ecole/plannings/versions'),
  });

  const requeteGroupes = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<GroupeResume[]>('/ecole/groupes'),
  });

  const requeteEnseignants = useQuery({
    queryKey: ['ecole', 'enseignants'],
    queryFn: () => apiFetch<EnseignantResume[]>('/ecole/enseignants'),
  });

  const requeteSalles = useQuery({
    queryKey: ['ecole', 'salles'],
    queryFn: () => apiFetch<SalleResume[]>('/ecole/salles'),
  });

  const options: OptionEntite[] =
    vue === 'GROUPE'
      ? aplatirGroupes(requeteGroupes.data ?? [])
      : vue === 'ENSEIGNANT'
        ? (requeteEnseignants.data ?? []).map((e) => ({
            id: e.id,
            libelle: e.nomComplet,
          }))
        : (requeteSalles.data ?? []).map((s) => ({
            id: s.id,
            libelle: s.nom,
          }));

  const entiteEffective =
    entiteId !== null && options.some((o) => o.id === entiteId)
      ? entiteId
      : (options[0]?.id ?? null);

  const segmentVue =
    vue === 'GROUPE' ? 'groupe' : vue === 'ENSEIGNANT' ? 'enseignant' : 'salle';

  const libelleEntite =
    vue === 'GROUPE' ? 'Groupe' : vue === 'ENSEIGNANT' ? 'Enseignant' : 'Salle';

  const parametres = new URLSearchParams({ semaine });
  if (versionId !== null) parametres.set('versionId', String(versionId));

  const requetePlanning = useQuery({
    queryKey: [
      'ecole',
      'planning',
      segmentVue,
      entiteEffective,
      versionId,
      semaine,
    ],
    queryFn: () =>
      apiFetch<PlanningVue>(
        `/ecole/plannings/${segmentVue}/${entiteEffective}?${parametres.toString()}`,
      ),
    enabled: entiteEffective !== null,
  });

  function invaliderPlanning() {
    void clientQuery.invalidateQueries({ queryKey: ['ecole', 'planning'] });
  }

  /**
   * Télécharge le PDF des emplois du temps. Sans groupeId, le PDF contient tous les groupes
   * (une page chacun). Les erreurs du backend (aucun planning actif…) sont affichées au lieu
   * d'enregistrer un fichier invalide.
   */
  async function telechargerPdf(groupeId?: number) {
    setErreurExport(null);
    setExportEnCours(true);
    try {
      const requete = new URLSearchParams({ semaine });
      if (versionId !== null) requete.set('versionId', String(versionId));
      if (groupeId !== undefined) requete.set('groupeId', String(groupeId));

      const reponse = await fetch(`/api/exports/plannings?${requete.toString()}`);
      if (!reponse.ok) {
        let message = "L'export a échoué.";
        try {
          const corps: unknown = await reponse.json();
          if (corps && typeof corps === 'object' && 'message' in corps) {
            const brut = (corps as { message?: unknown }).message;
            if (typeof brut === 'string' && brut.length > 0) message = brut;
          }
        } catch {
          // Réponse non JSON : on garde le message par défaut.
        }
        setErreurExport(message);
        return;
      }

      const enTete = reponse.headers.get('content-disposition') ?? '';
      const correspondance = /filename="?([^";]+)"?/.exec(enTete);
      const nomFichier = correspondance?.[1] ?? 'emplois-du-temps.pdf';

      const blob = await reponse.blob();
      const url = URL.createObjectURL(blob);
      const lien = document.createElement('a');
      lien.href = url;
      lien.download = nomFichier;
      document.body.appendChild(lien);
      lien.click();
      document.body.removeChild(lien);
      URL.revokeObjectURL(url);
    } catch {
      setErreurExport('Impossible de contacter le serveur.');
    } finally {
      setExportEnCours(false);
    }
  }

  const modifierSeance = useMutation({
    mutationFn: ({
      seanceId,
      corps,
    }: {
      seanceId: number;
      corps: { creneauId?: number; salleId?: number; enseignantId?: number };
    }) =>
      apiFetchDetaille<Seance>(`/ecole/seances/${seanceId}`, {
        method: 'PATCH',
        body: JSON.stringify(corps),
      }),
    onSuccess: () => {
      setConflits(null);
      invaliderPlanning();
    },
    onError: (erreur: Error) => {
      if (erreur instanceof ErreurApi && erreur.statut === 409) {
        const liste = extraireConflits(erreur.corps);
        setConflits(
          liste.length > 0
            ? liste
            : [{ regle: 'CONFLIT', message: erreur.message }],
        );
      } else {
        setConflits([{ regle: 'ERREUR', message: erreur.message }]);
      }
    },
  });

  const basculerVerrou = useMutation({
    mutationFn: (seance: Seance) =>
      apiFetch(
        `/ecole/seances/${seance.id}/${seance.verrouillee ? 'deverrouiller' : 'verrouiller'}`,
        { method: 'POST' },
      ),
    onSuccess: () => invaliderPlanning(),
    onError: (erreur: Error) =>
      setConflits([{ regle: 'ERREUR', message: erreur.message }]),
  });

  const restaurerVersion = useMutation({
    mutationFn: (id: number) =>
      apiFetch(`/ecole/plannings/versions/${id}/activer`, { method: 'POST' }),
    onSuccess: () => {
      setVersionARestaurer(null);
      setVersionId(null);
      void clientQuery.invalidateQueries({
        queryKey: ['ecole', 'plannings', 'versions'],
      });
      invaliderPlanning();
    },
    onError: (erreur: Error) => {
      setVersionARestaurer(null);
      setConflits([{ regle: 'ERREUR', message: erreur.message }]);
    },
  });

  if (requeteGrille.isLoading) {
    return <ChargementPage />;
  }

  if (requeteGrille.error !== null || requeteGrille.data === undefined) {
    return (
      <Alert tone="warning" title="Grille horaire indisponible">
        Configurez d’abord la grille horaire de l’établissement dans le
        Référentiel.
      </Alert>
    );
  }

  const grille = requeteGrille.data;
  const versions = requeteVersions.data ?? [];
  const salles = requeteSalles.data ?? [];
  const enseignants = requeteEnseignants.data ?? [];
  const seancesAffichees = requetePlanning.data?.seances ?? [];
  const legende = construireLegende(seancesAffichees, grille.dureeUniteMinutes);

  const nbConflits = conflits?.length ?? 0;
  const erreurSimple =
    conflits !== null && conflits.every((conflit) => conflit.regle === 'ERREUR');
  const titreConflits = erreurSimple
    ? 'Action refusée'
    : `Déplacement refusé : ${nbConflits} conflit${nbConflits > 1 ? 's' : ''} détecté${nbConflits > 1 ? 's' : ''}`;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          gap: 16,
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
          Consultation et ajustement manuel. Glissez une séance vers un autre
          créneau pour la déplacer.
        </p>
        <div style={{ display: 'flex', gap: 8 }}>
          <Button
            variant="secondary"
            size="md"
            onClick={() => telechargerPdf(entiteEffective ?? undefined)}
            disabled={
              exportEnCours || vue !== 'GROUPE' || entiteEffective === null
            }
            title={
              vue === 'GROUPE'
                ? 'Exporter le planning du groupe affiché'
                : "L'export PDF concerne les groupes"
            }
          >
            PDF · groupe affiché
          </Button>
          <Button
            variant="primary"
            size="md"
            loading={exportEnCours}
            onClick={() => telechargerPdf()}
            title="Exporter le planning de tous les groupes"
          >
            PDF · tous les groupes
          </Button>
        </div>
      </div>

      <Card padded={false}>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4, minmax(0, 1fr)) auto',
            gap: 16,
            alignItems: 'end',
            padding: '18px 24px',
          }}
        >
          <Select
            label="Vue"
            id="vue"
            value={vue}
            onChange={(evenement) => {
              setVue(evenement.target.value as VuePlanning);
              setEntiteId(null);
            }}
            options={[
              { value: 'GROUPE', label: 'Groupe' },
              { value: 'ENSEIGNANT', label: 'Enseignant' },
              { value: 'SALLE', label: 'Salle' },
            ]}
          />
          <Select
            label={libelleEntite}
            id="entite"
            value={entiteEffective ?? ''}
            onChange={(evenement) => setEntiteId(Number(evenement.target.value))}
          >
            {options.length === 0 && <option value="">Aucun élément</option>}
            {options.map((option) => (
              <option key={option.id} value={option.id}>
                {option.libelle}
              </option>
            ))}
          </Select>
          <Select
            label="Version"
            id="version"
            value={versionId ?? ''}
            onChange={(evenement) =>
              setVersionId(
                evenement.target.value === ''
                  ? null
                  : Number(evenement.target.value),
              )
            }
          >
            <option value="">Version active</option>
            {versions.map((version) => (
              <option key={version.id} value={version.id}>
                {version.libelle}
                {version.active ? ' (Active)' : ''}
              </option>
            ))}
          </Select>
          <Select
            label="Semaine"
            id="semaine"
            value={semaine}
            onChange={(evenement) =>
              setSemaine(evenement.target.value as Semaine)
            }
            options={[
              { value: 'TOUTES', label: 'Toutes' },
              { value: 'A', label: 'Semaine A' },
              { value: 'B', label: 'Semaine B' },
            ]}
          />
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              height: 38,
              paddingLeft: 16,
              borderLeft: '1px solid var(--border-subtle)',
            }}
          >
            <span
              style={{
                display: 'inline-block',
                width: 8,
                height: 8,
                borderRadius: 'var(--radius-pill)',
                background:
                  conflits === null
                    ? 'var(--status-success-solid)'
                    : 'var(--status-danger-solid)',
              }}
            />
            <span
              style={{
                fontSize: 'var(--text-sm)',
                color: 'var(--text-muted)',
                whiteSpace: 'nowrap',
              }}
            >
              {conflits === null
                ? 'Aucun conflit signalé'
                : `${nbConflits} conflit${nbConflits > 1 ? 's' : ''} signalé${nbConflits > 1 ? 's' : ''}`}
            </span>
          </div>
        </div>
      </Card>

      {conflits !== null && (
        <Alert
          tone="danger"
          title={titreConflits}
          onClose={() => setConflits(null)}
        >
          <ul
            style={{
              margin: '6px 0 0',
              padding: 0,
              listStyle: 'none',
              display: 'flex',
              flexDirection: 'column',
              gap: 4,
            }}
          >
            {conflits.map((conflit, index) => (
              <li key={`${conflit.regle}-${index}`}>
                {conflit.regle.length > 0 && conflit.regle !== 'ERREUR' && (
                  <>
                    <span
                      style={{
                        fontFamily: 'var(--font-mono)',
                        fontSize: 'var(--text-xs)',
                        fontWeight: 'var(--weight-semibold)',
                      }}
                    >
                      {conflit.regle}
                    </span>
                    {' · '}
                  </>
                )}
                {conflit.message}
              </li>
            ))}
          </ul>
        </Alert>
      )}

      {erreurExport !== null && (
        <Alert
          tone="danger"
          title="Export PDF impossible"
          onClose={() => setErreurExport(null)}
        >
          {erreurExport}
        </Alert>
      )}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(0, 1fr) 272px',
          gap: 20,
          alignItems: 'start',
        }}
      >
        <div style={{ minWidth: 0, position: 'relative' }}>
          {modifierSeance.isPending && (
            <div
              style={{
                position: 'absolute',
                right: 12,
                top: 12,
                zIndex: 'var(--z-sticky)',
              }}
            >
              <Spinner />
            </div>
          )}
          {requetePlanning.isLoading ? (
            <Card>
              <ChargementPage />
            </Card>
          ) : requetePlanning.error !== null ? (
            <Alert tone="danger" title="Planning indisponible">
              {requetePlanning.error.message}
            </Alert>
          ) : entiteEffective === null ? (
            <Card padded={false}>
              <EmptyState
                variant="gated"
                title="Aucune entité à afficher"
                description="Créez d’abord vos groupes, enseignants et salles dans le Référentiel."
              />
            </Card>
          ) : requetePlanning.data === undefined ? (
            <Card padded={false}>
              <EmptyState
                variant="gated"
                title="Aucun planning disponible"
                description="Lancez une génération pour produire un emploi du temps."
              />
            </Card>
          ) : (
            <GrillePlanning
              grille={grille}
              planning={requetePlanning.data}
              vue={vue}
              onDeplacer={(seanceId, creneauId) =>
                modifierSeance.mutate({ seanceId, corps: { creneauId } })
              }
              onBasculerVerrou={(seance) => basculerVerrou.mutate(seance)}
              onChangerSalle={(seance) => {
                setSalleChoisie(seance.salleId);
                setSeancePourSalle(seance);
              }}
              onChangerEnseignant={(seance) => {
                setEnseignantChoisi(seance.enseignantId);
                setSeancePourEnseignant(seance);
              }}
            />
          )}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <PanneauVersions
            versions={versions}
            chargement={requeteVersions.isLoading}
            onRestaurer={(version) => setVersionARestaurer(version)}
          />
          <LegendeMatieres lignes={legende} />
        </div>
      </div>

      {/* Dialog changement de salle */}
      <Dialog
        ouvert={seancePourSalle !== null}
        titre="Changer de salle"
        onFermer={() => setSeancePourSalle(null)}
      >
        {seancePourSalle !== null && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <p
              style={{
                margin: 0,
                fontSize: 'var(--text-sm)',
                color: 'var(--text-muted)',
              }}
            >
              Séance {seancePourSalle.matiereLibelle} —{' '}
              {seancePourSalle.groupeLibelle}
            </p>
            <Select
              label="Nouvelle salle"
              id="nouvelle-salle"
              value={salleChoisie ?? ''}
              onChange={(evenement) =>
                setSalleChoisie(
                  evenement.target.value === ''
                    ? null
                    : Number(evenement.target.value),
                )
              }
            >
              <option value="">Choisir une salle…</option>
              {salles.map((salle) => (
                <option key={salle.id} value={salle.id}>
                  {salle.nom} ({salle.capacite} places)
                </option>
              ))}
            </Select>
            <div
              style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}
            >
              <Button
                variant="secondary"
                onClick={() => setSeancePourSalle(null)}
              >
                Annuler
              </Button>
              <Button
                disabled={salleChoisie === null || modifierSeance.isPending}
                onClick={() => {
                  if (salleChoisie === null) return;
                  modifierSeance.mutate({
                    seanceId: seancePourSalle.id,
                    corps: { salleId: salleChoisie },
                  });
                  setSeancePourSalle(null);
                }}
              >
                Enregistrer
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Dialog changement d'enseignant */}
      <Dialog
        ouvert={seancePourEnseignant !== null}
        titre="Changer d’enseignant"
        onFermer={() => setSeancePourEnseignant(null)}
      >
        {seancePourEnseignant !== null && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <p
              style={{
                margin: 0,
                fontSize: 'var(--text-sm)',
                color: 'var(--text-muted)',
              }}
            >
              Séance {seancePourEnseignant.matiereLibelle} —{' '}
              {seancePourEnseignant.groupeLibelle}
            </p>
            <Select
              label="Nouvel enseignant"
              id="nouvel-enseignant"
              value={enseignantChoisi ?? ''}
              onChange={(evenement) =>
                setEnseignantChoisi(
                  evenement.target.value === ''
                    ? null
                    : Number(evenement.target.value),
                )
              }
            >
              <option value="">Choisir un enseignant…</option>
              {enseignants.map((enseignant) => (
                <option key={enseignant.id} value={enseignant.id}>
                  {enseignant.nomComplet}
                </option>
              ))}
            </Select>
            <div
              style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}
            >
              <Button
                variant="secondary"
                onClick={() => setSeancePourEnseignant(null)}
              >
                Annuler
              </Button>
              <Button
                disabled={enseignantChoisi === null || modifierSeance.isPending}
                onClick={() => {
                  if (enseignantChoisi === null) return;
                  modifierSeance.mutate({
                    seanceId: seancePourEnseignant.id,
                    corps: { enseignantId: enseignantChoisi },
                  });
                  setSeancePourEnseignant(null);
                }}
              >
                Enregistrer
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Dialog confirmation restauration (I-08) */}
      <Dialog
        ouvert={versionARestaurer !== null}
        titre="Restaurer une version"
        onFermer={() => setVersionARestaurer(null)}
      >
        {versionARestaurer !== null && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <p
              style={{
                margin: 0,
                fontSize: 'var(--text-sm)',
                color: 'var(--text-body)',
              }}
            >
              Restaurer la version «&nbsp;{versionARestaurer.libelle}&nbsp;» ?
              Elle deviendra la version active de l’emploi du temps pour tout
              l’établissement.
            </p>
            <div
              style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}
            >
              <Button
                variant="secondary"
                onClick={() => setVersionARestaurer(null)}
              >
                Annuler
              </Button>
              <Button
                loading={restaurerVersion.isPending}
                onClick={() => restaurerVersion.mutate(versionARestaurer.id)}
              >
                {restaurerVersion.isPending ? 'Restauration…' : 'Restaurer'}
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
}
