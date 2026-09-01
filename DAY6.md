# Day 6 — Dashboard Visual Design

No backend changes today — this is purely the frontend visual pass on top of Day 5's working (but unstyled) scaffold.

## What's built

**Design system** (`src/index.css`, via Tailwind v4's `@theme`):
- Ink navy background, not pure black — deliberate choice for a data-dense ops screen
- Two-tone accent pairing tied to content: cool blue (`--color-ai`) for the AI track, warm amber (`--color-naive`) for the naive/legacy track — cool-signal vs warm-legacy, not a decorative choice
- Type: Space Grotesk (display, used sparingly), Inter (body/UI), JetBrains Mono (every number — amounts, percentages, timestamps read as live telemetry). All three self-hosted via `@fontsource` so the demo doesn't depend on a live Google Fonts connection.

**Components** (`src/components/`):
- `PulseHero.jsx` — the signature element. Two SVG step-line traces (AI vs Naive) showing cumulative recoveries across every executed attempt in order, with a glowing dot at each recovery — literally "the signal that recovers," grounded in what dunning actually is. The AI-vs-naive lift stat sits directly above it in large mono type.
- `StatStrip.jsx` — 4 small stat cards (total, still-retrying, recovered, lost)
- `FunnelChart.jsx` — payment status breakdown (labeled honestly as AI/production-driven, since `payment.status` only reflects the AI track — see Day 3's design note)
- `ComparisonChart.jsx` — horizontal bar chart, AI vs Naive recovery rate %
- `TimelineChart.jsx` — stacked bar, recovered/failed/skipped breakdown per strategy
- `EventFeed.jsx` — reverse-chronological list of the 12 most recent executed attempts, built by flattening the existing `/api/payments` response (no new endpoint needed)
- `Badge.jsx` — small reusable strategy/outcome pill

**App.jsx** now polls all 4 endpoints every 15 seconds, so the dashboard stays live during a demo without a manual refresh.

## New dependencies
`recharts`, `@fontsource/inter`, `@fontsource/space-grotesk`, `@fontsource/jetbrains-mono` — all added to `package.json`/`package-lock.json`.

## Verified before shipping
- `npm run build` — clean, no errors
- `npm run lint` (oxlint, bundled with the scaffold) — 0 warnings, 0 errors

## Test it (PowerShell)

**Backend** (same as before):
```powershell
mvn spring-boot:run
```

**Frontend:**
```powershell
cd frontend
npm install
npm run dev
```
Open the printed URL. You should see the full styled dashboard — pulse-trace hero at top, stat strip, funnel + comparison side by side, outcome breakdown chart, and the live activity feed at the bottom, all on a dark navy theme with the blue/amber AI-vs-naive distinction running through every chart.

**To see the event feed and pulse trace populated with fresh data**, run force-run again first so more attempts have executed:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/retries/force-run" -Method Post
```
Then refresh the dashboard (or just wait — it auto-refreshes every 15s).

## Next: Day 7
Deploy — backend to Render, frontend to Vercel — plus final README polish.
