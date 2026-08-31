export default function FunnelChart({ funnel }) {
  if (!funnel) return null;
  const total = Math.max(1, funnel.totalPayments);
  const segments = [
    { label: 'Still retrying', value: funnel.failed, color: 'var(--color-pending)' },
    { label: 'Recovered', value: funnel.recovered, color: 'var(--color-recovered)' },
    { label: 'Lost', value: funnel.lost, color: 'var(--color-lost)' },
  ];

  return (
    <div className="rounded-lg border border-[var(--color-border-subtle)] bg-[var(--color-surface)] p-5">
      <h2 className="mb-1 font-[var(--font-display)] text-sm font-medium text-slate-300">Payment outcomes</h2>
      <p className="mb-4 text-xs text-slate-500">Production status — driven by the AI track, the one that would actually run</p>

      <div className="mb-3 flex h-4 w-full overflow-hidden rounded-full bg-slate-800">
        {segments.map((s) => (
          <div key={s.label} style={{ width: `${(s.value / total) * 100}%`, backgroundColor: s.color }} title={`${s.label}: ${s.value}`} />
        ))}
      </div>

      <div className="space-y-2">
        {segments.map((s) => (
          <div key={s.label} className="flex items-center justify-between text-sm">
            <span className="flex items-center gap-2 text-slate-300">
              <span className="h-2 w-2 rounded-full" style={{ backgroundColor: s.color }} />
              {s.label}
            </span>
            <span className="font-[var(--font-mono)] text-slate-400">
              {s.value} <span className="text-slate-600">({((s.value / total) * 100).toFixed(1)}%)</span>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
