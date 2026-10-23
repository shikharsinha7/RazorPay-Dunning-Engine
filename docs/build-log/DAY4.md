# Day 4 — Message Generation + Real Email (Resend)

## What's built
- `GeminiClient.generateMessage(...)` — a second Interactions API call (reusing the same plumbing as classification) that writes a short, personalized recovery message: subject + body, tailored to the failure reason, amount, and whether an alternate payment method should be requested.
- `MessageGenerationService` — wraps that call with a template-based fallback (same reliability pattern as `FailureClassificationService`).
- `ResendEmailService` — sends real email via Resend's API.
- `RecoveryMessageService` — orchestrates it all: generates the message, decides whether this attempt earns a **real** send, and persists a `RecoveryMessage` row either way.
- **Every executed (non-skipped) retry attempt now produces a message** — wired into `RetryExecutionService.execute()`.
- `GET /api/payments/{id}/messages` — inspect generated messages and their delivery provider.

## Design: who actually gets emailed
Only **AI-strategy + EMAIL-channel** attempts trigger a real Resend send. Everything else (the NAIVE shadow track, and any SMS attempt — there's no free real-SMS provider wired up) still generates and stores message content, for dashboard/comparison purposes, but is recorded with `provider: "simulated"`. This keeps real email quota spent only on the track that reflects what would actually run in production, and avoids double-emailing a customer once for AI and again for the NAIVE shadow run of the same failure.

If a real send fails for any reason (bad key, Resend outage, rate limit), it's caught and recorded as `"simulated"` rather than breaking the retry — same reliability posture as the Gemini fallback.

## Setup: get a free Resend API key
1. Go to [resend.com](https://resend.com) → sign up → API Keys → create one
2. No domain verification needed for testing — sending from `onboarding@resend.dev` works immediately
3. Set it in PowerShell:
   ```powershell
   $env:RESEND_API_KEY = "your-key-here"
   ```
   Free tier: 100 emails/day, 3,000/month — plenty for a hackathon demo.

If you skip this, everything still works — messages get generated and stored, just marked `"simulated"` instead of actually sent.

## Test it (PowerShell)

**1. Ingest a failure with an email you can actually check** (use your own inbox to see the real send):
```powershell
$body = @{
    customerName  = "Message Test"
    customerEmail = "your-real-email@example.com"
    amount        = 1800.00
    currency      = "INR"
    failureCode   = "EXPIRED_CARD"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/failures" -Method Post -Body $body -ContentType "application/json"
$response.id
```

**2. Force-run to trigger execution and message generation:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/retries/force-run" -Method Post
```

**3. Check the generated messages:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/payments/<id-from-step-1>/messages" | ConvertTo-Json -Depth 10
```
You should see multiple messages — some with `provider: "resend"` (the AI+EMAIL attempts) and some `"simulated"` (NAIVE attempts, and any SMS attempts). Check your inbox for the real one — `EXPIRED_CARD` should produce a message prompting you to update your payment method.

## Next: Day 5
Dashboard APIs — recovery funnel, AI-vs-naive comparison stats — then the React frontend scaffold.
