# Day 5 — Dashboard APIs + Frontend Scaffold

## What's built

**Backend (additive only — nothing existing changed behavior):**
- `PaymentRepository.countByStatus(...)` — one new method
- `RetryAttemptRepository.findByStrategyAndOutcome(...)` and `.findByExecutedAtIsNotNullOrderByExecutedAtAsc(...)` — two new methods
- `GET /api/dashboard/funnel` — total payments, and counts by status (still-failing/recovered/lost)
- `GET /api/dashboard/comparison` — the headline stat: AI recovery rate % vs NAIVE recovery rate %, and the lift between them
- `GET /api/dashboard/timeline` — every executed retry attempt (strategy, outcome, timestamp), for charting

**Frontend (`frontend/` — new folder, doesn't touch the backend):**
- Vite + React + Tailwind v4 scaffold, verified building clean (`npm run build` succeeded, 0 vulnerabilities on install)
- `src/lib/api.js` — fetch wrapper for the 4 read endpoints
- `src/App.jsx` — fetches all 4 on load, renders raw JSON in styled boxes. **Intentionally unstyled beyond basic layout** — Day 6 is the actual visual design pass
- Vite dev-server proxy (`/api` → `localhost:8080`) — no backend CORS config needed for local dev

## Test it (PowerShell)

**1. Backend — with your Day 1-4 environment variables still set, run:**
```powershell
mvn spring-boot:run
```

**2. Check the three new endpoints directly:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard/funnel" | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard/comparison" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard/timeline" | ConvertTo-Json -Depth 5
```
`comparison` should show AI's recovery rate meaningfully higher than NAIVE's, given the simulator's built-in multiplier — that gap is your demo's core proof point.

**3. Frontend — in a separate PowerShell window (keep the backend running):**
```powershell
cd frontend
npm install
npm run dev
```
Then open the URL it prints (usually `http://localhost:5173`). You should see four boxes of raw JSON — funnel, comparison, timeline, payments — all populated from your real Supabase data. If you see a red error instead, confirm the backend is still running on port 8080.

## Next: Day 6
Visual design pass on this same dashboard — funnel chart, comparison hero stat, live event feed. No new endpoints, just making this look like something you'd screenshot for judges.
