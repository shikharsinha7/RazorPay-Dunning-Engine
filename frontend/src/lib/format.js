export function formatRelativeTime(isoString) {
  if (!isoString) return '—';
  const diffMs = Date.now() - new Date(isoString).getTime();
  const diffSec = Math.max(0, Math.round(diffMs / 1000));
  if (diffSec < 60) return `${diffSec}s ago`;
  const diffMin = Math.round(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.round(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  const diffDay = Math.round(diffHr / 24);
  return `${diffDay}d ago`;
}

export function formatCurrency(amount, currency) {
  const n = Number(amount);
  return `${currency ?? 'INR'} ${n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function formatPercent(value) {
  return `${Number(value).toFixed(2)}%`;
}
