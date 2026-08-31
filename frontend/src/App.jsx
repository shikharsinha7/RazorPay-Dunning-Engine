import { useEffect, useState } from 'react';
import { getComparison, getFunnel, getPayments, getTimeline } from './lib/api';
import PulseHero from './components/PulseHero';
import StatStrip from './components/StatStrip';
import FunnelChart from './components/FunnelChart';
import ComparisonChart from './components/ComparisonChart';
import TimelineChart from './components/TimelineChart';
import EventFeed from './components/EventFeed';
import ErrorBoundary from './components/ErrorBoundary';

const REFRESH_INTERVAL_MS = 15_000;

export default function App() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [funnel, comparison, timeline, payments] = await Promise.all([
          getFunnel(), getComparison(), getTimeline(), getPayments(),
        ]);
        if (cancelled) return;
        setData({ funnel, comparison, timeline, payments });
        setLastUpdated(new Date());
        setError(null);
      } catch (e) {
        // Log to console regardless of whether the UI is showing the error,
        // so devtools always has the real stack trace.
        // eslint-disable-next-line no-console
        console.error('Dashboard data fetch failed:', e);
        if (!cancelled) setError(e.message);
      }
    }

    load();
    const interval = setInterval(load, REFRESH_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  return (
    <div className="min-h-screen bg-[var(--color-ink)] px-4 py-6 md:px-8 md:py-10">
      <div className="mx-auto max-w-5xl">
        <header className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="font-[var(--font-display)] text-xl font-semibold text-slate-100 md:text-2xl">
              Smart Dunning &amp; Recovery Engine
            </h1>
            <p className="text-xs text-slate-500">Razorpay Buildathon</p>
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-500">
            {!error && data && (
              <span className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-[var(--color-recovered)]" />
                Live
              </span>
            )}
            {lastUpdated && <span className="font-[var(--font-mono)]">Updated {lastUpdated.toLocaleTimeString()}</span>}
          </div>
        </header>

        {error && (
          <div className="rounded-lg border border-[var(--color-lost)]/40 bg-red-950/30 p-4 text-sm text-red-300">
            Failed to load dashboard data: {error}
            <br />
            Is the backend running on http://localhost:8080?
          </div>
        )}

        {!error && !data && (
          <div className="rounded-lg border border-[var(--color-border-subtle)] bg-[var(--color-surface)] p-4 text-sm text-slate-400">
            Loading dashboard data…
          </div>
        )}

        {data && (
          <div className="space-y-5">
            <ErrorBoundary label="Pulse hero">
              <PulseHero timeline={data.timeline} comparison={data.comparison} />
            </ErrorBoundary>

            <ErrorBoundary label="Stat strip">
              <StatStrip funnel={data.funnel} />
            </ErrorBoundary>

            <div className="grid gap-5 md:grid-cols-2">
              <ErrorBoundary label="Funnel chart">
                <FunnelChart funnel={data.funnel} />
              </ErrorBoundary>
              <ErrorBoundary label="Comparison chart">
                <ComparisonChart comparison={data.comparison} />
              </ErrorBoundary>
            </div>

            <ErrorBoundary label="Timeline chart">
              <TimelineChart timeline={data.timeline} />
            </ErrorBoundary>

            <ErrorBoundary label="Event feed">
              <EventFeed payments={data.payments} />
            </ErrorBoundary>
          </div>
        )}
      </div>
    </div>
  );
}
