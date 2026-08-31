const VARIANTS = {
  ai: 'bg-[var(--color-ai-dim)] text-[var(--color-ai)]',
  naive: 'bg-[var(--color-naive-dim)] text-[var(--color-naive)]',
  recovered: 'bg-teal-950 text-[var(--color-recovered)]',
  failed: 'bg-red-950 text-[var(--color-lost)]',
  skipped: 'bg-slate-800 text-slate-400',
  pending: 'bg-slate-800 text-slate-400',
};

export default function Badge({ variant, children }) {
  const classes = VARIANTS[variant] ?? VARIANTS.pending;
  return (
    <span className={`inline-flex items-center rounded px-2 py-0.5 text-xs font-medium uppercase tracking-wide ${classes}`}>
      {children}
    </span>
  );
}
