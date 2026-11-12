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

- `local`: `LocalSmsSender` (logs the message).
- `demo` (Implemented, 11 Nov 2026): for the free public deployment. Also uses `LocalSmsSender`, and sets `app.demo.show-otp: true`, so register, resend and verify-start responses include a `demoOtp` field (hidden by `@JsonInclude(NON_NULL)` everywhere else). `DemoAccountSeeder` creates a verified public demo account (`+919999900001` / `milaavo-demo-2026`, fake data only) with 3 sample reports, once. **Never use this profile with real users.**
- `local` and `demo` are the only profiles with an `SmsSender`, so any other profile fails at startup (G-09).
- Proposed: a `prod` (or provider-named) `SmsSender` implementation with timeouts, plus `application-local.yml` for local-only settings such as `DEBUG` logging.

## Build and CI (Implemented, 11 Nov 2026)

`.github/workflows/ci.yml` runs on every push to `main` and every pull request. Permissions are read-only, and a newer push cancels the older run on the same branch.

| Job | Steps | Notes |
|---|---|---|
| `frontend` | `npm ci`, `npm run lint`, `npm run build` (`tsc -b` + `vite build`) | Node 22, npm cache keyed on `package-lock.json` |
| `backend` | `./gradlew build` (compile, all tests, boot jar) | JDK 17 Temurin, `gradle/actions/setup-gradle` cache. A **`postgres:17` service container** runs next to the job, so `contextLoads` applies every Liquibase changeset to an empty database. `JWT_SECRET`, `OTP_HMAC_SECRET` and `SMS_OUTBOX_ENCRYPTION_KEY` are **generated with `openssl rand` in each run** and masked: nothing is stored in GitHub. The HTML test report is uploaded only on failure |
| `docker` | `docker build -t milaavo-backend:<git sha> backend` | `needs: backend`. Proves the Dockerfile still builds. Pushing the image comes with CD |

