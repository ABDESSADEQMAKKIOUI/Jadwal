export function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center gap-3 py-10 text-center">
      <span className="flex h-11 w-11 items-center justify-center rounded-[var(--radius-pill)] bg-surface-sunken">
        <svg
          className="h-5 w-5 text-ink-subtle"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
        >
          <rect x="3" y="5" width="18" height="14" rx="2" />
          <path d="M3 9h18" strokeLinecap="round" />
        </svg>
      </span>
      <p className="text-base text-ink-muted">{message}</p>
    </div>
  );
}
