'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  apiFetchDetaille,
  ErreurApi,
  extraireConflits,
} from '@/lib/api-planning';
import { apiFetch } from '@/lib/api';
import { formatDate } from '@/lib/format';
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
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Dialog } from '@/components/ui/dialog';
import { EmptyState } from '@/components/ui/empty-state';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
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
      <div className="space-y-4">
        <h1 className="text-xl font-semibold text-gray-900">Planning</h1>
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Grille horaire indisponible. Configurez d’abord la grille horaire de
          l’établissement dans le Référentiel.
        </p>
      </div>
    );
  }

  const grille = requeteGrille.data;
  const versions = requeteVersions.data ?? [];
  const salles = requeteSalles.data ?? [];
  const enseignants = requeteEnseignants.data ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Planning</h1>
        <p className="mt-1 text-sm text-gray-500">
          Consultation et ajustement manuel des emplois du temps. Glissez une
          séance vers un autre créneau pour la déplacer.
        </p>
      </div>

      <Card>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <Label htmlFor="vue">Vue</Label>
            <Select
              id="vue"
              value={vue}
              onChange={(evenement) => {
                setVue(evenement.target.value as VuePlanning);
                setEntiteId(null);
              }}
            >
              <option value="GROUPE">Groupe</option>
              <option value="ENSEIGNANT">Enseignant</option>
              <option value="SALLE">Salle</option>
            </Select>
          </div>
          <div>
            <Label htmlFor="entite">
              {vue === 'GROUPE'
                ? 'Groupe'
                : vue === 'ENSEIGNANT'
                  ? 'Enseignant'
                  : 'Salle'}
            </Label>
            <Select
              id="entite"
              value={entiteEffective ?? ''}
              onChange={(evenement) =>
                setEntiteId(Number(evenement.target.value))
              }
            >
              {options.length === 0 && <option value="">Aucun élément</option>}
              {options.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.libelle}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="version">Version</Label>
            <Select
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
          </div>
          <div>
            <Label htmlFor="semaine">Semaine</Label>
            <Select
              id="semaine"
              value={semaine}
              onChange={(evenement) =>
                setSemaine(evenement.target.value as Semaine)
              }
            >
              <option value="TOUTES">Toutes</option>
              <option value="A">Semaine A</option>
              <option value="B">Semaine B</option>
            </Select>
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-4">
          <span className="text-sm text-slate-500">Export PDF :</span>
          <Button
            variante="secondary"
            taille="sm"
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
            Groupe affiché
          </Button>
          <Button
            variante="primary"
            taille="sm"
            onClick={() => telechargerPdf()}
            disabled={exportEnCours}
          >
            {exportEnCours ? 'Génération…' : 'Tous les groupes'}
          </Button>
          {erreurExport && (
            <span className="text-sm text-red-700">{erreurExport}</span>
          )}
        </div>
      </Card>

      {conflits !== null && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-red-800">
                Déplacement refusé : conflits détectés
              </p>
              <ul className="mt-1 space-y-0.5 text-sm text-red-700">
                {conflits.map((conflit, index) => (
                  <li key={`${conflit.regle}-${index}`}>
                    <span className="font-mono text-xs font-semibold">
                      {conflit.regle}
                    </span>
                    {conflit.regle.length > 0 ? ' : ' : ''}
                    {conflit.message}
                  </li>
                ))}
              </ul>
            </div>
            <button
              type="button"
              onClick={() => setConflits(null)}
              aria-label="Fermer"
              className="rounded-md p-1 text-red-400 hover:bg-red-100 hover:text-red-600"
            >
              <svg
                className="h-4 w-4"
                viewBox="0 0 20 20"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
              >
                <path d="M5 5l10 10M15 5L5 15" strokeLinecap="round" />
              </svg>
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_18rem]">
        <div className="min-w-0">
          {requetePlanning.isLoading ? (
            <ChargementPage />
          ) : requetePlanning.error !== null ? (
            <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {requetePlanning.error.message}
            </p>
          ) : entiteEffective === null ? (
            <EmptyState message="Aucune entité à afficher. Créez d’abord vos groupes, enseignants et salles dans le Référentiel." />
          ) : requetePlanning.data === undefined ? (
            <EmptyState message="Aucun planning disponible." />
          ) : (
            <div className="relative">
              {modifierSeance.isPending && (
                <div className="absolute right-3 top-3 z-50">
                  <Spinner />
                </div>
              )}
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
            </div>
          )}
        </div>

        <Card titre="Versions du planning" className="self-start">
          {versions.length === 0 ? (
            <EmptyState message="Aucune version. Lancez une génération pour créer un planning." />
          ) : (
            <ul className="space-y-3">
              {versions.map((version) => (
                <li
                  key={version.id}
                  className="rounded-lg border border-gray-200 p-3"
                >
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-sm font-medium text-gray-900">
                      {version.libelle}
                    </p>
                    {version.active && (
                      <span className="inline-flex items-center rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800">
                        Active
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-xs text-gray-500">
                    Créée le {formatDate(version.creeeLe)} ·{' '}
                    {version.nbSeances} séances
                  </p>
                  {!version.active && (
                    <Button
                      variante="secondary"
                      taille="sm"
                      className="mt-2 w-full"
                      onClick={() => setVersionARestaurer(version)}
                    >
                      Restaurer cette version
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      {/* Dialog changement de salle */}
      <Dialog
        ouvert={seancePourSalle !== null}
        titre="Changer de salle"
        onFermer={() => setSeancePourSalle(null)}
      >
        {seancePourSalle !== null && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Séance {seancePourSalle.matiereLibelle} —{' '}
              {seancePourSalle.groupeLibelle}
            </p>
            <div>
              <Label htmlFor="nouvelle-salle">Nouvelle salle</Label>
              <Select
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
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variante="secondary"
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
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Séance {seancePourEnseignant.matiereLibelle} —{' '}
              {seancePourEnseignant.groupeLibelle}
            </p>
            <div>
              <Label htmlFor="nouvel-enseignant">Nouvel enseignant</Label>
              <Select
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
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variante="secondary"
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
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Restaurer la version «&nbsp;{versionARestaurer.libelle}&nbsp;» ?
              Elle deviendra la version active de l’emploi du temps pour tout
              l’établissement.
            </p>
            <div className="flex justify-end gap-2">
              <Button
                variante="secondary"
                onClick={() => setVersionARestaurer(null)}
              >
                Annuler
              </Button>
              <Button
                disabled={restaurerVersion.isPending}
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
