'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import { apiFetch, apiFetchMultipart } from '@/lib/api';
import type {
  AnalyseImportEleves,
  ChampExportEleve,
  CreationEleve,
  Eleve,
  FormatExportEleves,
  LigneImportEleve,
  ModificationEleve,
  PageReponse,
  StatutEleve,
  ValidationImportEleves,
} from '@/lib/types-scolarite';
import type { Groupe, Niveau } from '@/lib/types';
import { Alert, Badge, Button, Card, EmptyState, Input, Pagination, Select } from '@/components/ds';
import { Dialog } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { ChargementPage, Spinner } from '@/components/ui/spinner';
import {
  CODE_MASSAR,
  LIBELLE_LIGNE_IMPORT,
  LIBELLE_STATUT,
  TABLE,
  TD,
  TD_RIGHT,
  TEXTE_ARABE,
  TH,
  TH_RIGHT,
  TON_LIGNE_IMPORT,
  TON_STATUT,
  formatDateIso,
} from './tableau';

const TAILLE_PAGE = 50;

/** Colonnes exportables, dans l'ordre proposé à l'utilisateur. */
const CHAMPS_EXPORT: { cle: ChampExportEleve; libelle: string }[] = [
  { cle: 'codeMassar', libelle: 'Code Massar' },
  { cle: 'nom', libelle: 'Nom' },
  { cle: 'prenom', libelle: 'Prénom' },
  { cle: 'nomAr', libelle: 'Nom (arabe)' },
  { cle: 'prenomAr', libelle: 'Prénom (arabe)' },
  { cle: 'dateNaissance', libelle: 'Date de naissance' },
  { cle: 'lieuNaissance', libelle: 'Lieu de naissance' },
  { cle: 'sexe', libelle: 'Sexe' },
  { cle: 'statut', libelle: 'Statut' },
  { cle: 'groupeLibelle', libelle: 'Classe' },
  { cle: 'niveauLibelle', libelle: 'Niveau' },
  { cle: 'tuteurNom', libelle: 'Tuteur' },
  { cle: 'tuteurTelephone', libelle: 'Téléphone du tuteur' },
];

const CHAMPS_PAR_DEFAUT: ChampExportEleve[] = [
  'codeMassar', 'nom', 'prenom', 'dateNaissance', 'sexe', 'groupeLibelle', 'statut',
];

const STATUTS: StatutEleve[] = ['INSCRIT', 'REDOUBLANT', 'PARTI'];

const FORMULAIRE_VIDE: CreationEleve = {
  codeMassar: '', nom: '', prenom: '', nomAr: '', prenomAr: '',
  dateNaissance: '', lieuNaissance: '', sexe: '', statut: 'INSCRIT',
  tuteurNom: '', tuteurTelephone: '', groupeId: null,
};

const ETIQUETTE: CSSProperties = {
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
};

/** Aplatit les classes et leurs sous-groupes pour les sélecteurs. */
function aplatirGroupes(groupes: Groupe[]): { id: number; libelle: string }[] {
  const sortie: { id: number; libelle: string }[] = [];
  for (const groupe of groupes) {
    sortie.push({ id: groupe.id, libelle: groupe.libelle });
    for (const sousGroupe of groupe.sousGroupes ?? []) {
      sortie.push({ id: sousGroupe.id, libelle: `— ${sousGroupe.libelle}` });
    }
  }
  return sortie;
}

/** Ne transmet que les champs réellement saisis : le backend distingue absent et vide. */
function corpsCreation(formulaire: CreationEleve, groupeId: number | null): CreationEleve {
  const corps: CreationEleve = {
    codeMassar: formulaire.codeMassar.trim(),
    nom: formulaire.nom.trim(),
    prenom: formulaire.prenom.trim(),
    statut: formulaire.statut ?? 'INSCRIT',
  };
  const facultatifs: (keyof CreationEleve)[] = [
    'nomAr', 'prenomAr', 'dateNaissance', 'lieuNaissance', 'sexe', 'tuteurNom', 'tuteurTelephone',
  ];
  for (const champ of facultatifs) {
    const valeur = formulaire[champ];
    if (typeof valeur === 'string' && valeur.trim().length > 0) {
      Object.assign(corps, { [champ]: valeur.trim() });
    }
  }
  if (groupeId !== null) corps.groupeId = groupeId;
  return corps;
}

