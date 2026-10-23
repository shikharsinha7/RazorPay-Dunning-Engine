# Day 3 — AI Layer: Gemini Classification + AI Retry Track

## What's built
- `GeminiClient` — calls Gemini 2.5 Flash with a structured JSON response schema (not free-text parsing) to classify a failure and recommend: retry window (hours), channel (EMAIL/SMS), whether to prompt for an alternate payment method, confidence, and a one-sentence reasoning.
- `FailureClassificationService` — wraps the Gemini call with a rule-based fallback per failure code. If the API key is missing, Gemini is down, or a call errors out, it degrades to sensible hardcoded defaults instead of breaking ingest. **This matters for your live demo** — a flaky network at the venue shouldn't take down the whole thing.
- `AiRetryPlanner` — schedules 3 AI-driven retry attempts using exponential backoff (window, 2x window, 4x window) with ±15% jitter around the AI-recommended base window, and channel diversity (starts on the AI's recommended channel, alternates on later attempts) — versus the naive track's blind, fixed, all-EMAIL calendar.
- **Every ingested failure now produces both tracks** (AI + NAIVE) for the same payment, so the dashboard can compare them head-to-head later.
- **Design fix:** `payment.status` is now driven only by the **AI** track — that's what would actually run in production. NAIVE keeps executing and resolving independently, but is now a pure shadow simulation that only exists to produce comparison stats; it no longer touches `payment.status`.
- `RetryScheduler` (replaces `NaiveRetryScheduler`) now polls and executes due attempts from **both** strategies in one pass.

## Note: Jackson 3, not Jackson 2
Spring Boot 4 defaults to **Jackson 3**, which renamed its base package from `com.fasterxml.jackson` to `tools.jackson` (annotations like `@JsonProperty` stay in the old namespace for backward compatibility, but `ObjectMapper`/`JsonNode` moved, and `ObjectMapper` itself is superseded by the immutable `JsonMapper`). `GeminiClient` uses `tools.jackson.databind.json.JsonMapper` and `tools.jackson.databind.JsonNode` for this reason — if you ever add your own Jackson usage elsewhere in this project, use the same `tools.jackson` imports, not `com.fasterxml.jackson.databind`.

## Update: migrated to the Interactions API + gemini-3.6-flash
`gemini-2.5-flash` was retired for new API keys, and Google's current recommended endpoint is the **Interactions API** (`POST /v1beta/interactions`), not the older `generateContent` endpoint. This changes more than just the model name:
- Endpoint: `https://generativelanguage.googleapis.com/v1beta/interactions` (model is a body field now, not part of the URL path)
- Auth: `x-goog-api-key` header instead of a `?key=` query param
- Request shape: `input` (a plain string) instead of `contents`/`parts`; `response_format: { type, mime_type, schema }` instead of `generationConfig.responseMimeType/responseSchema`
- Schema types are lowercase JSON Schema (`"type": "object"`) — the old `generateContent` endpoint used uppercase OpenAPI-style types (`"OBJECT"`)
- Response shape: a `steps` array (each with a `type` like `model_output`) replaced the old flat `candidates` array, following a breaking schema change Google shipped in May 2026

`FailureClassificationService` (the fallback wrapper) didn't need to change at all — it only calls `geminiClient.classify(...)`, and that method signature is untouched.

## Setup: get a free Gemini API key
1. Go to [aistudio.google.com](https://aistudio.google.com)
2. Click "Get API key" → create one (no billing/card required for the free tier — 15 requests/min, 1,500/day)
3. Set it in PowerShell before running:
   ```powershell
   $env:GEMINI_API_KEY = "your-key-here"
   ```
   (Same caveat as the DB vars — this only lasts the current PowerShell session.)

If you skip this step entirely, the app still works — `FailureClassificationService` will log a warning and use the rule-based fallback for every ingest. Good to verify both paths work.

## Test it (PowerShell)

**1. Ingest a new failure and inspect both tracks:**
```powershell
$body = @{
    customerName  = "AI Test User"
    customerEmail = "ai.test@example.com"
    amount        = 4200.00
    currency      = "INR"
    failureCode   = "EXPIRED_CARD"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/failures" -Method Post -Body $body -ContentType "application/json"
$response | ConvertTo-Json -Depth 10
```
You should see 6 `retryAttempts` — 3 `NAIVE` (all EMAIL, fixed 24h/96h/264h schedule) and 3 `AI` (channel varies, and for `EXPIRED_CARD` specifically, `aiSuggestAltPaymentMethod` should be `true` and the window should be short — a couple hours, not days).

**2. Check the console log** — you'll see either a real Gemini call succeed, or (if no key set) a line like:
```
WARN ... Gemini classification failed (...) — using rule-based fallback for EXPIRED_CARD
```

**3. Force-run and confirm the AI-vs-naive status split:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/retries/force-run" -Method Post
Invoke-RestMethod -Uri "http://localhost:8080/api/payments/<id-from-step-1>" | ConvertTo-Json -Depth 10
```
Confirm: `payment.status` reflects only what the `AI` attempts resolved to — even if a `NAIVE` attempt recovered while the AI attempts all failed (or vice versa), `status` should match the AI track's outcome, not NAIVE's.

## Next: Day 4
Message generation (Gemini writes the actual recovery message per attempt) + real email sending via Resend for the AI track.
