import { useMemo } from 'react';
import Badge from './Badge';
import { formatCurrency, formatRelativeTime } from '../lib/format';

// Flattens every payment's nested retry attempts into one reverse-chronological
// feed of executed events — reuses the /api/payments data already fetched,
// no separate endpoint needed.
function buildEvents(payments) {
  const events = [];
  (payments ?? []).forEach((payment) => {
    (payment.retryAttempts ?? []).forEach((attempt) => {
      if (!attempt.executedAt) return;
      events.push({
        id: attempt.id,
        executedAt: attempt.executedAt,
        strategy: attempt.strategy,
        outcome: attempt.outcome,
        channel: attempt.channel,
        customerName: payment.customerName,
        amount: payment.amount,
        currency: payment.currency,
      });
    });
  });
  return events.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt));
}

export default function EventFeed({ payments }) {
  const events = useMemo(() => buildEvents(payments).slice(0, 12), [payments]);

  return (
    <div className="rounded-lg border border-[var(--color-border-subtle)] bg-[var(--color-surface)] p-5">
      <h2 className="mb-4 font-[var(--font-display)] text-sm font-medium text-slate-300">Recent activity</h2>
      {events.length === 0 ? (
        <p className="text-sm text-slate-500">No retry attempts executed yet — run force-run to see activity here.</p>
      ) : (
        <ul className="divide-y divide-[var(--color-border-subtle)]">
          {events.map((e) => (
            <li key={e.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
              <div className="flex min-w-0 items-center gap-3">
                <Badge variant={e.strategy === 'AI' ? 'ai' : 'naive'}>{e.strategy}</Badge>
                <Badge variant={e.outcome.toLowerCase()}>{e.outcome}</Badge>
                <span className="truncate text-slate-300">{e.customerName}</span>
              </div>
              <div className="flex shrink-0 items-center gap-3 font-[var(--font-mono)] text-xs text-slate-500">
                <span>{formatCurrency(e.amount, e.currency)}</span>
                <span>{formatRelativeTime(e.executedAt)}</span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
