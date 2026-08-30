// Thin fetch wrapper for the dashboard API. Uses relative paths ('/api/...')
// so the Vite dev-server proxy (see vite.config.js) forwards to the Spring
// Boot backend on localhost:8080 — no CORS setup needed for local dev.

async function getJson(path) {
  const response = await fetch(path);
  if (!response.ok) {
    throw new Error(`${path} failed: ${response.status} ${response.statusText}`);
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
