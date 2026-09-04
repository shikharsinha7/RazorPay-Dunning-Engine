# Day 7 — Deploy (Render + Vercel)

## What's built
- `Dockerfile` — Render has no native Java support (Node/Python/Ruby/Go/Rust/Elixir only), so Java deploys as Docker there. Multi-stage build: `eclipse-temurin:25-jdk` + Maven to compile, `eclipse-temurin:25-jre` to run.
- `.dockerignore`
- `WebConfig.java` — CORS, needed now that frontend and backend live on different domains (the Vite dev-proxy trick was local-only)
- `frontend/src/lib/api.js` — reads `VITE_API_BASE_URL` at build time in production, falls back to relative paths locally (your dev workflow is unchanged)
- Root `README.md`

## Verifying the Docker build locally, before deploying
Java has to deploy as Docker on Render, and Docker builds can fail on things a local `mvn` build wouldn't catch — worth confirming locally first rather than debugging blind on Render's build logs:

```powershell
cd C:\Users\SAMSUNG\Downloads\razorpay-dunning-engine
docker build -t dunning-engine-test .
```

If `eclipse-temurin:25-jdk` or `:25-jre` ever aren't available as tags, check [hub.docker.com/_/eclipse-temurin/tags](https://hub.docker.com/_/eclipse-temurin/tags) for the closest available one and update the `Dockerfile`.

If the build succeeds, run it:
```powershell
docker run --rm -p 8080:8080 `
  -e SPRING_DATASOURCE_URL="your-supabase-url" `
  -e SPRING_DATASOURCE_USERNAME="your-username" `
  -e SPRING_DATASOURCE_PASSWORD="your-password" `
  -e GEMINI_API_KEY="your-key" `
  -e RESEND_API_KEY="your-key" `
  dunning-engine-test
```
Then hit `http://localhost:8080/api/dashboard/funnel` — if that returns real JSON, the Docker image works and Render will run it the same way.

## Deploy order matters here
Backend and frontend each need the other's URL (backend needs `FRONTEND_URL` for CORS, frontend needs `VITE_API_BASE_URL`) — a chicken-and-egg problem. Resolve it in this order:

### 1. Deploy the backend to Render first
1. [render.com](https://render.com) → **New** → **Web Service**
2. Connect your GitHub repo, select `RazorPay-Dunning-Engine`
3. Render should auto-detect the `Dockerfile` at the repo root (Runtime: Docker). If it asks for a Root Directory, leave it blank — the Dockerfile is at the repo root, not in a subfolder.
4. Instance type: **Free**
5. Add environment variables (same values as your local `.env`): `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `GEMINI_API_KEY`, `RESEND_API_KEY`. Leave `FRONTEND_URL` unset for now (defaults to `localhost:5173`, harmless until step 3).
6. **Create Web Service** — first build takes a few minutes. Copy the resulting URL (`https://xxxxx.onrender.com`).
7. Sanity check: visit `https://xxxxx.onrender.com/api/dashboard/funnel` in a browser — should return JSON.

### 2. Deploy the frontend to Vercel
1. [vercel.com](https://vercel.com) → **Add New** → **Project** → import the same repo
2. **Root Directory**: set to `frontend` (this is the part that's easy to miss — Vercel needs to know the frontend isn't at the repo root)
3. Framework preset: Vite (should auto-detect)
4. Environment variable: `VITE_API_BASE_URL` = your Render URL from step 1 (no trailing slash, e.g. `https://xxxxx.onrender.com`)
5. **Deploy** — copy the resulting URL (`https://xxxxx.vercel.app`)

### 3. Close the loop — update the backend's CORS setting
Back in Render → your service → **Environment** → add `FRONTEND_URL` = your Vercel URL from step 2 → save (auto-redeploys, takes a minute or two).

### 4. Verify end-to-end
Open your Vercel URL in a browser. The dashboard should load real data. If you see a CORS error in the console, double check `FRONTEND_URL` on Render matches your Vercel URL **exactly** (protocol + no trailing slash).

## Cold starts
Render's free tier spins down after ~15 min of inactivity — the first request after that takes 30-50s to wake back up. Hit your Render URL a minute or two before any live demo so it's warm.

## Next: Day 8
Buffer / pitch — record the 5-min demo, write Project Objectives + Build Challenges sections.
