import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

export default function ComparisonChart({ comparison }) {
  if (!comparison) return null;
  const data = [
    { name: 'AI-driven', rate: comparison.ai.recoveryRatePercent, recovered: comparison.ai.recoveredPayments },
    { name: 'Naive fixed', rate: comparison.naive.recoveryRatePercent, recovered: comparison.naive.recoveredPayments },
  ];

  return (
    <div className="rounded-lg border border-[var(--color-border-subtle)] bg-[var(--color-surface)] p-5">
      <h2 className="mb-4 font-[var(--font-display)] text-sm font-medium text-slate-300">Recovery rate by strategy</h2>
      <ResponsiveContainer width="100%" height={200}>
        <BarChart data={data} layout="vertical" margin={{ left: 10 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border-subtle)" horizontal={false} />
          <XAxis type="number" tickFormatter={(v) => `${v}%`} tick={{ fill: '#94a3b8', fontSize: 12 }} axisLine={{ stroke: 'var(--color-border-subtle)' }} />
          <YAxis type="category" dataKey="name" tick={{ fill: '#e6ebf5', fontSize: 13 }} axisLine={false} tickLine={false} width={90} />
          <Tooltip
            contentStyle={{ background: 'var(--color-surface-raised)', border: '1px solid var(--color-border-subtle)', borderRadius: 6 }}
            labelStyle={{ color: '#e6ebf5' }}
            formatter={(value, key, item) => [`${value}% (${item.payload.recovered} payments)`, 'Recovery rate']}
          />
          <Bar dataKey="rate" radius={[0, 4, 4, 0]} barSize={28}>
            <Cell fill="var(--color-ai)" />
            <Cell fill="var(--color-naive)" />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
