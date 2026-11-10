# 8. Configuration and deployment

## Configuration today (Implemented)

- `application.yml` reads all secrets from environment variables: `DB_*`, `JWT_SECRET`, `OTP_HMAC_SECRET`. No secrets are committed.
- `backend/scripts/run-local.sh` exports `backend/.env` (gitignored) and runs Gradle. Run it from `backend/`.
- Refresh token: `app.refresh-token.ttl` (default `7d`) and `app.refresh-token.cookie-secure` from `REFRESH_COOKIE_SECURE` (default `true`; set `false` only for local Safari over plain http).
- `OtpResendProperties` is a typed `@ConfigurationProperties` record. That is the pattern to copy for the remaining hard-coded values.
- `JwtConfig` and `OtpConfig` **fail fast** at startup if a secret is shorter than 32 bytes. Configuration errors show up at deploy time, not on the first request.

### Hard-coded values to move into configuration (G-18)

| Value | Where | Proposed key |
|---|---|---|
| OTP validity 5 min (also hard-coded in the SMS text) | `OtpServiceImpl` | `app.otp.validity` |
| Max failed OTP attempts 5 | `OtpVerificationTransaction` | `app.otp.max-failed-attempts` |
| JWT lifetime 15 min, issuer `milaavo` | `JwtService` | `app.jwt.access-token-ttl`, `app.jwt.issuer` |
| Outbox max attempts 5, batch size 50 | `SmsOutboxProcessor`, `SmsOutboxRepository` | `app.sms-outbox.max-attempts`, `app.sms-outbox.batch-size` |
| Recovery cutoff 2 min | `SmsOutboxRecoveryService` | `app.sms-outbox.lease` |
| BCrypt cost 12 | `PasswordConfig` | Keep in code, but document it |

Put them in one `@ConfigurationProperties` record per module with `@Validated` and `java.time.Duration` types (for example `validity: 5m`).

### Profiles

- `local`: `LocalSmsSender` (logs the message). This is the **only** `SmsSender` today, so other profiles fail at startup (G-09).
- Proposed: a `prod` (or provider-named) `SmsSender` implementation with timeouts, plus `application-local.yml` for local-only settings such as `DEBUG` logging. With that in place, the base `application.yml` holds safe production defaults.

## Build and CI (Proposed)

A GitHub Actions workflow for every push and PR:

1. Check out, set up JDK 17 (Temurin), cache Gradle.
2. `./gradlew build`, which compiles, runs the tests and builds the boot jar.
3. Integration tests with a PostgreSQL service container (or Testcontainers), so `contextLoads` and the Liquibase changesets are tested against a real database.
4. Optionally: `./gradlew dependencyCheckAnalyze` or GitHub Dependabot for vulnerable dependencies.

## Container (Proposed)

- `./gradlew bootBuildImage` builds an OCI image with Cloud Native Buildpacks, no Dockerfile needed. Alternatively, a multi-stage Dockerfile on `eclipse-temurin:17-jre` running as a non-root user.
- A `docker-compose.yml` with PostgreSQL for one-command local setup.
- JVM flags: `-XX:MaxRAMPercentage=75` so the heap follows the container memory limit.

Docker is a convenience here, not a prerequisite for learning the core Java and Spring material.

## Deployment checklist

- [ ] **Refresh cookie and sites.** The cookie is `SameSite=Strict`, so the frontend and the API must be on the **same site** (same registrable domain, for example `app.example.com` and `api.example.com`, or one origin with `/api` proxied). Free hosts such as `*.vercel.app` and `*.onrender.com` are different sites, and the cookie would never be sent. Options: proxy `/api` through the frontend host (simplest), or a custom domain for both. Avoid `SameSite=None`: browsers increasingly block third-party cookies.

- [ ] Build once and promote the same artifact (image digest) through environments.
- [ ] Inject secrets from a secret manager (AWS Secrets Manager, GCP Secret Manager, Vault). Rotate `JWT_SECRET` with a documented plan: with HS256 and no `kid`, rotation logs everyone out, which is acceptable at a 15-minute TTL.
- [ ] Run Liquibase with a dedicated migration identity. Review every changeset for locking or destructive operations (for example, `ADD COLUMN … NOT NULL` without a default rewrites the table on old PostgreSQL versions, and `CREATE INDEX` without `CONCURRENTLY` blocks writes).
- [ ] Use **expand → migrate → contract** for breaking schema changes, so old and new app versions both work during a rolling deploy.
- [ ] Size the Hikari pool: `instances × pool size` must be less than PostgreSQL `max_connections` minus a margin. Account for REQUIRES_NEW (2 connections per verify).
- [ ] Set timeouts: Hikari `connection-timeout`, JDBC `socketTimeout`, `statement_timeout` on the DB role, SMS provider connect/read timeouts.
- [ ] Terminate TLS at the load balancer or ingress. Use HTTPS only and enable HSTS.
- [ ] Use least-privilege DB users: the app user cannot `DROP`, and the migration user is separate.
- [ ] Wire liveness and readiness probes to Actuator ([07](07-observability-operations.md)).
- [ ] Enable graceful shutdown, and set the termination grace period longer than the shutdown timeout.
- [ ] Set CPU and memory limits, and load-test once at the expected peak.
- [ ] Use log redaction and encrypted backups, and **test a restore** at least once.
- [ ] Rollback plan: the application rolls back to the previous image. Schema changes roll forward (write a fix changeset). Never edit applied changesets.
