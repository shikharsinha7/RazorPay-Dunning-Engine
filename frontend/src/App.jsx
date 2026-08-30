import { useEffect, useState } from 'react';
import { getComparison, getFunnel, getPayments, getTimeline } from './lib/api';

// Day 5: minimal, unstyled-beyond-basics scaffold — just proves every
// dashboard endpoint is wired to real data. Visual polish is Day 6.
export default function App() {
  const [funnel, setFunnel] = useState(null);
  const [comparison, setComparison] = useState(null);
  const [timeline, setTimeline] = useState(null);
  const [payments, setPayments] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([getFunnel(), getComparison(), getTimeline(), getPayments()])
      .then(([funnelData, comparisonData, timelineData, paymentsData]) => {
        setFunnel(funnelData);
        setComparison(comparisonData);
        setTimeline(timelineData);
        setPayments(paymentsData);
      })
      .catch((e) => setError(e.message));
  }, []);

  if (error) {
    return (
      <div className="p-6 font-mono text-red-600">
        Failed to load dashboard data: {error}
        <br />
        Is the backend running on http://localhost:8080?
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 p-8 font-sans text-slate-900">
      <h1 className="mb-6 text-2xl font-bold">Smart Dunning &amp; Recovery Engine</h1>

      <section className="mb-8">
        <h2 className="mb-2 text-lg font-semibold">Funnel</h2>
        {funnel ? (
          <pre className="rounded bg-white p-4 text-sm shadow">{JSON.stringify(funnel, null, 2)}</pre>
        ) : (
          <p>Loading...</p>
        )}
      </section>

      <section className="mb-8">
        <h2 className="mb-2 text-lg font-semibold">AI vs Naive Comparison</h2>
        {comparison ? (
          <pre className="rounded bg-white p-4 text-sm shadow">{JSON.stringify(comparison, null, 2)}</pre>
        ) : (
          <p>Loading...</p>
        )}
      </section>

      <section className="mb-8">
        <h2 className="mb-2 text-lg font-semibold">Timeline ({timeline?.length ?? 0} executed attempts)</h2>
        {timeline ? (
          <pre className="max-h-64 overflow-auto rounded bg-white p-4 text-xs shadow">
            {JSON.stringify(timeline.slice(-10), null, 2)}
          </pre>
        ) : (
          <p>Loading...</p>
        )}
      </section>

      <section>
        <h2 className="mb-2 text-lg font-semibold">Payments ({payments?.length ?? 0})</h2>
        {payments ? (
          <pre className="max-h-64 overflow-auto rounded bg-white p-4 text-xs shadow">
            {JSON.stringify(payments.slice(-5), null, 2)}
          </pre>
        ) : (
          <p>Loading...</p>
        )}
      </section>
    </div>
  );
}