export default function PageEleves() {
  const clientQuery = useQueryClient();

  const [recherche, setRecherche] = useState('');
  const [groupeFiltre, setGroupeFiltre] = useState('');
  const [niveauFiltre, setNiveauFiltre] = useState('');
  const [statutFiltre, setStatutFiltre] = useState('');
  const [page, setPage] = useState(0);

  const [dialogFiche, setDialogFiche] = useState(false);
  const [eleveEdite, setEleveEdite] = useState<Eleve | null>(null);
  const [formulaire, setFormulaire] = useState<CreationEleve>(FORMULAIRE_VIDE);
  const [groupeFiche, setGroupeFiche] = useState('');
  const [erreurFiche, setErreurFiche] = useState<string | null>(null);

  const [eleveASupprimer, setEleveASupprimer] = useState<Eleve | null>(null);

  const [dialogImport, setDialogImport] = useState(false);
  const [analyse, setAnalyse] = useState<AnalyseImportEleves | null>(null);
  const [lignesRetenues, setLignesRetenues] = useState<Set<number>>(new Set());
  const [majExistants, setMajExistants] = useState(true);
  const [erreurImport, setErreurImport] = useState<string | null>(null);
  const [bilanImport, setBilanImport] = useState<ValidationImportEleves | null>(null);
  const champFichier = useRef<HTMLInputElement>(null);

  const [dialogExport, setDialogExport] = useState(false);
  const [champsExport, setChampsExport] = useState<ChampExportEleve[]>(CHAMPS_PAR_DEFAUT);
  const [formatExport, setFormatExport] = useState<FormatExportEleves>('csv');
  const [exportEnCours, setExportEnCours] = useState(false);
  const [erreurExport, setErreurExport] = useState<string | null>(null);

  const requeteNiveaux = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });
  const requeteGroupes = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<Groupe[]>('/ecole/groupes'),
  });

  const filtres = useMemo(() => {
    const p = new URLSearchParams({ page: String(page), taille: String(TAILLE_PAGE) });
    if (recherche.trim().length > 0) p.set('recherche', recherche.trim());
    if (groupeFiltre !== '') p.set('groupeId', groupeFiltre);
    if (niveauFiltre !== '') p.set('niveauId', niveauFiltre);
    if (statutFiltre !== '') p.set('statut', statutFiltre);
    return p;
  }, [page, recherche, groupeFiltre, niveauFiltre, statutFiltre]);

  const requeteEleves = useQuery({
    queryKey: ['ecole', 'eleves', filtres.toString()],
    queryFn: () => apiFetch<PageReponse<Eleve>>(`/ecole/eleves?${filtres.toString()}`),
  });

  const groupesPlats = aplatirGroupes(requeteGroupes.data ?? []);

  function invalider() {
    void clientQuery.invalidateQueries({ queryKey: ['ecole', 'eleves'] });
    void clientQuery.invalidateQueries({ queryKey: ['ecole', 'groupes'] });
  }

  function reinitialiserFiltre(action: () => void) {
    setPage(0);
    action();
  }

  // ---------------- Fiche élève ----------------

  function ouvrirCreation() {
    setEleveEdite(null);
    setFormulaire(FORMULAIRE_VIDE);
    setGroupeFiche('');
    setErreurFiche(null);
    setDialogFiche(true);
  }

  function ouvrirEdition(eleve: Eleve) {
    setEleveEdite(eleve);
    setFormulaire({
      codeMassar: eleve.codeMassar,
      nom: eleve.nom,
      prenom: eleve.prenom,
      nomAr: eleve.nomAr ?? '',
      prenomAr: eleve.prenomAr ?? '',
      dateNaissance: eleve.dateNaissance ?? '',
      lieuNaissance: eleve.lieuNaissance ?? '',
      sexe: eleve.sexe ?? '',
      statut: eleve.statut,
      tuteurNom: eleve.tuteurNom ?? '',
      tuteurTelephone: eleve.tuteurTelephone ?? '',
    });
    setGroupeFiche(eleve.groupeId === null ? '' : String(eleve.groupeId));
    setErreurFiche(null);
    setDialogFiche(true);
  }

  const enregistrement = useMutation({
    mutationFn: () => {
      const groupeId = groupeFiche === '' ? null : Number(groupeFiche);
      if (eleveEdite === null) {
        return apiFetch<Eleve>('/ecole/eleves', {
          method: 'POST',
          body: JSON.stringify(corpsCreation(formulaire, groupeId)),
        });
      }
      // PATCH : chaîne vide = effacer le champ facultatif, groupeId absent = inchangé.
      const corps: ModificationEleve = {
        codeMassar: formulaire.codeMassar.trim(),
        nom: formulaire.nom.trim(),
        prenom: formulaire.prenom.trim(),
        nomAr: formulaire.nomAr ?? '',
        prenomAr: formulaire.prenomAr ?? '',
        lieuNaissance: formulaire.lieuNaissance ?? '',
        sexe: formulaire.sexe ?? '',
        statut: formulaire.statut,
        tuteurNom: formulaire.tuteurNom ?? '',
        tuteurTelephone: formulaire.tuteurTelephone ?? '',
      };
      if ((formulaire.dateNaissance ?? '').length > 0) {
        corps.dateNaissance = formulaire.dateNaissance;
      }
      if (groupeId === null) corps.retirerDuGroupe = true;
      else corps.groupeId = groupeId;
      return apiFetch<Eleve>(`/ecole/eleves/${eleveEdite.id}`, {
        method: 'PATCH',
        body: JSON.stringify(corps),
      });
    },
    onSuccess: () => {
      setDialogFiche(false);
      invalider();
    },
    onError: (erreur: Error) => setErreurFiche(erreur.message),
  });

  const suppression = useMutation({
    mutationFn: (eleve: Eleve) =>
      apiFetch<unknown>(`/ecole/eleves/${eleve.id}`, { method: 'DELETE' }),
    onSuccess: () => {
      setEleveASupprimer(null);
      invalider();
    },
    onError: (erreur: Error) => setErreurFiche(erreur.message),
  });

  // ---------------- Import Massar ----------------

  const analyseImport = useMutation({
    mutationFn: (fichier: File) => {
      const formulaireEnvoi = new FormData();
      formulaireEnvoi.append('fichier', fichier);
      return apiFetchMultipart<AnalyseImportEleves>(
        '/ecole/eleves/import/analyser',
        formulaireEnvoi,
      );
    },
    onSuccess: (rapport) => {
      setAnalyse(rapport);
      // Par défaut : tout ce qui est exploitable, jamais les lignes en erreur.
      setLignesRetenues(
        new Set(rapport.lignes.filter((l) => l.statut !== 'ERREUR').map((l) => l.numero)),
      );
      setErreurImport(null);
    },
    onError: (erreur: Error) => {
      setAnalyse(null);
      setErreurImport(erreur.message);
    },
  });

  const validationImport = useMutation({
    mutationFn: () => {
      const retenues = (analyse?.lignes ?? []).filter((l) => lignesRetenues.has(l.numero));
      return apiFetch<ValidationImportEleves>('/ecole/eleves/import/valider', {
        method: 'POST',
        body: JSON.stringify({ lignes: retenues, mettreAJourExistants: majExistants }),
      });
    },
    onSuccess: (bilan) => {
      setBilanImport(bilan);
      setAnalyse(null);
      setLignesRetenues(new Set());
      invalider();
    },
    onError: (erreur: Error) => setErreurImport(erreur.message),
  });

  function ouvrirImport() {
    setAnalyse(null);
    setLignesRetenues(new Set());
    setErreurImport(null);
    setBilanImport(null);
    setMajExistants(true);
    analyseImport.reset();
    validationImport.reset();
    setDialogImport(true);
  }

  function basculerLigne(numero: number) {
    setLignesRetenues((precedent) => {
      const suivant = new Set(precedent);
      if (suivant.has(numero)) suivant.delete(numero);
      else suivant.add(numero);
      return suivant;
    });
  }

  // ---------------- Export ----------------

  function basculerChamp(champ: ChampExportEleve) {
    setChampsExport((precedent) =>
      precedent.includes(champ)
        ? precedent.filter((c) => c !== champ)
        : // Conserve l'ordre canonique : c'est l'ordre des colonnes du fichier.
          CHAMPS_EXPORT.map((c) => c.cle).filter((c) => precedent.includes(c) || c === champ),
    );
  }

  async function telecharger() {
    setErreurExport(null);
    setExportEnCours(true);
    try {
      const parametres = new URLSearchParams({ format: formatExport });
      if (champsExport.length > 0) parametres.set('champs', champsExport.join(','));
      if (recherche.trim().length > 0) parametres.set('recherche', recherche.trim());
      if (groupeFiltre !== '') parametres.set('groupeId', groupeFiltre);
      if (niveauFiltre !== '') parametres.set('niveauId', niveauFiltre);
      if (statutFiltre !== '') parametres.set('statut', statutFiltre);

      const reponse = await fetch(`/api/exports/eleves?${parametres.toString()}`);
      if (!reponse.ok) {
        let message = "L'export a échoué.";
        try {
          const corps: unknown = await reponse.json();
          if (corps !== null && typeof corps === 'object' && 'message' in corps) {
            const brut = (corps as { message?: unknown }).message;
            if (typeof brut === 'string' && brut.length > 0) message = brut;
          }
        } catch {
          // Réponse non JSON : message par défaut.
        }
        setErreurExport(message);
        return;
      }
      const enTete = reponse.headers.get('content-disposition') ?? '';
      const trouve = /filename="?([^";]+)"?/.exec(enTete);
      const blob = await reponse.blob();
      const url = URL.createObjectURL(blob);
      const lien = document.createElement('a');
      lien.href = url;
      lien.download = trouve?.[1] ?? `eleves.${formatExport}`;
      document.body.appendChild(lien);
      lien.click();
      document.body.removeChild(lien);
      URL.revokeObjectURL(url);
      setDialogExport(false);
    } catch {
      setErreurExport('Impossible de contacter le serveur.');
    } finally {
      setExportEnCours(false);
    }
  }

  // ---------------- Rendu ----------------

  if (requeteEleves.isLoading && requeteEleves.data === undefined) return <ChargementPage />;

  const erreurListe = requeteEleves.error;
  const donnees = requeteEleves.data;
  const eleves = donnees?.contenu ?? [];
  const retenuesExploitables = (analyse?.lignes ?? []).filter((l) => lignesRetenues.has(l.numero));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <p style={{ margin: 0, fontSize: 'var(--text-base)', color: 'var(--text-muted)' }}>
        Élèves inscrits, codes Massar, import des listes du ministère et exports.
      </p>

      {erreurListe !== null && (
        <Alert tone="danger" title="Chargement impossible">
          {erreurListe.message}
        </Alert>
      )}

      {bilanImport !== null && (
        <Alert tone="success" title="Import terminé" onClose={() => setBilanImport(null)}>
          {bilanImport.crees} élève(s) créé(s), {bilanImport.misAJour} mis à jour,{' '}
          {bilanImport.ignores} ignoré(s).
        </Alert>
      )}

      <Card padded={false}>
        {/* Barre de recherche, filtres et actions */}
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'flex-end',
            gap: '12px',
            padding: '18px 24px',
            borderBottom: '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ minWidth: '220px', flex: '1 1 220px' }}>
            <Input
              label="Rechercher"
              placeholder="Nom, prénom ou code Massar"
              value={recherche}
              onChange={(e) => reinitialiserFiltre(() => setRecherche(e.target.value))}
            />
          </div>
          <div style={{ minWidth: '150px' }}>
            <Select
              label="Classe"
              value={groupeFiltre}
              onChange={(e) => reinitialiserFiltre(() => setGroupeFiltre(e.target.value))}
            >
              <option value="">Toutes</option>
              {groupesPlats.map((g) => (
                <option key={g.id} value={g.id}>{g.libelle}</option>
              ))}
            </Select>
          </div>
          <div style={{ minWidth: '140px' }}>
            <Select
              label="Niveau"
              value={niveauFiltre}
              onChange={(e) => reinitialiserFiltre(() => setNiveauFiltre(e.target.value))}
            >
              <option value="">Tous</option>
              {(requeteNiveaux.data ?? []).map((n) => (
                <option key={n.id} value={n.id}>{n.libelle}</option>
              ))}
            </Select>
          </div>
          <div style={{ minWidth: '140px' }}>
            <Select
              label="Statut"
              value={statutFiltre}
              onChange={(e) => reinitialiserFiltre(() => setStatutFiltre(e.target.value))}
            >
              <option value="">Tous</option>
              {STATUTS.map((s) => (
                <option key={s} value={s}>{LIBELLE_STATUT[s]}</option>
              ))}
            </Select>
          </div>
          <div style={{ display: 'flex', gap: '8px', marginLeft: 'auto' }}>
            <Button variant="secondary" onClick={ouvrirImport}>Importer Massar</Button>
            <Button variant="secondary" onClick={() => setDialogExport(true)}>Exporter</Button>
            <Button onClick={ouvrirCreation}>Nouvel élève</Button>
          </div>
        </div>

        {eleves.length === 0 ? (
          <div style={{ padding: '8px 24px 24px' }}>
            <EmptyState
              title="Aucun élève"
              description={
                recherche !== '' || groupeFiltre !== '' || niveauFiltre !== '' || statutFiltre !== ''
                  ? 'Aucun élève ne correspond aux filtres.'
                  : 'Importez une liste Massar ou créez un élève pour commencer.'
              }
              action={<Button onClick={ouvrirImport}>Importer une liste Massar</Button>}
            />
          </div>
        ) : (
          <>
            <div style={{ overflowX: 'auto' }}>
              <table style={TABLE}>
                <thead>
                  <tr>
                    <th style={TH}>Code Massar</th>
                    <th style={TH}>Nom et prénom</th>
                    <th style={TH}>Arabe</th>
                    <th style={TH}>Naissance</th>
                    <th style={TH}>Classe</th>
                    <th style={TH}>Statut</th>
                    <th style={TH_RIGHT}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {eleves.map((eleve) => (
                    <tr key={eleve.id}>
                      <td style={TD}><span style={CODE_MASSAR}>{eleve.codeMassar}</span></td>
                      <td style={{ ...TD, color: 'var(--text-strong)' }}>
                        {eleve.nom} {eleve.prenom}
                      </td>
                      <td style={{ ...TD, ...TEXTE_ARABE }}>
                        {eleve.nomAr !== null || eleve.prenomAr !== null
                          ? `${eleve.nomAr ?? ''} ${eleve.prenomAr ?? ''}`.trim()
                          : '—'}
                      </td>
                      <td style={TD}>{formatDateIso(eleve.dateNaissance)}</td>
                      <td style={TD}>{eleve.groupeLibelle ?? '—'}</td>
                      <td style={TD}>
                        <Badge tone={TON_STATUT[eleve.statut]}>
                          {LIBELLE_STATUT[eleve.statut]}
                        </Badge>
                      </td>
                      <td style={TD_RIGHT}>
                        <Button variant="ghost" size="sm" onClick={() => ouvrirEdition(eleve)}>
                          Modifier
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => setEleveASupprimer(eleve)}>
                          Supprimer
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div style={{ padding: '14px 24px' }}>
              <Pagination
                page={donnees?.page ?? 0}
                size={donnees?.taille ?? TAILLE_PAGE}
                totalElements={donnees?.total ?? 0}
                onPageChange={setPage}
              />
            </div>
          </>
        )}
      </Card>

      {/* ---------------- Fiche élève ---------------- */}
      <Dialog
        ouvert={dialogFiche}
        titre={eleveEdite === null ? 'Nouvel élève' : 'Modifier la fiche'}
        taille="lg"
        onFermer={() => setDialogFiche(false)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {erreurFiche !== null && <Alert tone="danger">{erreurFiche}</Alert>}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
            <Input
              label="Code Massar"
              required
              value={formulaire.codeMassar}
              onChange={(e) => setFormulaire({ ...formulaire, codeMassar: e.target.value })}
            />
            <Select
              label="Statut"
              value={formulaire.statut ?? 'INSCRIT'}
              onChange={(e) =>
                setFormulaire({ ...formulaire, statut: e.target.value as StatutEleve })
              }
            >
              {STATUTS.map((s) => (
                <option key={s} value={s}>{LIBELLE_STATUT[s]}</option>
              ))}
            </Select>
            <Input
              label="Nom"
              required
              value={formulaire.nom}
              onChange={(e) => setFormulaire({ ...formulaire, nom: e.target.value })}
            />
            <Input
              label="Prénom"
              required
              value={formulaire.prenom}
              onChange={(e) => setFormulaire({ ...formulaire, prenom: e.target.value })}
            />
            <Input
              label="Nom en arabe"
              value={formulaire.nomAr ?? ''}
              style={TEXTE_ARABE}
              onChange={(e) => setFormulaire({ ...formulaire, nomAr: e.target.value })}
            />
            <Input
              label="Prénom en arabe"
              value={formulaire.prenomAr ?? ''}
              style={TEXTE_ARABE}
              onChange={(e) => setFormulaire({ ...formulaire, prenomAr: e.target.value })}
            />
            <Input
              label="Date de naissance"
              type="date"
              value={formulaire.dateNaissance ?? ''}
              onChange={(e) => setFormulaire({ ...formulaire, dateNaissance: e.target.value })}
            />
            <Input
              label="Lieu de naissance"
              value={formulaire.lieuNaissance ?? ''}
              onChange={(e) => setFormulaire({ ...formulaire, lieuNaissance: e.target.value })}
            />
            <Select
              label="Sexe"
              value={formulaire.sexe ?? ''}
              onChange={(e) =>
                setFormulaire({ ...formulaire, sexe: e.target.value as CreationEleve['sexe'] })
              }
            >
              <option value="">Non renseigné</option>
              <option value="M">Masculin</option>
              <option value="F">Féminin</option>
            </Select>
            <Select
              label="Classe"
              value={groupeFiche}
              onChange={(e) => setGroupeFiche(e.target.value)}
            >
              <option value="">Sans classe</option>
              {groupesPlats.map((g) => (
                <option key={g.id} value={g.id}>{g.libelle}</option>
              ))}
            </Select>
            <Input
              label="Tuteur"
              value={formulaire.tuteurNom ?? ''}
              onChange={(e) => setFormulaire({ ...formulaire, tuteurNom: e.target.value })}
            />
            <Input
              label="Téléphone du tuteur"
              value={formulaire.tuteurTelephone ?? ''}
              onChange={(e) => setFormulaire({ ...formulaire, tuteurTelephone: e.target.value })}
            />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
            <Button variant="secondary" onClick={() => setDialogFiche(false)}>Annuler</Button>
            <Button
              onClick={() => enregistrement.mutate()}
              loading={enregistrement.isPending}
              disabled={
                formulaire.codeMassar.trim() === '' ||
                formulaire.nom.trim() === '' ||
                formulaire.prenom.trim() === ''
              }
            >
              Enregistrer
            </Button>
          </div>
        </div>
      </Dialog>

      {/* ---------------- Suppression ---------------- */}
      <Dialog
        ouvert={eleveASupprimer !== null}
        titre="Supprimer cet élève ?"
        onFermer={() => setEleveASupprimer(null)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <p style={{ margin: 0, fontSize: 'var(--text-base)', color: 'var(--text-body)' }}>
            {eleveASupprimer?.nom} {eleveASupprimer?.prenom} ({eleveASupprimer?.codeMassar}) sera
            supprimé, ainsi que <strong>toutes ses absences</strong>. Cette action est irréversible.
          </p>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
            <Button variant="secondary" onClick={() => setEleveASupprimer(null)}>Annuler</Button>
            <Button
              variant="danger"
              loading={suppression.isPending}
              onClick={() => {
                if (eleveASupprimer !== null) suppression.mutate(eleveASupprimer);
              }}
            >
              Supprimer
            </Button>
          </div>
        </div>
      </Dialog>

      {/* ---------------- Import Massar ---------------- */}
      <Dialog
        ouvert={dialogImport}
        titre="Importer une liste Massar"
        taille="xl"
        onFermer={() => setDialogImport(false)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {erreurImport !== null && <Alert tone="danger">{erreurImport}</Alert>}

          {analyse === null ? (
            <>
              <Alert tone="info" title="Rien n'est enregistré à cette étape">
                Le fichier est seulement analysé. Vous verrez ensuite chaque ligne et son état, et
                rien ne sera écrit avant votre validation.
              </Alert>
              <div>
                <Label htmlFor="fichierMassar">Fichier CSV exporté de Massar</Label>
                <input
                  id="fichierMassar"
                  ref={champFichier}
                  type="file"
                  accept=".csv,text/csv"
                  style={{ fontSize: 'var(--text-base)', color: 'var(--text-body)' }}
                  onChange={(e) => {
                    const fichier = e.target.files?.[0];
                    if (fichier !== undefined) analyseImport.mutate(fichier);
                  }}
                />
                <p style={{ margin: '8px 0 0', fontSize: 'var(--text-xs)', color: 'var(--text-subtle)' }}>
                  Séparateur « ; » ou « , » détecté automatiquement. Les en-têtes usuels de Massar
                  sont reconnus, accents et arabe compris.
                </p>
              </div>
              {analyseImport.isPending && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Spinner /> <span style={{ fontSize: 'var(--text-base)' }}>Analyse du fichier…</span>
                </div>
              )}
            </>
          ) : (
            <>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px' }}>
                {[
                  { libelle: 'Lignes', valeur: analyse.resume.total, tone: 'neutral' as const },
                  { libelle: 'Nouveaux', valeur: analyse.resume.nouveaux, tone: 'success' as const },
                  { libelle: 'Déjà inscrits', valeur: analyse.resume.existants, tone: 'warning' as const },
                  { libelle: 'Erreurs', valeur: analyse.resume.erreurs, tone: 'danger' as const },
                ].map((tuile) => (
                  <div
                    key={tuile.libelle}
                    style={{
                      flex: '1 1 120px',
                      borderRadius: 'var(--radius-sm)',
                      background: 'var(--surface-sunken)',
                      padding: '12px',
                    }}
                  >
                    <div style={ETIQUETTE}>{tuile.libelle}</div>
                    <div
                      style={{
                        marginTop: '4px',
                        fontSize: 'var(--text-xl)',
                        fontWeight: 'var(--weight-bold)',
                        color: 'var(--text-strong)',
                        fontVariantNumeric: 'tabular-nums',
                      }}
                    >
                      {tuile.valeur}
                    </div>
                  </div>
                ))}
              </div>

              {analyse.colonnesIgnorees.length > 0 && (
                <Alert tone="warning" title="Colonnes non reconnues">
                  {analyse.colonnesIgnorees.join(', ')} — ces colonnes seront ignorées.
                </Alert>
              )}

              <div style={{ maxHeight: '340px', overflowY: 'auto' }}>
                <table style={TABLE}>
                  <thead>
                    <tr>
                      <th style={TH}>Retenir</th>
                      <th style={TH}>Ligne</th>
                      <th style={TH}>État</th>
                      <th style={TH}>Code Massar</th>
                      <th style={TH}>Nom et prénom</th>
                      <th style={TH}>Classe</th>
                      <th style={TH}>Observations</th>
                    </tr>
                  </thead>
                  <tbody>
                    {analyse.lignes.map((ligne: LigneImportEleve) => (
                      <tr key={ligne.numero}>
                        <td style={TD}>
                          <input
                            type="checkbox"
                            checked={lignesRetenues.has(ligne.numero)}
                            disabled={ligne.statut === 'ERREUR'}
                            aria-label={`Retenir la ligne ${ligne.numero}`}
                            onChange={() => basculerLigne(ligne.numero)}
                          />
                        </td>
                        <td style={TD}>{ligne.numero}</td>
                        <td style={TD}>
                          <Badge tone={TON_LIGNE_IMPORT[ligne.statut]}>
                            {LIBELLE_LIGNE_IMPORT[ligne.statut]}
                          </Badge>
                        </td>
                        <td style={TD}>
                          <span style={CODE_MASSAR}>{ligne.donnees.codeMassar ?? '—'}</span>
                        </td>
                        <td style={{ ...TD, color: 'var(--text-strong)' }}>
                          {`${ligne.donnees.nom ?? ''} ${ligne.donnees.prenom ?? ''}`.trim() || '—'}
                        </td>
                        <td style={TD}>{ligne.donnees.groupeLibelle ?? '—'}</td>
                        <td style={{ ...TD, whiteSpace: 'normal', color: 'var(--text-muted)' }}>
                          {ligne.messages.length > 0 ? ligne.messages.join(' · ') : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  fontSize: 'var(--text-base)',
                  color: 'var(--text-body)',
                }}
              >
                <input
                  type="checkbox"
                  checked={majExistants}
                  onChange={(e) => setMajExistants(e.target.checked)}
                />
                Mettre à jour les élèves déjà inscrits (rapprochés par code Massar)
              </label>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '12px',
                }}
              >
                <span style={{ fontSize: 'var(--text-sm)', color: 'var(--text-muted)' }}>
                  {retenuesExploitables.length} ligne(s) retenue(s) sur {analyse.resume.total}
                </span>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <Button variant="secondary" onClick={() => setAnalyse(null)}>
                    Choisir un autre fichier
                  </Button>
                  <Button
                    onClick={() => validationImport.mutate()}
                    loading={validationImport.isPending}
                    disabled={retenuesExploitables.length === 0}
                  >
                    Enregistrer {retenuesExploitables.length} élève(s)
                  </Button>
                </div>
              </div>
            </>
          )}
        </div>
      </Dialog>

      {/* ---------------- Export ---------------- */}
      <Dialog ouvert={dialogExport} titre="Exporter la liste" onFermer={() => setDialogExport(false)}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {erreurExport !== null && <Alert tone="danger">{erreurExport}</Alert>}
          <div>
            <div style={{ ...ETIQUETTE, marginBottom: '8px' }}>Colonnes à exporter</div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px' }}>
              {CHAMPS_EXPORT.map((champ) => (
                <label
                  key={champ.cle}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: 'var(--text-sm)',
                    color: 'var(--text-body)',
                  }}
                >
                  <input
                    type="checkbox"
                    checked={champsExport.includes(champ.cle)}
                    onChange={() => basculerChamp(champ.cle)}
                  />
                  {champ.libelle}
                </label>
              ))}
            </div>
          </div>
          <Select
            label="Format"
            value={formatExport}
            onChange={(e) => setFormatExport(e.target.value as FormatExportEleves)}
          >
            <option value="csv">CSV (ouvrable dans Excel)</option>
            <option value="xlsx">Excel (.xlsx)</option>
          </Select>
          <p style={{ margin: 0, fontSize: 'var(--text-xs)', color: 'var(--text-subtle)' }}>
            Les filtres actifs de la liste sont appliqués à l'export.
          </p>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
            <Button variant="secondary" onClick={() => setDialogExport(false)}>Annuler</Button>
            <Button
              onClick={() => void telecharger()}
              loading={exportEnCours}
              disabled={champsExport.length === 0}
            >
              Télécharger
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
