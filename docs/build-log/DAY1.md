# Day 1 — Data Model + Seed Data

## What's built
- Spring Boot 4.1.1 project (Java 25, Maven)
- 5 entities matching the plan's data model: `Customer`, `Payment`, `FailureEvent`, `RetryAttempt`, `RecoveryMessage`
- A repository per entity (Spring Data JPA)
- `SeedDataRunner` — runs once on startup, generates 60 customers and roughly 150 failed payments, each with a `FailureEvent` drawn from a weighted distribution (INSUFFICIENT_FUNDS ~36%, BANK_DECLINE ~27%, EXPIRED_CARD ~18%, GATEWAY_TIMEOUT ~18%) — close to how these actually distribute in real dunning data
- `RetryAttempt` and `RecoveryMessage` entities exist now but stay empty until Day 2 (naive scheduler) and Day 3 (AI scheduler)

## Setup

1. **Get your Supabase connection string**
   Supabase dashboard → your project → Project Settings → Database → Connection string → copy the **URI**, using the **Session pooler** (port 5432).

2. **Set env vars.** Copy `.env.example` to `.env` for reference, then set the three vars in your PowerShell session before running (Spring Boot reads real env vars, it doesn't auto-load `.env` files):
   ```powershell
   $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://<host>:5432/postgres"
   $env:SPRING_DATASOURCE_USERNAME = "postgres.<project-ref>"
   $env:SPRING_DATASOURCE_PASSWORD = "<your-db-password>"
   ```
   Note: these only last for the current PowerShell session — you'll need to re-set them each time you open a new terminal (or set them permanently via System Properties → Environment Variables).

3. **Run it:**
   ```bash
   mvn spring-boot:run
   ```

4. **Confirm it worked** — you should see this in the console:
   ```
   Seeding customers and failed payments...
   Seeded 60 customers and 150 failed payments.
   ```

5. **Verify in Supabase** — open Table Editor, check `customers`, `payments`, and `failure_events` are populated.

## Next: Day 2
Ingest endpoint (`POST /api/failures`) + naive fixed-interval retry scheduler (24h → 72h → 7d, 3 attempts).
