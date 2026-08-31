import { useMemo } from 'react';

const WIDTH = 900;
const HEIGHT = 220;
const PAD_Y = 24;

// Builds a monotonic step-line of cumulative recoveries for one strategy,
// aligned to the shared x-axis of "attempts executed so far" (chronological,
// across both strategies) — so at any point on the line you can read "how
// many has AI recovered vs Naive, this far into the run."
function buildStepPoints(timeline, strategy, maxCount) {
  let cumulative = 0;
  const points = [[0, HEIGHT - PAD_Y]];
  timeline.forEach((point, i) => {
    if (point.strategy === strategy && point.outcome === 'RECOVERED') {
      cumulative += 1;
    }
    const x = (i / Math.max(1, timeline.length - 1)) * WIDTH;
    const y = HEIGHT - PAD_Y - (cumulative / Math.max(1, maxCount)) * (HEIGHT - PAD_Y * 2);
    points.push([x, y]);
  });
  return points;
}

function pointsToPath(points) {
  return points.map(([x, y], i) => `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`).join(' ');
}

function recoveryDots(timeline, strategy, maxCount) {
  let cumulative = 0;
  const dots = [];
  timeline.forEach((point, i) => {
    if (point.strategy === strategy && point.outcome === 'RECOVERED') {
      cumulative += 1;
      const x = (i / Math.max(1, timeline.length - 1)) * WIDTH;
      const y = HEIGHT - PAD_Y - (cumulative / Math.max(1, maxCount)) * (HEIGHT - PAD_Y * 2);
      dots.push([x, y]);
    }
  });
  return dots;
}

export default function PulseHero({ timeline, comparison }) {
  const { aiPath, naivePath, aiDots, naiveDots } = useMemo(() => {
    if (!timeline || timeline.length === 0) {
      return { aiPath: '', naivePath: '', aiDots: [], naiveDots: [] };
    }
    const maxCount = Math.max(
      1,
      timeline.filter((p) => p.strategy === 'AI' && p.outcome === 'RECOVERED').length,
      timeline.filter((p) => p.strategy === 'NAIVE' && p.outcome === 'RECOVERED').length
    );
    return {
      aiPath: pointsToPath(buildStepPoints(timeline, 'AI', maxCount)),
      naivePath: pointsToPath(buildStepPoints(timeline, 'NAIVE', maxCount)),
      aiDots: recoveryDots(timeline, 'AI', maxCount),
      naiveDots: recoveryDots(timeline, 'NAIVE', maxCount),
    };
  }, [timeline]);

  const lift = comparison?.liftPercentagePoints ?? 0;
  const liftSign = lift > 0 ? '+' : '';

  return (
    <div className="relative overflow-hidden rounded-xl border border-[var(--color-border-subtle)] bg-[var(--color-surface)] p-6 md:p-8">
      <div className="relative z-10 mb-2 flex items-baseline gap-3">
        <span
          className="font-[var(--font-mono)] text-5xl font-medium tracking-tight md:text-6xl"
          style={{ color: lift >= 0 ? 'var(--color-ai)' : 'var(--color-lost)' }}
        >
          {liftSign}{lift.toFixed(2)}pp
        </span>
        <span className="font-[var(--font-display)] text-sm text-slate-400 md:text-base">
          AI recovery lift over naive fixed-interval retries
        </span>
      </div>
      <p className="relative z-10 mb-4 text-xs text-slate-500">
        Every executed retry attempt, in order — each step up is a recovered payment.
      </p>

      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="relative z-10 h-40 w-full md:h-48" preserveAspectRatio="none">
        <line x1="0" y1={HEIGHT - PAD_Y} x2={WIDTH} y2={HEIGHT - PAD_Y} stroke="var(--color-border-subtle)" strokeWidth="1" />

        <path d={naivePath} fill="none" stroke="var(--color-naive)" strokeWidth="2" opacity="0.85" />
        {naiveDots.map(([x, y], i) => (
          <circle key={`naive-${i}`} cx={x} cy={y} r="3" fill="var(--color-naive)" />
        ))}

        <path
          d={aiPath}
          fill="none"
          stroke="var(--color-ai)"
          strokeWidth="2.5"
          style={{ filter: 'drop-shadow(0 0 4px var(--color-ai))' }}
        />
        {aiDots.map(([x, y], i) => (
          <circle key={`ai-${i}`} cx={x} cy={y} r="3.5" fill="var(--color-ai)" style={{ filter: 'drop-shadow(0 0 3px var(--color-ai))' }} />
        ))}
      </svg>

      <div className="relative z-10 mt-3 flex gap-5 text-xs">
        <span className="flex items-center gap-1.5 text-[var(--color-ai)]">
          <span className="h-2 w-2 rounded-full bg-[var(--color-ai)]" /> AI-driven retries
        </span>
        <span className="flex items-center gap-1.5 text-[var(--color-naive)]">
          <span className="h-2 w-2 rounded-full bg-[var(--color-naive)]" /> Naive fixed schedule
        </span>
      </div>
    </div>
  );
}
