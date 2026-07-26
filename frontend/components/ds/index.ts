/**
 * Composants du design system Ynexis, portés verbatim depuis le bundle
 * `ynexis-design-system-f04fb46e` (styles inline `var(--token)`, API anglaise).
 *
 * Ne pas confondre avec `components/ui/*`, qui garde l'API française
 * historique de JADWAL (`variante`, `taille`, `titre`…).
 *
 * Seuls les composants réellement consommés par un écran sont portés ici.
 * Les autres restent dans le bundle source et seront portés au moment où
 * un écran en aura besoin.
 */

export { Alert, type AlertProps, type AlertTone } from './alert';
export { Badge, type BadgeProps, type BadgeSize, type BadgeTone } from './badge';
export { Button, type ButtonProps, type ButtonSize, type ButtonVariant } from './button';
export {
  Card,
  type CardBodyProps,
  type CardFooterProps,
  type CardHeaderProps,
  type CardProps,
  type CardShadow,
} from './card';
export { EmptyState, type EmptyStateProps, type EmptyStateVariant } from './empty-state';
export { Input, type InputProps, type InputSize } from './input';
export { KpiTile, type KpiDeltaTone, type KpiTileProps } from './kpi-tile';
export { Select, type SelectOption, type SelectProps, type SelectSize } from './select';
export { Tabs, type TabItem, type TabsProps } from './tabs';
