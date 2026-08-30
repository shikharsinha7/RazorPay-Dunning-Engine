# Day 2 — Ingest Endpoint + Naive Retry Track

## What's built
- `POST /api/failures` — simulates a gateway webhook for a failed payment. Finds/creates the customer, records the `Payment` + `FailureEvent`, and schedules 3 **naive** retry attempts on a fixed calendar (24h, 96h, 264h from the failure — a blind schedule that ignores the failure reason).
- `NaiveRetryScheduler` — polls every minute for naive attempts whose `scheduledAt` has passed and executes them.
- `RetryExecutionService` — resolves each attempt's outcome via `RecoveryOutcomeSimulator` (weighted by failure code, since there's no real gateway), updates the payment to `RECOVERED`/`LOST`, and skips any remaining attempts once one succeeds.
- `GET /api/payments` / `GET /api/payments/{id}` — inspect ingested payments and their retry attempts.
- `POST /api/admin/retries/force-run` — **demo-only**: executes every pending attempt immediately, ignoring `scheduledAt`, so you don't wait real days to see outcomes.

## Design note for later
`RecoveryOutcomeSimulator` already has an `AI` strategy multiplier (1.6x) sitting next to `NAIVE` (1.0x) — that's the mechanism the Day 3-4 AI track will plug into to produce the recovery-rate lift on the dashboard. Nothing here needs to change when that lands.

## Test it (PowerShell)

**1. Ingest a failed payment:**
```powershell
$body = @{
    customerName  = "Test User"
    customerEmail = "test.user@example.com"
    amount        = 2500.00
    currency      = "INR"
    failureCode   = "GATEWAY_TIMEOUT"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/failures" -Method Post -Body $body -ContentType "application/json"
```
You should get back the created payment with `status: "FAILED"` and 3 `retryAttempts`, all `outcome: "PENDING"`.

**2. Force-run all pending retries (don't wait for real scheduling):**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/retries/force-run" -Method Post
```

**3. Check the payment again — status should now be `RECOVERED` or `LOST`:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/payments/<id-from-step-1>"
```

**4. See everything at once:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/payments" | ConvertTo-Json -Depth 5
```
(Your Day 1 seed data won't have retry attempts yet — only payments ingested through `/api/failures` get scheduled. That's expected; seed data was for testing the data model, not the retry flow.)

## Incident: stale PostgreSQL check constraint on `outcome`

If you ingested a payment and ran `force-run` and hit:
```
ERROR: new row for relation "retry_attempts" violates check constraint "retry_attempts_outcome_check"
```
that's because Hibernate ORM auto-generates a `CHECK` constraint from `@Enumerated(EnumType.STRING)` fields at table-creation time — and `ddl-auto: update` never refreshes that constraint when the enum later gains a new value. `SKIPPED` was added to `RetryOutcome` after the `retry_attempts` table already existed in Supabase, so the live constraint was still frozen to the original 3 values. Confirmed as a known Hibernate limitation, not a code bug (tracked upstream: spring-data-jpa#3699).

**Fix applied:**
- `RetryAttempt.outcome` now has an explicit `columnDefinition = "varchar(20)"`, which stops Hibernate from auto-generating this kind of constraint going forward.
- The stale constraint on your existing Supabase table still needs to be dropped manually — see the PowerShell steps below. This only needs to be run once.

## Applying the fix to your existing Supabase database

You have Docker installed, so run this in PowerShell (no local `psql` install needed) — replace `<HOST>`, `<PROJECT_REF>`, and `<PASSWORD>` with your Supabase Session pooler values from `.env`:

```powershell
docker run --rm postgres:17 psql "postgresql://postgres.<PROJECT_REF>:<PASSWORD>@<HOST>:5432/postgres" -c "ALTER TABLE retry_attempts DROP CONSTRAINT IF EXISTS retry_attempts_outcome_check;"
```

You should see:
```
ALTER TABLE
```

Alternative if that's slow to pull the Docker image the first time: open your Supabase project → SQL Editor → paste and run the same `ALTER TABLE ... DROP CONSTRAINT ...` line above.

## Retest

```powershell
mvn spring-boot:run
```
Then re-run the force-run test from the Test section above with a **new** ingested payment. Attempt #3 should now save as `SKIPPED` instead of erroring.

## Next: Day 3
AI layer — Gemini integration for failure classification + retry strategy recommendation, wired into a new AI retry track running alongside the naive one.

