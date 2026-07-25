export function Spinner({ className = '' }: { className?: string }) {
  return (
    <span
      role="status"
      aria-label="Chargement en cours"
      className={`inline-block h-6 w-6 animate-spin rounded-full border-2 border-indigo-600 border-t-transparent ${className}`}
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
