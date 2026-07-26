'use client';

import {
  useEffect,
  useState,
  type CSSProperties,
  type FormEvent,
  type ReactNode,
} from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';
import { formatUnites } from '@/lib/format';
import type {
  Barrette,
  Grille,
  Groupe,
  Jour,
  Matiere,
  Niveau,
  Salle,
} from '@/lib/types';
import {
  Alert,
  Badge,
  Button,
  Card,
  EmptyState,
  Input,
  Tabs,
  type TabItem,
} from '@/components/ds';
import { Dialog } from '@/components/ui/dialog';
/*
 * Les formulaires des dialogues gardent les champs de `components/ui` :
 * l’Input du design system consomme la prop `required` (elle ne sert qu’à
 * l’astérisque) sans la transmettre au DOM, ce qui ferait perdre la
 * validation HTML native de chaque dialogue de création / édition.
 */
import { Input as Champ } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select as Selecteur } from '@/components/ui/select';
import { ChargementPage } from '@/components/ui/spinner';

const TOUS_LES_JOURS: Jour[] = [
  'LUNDI',
  'MARDI',
  'MERCREDI',
  'JEUDI',
  'VENDREDI',
  'SAMEDI',
  'DIMANCHE',
];

const LIBELLES_JOURS: Record<Jour, string> = {
  LUNDI: 'Lundi',
  MARDI: 'Mardi',
  MERCREDI: 'Mercredi',
  JEUDI: 'Jeudi',
  VENDREDI: 'Vendredi',
  SAMEDI: 'Samedi',
  DIMANCHE: 'Dimanche',
};

/* ------------------------------------------------------------------ */
/* Styles de la maquette (TH / TD, en-têtes de carte)                 */
/* ------------------------------------------------------------------ */

const TABLEAU: CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
  fontSize: 'var(--text-sm)',
};

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

const TD_FORT: CSSProperties = {
  ...TD,
  fontWeight: 'var(--weight-medium)',
  color: 'var(--text-strong)',
};

/** Ligne de sous-groupe : indentée et atténuée (maquette). */
const TD_SOUS: CSSProperties = {
  ...TD,
  paddingLeft: '36px',
  color: 'var(--text-muted)',
};

const TITRE_CARTE: CSSProperties = {
  margin: 0,
  fontSize: 'var(--type-card-title-size)',
  fontWeight: 'var(--type-card-title-weight)',
  color: 'var(--text-strong)',
};

/** Micro-capitales d’intitulé de bloc dans un corps de carte. */
const SURTITRE: CSSProperties = {
  margin: '0 0 10px',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--weight-semibold)',
  letterSpacing: 'var(--tracking-caps)',
  textTransform: 'uppercase',
  color: 'var(--text-muted)',
};

const CORPS_CARTE: CSSProperties = { padding: '20px 24px 24px' };

/* ------------------------------------------------------------------ */
/* Petits blocs partagés                                             */
/* ------------------------------------------------------------------ */

function EnteteCarte({
  titre,
  sousTitre,
  actions,
}: {
  titre: string;
  sousTitre?: string;
  actions?: ReactNode;
}) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: sousTitre === undefined ? 'center' : 'flex-start',
        justifyContent: 'space-between',
        gap: '16px',
        padding: '18px 24px',
        borderBottom: '1px solid var(--border-subtle)',
      }}
    >
      <div>
        <h3 style={TITRE_CARTE}>{titre}</h3>
        {sousTitre !== undefined && (
          <p
            style={{
              margin: '4px 0 0',
              fontSize: 'var(--text-sm)',
              color: 'var(--text-muted)',
              whiteSpace: 'normal',
            }}
          >
            {sousTitre}
          </p>
        )}
      </div>
      {actions !== undefined && (
        <div style={{ display: 'flex', gap: '8px', flex: 'none' }}>{actions}</div>
      )}
    </div>
  );
}

function MessageErreur({ message }: { message: string }) {
  return <Alert tone="danger">{message}</Alert>;
}

/** Bandeau d’erreur posé dans un corps de carte non capitonné. */
function BandeauErreur({ message }: { message: string }) {
  return (
    <div style={{ padding: '16px 24px 0' }}>
      <MessageErreur message={message} />
    </div>
  );
}

/**
 * Liste de pastilles neutres. La liste vide rend un tiret nu, pas une
 * pastille : « — » n'est pas une valeur et ne doit pas se lire comme telle.
 */
function Pastilles({ elements }: { elements: string[] }) {
  if (elements.length === 0) {
    return <span style={{ color: 'var(--text-subtle)' }}>—</span>;
  }
  return (
    <span style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
      {elements.map((element, index) => (
        <Badge key={`${element}-${index}`} tone="neutral" size="sm">
          {element}
        </Badge>
      ))}
    </span>
  );
}

function ActionsLigne({ children }: { children: ReactNode }) {
  return <span style={{ display: 'inline-flex', gap: '6px' }}>{children}</span>;
}

function csvVersListe(texte: string): string[] {
  return texte
    .split(',')
    .map((element) => element.trim())
    .filter((element) => element.length > 0);
}

/* ------------------------------------------------------------------ */
/* Page                                                              */
/* ------------------------------------------------------------------ */

type Onglet =
  | 'grille'
  | 'niveaux'
  | 'groupes'
  | 'matieres'
  | 'salles'
  | 'barrettes';

