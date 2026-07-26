# Design system Ynexis dans JADWAL

JADWAL utilise le **design system Ynexis** (celui du tableau de bord Ynexis AI Call Center), pour
que les produits de la maison partagent une identité visuelle. Marque : teal **Ocean Green `#47a398`**
(Yakeey), police **Inter**, neutres froids légèrement teintés teal, registre « SaaS admin calme et
dense » — pas une interface marketing.

## Où vit la vérité

| Fichier | Rôle |
|---|---|
| `frontend/app/tokens/*.css` | **Copie verbatim** du design system (`colors`, `typography`, `spacing`, `fonts`, `base`). Source unique des valeurs. **Ne pas éditer à la main** : remplacer lors d'une mise à jour du DS. |
| `frontend/app/globals.css` | Importe les tokens, puis le bloc `@theme` qui les expose comme utilitaires Tailwind. |

Origine : export claude.ai/design `ynexis-design-system-f04fb46e`. L'idiome natif du DS est le style
inline `var(--token)` ; ici les tokens sont **liés au thème Tailwind**, donc les utilitaires rendent
exactement les valeurs Ynexis tout en restant idiomatiques pour Next.js.

## Vocabulaire à utiliser

Les utilitaires ci-dessous sont définis dans `@theme` et pointent chacun sur un token du DS.
**N'inventez pas de couleur** : si une teinte manque, ajoutez-la au `@theme` en visant un token.

| Famille | Utilitaires | Pointe sur |
|---|---|---|
| Marque | `brand`, `brand-hover`, `brand-active`, `brand-weak`, `brand-soft`, `brand-border`, `on-brand` | `--teal-500` → `--teal-700`, `--teal-50` |
| Accent | `accent`, `accent-hover` | `--coral-500`, `--coral-600` |
| Surfaces | `surface-app`, `surface-card`, `surface-sunken`, `surface-inverse`, `surface-hover`, `surface-selected` | `--neutral-50/0/100/900`, `--teal-50` |
| Texte | `ink-strong`, `ink-body`, `ink-muted`, `ink-subtle`, `ink-on-dark`, `ink-link` | `--neutral-900/700/500/400` |
| Bordures | `line-subtle`, `line-default`, `line-strong` | `--neutral-200/300/400` |
| Statuts | `ok-*`, `warn-*`, `bad-*`, `info-*`, `idle-*` (suffixes `-bg`, `-fg`, `-solid`) | `--status-success-*`, `-warning-*`, `-danger-*`, `-info-*`, `-neutral-*` |
| Échelles | `teal-50…900`, `neutral-0…950`, `red/green/amber/blue-50…800` | échelles Ynexis |

S'écrivent en `bg-`, `text-`, `border-`, `divide-` comme tout utilitaire Tailwind :
`bg-surface-card`, `text-ink-muted`, `border-line-subtle`.

**Typographie** : `text-2xs` (11 px) · `text-xs` (12) · `text-sm` (13) · `text-base` (14, défaut
des contrôles) · `text-md` (16) · `text-lg` (18, titre de carte) · `text-xl` (22) · `text-2xl` (28,
titre de page) · `text-3xl` (36, KPI). Plus dense que Tailwind par défaut — c'est voulu.

**Rayons** : `rounded-xs` (4) · `rounded-sm` (6, contrôles) · `rounded-md` (8, cartes) ·
`rounded-lg` (12, modales) · `rounded-[var(--radius-pill)]` (pastilles, avatars).

**Tokens sans utilitaire** — s'utilisent en valeur arbitraire ou en `style` :
`shadow-[var(--shadow-sm)]` (cartes), `shadow-[var(--shadow-lg)]` (modales),
`focus-visible:shadow-[var(--ring)]` (anneau de focus, **jamais** `ring-*` de Tailwind),
`h-[var(--control-height-md)]` (38 px, hauteur de contrôle), `tracking-[var(--tracking-caps)]`
(capitales d'en-tête de tableau), `duration-[var(--duration-fast)]`, `bg-[var(--surface-overlay)]`
(voile de modale), `w-sidebar` (248 px).

## Exemple

```tsx
<Card titre="Mon abonnement">
  <div className="flex items-center justify-between gap-4">
    <div>
      <p className="text-lg font-semibold text-ink-strong">Essentiel</p>
      <p className="text-sm text-ink-muted">Du 01/09/2026 au 30/06/2027</p>
    </div>
    <span className="rounded-[var(--radius-pill)] bg-ok-bg px-2.5 py-0.5 text-xs font-medium text-ok-fg">
      Actif
    </span>
  </div>
  <Button className="mt-4">Enregistrer</Button>
</Card>
```

## Règles d'adhérence du design system

Le design system livre ses propres règles de lint (`_adherence.oxlintrc.json` dans l'export). Les
plus utiles ici, transposées :

1. **Pas de couleur en dur** — jamais `#47a398` dans le code : `var(--teal-500)` ou l'utilitaire
   `bg-brand`. Une couleur en dur échappe à toute mise à jour du design system.
2. **Polices** — uniquement **Inter** et **Noto Sans Arabic** (déjà chargées par `tokens/fonts.css`).
   Ne pas déclarer d'autre `font-family`.
3. **Props des composants portés** — les composants de `components/ds/` reprennent l'API du design
   system : leurs props et leurs valeurs d'énumération sont **strictes**. Exemple : `<Alert>`
   n'accepte que `tone`, `title`, `children`, `icon`, `onClose`, `style`, et `tone` vaut
   `success | danger | warning | info | neutral`. Ne pas inventer de prop.
4. **Importer depuis l'index** — `import { Card, Alert } from '@/components/ds'`, pas depuis les
   fichiers internes.
5. **Espacements** : préférer l'échelle (`var(--space-*)` ou les utilitaires Tailwind, grille de
   4 px). Le design system tolère une valeur en px pour une géométrie ponctuelle (largeur d'un champ
   de recherche, hauteur d'un logo) — ses propres écrans le font.

## Règles

- **Composants d'abord** : `components/ds/` porte fidèlement les composants du design system (API
  d'origine en anglais : `variant`, `size`, `tone`). `components/ui/` conserve l'API française
  historique (`variante`, `taille`, `titre`) utilisée par les écrans admin. Restylez le composant,
  pas l'écran.
- **Pas de palette Tailwind par défaut** : `gray-*`, `slate-*`, `indigo-*` ne doivent plus
  apparaître — `neutral-*` et `teal-*` sont liés aux valeurs Ynexis.
- **Couleurs de matières** : ce sont des **données** (`matiere.couleur`), pas des tokens. Le jeu de
  démonstration utilise une palette catégorielle mate accordée au teal ; chaque établissement peut
  la changer.
- Le fond de page et la police viennent de `tokens/base.css` (`--surface-app`, `--font-sans`) :
  rien à redéfinir.
