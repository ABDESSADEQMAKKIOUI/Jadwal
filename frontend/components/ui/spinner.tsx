export function Spinner({ className = '' }: { className?: string }) {
  return (
    <span
      role="status"
      aria-label="Chargement en cours"
      className={`inline-block h-6 w-6 animate-spin rounded-[var(--radius-pill)] border-2 border-brand border-t-transparent ${className}`}
    />
  );
}

export function ChargementPage() {
  return (
    <div className="flex justify-center py-16">
      <Spinner className="h-8 w-8" />
    </div>
  );
}