export default function PageReferentiel() {
  const [onglet, setOnglet] = useState<Onglet>('grille');

  /*
   * Compteurs des onglets (maquette). Les clés de requête sont celles des
   * onglets : le cache TanStack Query est partagé, aucune requête en double.
   */
  const niveaux = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });
  const groupes = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<Groupe[]>('/ecole/groupes'),
  });
  const matieres = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });
  const salles = useQuery({
    queryKey: ['ecole', 'salles'],
    queryFn: () => apiFetch<Salle[]>('/ecole/salles'),
  });
  const barrettes = useQuery({
    queryKey: ['ecole', 'barrettes'],
    queryFn: () => apiFetch<Barrette[]>('/ecole/barrettes'),
  });

  const nombreGroupes = groupes.data?.reduce(
    (total, groupe) => total + 1 + (groupe.sousGroupes ?? []).length,
    0,
  );

  const onglets: TabItem[] = [
    { id: 'grille', label: 'Grille horaire' },
    { id: 'niveaux', label: 'Niveaux', count: niveaux.data?.length ?? null },
    { id: 'groupes', label: 'Groupes', count: nombreGroupes ?? null },
    { id: 'matieres', label: 'Matières', count: matieres.data?.length ?? null },
    { id: 'salles', label: 'Salles', count: salles.data?.length ?? null },
    {
      id: 'barrettes',
      label: 'Barrettes',
      count: barrettes.data?.length ?? null,
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <p
        style={{
          margin: 0,
          fontSize: 'var(--text-base)',
          color: 'var(--text-muted)',
          maxWidth: '78ch',
        }}
      >
        Grille horaire, niveaux, groupes, matières, salles et barrettes. Toutes
        les durées sont exprimées en unités de 30 minutes.
      </p>

      <Tabs
        tabs={onglets}
        value={onglet}
        onChange={(id) => setOnglet(id as Onglet)}
      />

      {onglet === 'grille' && <OngletGrille />}
      {onglet === 'niveaux' && <OngletNiveaux />}
      {onglet === 'groupes' && <OngletGroupes />}
      {onglet === 'matieres' && <OngletMatieres />}
      {onglet === 'salles' && <OngletSalles />}
      {onglet === 'barrettes' && <OngletBarrettes />}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Grille horaire                                                     */
/* ------------------------------------------------------------------ */

function copierGrille(grille: Grille): Grille {
  return {
    ...grille,
    joursActifs: [...grille.joursActifs],
    plagesBloquees: grille.plagesBloquees.map((plage) => ({ ...plage })),
  };
}

function OngletGrille() {
  const queryClient = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ['ecole', 'grille'],
    queryFn: () => apiFetch<Grille>('/ecole/grille'),
  });

  const [formulaire, setFormulaire] = useState<Grille | null>(null);
  const [dialogForcer, setDialogForcer] = useState(false);
  const [messageSucces, setMessageSucces] = useState<string | null>(null);
  const [messageValidation, setMessageValidation] = useState<string | null>(
    null,
  );

  useEffect(() => {
    if (data !== undefined) {
      setFormulaire(copierGrille(data));
    }
  }, [data]);

  const enregistrement = useMutation({
    mutationFn: (forcer: boolean) =>
      apiFetch<unknown>(`/ecole/grille?forcer=${forcer ? 'true' : 'false'}`, {
        method: 'PUT',
        body: JSON.stringify(formulaire),
      }),
    onSuccess: (_donnees, forcer) => {
      setDialogForcer(false);
      setMessageSucces(
        forcer
          ? 'Grille enregistrée. Les versions et séances existantes ont été supprimées et les créneaux régénérés.'
          : 'Grille horaire enregistrée.',
      );
      void queryClient.invalidateQueries({ queryKey: ['ecole'] });
    },
    onError: (_erreur, forcer) => {
      if (!forcer) {
        setDialogForcer(true);
      }
    },
  });

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;
  if (formulaire === null) {
    return (
      <Card padded={false}>
        <EmptyState
          variant="gated"
          title="Aucune grille horaire"
          description="Aucune grille horaire n’est disponible pour cet établissement."
        />
      </Card>
    );
  }

  function basculerJour(jour: Jour) {
    if (formulaire === null) return;
    const actifs = formulaire.joursActifs.includes(jour)
      ? formulaire.joursActifs.filter((element) => element !== jour)
      : TOUS_LES_JOURS.filter(
          (element) =>
            formulaire.joursActifs.includes(element) || element === jour,
        );
    setFormulaire({ ...formulaire, joursActifs: actifs });
  }

  function modifierPlage(
    index: number,
    champ: 'type' | 'indexDebut' | 'dureeUnites',
    valeur: string,
  ) {
    if (formulaire === null) return;
    const plages = formulaire.plagesBloquees.map((plage, i) => {
      if (i !== index) return plage;
      if (champ === 'type') return { ...plage, type: valeur };
      return { ...plage, [champ]: Number(valeur) };
    });
    setFormulaire({ ...formulaire, plagesBloquees: plages });
  }

  function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (formulaire === null) return;
    setMessageSucces(null);
    /*
     * Contrôle équivalent aux attributs `required` / `min` des champs :
     * l’Input du design system ne transmet pas `required` au DOM.
     */
    const invalide =
      formulaire.heureDebut.trim().length === 0 ||
      !Number.isFinite(formulaire.dureeUniteMinutes) ||
      formulaire.dureeUniteMinutes < 5 ||
      formulaire.unitesParJour < 1 ||
      formulaire.amplitudeMaxUnites < 1 ||
      formulaire.plagesBloquees.some(
        (plage) =>
          plage.type.trim().length === 0 ||
          plage.indexDebut < 0 ||
          plage.dureeUnites < 1,
      );
    if (invalide) {
      setMessageValidation(
        'Vérifiez la structure de la semaine et les plages bloquées : chaque champ doit être renseigné avec une valeur valide.',
      );
      return;
    }
    setMessageValidation(null);
    enregistrement.mutate(false);
  }

  return (
    <>
      <form
        onSubmit={soumettre}
        style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}
      >
        <Card padded={false}>
          <EnteteCarte titre="Structure de la semaine" />
          <div
            style={{
              ...CORPS_CARTE,
              display: 'flex',
              flexDirection: 'column',
              gap: '20px',
            }}
          >
            <div>
              <p style={SURTITRE}>Jours actifs</p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {TOUS_LES_JOURS.map((jour) => {
                  const actif = formulaire.joursActifs.includes(jour);
                  return (
                    <button
                      key={jour}
                      type="button"
                      aria-pressed={actif}
                      onClick={() => basculerJour(jour)}
                      style={{
                        borderRadius: 'var(--radius-pill)',
                        padding: '6px 14px',
                        fontSize: 'var(--text-sm)',
                        fontWeight: 'var(--weight-medium)',
                        cursor: 'pointer',
                        fontFamily: 'inherit',
                        transition: 'all var(--duration-fast) var(--ease-standard)',
                        ...(actif
                          ? {
                              border: '1px solid var(--color-primary)',
                              background: 'var(--surface-selected)',
                              color: 'var(--teal-700)',
                            }
                          : {
                              border: '1px solid var(--border-subtle)',
                              background: 'var(--surface-card)',
                              color: 'var(--text-muted)',
                            }),
                      }}
                    >
                      {LIBELLES_JOURS[jour]}
                    </button>
                  );
                })}
              </div>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(4,1fr)',
                gap: '16px',
              }}
            >
              <Input
                id="grille-heure-debut"
                label="Heure de début"
                type="time"
                value={formulaire.heureDebut}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, heureDebut: e.target.value })
                }
              />
              <Input
                id="grille-duree-unite"
                label="Durée d’une unité"
                type="number"
                min={5}
                hint="minutes"
                value={formulaire.dureeUniteMinutes}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    dureeUniteMinutes: Number(e.target.value),
                  })
                }
              />
              <Input
                id="grille-unites-jour"
                label="Unités par jour"
                type="number"
                min={1}
                hint={`soit ${formatUnites(formulaire.unitesParJour)} par jour`}
                value={formulaire.unitesParJour}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    unitesParJour: Number(e.target.value),
                  })
                }
              />
              <Input
                id="grille-amplitude-max"
                label="Amplitude max"
                type="number"
                min={1}
                hint={`soit ${formatUnites(
                  formulaire.amplitudeMaxUnites,
                )} maximum`}
                value={formulaire.amplitudeMaxUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    amplitudeMaxUnites: Number(e.target.value),
                  })
                }
              />
            </div>
          </div>
        </Card>

        <Card padded={false}>
          <EnteteCarte
            titre="Plages bloquées"
            actions={
              <Button
                variant="secondary"
                size="sm"
                onClick={() =>
                  setFormulaire({
                    ...formulaire,
                    plagesBloquees: [
                      ...formulaire.plagesBloquees,
                      { type: 'DEJEUNER', indexDebut: 8, dureeUnites: 2 },
                    ],
                  })
                }
              >
                Ajouter une plage
              </Button>
            }
          />
          <div style={CORPS_CARTE}>
            {formulaire.plagesBloquees.length === 0 ? (
              <EmptyState
                variant="gated"
                title="Aucune plage bloquée"
                description="Ajoutez les plages non enseignées (déjeuner, pause…) : elles seront exclues de tous les créneaux."
                style={{ padding: '12px 0 4px' }}
              />
            ) : (
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '12px',
                }}
              >
                {formulaire.plagesBloquees.map((plage, index) => (
                  <div
                    key={index}
                    style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      alignItems: 'flex-end',
                      gap: '16px',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: 'var(--radius-sm)',
                      background: 'var(--surface-sunken)',
                      padding: '16px',
                    }}
                  >
                    <div style={{ width: '180px' }}>
                      <Input
                        id={`plage-type-${index}`}
                        label="Type"
                        value={plage.type}
                        placeholder="DEJEUNER"
                        onChange={(e) =>
                          modifierPlage(index, 'type', e.target.value)
                        }
                      />
                    </div>
                    <div style={{ width: '140px' }}>
                      <Input
                        id={`plage-debut-${index}`}
                        label="Index de début"
                        type="number"
                        min={0}
                        value={plage.indexDebut}
                        onChange={(e) =>
                          modifierPlage(index, 'indexDebut', e.target.value)
                        }
                      />
                    </div>
                    <div style={{ width: '140px' }}>
                      <Input
                        id={`plage-duree-${index}`}
                        label="Durée"
                        type="number"
                        min={1}
                        hint={formatUnites(plage.dureeUnites)}
                        value={plage.dureeUnites}
                        onChange={(e) =>
                          modifierPlage(index, 'dureeUnites', e.target.value)
                        }
                      />
                    </div>
                    <div style={{ marginLeft: 'auto' }}>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() =>
                          setFormulaire({
                            ...formulaire,
                            plagesBloquees: formulaire.plagesBloquees.filter(
                              (_plage, i) => i !== index,
                            ),
                          })
                        }
                      >
                        Retirer
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Card>

        {messageValidation !== null && (
          <Alert
            tone="warning"
            onClose={() => setMessageValidation(null)}
          >
            {messageValidation}
          </Alert>
        )}

        {messageSucces !== null && (
          <Alert tone="success" onClose={() => setMessageSucces(null)}>
            {messageSucces}
          </Alert>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
          <Button
            variant="secondary"
            size="md"
            disabled={data === undefined || enregistrement.isPending}
            onClick={() => {
              if (data !== undefined) setFormulaire(copierGrille(data));
              setMessageSucces(null);
              setMessageValidation(null);
            }}
          >
            Annuler
          </Button>
          <Button
            type="submit"
            variant="primary"
            size="md"
            loading={enregistrement.isPending}
          >
            {enregistrement.isPending
              ? 'Enregistrement…'
              : 'Enregistrer la grille'}
          </Button>
        </div>
      </form>

      <Dialog
        ouvert={dialogForcer}
        titre="Enregistrement impossible"
        onFermer={() => setDialogForcer(false)}
      >
        <div className="space-y-4">
          {enregistrement.error !== null && (
            <MessageErreur message={enregistrement.error.message} />
          )}
          <Alert tone="warning" title="Action destructive">
            Forcer l’enregistrement supprimera définitivement toutes les
            versions de planning et toutes les séances existantes, puis
            régénérera les créneaux à partir de la nouvelle grille.
          </Alert>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setDialogForcer(false)}>
              Annuler
            </Button>
            <Button
              variant="danger"
              loading={enregistrement.isPending}
              onClick={() => enregistrement.mutate(true)}
            >
              {enregistrement.isPending
                ? 'Enregistrement…'
                : 'Forcer l’enregistrement'}
            </Button>
          </div>
        </div>
      </Dialog>
    </>
  );
}

