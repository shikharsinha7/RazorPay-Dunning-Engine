function Stat({ label, value, accent }) {
  return (
    <div className="rounded-lg border border-[var(--color-border-subtle)] bg-[var(--color-surface)] p-4">
      <div className="mb-1 text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="font-[var(--font-mono)] text-2xl font-medium" style={{ color: accent }}>{value}</div>
    </div>
  );
}

export default function StatStrip({ funnel }) {
  if (!funnel) return null;
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      <Stat label="Total ingested" value={funnel.totalPayments} accent="#e6ebf5" />
      <Stat label="Still retrying" value={funnel.failed} accent="var(--color-pending)" />
      <Stat label="Recovered" value={funnel.recovered} accent="var(--color-recovered)" />
      <Stat label="Lost" value={funnel.lost} accent="var(--color-lost)" />
    </div>
  );
}
