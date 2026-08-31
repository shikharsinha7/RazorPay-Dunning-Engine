import { useMemo } from 'react';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

const OUTCOME_COLORS = {
  RECOVERED: 'var(--color-recovered)',
  FAILED: 'var(--color-lost)',
  SKIPPED: '#475569',
};

export default function TimelineChart({ timeline }) {
  const data = useMemo(() => {
    if (!timeline) return [];
    const byStrategy = { AI: { name: 'AI-driven', RECOVERED: 0, FAILED: 0, SKIPPED: 0 }, NAIVE: { name: 'Naive fixed', RECOVERED: 0, FAILED: 0, SKIPPED: 0 } };
    timeline.forEach((point) => {
      if (byStrategy[point.strategy] && byStrategy[point.strategy][point.outcome] !== undefined) {
        byStrategy[point.strategy][point.outcome] += 1;
      }
    });
    return [byStrategy.AI, byStrategy.NAIVE];
  }, [timeline]);

  return (
    <div className="rounded-lg border border-[var(--color-border-subtle)] bg-[var(--color-surface)] p-5">
      <div className="mb-4 flex items-baseline justify-between">
        <h2 className="font-[var(--font-display)] text-sm font-medium text-slate-300">Outcome breakdown by strategy</h2>
        <span className="font-[var(--font-mono)] text-xs text-slate-500">{timeline?.length ?? 0} attempts executed</span>
      </div>
      <ResponsiveContainer width="100%" height={200}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border-subtle)" vertical={false} />
          <XAxis dataKey="name" tick={{ fill: '#e6ebf5', fontSize: 13 }} axisLine={{ stroke: 'var(--color-border-subtle)' }} tickLine={false} />
          <YAxis tick={{ fill: '#94a3b8', fontSize: 12 }} axisLine={false} tickLine={false} allowDecimals={false} />
          <Tooltip contentStyle={{ background: 'var(--color-surface-raised)', border: '1px solid var(--color-border-subtle)', borderRadius: 6 }} labelStyle={{ color: '#e6ebf5' }} />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          <Bar dataKey="RECOVERED" stackId="a" fill={OUTCOME_COLORS.RECOVERED} name="Recovered" />
          <Bar dataKey="FAILED" stackId="a" fill={OUTCOME_COLORS.FAILED} name="Failed" />
          <Bar dataKey="SKIPPED" stackId="a" fill={OUTCOME_COLORS.SKIPPED} name="Skipped" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
