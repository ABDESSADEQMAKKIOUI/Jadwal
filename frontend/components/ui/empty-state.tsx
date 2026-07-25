export function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center gap-2 py-10 text-center">
      <svg
        className="h-8 w-8 text-gray-300"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
      >
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="M3 9h18" strokeLinecap="round" />
      </svg>
      <p className="text-sm text-gray-500">{message}</p>
    </div>
  );
}
