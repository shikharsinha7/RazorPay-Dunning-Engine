// Thin fetch wrapper for the dashboard API.
//
// Local dev: VITE_API_BASE_URL is unset, so paths stay relative ('/api/...')
// and the Vite dev-server proxy (see vite.config.js) forwards to Spring Boot
// on localhost:8080 — no CORS setup needed.
//
// Production (Vercel): VITE_API_BASE_URL is set to the deployed Render URL
// at build time, so requests go directly cross-origin to the real backend
// (CORS handled there — see WebConfig.java).
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

async function getJson(path) {
  const url = `${API_BASE}${path}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`${url} failed: ${response.status} ${response.statusText}`);
  }
  return response.json();
}

export function getFunnel() {
  return getJson('/api/dashboard/funnel');
}

export function getComparison() {
  return getJson('/api/dashboard/comparison');
}

export function getTimeline() {
  return getJson('/api/dashboard/timeline');
}

export function getPayments() {
  return getJson('/api/payments');
}