/* ------------------------------------------------------------------ */
/* Niveaux                                                            */
/* ------------------------------------------------------------------ */

const FORM_NIVEAU_INITIAL = {
  libelle: '',
  cycle: '',
  ordre: '1',
  chargeMaxUnitesJour: '14',
};

function OngletNiveaux() {
  const queryClient = useQueryClient();
  const { data: niveaux, isLoading, error } = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Niveau | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_NIVEAU_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'niveaux'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () => {
      const corps = JSON.stringify({
        libelle: formulaire.libelle,
        cycle: formulaire.cycle,
        ordre: Number(formulaire.ordre),
        chargeMaxUnitesJour: Number(formulaire.chargeMaxUnitesJour),
      });
      return enEdition === null
        ? apiFetch<Niveau>('/ecole/niveaux', { method: 'POST', body: corps })
        : apiFetch<Niveau>(`/ecole/niveaux/${enEdition.id}`, {
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
      apiFetch<unknown>(`/ecole/niveaux/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_NIVEAU_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(niveau: Niveau) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(niveau);
    setFormulaire({
      libelle: niveau.libelle,
      cycle: niveau.cycle,
      ordre: String(niveau.ordre),
      chargeMaxUnitesJour: String(niveau.chargeMaxUnitesJour),
    });
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = [...(niveaux ?? [])].sort((a, b) => a.ordre - b.ordre);

  return (
    <Card padded={false} style={{ overflow: 'hidden' }}>
      <EnteteCarte
        titre="Niveaux"
        actions={
          <Button variant="primary" size="sm" onClick={ouvrirCreation}>
            Nouveau niveau
          </Button>
        }
      />

      {suppression.error !== null && (
        <BandeauErreur message={suppression.error.message} />
      )}

      {liste.length === 0 ? (
        <EmptyState
          title="Aucun niveau"
          description="Créez d’abord vos niveaux (ex. 1AC, 2AC…) : groupes, maquettes et matières s’y rattachent."
          action={
            <Button variant="primary" size="sm" onClick={ouvrirCreation}>
              Nouveau niveau
            </Button>
          }
        />
      ) : (
        <table style={TABLEAU}>
          <thead>
            <tr>
              <th style={TH_DROITE}>Ordre</th>
              <th style={TH}>Libellé</th>
              <th style={TH}>Cycle</th>
              <th style={TH}>Charge max / jour</th>
              <th style={TH_DROITE}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {liste.map((niveau) => (
              <tr key={niveau.id}>
                <td style={TD_DROITE}>{niveau.ordre}</td>
                <td style={TD_FORT}>{niveau.libelle}</td>
                <td style={TD}>{niveau.cycle}</td>
                <td style={TD}>
                  {niveau.chargeMaxUnitesJour} unités (
                  {formatUnites(niveau.chargeMaxUnitesJour)})
                </td>
                <td style={TD_DROITE}>
                  <ActionsLigne>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => ouvrirEdition(niveau)}
                    >
                      Modifier
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={suppression.isPending}
                      onClick={() => {
                        if (
                          window.confirm(
                            `Supprimer le niveau « ${niveau.libelle} » ?`,
                          )
                        ) {
                          suppression.mutate(niveau.id);
                        }
                      }}
                    >
                      Supprimer
                    </Button>
                  </ActionsLigne>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre={enEdition === null ? 'Nouveau niveau' : 'Modifier le niveau'}
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sauvegarde.mutate();
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="libelleNiveau">Libellé</Label>
              <Champ
                id="libelleNiveau"
                value={formulaire.libelle}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, libelle: e.target.value })
                }
                placeholder="1AC"
                required
              />
            </div>
            <div>
              <Label htmlFor="cycleNiveau">Cycle</Label>
              <Champ
                id="cycleNiveau"
                value={formulaire.cycle}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, cycle: e.target.value })
                }
                placeholder="COLLEGE"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="ordreNiveau">Ordre</Label>
              <Champ
                id="ordreNiveau"
                type="number"
                min="1"
                value={formulaire.ordre}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, ordre: e.target.value })
                }
                required
              />
            </div>
            <div>
              <Label htmlFor="chargeNiveau">Charge max / jour (unités)</Label>
              <Champ
                id="chargeNiveau"
                type="number"
                min="1"
                value={formulaire.chargeMaxUnitesJour}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    chargeMaxUnitesJour: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-ink-muted">
                Soit {formatUnites(Number(formulaire.chargeMaxUnitesJour) || 0)}{' '}
                par jour.
              </p>
            </div>
          </div>

          {sauvegarde.error !== null && (
            <MessageErreur message={sauvegarde.error.message} />
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
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Groupes                                                            */
/* ------------------------------------------------------------------ */

const FORM_GROUPE_INITIAL = { niveauId: '', libelle: '', effectif: '30' };

function OngletGroupes() {
  const queryClient = useQueryClient();
  const { data: groupes, isLoading, error } = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<Groupe[]>('/ecole/groupes'),
  });
  const { data: niveaux } = useQuery({
    queryKey: ['ecole', 'niveaux'],
    queryFn: () => apiFetch<Niveau[]>('/ecole/niveaux'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Groupe | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_GROUPE_INITIAL);
  const [dialogDedoubler, setDialogDedoubler] = useState<Groupe | null>(null);
  const [nombreSousGroupes, setNombreSousGroupes] = useState('2');

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'groupes'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () =>
      enEdition === null
        ? apiFetch<Groupe>('/ecole/groupes', {
            method: 'POST',
            body: JSON.stringify({
              niveauId: Number(formulaire.niveauId),
              libelle: formulaire.libelle,
              effectif: Number(formulaire.effectif),
            }),
          })
        : apiFetch<Groupe>(`/ecole/groupes/${enEdition.id}`, {
            method: 'PATCH',
            body: JSON.stringify({
              libelle: formulaire.libelle,
              effectif: Number(formulaire.effectif),
            }),
          }),
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
    },
  });

  const suppression = useMutation({
    mutationFn: (id: number) =>
      apiFetch<unknown>(`/ecole/groupes/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  const dedoublement = useMutation({
    mutationFn: (groupeId: number) =>
      apiFetch<unknown>(`/ecole/groupes/${groupeId}/sous-groupes`, {
        method: 'POST',
        body: JSON.stringify({ nombre: Number(nombreSousGroupes) }),
      }),
    onSuccess: () => {
      invalider();
      setDialogDedoubler(null);
    },
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_GROUPE_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(groupe: Groupe) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(groupe);
    setFormulaire({
      niveauId: String(groupe.niveauId),
      libelle: groupe.libelle,
      effectif: String(groupe.effectif),
    });
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = groupes ?? [];

  function ligneGroupe(groupe: Groupe, sousGroupe: boolean) {
    return (
      <tr key={groupe.id}>
        <td style={sousGroupe ? TD_SOUS : TD_FORT}>
          {sousGroupe ? `↳ ${groupe.libelle}` : groupe.libelle}
        </td>
        <td style={TD}>{groupe.niveauLibelle}</td>
        <td style={TD_DROITE}>{groupe.effectif}</td>
        <td style={TD}>
          <Badge tone={sousGroupe ? 'neutral' : 'info'} size="sm">
            {groupe.type}
          </Badge>
        </td>
        <td style={TD_DROITE}>
          <ActionsLigne>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => ouvrirEdition(groupe)}
            >
              Modifier
            </Button>
            {!sousGroupe && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                  dedoublement.reset();
                  setNombreSousGroupes('2');
                  setDialogDedoubler(groupe);
                }}
              >
                Dédoubler
              </Button>
            )}
            <Button
              variant="ghost"
              size="sm"
              disabled={suppression.isPending}
              onClick={() => {
                if (
                  window.confirm(`Supprimer le groupe « ${groupe.libelle} » ?`)
                ) {
                  suppression.mutate(groupe.id);
                }
              }}
            >
              Supprimer
            </Button>
          </ActionsLigne>
        </td>
      </tr>
    );
  }

  return (
    <Card padded={false} style={{ overflow: 'hidden' }}>
      <EnteteCarte
        titre="Groupes (classes et sous-groupes)"
        actions={
          <Button variant="primary" size="sm" onClick={ouvrirCreation}>
            Nouveau groupe
          </Button>
        }
      />

      {suppression.error !== null && (
        <BandeauErreur message={suppression.error.message} />
      )}

      {liste.length === 0 ? (
        <EmptyState
          title="Aucun groupe"
          description="Créez vos classes (ex. 1AC-A), puis dédoublez-les en sous-groupes si nécessaire."
          action={
            <Button variant="primary" size="sm" onClick={ouvrirCreation}>
              Nouveau groupe
            </Button>
          }
        />
      ) : (
        <table style={TABLEAU}>
          <thead>
            <tr>
              <th style={TH}>Libellé</th>
              <th style={TH}>Niveau</th>
              <th style={TH_DROITE}>Effectif</th>
              <th style={TH}>Type</th>
              <th style={TH_DROITE}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {liste.map((groupe) => [
              ligneGroupe(groupe, false),
              ...(groupe.sousGroupes ?? []).map((sousGroupe) =>
                ligneGroupe(sousGroupe, true),
              ),
            ])}
          </tbody>
        </table>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre={enEdition === null ? 'Nouveau groupe' : 'Modifier le groupe'}
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sauvegarde.mutate();
          }}
          className="space-y-4"
        >
          {enEdition === null && (
            <div>
              <Label htmlFor="niveauGroupe">Niveau</Label>
              <Selecteur
                id="niveauGroupe"
                value={formulaire.niveauId}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, niveauId: e.target.value })
                }
                required
              >
                <option value="">— Choisir un niveau —</option>
                {(niveaux ?? []).map((niveau) => (
                  <option key={niveau.id} value={niveau.id}>
                    {niveau.libelle}
                  </option>
                ))}
              </Selecteur>
            </div>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="libelleGroupe">Libellé</Label>
              <Champ
                id="libelleGroupe"
                value={formulaire.libelle}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, libelle: e.target.value })
                }
                placeholder="1AC-A"
                required
              />
            </div>
            <div>
              <Label htmlFor="effectifGroupe">Effectif</Label>
              <Champ
                id="effectifGroupe"
                type="number"
                min="1"
                value={formulaire.effectif}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, effectif: e.target.value })
                }
                required
              />
            </div>
          </div>

          {sauvegarde.error !== null && (
            <MessageErreur message={sauvegarde.error.message} />
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

      <Dialog
        ouvert={dialogDedoubler !== null}
        titre={`Dédoubler « ${dialogDedoubler?.libelle ?? ''} »`}
        onFermer={() => setDialogDedoubler(null)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (dialogDedoubler !== null) {
              dedoublement.mutate(dialogDedoubler.id);
            }
          }}
          className="space-y-4"
        >
          <p className="text-sm text-ink-body">
            Des sous-groupes (G1), (G2)… seront créés et l’effectif sera réparti
            équitablement.
          </p>
          <div>
            <Label htmlFor="nombreSousGroupes">Nombre de sous-groupes</Label>
            <Champ
              id="nombreSousGroupes"
              type="number"
              min="2"
              max="6"
              value={nombreSousGroupes}
              onChange={(e) => setNombreSousGroupes(e.target.value)}
              required
            />
          </div>

          {dedoublement.error !== null && (
            <MessageErreur message={dedoublement.error.message} />
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button
              variant="secondary"
              onClick={() => setDialogDedoubler(null)}
            >
              Annuler
            </Button>
            <Button type="submit" loading={dedoublement.isPending}>
              {dedoublement.isPending ? 'Création…' : 'Dédoubler'}
            </Button>
          </div>
        </form>
      </Dialog>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Matières                                                           */
/* ------------------------------------------------------------------ */

const FORM_MATIERE_INITIAL = {
  libelle: '',
  code: '',
  coefficient: '1',
  poidsCognitif: '3',
  couleur: '#6366f1',
  typeSalleRequis: '',
  equipementsRequis: '',
  dureeMinUnites: '1',
  dureeMaxUnites: '4',
  eviterAvantDejeuner: false,
  eviterFinJournee: false,
};

function OngletMatieres() {
  const queryClient = useQueryClient();
  const { data: matieres, isLoading, error } = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Matiere | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_MATIERE_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'matieres'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () => {
      const corps = JSON.stringify({
        libelle: formulaire.libelle,
        code: formulaire.code,
        coefficient: Number(formulaire.coefficient),
        poidsCognitif: Number(formulaire.poidsCognitif),
        couleur: formulaire.couleur,
        typeSalleRequis:
          formulaire.typeSalleRequis.trim().length > 0
            ? formulaire.typeSalleRequis.trim()
            : null,
        equipementsRequis: csvVersListe(formulaire.equipementsRequis),
        dureeMinUnites: Number(formulaire.dureeMinUnites),
        dureeMaxUnites: Number(formulaire.dureeMaxUnites),
        eviterAvantDejeuner: formulaire.eviterAvantDejeuner,
        eviterFinJournee: formulaire.eviterFinJournee,
      });
      return enEdition === null
        ? apiFetch<Matiere>('/ecole/matieres', { method: 'POST', body: corps })
        : apiFetch<Matiere>(`/ecole/matieres/${enEdition.id}`, {
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
      apiFetch<unknown>(`/ecole/matieres/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_MATIERE_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(matiere: Matiere) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(matiere);
    setFormulaire({
      libelle: matiere.libelle,
      code: matiere.code,
      coefficient: String(matiere.coefficient),
      poidsCognitif: String(matiere.poidsCognitif),
      couleur: matiere.couleur,
      typeSalleRequis: matiere.typeSalleRequis ?? '',
      equipementsRequis: (matiere.equipementsRequis ?? []).join(', '),
      dureeMinUnites: String(matiere.dureeMinUnites),
      dureeMaxUnites: String(matiere.dureeMaxUnites),
      eviterAvantDejeuner: matiere.eviterAvantDejeuner,
      eviterFinJournee: matiere.eviterFinJournee,
    });
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = matieres ?? [];

  return (
    <Card padded={false} style={{ overflow: 'hidden' }}>
      <EnteteCarte
        titre="Matières"
        actions={
          <Button variant="primary" size="sm" onClick={ouvrirCreation}>
            Nouvelle matière
          </Button>
        }
      />

      {suppression.error !== null && (
        <BandeauErreur message={suppression.error.message} />
      )}

      {liste.length === 0 ? (
        <EmptyState
          title="Aucune matière"
          description="Déclarez les matières enseignées : coefficient, poids cognitif, durées et contraintes de placement."
          action={
            <Button variant="primary" size="sm" onClick={ouvrirCreation}>
              Nouvelle matière
            </Button>
          }
        />
      ) : (
        <table style={TABLEAU}>
          <thead>
            <tr>
              <th style={TH}>Matière</th>
              <th style={TH}>Code</th>
              <th style={TH_DROITE}>Coef.</th>
              <th style={TH_DROITE}>Poids cognitif</th>
              <th style={TH}>Salle requise</th>
              <th style={TH}>Équipements</th>
              <th style={TH}>Durées</th>
              <th style={TH}>Contraintes</th>
              <th style={TH_DROITE}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {liste.map((matiere) => (
              <tr key={matiere.id}>
                <td style={TD}>
                  <span
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '10px',
                      fontWeight: 'var(--weight-medium)',
                      color: 'var(--text-strong)',
                    }}
                  >
                    <span
                      aria-hidden="true"
                      style={{
                        display: 'inline-block',
                        width: '10px',
                        height: '10px',
                        flex: 'none',
                        borderRadius: '3px',
                        background: matiere.couleur,
                      }}
                    />
                    {matiere.libelle}
                  </span>
                </td>
                <td style={TD_MONO}>{matiere.code}</td>
                <td style={TD_DROITE}>{matiere.coefficient}</td>
                <td style={TD_DROITE}>{matiere.poidsCognitif}</td>
                <td style={TD}>{matiere.typeSalleRequis ?? '—'}</td>
                <td style={TD}>
                  <Pastilles elements={matiere.equipementsRequis ?? []} />
                </td>
                <td style={TD}>
                  {formatUnites(matiere.dureeMinUnites)} –{' '}
                  {formatUnites(matiere.dureeMaxUnites)}
                </td>
                <td style={TD}>
                  <Pastilles
                    elements={[
                      ...(matiere.eviterAvantDejeuner
                        ? ['Éviter avant déjeuner']
                        : []),
                      ...(matiere.eviterFinJournee
                        ? ['Éviter fin de journée']
                        : []),
                    ]}
                  />
                </td>
                <td style={TD_DROITE}>
                  <ActionsLigne>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => ouvrirEdition(matiere)}
                    >
                      Modifier
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={suppression.isPending}
                      onClick={() => {
                        if (
                          window.confirm(
                            `Supprimer la matière « ${matiere.libelle} » ?`,
                          )
                        ) {
                          suppression.mutate(matiere.id);
                        }
                      }}
                    >
                      Supprimer
                    </Button>
                  </ActionsLigne>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre={enEdition === null ? 'Nouvelle matière' : 'Modifier la matière'}
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sauvegarde.mutate();
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="libelleMatiere">Libellé</Label>
              <Champ
                id="libelleMatiere"
                value={formulaire.libelle}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, libelle: e.target.value })
                }
                placeholder="Mathématiques"
                required
              />
            </div>
            <div>
              <Label htmlFor="codeMatiere">Code</Label>
              <Champ
                id="codeMatiere"
                value={formulaire.code}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, code: e.target.value })
                }
                placeholder="MATH"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <Label htmlFor="coefficientMatiere">Coefficient</Label>
              <Champ
                id="coefficientMatiere"
                type="number"
                min="0"
                step="0.5"
                value={formulaire.coefficient}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, coefficient: e.target.value })
                }
                required
              />
            </div>
            <div>
              <Label htmlFor="poidsMatiere">Poids cognitif</Label>
              <Champ
                id="poidsMatiere"
                type="number"
                min="1"
                max="5"
                value={formulaire.poidsCognitif}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    poidsCognitif: e.target.value,
                  })
                }
                required
              />
            </div>
            <div>
              <Label htmlFor="couleurMatiere">Couleur</Label>
              {/* La couleur d’une matière est une donnée, pas un token. */}
              <input
                id="couleurMatiere"
                type="color"
                value={formulaire.couleur}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, couleur: e.target.value })
                }
                className="h-[var(--control-height-md)] w-full cursor-pointer rounded-sm border border-line-default bg-surface-card p-1"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="typeSalleMatiere">Type de salle requis</Label>
              <Champ
                id="typeSalleMatiere"
                value={formulaire.typeSalleRequis}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    typeSalleRequis: e.target.value,
                  })
                }
                placeholder="LABO (vide = aucune exigence)"
              />
            </div>
            <div>
              <Label htmlFor="equipementsMatiere">
                Équipements requis (séparés par des virgules)
              </Label>
              <Champ
                id="equipementsMatiere"
                value={formulaire.equipementsRequis}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    equipementsRequis: e.target.value,
                  })
                }
                placeholder="PROJECTEUR, PAILLASSE"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="dureeMinMatiere">Durée min (unités)</Label>
              <Champ
                id="dureeMinMatiere"
                type="number"
                min="1"
                value={formulaire.dureeMinUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    dureeMinUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-ink-muted">
                {formatUnites(Number(formulaire.dureeMinUnites) || 0)}
              </p>
            </div>
            <div>
              <Label htmlFor="dureeMaxMatiere">Durée max (unités)</Label>
              <Champ
                id="dureeMaxMatiere"
                type="number"
                min="1"
                value={formulaire.dureeMaxUnites}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    dureeMaxUnites: e.target.value,
                  })
                }
                required
              />
              <p className="mt-1 text-xs text-ink-muted">
                {formatUnites(Number(formulaire.dureeMaxUnites) || 0)}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap gap-6">
            <label className="flex items-center gap-2 text-sm text-ink-body">
              <input
                type="checkbox"
                checked={formulaire.eviterAvantDejeuner}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    eviterAvantDejeuner: e.target.checked,
                  })
                }
                className="h-4 w-4 rounded-xs border-line-default"
                style={{ accentColor: 'var(--color-primary)' }}
              />
              Éviter avant le déjeuner
            </label>
            <label className="flex items-center gap-2 text-sm text-ink-body">
              <input
                type="checkbox"
                checked={formulaire.eviterFinJournee}
                onChange={(e) =>
                  setFormulaire({
                    ...formulaire,
                    eviterFinJournee: e.target.checked,
                  })
                }
                className="h-4 w-4 rounded-xs border-line-default"
                style={{ accentColor: 'var(--color-primary)' }}
              />
              Éviter en fin de journée
            </label>
          </div>

          {sauvegarde.error !== null && (
            <MessageErreur message={sauvegarde.error.message} />
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
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Salles                                                             */
/* ------------------------------------------------------------------ */

const FORM_SALLE_INITIAL = {
  nom: '',
  capacite: '30',
  type: '',
  equipements: '',
  batiment: '',
};

function OngletSalles() {
  const queryClient = useQueryClient();
  const { data: salles, isLoading, error } = useQuery({
    queryKey: ['ecole', 'salles'],
    queryFn: () => apiFetch<Salle[]>('/ecole/salles'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [enEdition, setEnEdition] = useState<Salle | null>(null);
  const [formulaire, setFormulaire] = useState(FORM_SALLE_INITIAL);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'salles'] });
  }

  const sauvegarde = useMutation({
    mutationFn: () => {
      const corps = JSON.stringify({
        nom: formulaire.nom,
        capacite: Number(formulaire.capacite),
        type: formulaire.type,
        equipements: csvVersListe(formulaire.equipements),
        batiment:
          formulaire.batiment.trim().length > 0
            ? formulaire.batiment.trim()
            : null,
      });
      return enEdition === null
        ? apiFetch<Salle>('/ecole/salles', { method: 'POST', body: corps })
        : apiFetch<Salle>(`/ecole/salles/${enEdition.id}`, {
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
      apiFetch<unknown>(`/ecole/salles/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(null);
    setFormulaire(FORM_SALLE_INITIAL);
    setDialogOuvert(true);
  }

  function ouvrirEdition(salle: Salle) {
    sauvegarde.reset();
    suppression.reset();
    setEnEdition(salle);
    setFormulaire({
      nom: salle.nom,
      capacite: String(salle.capacite),
      type: salle.type,
      equipements: (salle.equipements ?? []).join(', '),
      batiment: salle.batiment ?? '',
    });
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = salles ?? [];

  return (
    <Card padded={false} style={{ overflow: 'hidden' }}>
      <EnteteCarte
        titre="Salles"
        actions={
          <Button variant="primary" size="sm" onClick={ouvrirCreation}>
            Nouvelle salle
          </Button>
        }
      />

      {suppression.error !== null && (
        <BandeauErreur message={suppression.error.message} />
      )}

      {liste.length === 0 ? (
        <EmptyState
          title="Aucune salle"
          description="Déclarez vos salles, leur capacité, leur type et leurs équipements pour que le moteur puisse les affecter."
          action={
            <Button variant="primary" size="sm" onClick={ouvrirCreation}>
              Nouvelle salle
            </Button>
          }
        />
      ) : (
        <table style={TABLEAU}>
          <thead>
            <tr>
              <th style={TH}>Nom</th>
              <th style={TH_DROITE}>Capacité</th>
              <th style={TH}>Type</th>
              <th style={TH}>Équipements</th>
              <th style={TH}>Bâtiment</th>
              <th style={TH_DROITE}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {liste.map((salle) => (
              <tr key={salle.id}>
                <td style={TD_FORT}>{salle.nom}</td>
                <td style={TD_DROITE}>{salle.capacite}</td>
                <td style={TD}>{salle.type}</td>
                <td style={TD}>
                  <Pastilles elements={salle.equipements ?? []} />
                </td>
                <td style={TD}>{salle.batiment ?? '—'}</td>
                <td style={TD_DROITE}>
                  <ActionsLigne>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => ouvrirEdition(salle)}
                    >
                      Modifier
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={suppression.isPending}
                      onClick={() => {
                        if (
                          window.confirm(
                            `Supprimer la salle « ${salle.nom} » ?`,
                          )
                        ) {
                          suppression.mutate(salle.id);
                        }
                      }}
                    >
                      Supprimer
                    </Button>
                  </ActionsLigne>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre={enEdition === null ? 'Nouvelle salle' : 'Modifier la salle'}
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sauvegarde.mutate();
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="nomSalle">Nom</Label>
              <Champ
                id="nomSalle"
                value={formulaire.nom}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, nom: e.target.value })
                }
                placeholder="Salle 101"
                required
              />
            </div>
            <div>
              <Label htmlFor="capaciteSalle">Capacité</Label>
              <Champ
                id="capaciteSalle"
                type="number"
                min="1"
                value={formulaire.capacite}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, capacite: e.target.value })
                }
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="typeSalle">Type</Label>
              <Champ
                id="typeSalle"
                value={formulaire.type}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, type: e.target.value })
                }
                placeholder="STANDARD, LABO, GYMNASE…"
                required
              />
            </div>
            <div>
              <Label htmlFor="batimentSalle">Bâtiment</Label>
              <Champ
                id="batimentSalle"
                value={formulaire.batiment}
                onChange={(e) =>
                  setFormulaire({ ...formulaire, batiment: e.target.value })
                }
                placeholder="Bâtiment A (optionnel)"
              />
            </div>
          </div>
          <div>
            <Label htmlFor="equipementsSalle">
              Équipements (séparés par des virgules)
            </Label>
            <Champ
              id="equipementsSalle"
              value={formulaire.equipements}
              onChange={(e) =>
                setFormulaire({ ...formulaire, equipements: e.target.value })
              }
              placeholder="PROJECTEUR, TABLEAU_INTERACTIF"
            />
          </div>

          {sauvegarde.error !== null && (
            <MessageErreur message={sauvegarde.error.message} />
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
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Barrettes                                                          */
/* ------------------------------------------------------------------ */

function aplatirGroupes(groupes: Groupe[]): { id: number; libelle: string }[] {
  const resultat: { id: number; libelle: string }[] = [];
  for (const groupe of groupes) {
    resultat.push({
      id: groupe.id,
      libelle: `${groupe.libelle} (${groupe.niveauLibelle})`,
    });
    for (const sousGroupe of groupe.sousGroupes ?? []) {
      const libelle = sousGroupe.libelle.includes(groupe.libelle)
        ? sousGroupe.libelle
        : `${groupe.libelle} ${sousGroupe.libelle}`;
      resultat.push({ id: sousGroupe.id, libelle });
    }
  }
  return resultat;
}

function OngletBarrettes() {
  const queryClient = useQueryClient();
  const { data: barrettes, isLoading, error } = useQuery({
    queryKey: ['ecole', 'barrettes'],
    queryFn: () => apiFetch<Barrette[]>('/ecole/barrettes'),
  });
  const { data: matieres } = useQuery({
    queryKey: ['ecole', 'matieres'],
    queryFn: () => apiFetch<Matiere[]>('/ecole/matieres'),
  });
  const { data: groupes } = useQuery({
    queryKey: ['ecole', 'groupes'],
    queryFn: () => apiFetch<Groupe[]>('/ecole/groupes'),
  });

  const [dialogOuvert, setDialogOuvert] = useState(false);
  const [libelle, setLibelle] = useState('');
  const [matiereId, setMatiereId] = useState('');
  const [groupeIds, setGroupeIds] = useState<number[]>([]);

  const groupesAplatis = aplatirGroupes(groupes ?? []);
  const libellesGroupes = new Map(
    groupesAplatis.map((groupe) => [groupe.id, groupe.libelle]),
  );

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ['ecole', 'barrettes'] });
  }

  const creation = useMutation({
    mutationFn: () =>
      apiFetch<Barrette>('/ecole/barrettes', {
        method: 'POST',
        body: JSON.stringify({
          libelle,
          matiereId: Number(matiereId),
          groupeIds,
        }),
      }),
    onSuccess: () => {
      invalider();
      setDialogOuvert(false);
    },
  });

  const suppression = useMutation({
    mutationFn: (id: number) =>
      apiFetch<unknown>(`/ecole/barrettes/${id}`, { method: 'DELETE' }),
    onSuccess: invalider,
  });

  function ouvrirCreation() {
    creation.reset();
    suppression.reset();
    setLibelle('');
    setMatiereId('');
    setGroupeIds([]);
    setDialogOuvert(true);
  }

  if (isLoading) return <ChargementPage />;
  if (error !== null) return <MessageErreur message={error.message} />;

  const liste = barrettes ?? [];

  return (
    <Card padded={false} style={{ overflow: 'hidden' }}>
      <EnteteCarte
        titre="Barrettes (cours alignés)"
        sousTitre="Une barrette aligne une matière sur le même créneau pour plusieurs groupes (ex. langues vivantes)."
        actions={
          <Button variant="primary" size="sm" onClick={ouvrirCreation}>
            Nouvelle barrette
          </Button>
        }
      />

      {suppression.error !== null && (
        <BandeauErreur message={suppression.error.message} />
      )}

      {liste.length === 0 ? (
        <EmptyState
          title="Aucune barrette"
          description="Créez une barrette pour aligner une matière sur le même créneau dans plusieurs groupes."
          action={
            <Button variant="primary" size="sm" onClick={ouvrirCreation}>
              Nouvelle barrette
            </Button>
          }
        />
      ) : (
        <table style={TABLEAU}>
          <thead>
            <tr>
              <th style={TH}>Libellé</th>
              <th style={TH}>Matière</th>
              <th style={TH}>Groupes</th>
              <th style={TH_DROITE}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {liste.map((barrette) => (
              <tr key={barrette.id}>
                <td style={TD_FORT}>{barrette.libelle}</td>
                <td style={TD}>{barrette.matiereLibelle}</td>
                <td style={TD}>
                  <Pastilles
                    elements={barrette.groupeIds.map(
                      (id) => libellesGroupes.get(id) ?? `Groupe ${id}`,
                    )}
                  />
                </td>
                <td style={TD_DROITE}>
                  <ActionsLigne>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={suppression.isPending}
                      onClick={() => {
                        if (
                          window.confirm(
                            `Supprimer la barrette « ${barrette.libelle} » ?`,
                          )
                        ) {
                          suppression.mutate(barrette.id);
                        }
                      }}
                    >
                      Supprimer
                    </Button>
                  </ActionsLigne>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Dialog
        ouvert={dialogOuvert}
        titre="Nouvelle barrette"
        onFermer={() => setDialogOuvert(false)}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            creation.mutate();
          }}
          className="space-y-4"
        >
          <div>
            <Label htmlFor="libelleBarrette">Libellé</Label>
            <Champ
              id="libelleBarrette"
              value={libelle}
              onChange={(e) => setLibelle(e.target.value)}
              placeholder="Barrette langues 1AC"
              required
            />
          </div>
          <div>
            <Label htmlFor="matiereBarrette">Matière</Label>
            <Selecteur
              id="matiereBarrette"
              value={matiereId}
              onChange={(e) => setMatiereId(e.target.value)}
              required
            >
              <option value="">— Choisir une matière —</option>
              {(matieres ?? []).map((matiere) => (
                <option key={matiere.id} value={matiere.id}>
                  {matiere.libelle}
                </option>
              ))}
            </Selecteur>
          </div>
          <div>
            <Label>Groupes concernés</Label>
            <div className="max-h-52 space-y-1 overflow-y-auto rounded-sm border border-line-subtle p-3">
              {groupesAplatis.length === 0 && (
                <p className="text-sm text-ink-muted">Aucun groupe disponible.</p>
              )}
              {groupesAplatis.map((groupe) => (
                <label
                  key={groupe.id}
                  className="flex items-center gap-2 text-sm text-ink-body"
                >
                  <input
                    type="checkbox"
                    checked={groupeIds.includes(groupe.id)}
                    onChange={(e) =>
                      setGroupeIds(
                        e.target.checked
                          ? [...groupeIds, groupe.id]
                          : groupeIds.filter((id) => id !== groupe.id),
                      )
                    }
                    className="h-4 w-4 rounded-xs border-line-default"
                    style={{ accentColor: 'var(--color-primary)' }}
                  />
                  {groupe.libelle}
                </label>
              ))}
            </div>
          </div>

          {creation.error !== null && (
            <MessageErreur message={creation.error.message} />
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" onClick={() => setDialogOuvert(false)}>
              Annuler
            </Button>
            <Button
              type="submit"
              loading={creation.isPending}
              disabled={groupeIds.length < 2}
            >
              {creation.isPending ? 'Création…' : 'Créer'}
            </Button>
          </div>
        </form>
      </Dialog>
    </Card>
  );
}