Tests (24): `ReportServiceImplTest` (owner check: someone else's report is 404 and nothing is written), `ReportControllerTest` (`@WebMvcTest` with the **real** `SecurityConfig`; only `JwtDecoder` is mocked, so a Bearer header flows through `AuthUserJwtConverter` to `@CurrentUser`), `RefreshTokenServiceTest` (hash-only storage, rotation, reuse detection revokes the family), `contextLoads`.

Spring Boot 4 notes: security in test slices needs `spring-boot-starter-security-test`; slice tests don't run `@ConfigurationPropertiesScan` (use `@EnableConfigurationProperties`); and `spring.profiles.active: ${SPRING_PROFILES_ACTIVE}` has no default, so slice tests set `spring.profiles.active=test`.

Proposed: Dependabot for vulnerable dependencies, and Testcontainers for repository-level integration tests (locking, `noRollbackFor`).

## Container (Implemented)

- `backend/Dockerfile`: multi-stage. Build stage `eclipse-temurin:17-jdk` copies the Gradle files first and downloads dependencies (a cached layer), then copies `src` and runs `bootJar`. Run stage `eclipse-temurin:17-jre`, non-root user `milaavo` (uid 1001), only `app.jar`, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75` so the heap follows the container memory limit, exec-form `ENTRYPOINT`.
- `backend/.dockerignore` keeps `.env`, `build/` and IDE files out of the build context.
- `backend/compose.yaml`: `db` (`postgres:17`, published on host port **5433**, `pg_isready` healthcheck, `pgdata` volume) and `backend` (built from the Dockerfile, `env_file: .env`, `DB_URL` points at `db:5432`, starts only when `db` is healthy, healthcheck on `/actuator/health/readiness`). Run from `backend/`: `docker compose up --build`. `docker compose down -v` also deletes the data.

## Live deployment (Implemented, 12 Nov 2026)

Free hosting. The demo is public, so it runs the `demo` profile with fake data only.

```
Browser ──▶ https://milaavo.vercel.app/          React build on Vercel's CDN
        ──▶ https://milaavo.vercel.app/api/...  Vercel rewrite ──▶ https://milaavo-api.onrender.com/api/...
                                                                    Docker container on Render (Singapore)
                                                                          │
                                                                          ▼
                                                                    Neon PostgreSQL 17 (Singapore)
```

| Part | Host | Settings |
|---|---|---|
| Database | Neon free | Database `milaavo`, **direct** endpoint (not `-pooler`), because the app has its own Hikari pool and Liquibase is safest on a direct connection. JDBC URL ends in `?sslmode=require` (libpq's `channel_binding` option is dropped; the JDBC driver uses other names). Compute suspends after 5 min idle and wakes in under a second |
| Backend | Render free web service | Built from `backend/Dockerfile`, root directory `backend`. Env: `SPRING_PROFILES_ACTIVE=demo`, `PORT=8091` (Render routes to `PORT`), `DB_*` from Neon, **new** secrets generated for production (never the local ones), `CORS_ALLOWED_ORIGINS=https://milaavo.vercel.app`. Health check path `/actuator/health/readiness`. Auto-deploys on every push to `main`. Sleeps after 15 min idle; the next request waits for a cold start |
| Frontend | Vercel | Root directory `frontend`, Vite preset. `frontend/vercel.json` rewrites `/api/:path*` to Render, then sends every other path to `/index.html` (SPA fallback, so a reload on `/reports/123` works). `frontend/.env.production` sets `VITE_API_BASE_URL=` (empty), so the app calls relative `/api/...` URLs. `VITE_DEMO_MOBILE` and `VITE_DEMO_PASSWORD` are set in Vercel |

**Why the proxy:** the refresh cookie is `SameSite=Strict`. The browser only ever talks to `milaavo.vercel.app`, so the cookie is first-party and same-site, and there is no CORS preflight. `CORS_ALLOWED_ORIGINS` is still needed: the browser sends `Origin: https://milaavo.vercel.app` on POST, Vercel forwards it, and Spring sees an origin that differs from its own host. A foreign origin gets 403.

**Why no Docker image for the frontend:** after `npm run build` the frontend is static files that the browser runs. A CDN serves them; there is no server process to package. `VITE_*` values are baked in at build time, so changing them needs a rebuild, and they must never hold secrets.

Verified on 12 Nov 2026 through the proxy: deep link returns `index.html`; login returns 200 with `milaavo_rt; Path=/api/auth; Secure; HttpOnly; SameSite=Strict`; refresh with the cookie 200; logout 204; login with `Origin: https://evil.example` 403; the production bundle contains no `localhost:8091`.

**At scale on AWS (not built, for comparison):** S3 + CloudFront for the frontend, a CloudFront `/api/*` behaviour to the backend (the same same-site trick), ECS Fargate in private subnets behind an ALB across 2 AZs, RDS PostgreSQL Multi-AZ, secrets in Secrets Manager, images in ECR, and GitHub Actions deploying through OIDC with no stored AWS keys.

## Deployment checklist

- [x] **Refresh cookie and sites.** The cookie is `SameSite=Strict`, so the frontend and the API must be on the **same site** (same registrable domain, for example `app.example.com` and `api.example.com`, or one origin with `/api` proxied). Free hosts such as `*.vercel.app` and `*.onrender.com` are different sites, and the cookie would never be sent. **Done** by proxying `/api` through Vercel (see Live deployment). Avoid `SameSite=None`: browsers increasingly block third-party cookies.
- [x] Liveness and readiness probes on Actuator; the host's health check uses readiness.
- [x] Production secrets are separate from local ones and live only in the host's environment settings.

- [ ] Build once and promote the same artifact (image digest) through environments.
- [ ] Inject secrets from a secret manager (AWS Secrets Manager, GCP Secret Manager, Vault). Rotate `JWT_SECRET` with a documented plan: with HS256 and no `kid`, rotation logs everyone out, which is acceptable at a 15-minute TTL.
- [ ] Run Liquibase with a dedicated migration identity. Review every changeset for locking or destructive operations (for example, `ADD COLUMN … NOT NULL` without a default rewrites the table on old PostgreSQL versions, and `CREATE INDEX` without `CONCURRENTLY` blocks writes).
- [ ] Use **expand → migrate → contract** for breaking schema changes, so old and new app versions both work during a rolling deploy.
- [ ] Size the Hikari pool: `instances × pool size` must be less than PostgreSQL `max_connections` minus a margin. Account for REQUIRES_NEW (2 connections per verify).
- [ ] Set timeouts: Hikari `connection-timeout`, JDBC `socketTimeout`, `statement_timeout` on the DB role, SMS provider connect/read timeouts.
- [ ] Terminate TLS at the load balancer or ingress. Use HTTPS only and enable HSTS.
- [ ] Use least-privilege DB users: the app user cannot `DROP`, and the migration user is separate.
- [ ] Enable graceful shutdown, and set the termination grace period longer than the shutdown timeout.
- [ ] Set CPU and memory limits, and load-test once at the expected peak.
- [ ] Use log redaction and encrypted backups, and **test a restore** at least once.
- [ ] Rollback plan: the application rolls back to the previous image. Schema changes roll forward (write a fix changeset). Never edit applied changesets.
