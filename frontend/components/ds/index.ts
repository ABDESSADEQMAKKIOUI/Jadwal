/**
 * Composants du design system Ynexis, portés verbatim depuis le bundle
 * `ynexis-design-system-f04fb46e` (styles inline `var(--token)`, API anglaise).
 *
 * Ne pas confondre avec `components/ui/*`, qui garde l'API française
 * historique de JADWAL (`variante`, `taille`, `titre`…).
 *
 * Les 15 composants du design system sont portés, y compris ceux qu'aucun
 * écran ne consomme encore : c'est une bibliothèque, elle doit être complète.
 * Seul `DataTable` reste à porter (JADWAL utilise ses propres tableaux).
 */

export { Alert, type AlertProps, type AlertTone } from './alert';
export { Avatar, type AvatarProps, type AvatarShape } from './avatar';
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
export {
  IconButton,
  type IconButtonProps,
  type IconButtonSize,
  type IconButtonVariant,
} from './icon-button';
export { Input, type InputProps, type InputSize } from './input';
export { KpiTile, type KpiDeltaTone, type KpiTileProps } from './kpi-tile';
export { Pagination, type PaginationProps } from './pagination';
export { Select, type SelectOption, type SelectProps, type SelectSize } from './select';
export { StatusBadge, type StatusBadgeProps } from './status-badge';
export { Switch, type SwitchProps } from './switch';
export { Tabs, type TabItem, type TabsProps } from './tabs';
export { Textarea, type TextareaProps } from './textarea';
