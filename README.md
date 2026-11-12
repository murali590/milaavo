# Milaavo

Milaavo reunites people with what they lost. Owners report lost items, finders report found items, and Milaavo matches the two using rules and, later, AI image understanding. It then connects both people privately so the item can be returned. See the [vision](docs/product/vision.md) and [requirements](docs/product/requirements.md).

**Live demo:** https://milaavo.vercel.app. Click **"Use demo account"** on the login page, or register: the demo shows the OTP on screen instead of sending an SMS. The backend runs on a free host that sleeps when idle, so the first request after a quiet spell can take up to a minute.

**What exists today:** a React web app and a Spring Boot API. Users register with a mobile number, verify it with an OTP, and log in (short-lived JWT access token plus a rotating refresh token in an `HttpOnly` cookie). They create, read, update and delete their own reports of type Lost, Found or Swapped. CI runs on every push, and the app is deployed (Vercel + Render + Neon, see [08](docs/design/08-configuration-deployment.md#live-deployment-implemented-12-nov-2026)). Matching, claims and AI are planned.

The project is also a learning vehicle for SDE3 interview preparation. The design docs explain *why* each decision was made, which failure modes exist, and which parts are still proposals.

> **Evidence labels used in all docs**
> - **Implemented**: verified in the current code on `main` (including uncommitted work at the time of writing, 24 Oct 2026).
> - **Proposed**: recommended design, not yet built.
> - **Gap**: a mismatch or risk found in the current code that should be fixed or consciously accepted.

---

## Tech stack

| Area | Choice |
|---|---|
| Language | Java 17 (Gradle toolchain) |
| Framework | Spring Boot 4.1.1 (Web MVC, Validation, Data JPA, Security, OAuth2 Resource Server) |
| Build | Gradle 9.7.1 via the wrapper (`./gradlew`) |
| Database | PostgreSQL, schema managed by Liquibase (`ddl-auto: validate`) |
| Auth | BCrypt (cost 12) passwords, HS256 JWT access tokens (15 min) |
| OTP | 6-digit `SecureRandom`, HMAC-SHA256 hash, 5 min validity, 5 attempts |
| SMS | Transactional outbox table + scheduled worker (`FOR UPDATE SKIP LOCKED`) |
| Boilerplate | Lombok |

## Quick start (local)

### 1. Prerequisites

- JDK 17 (the Gradle toolchain will use it)
- PostgreSQL 14+ running locally. `gen_random_uuid()` is used as a column default, which is built into PostgreSQL 13+.

```bash
createdb milaavo
```

### 2. Environment variables

All backend commands run from the `backend/` folder:

```bash
cd backend
```

Copy the template and fill it in:

```bash
cp .env.example .env
```

| Variable | Required | Notes |
|---|---|---|
| `DB_URL` | no | Defaults to `jdbc:postgresql://localhost:5432/milaavo` |
| `DB_USERNAME` | yes | |
| `DB_PASSWORD` | yes | |
| `JWT_SECRET` | yes | **Base64**-encoded, at least 32 bytes after decoding. Startup fails otherwise (`JwtConfig`). |
| `OTP_HMAC_SECRET` | yes | **Base64**-encoded, at least 32 bytes after decoding (`OtpConfig`). Use a different value from `JWT_SECRET`. |
| `SMS_OUTBOX_ENCRYPTION_KEY` | yes | **Base64**, exactly 32 bytes decoded (AES-256). Encrypts SMS outbox messages. Use its own value, and never change it while SMS are pending |
| `REFRESH_COOKIE_SECURE` | no | Default `true`. Set `false` only for local Safari over plain http |
| `SPRING_PROFILES_ACTIVE` | yes | Use `local`. `demo` is for the public deployment only (it shows the OTP in API responses and seeds a demo account). These are the only profiles with an `SmsSender` (`LocalSmsSender`), so any other profile fails at startup with a missing bean. |

Generate secrets with:

```bash
openssl rand -base64 32
```

`.env` is gitignored. Never commit real secrets.

### 3. Run

```bash
./scripts/run-local.sh
```

The script exports `.env` and runs `./gradlew bootRun`. Extra arguments are passed to Gradle, for example `./scripts/run-local.sh test`.

The API listens on **http://localhost:8091**. Liquibase applies all changesets at startup.

### 4. Try it

```bash
curl -s localhost:8091/api/health
```

```bash
curl -s -X POST localhost:8091/api/auth/register -H 'Content-Type: application/json' -d '{"name":"Asha","mobileNumber":"+919800000001","password":"correct-horse-battery"}'
```

With the `local` profile, the OTP is printed in the application log by `LocalSmsSender` within about 5 seconds (the outbox poll delay). Use it with the returned `userId` and `challengeId`:

```bash
curl -s -X POST localhost:8091/api/auth/verify-mobile -H 'Content-Type: application/json' -d '{"userId":"<userId>","challengeId":"<challengeId>","otp":"123456"}'
```

```bash
curl -s -X POST localhost:8091/api/auth/login -H 'Content-Type: application/json' -d '{"mobileNumber":"+919800000001","password":"correct-horse-battery"}'
```

```bash
curl -s localhost:8091/api/reports -H "Authorization: Bearer <accessToken>"
```

### Or run it with Docker

From `backend/`, with `.env` filled in (`DB_PASSWORD` must not be empty, because the Postgres image refuses an empty password):

```bash
docker compose up --build
```

This starts PostgreSQL 17 (on host port 5433) and the backend (on 8091). The backend waits until the database is healthy. `docker compose down` stops both; `docker compose down -v` also deletes the data.

### 5. Tests

```bash
./scripts/run-local.sh test
```

24 tests: `ReportServiceImplTest`, `RefreshTokenServiceTest` (unit), `ReportControllerTest` (`@WebMvcTest` with the real security config), and `MilaavoApplicationTests.contextLoads`, which starts the full app and needs the database and the environment variables above. CI (`.github/workflows/ci.yml`) runs the same `./gradlew build` against a Postgres service container on every push and pull request.

## API summary

Full contract, status codes and error codes: [docs/design/03-api.md](docs/design/03-api.md).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/health` | public | Liveness check |
| POST | `/api/auth/register` | public | Create user (`PENDING_VERIFICATION`) and send OTP |
| POST | `/api/auth/verify-mobile` | public | Verify OTP, activate the user |
| POST | `/api/auth/verify/resend-otp` | public | Issue a new OTP (cooldown and window limits) |
| POST | `/api/auth/verify/start` | public | Restart verification with mobile and password, for users who never finished it |
| POST | `/api/auth/login` | public | Exchange mobile and password for a JWT |
| POST | `/api/auth/refresh` | public (cookie) | New access token from the `HttpOnly` refresh cookie; rotates the cookie |
| POST | `/api/auth/logout` | public (cookie) | Revoke the refresh token and clear the cookie |
| POST | `/api/reports` | Bearer | Create a lost-item report |
| GET | `/api/reports?page=&size=` | Bearer | My reports, paged, newest first |
| GET | `/api/reports/{id}` | Bearer | Report detail |
| PUT | `/api/reports/{id}` | Bearer | Replace report fields |
| DELETE | `/api/reports/{id}` | Bearer | Delete report |

## Configuration reference

From `backend/src/main/resources/application.yml`:

| Key | Default | Meaning |
|---|---|---|
| `server.port` | `8091` | HTTP port |
| `app.otp.resend.cooldown-seconds` | `60` | Minimum gap between resends |
| `app.otp.resend.max-resends` | `3` | Max resends per window |
| `app.otp.resend.window-minutes` | `15` | Rolling window for `max-resends` |
| `app.sms-outbox.poll-delay-ms` | `5000` | Delay between outbox claim runs |
| `app.sms-outbox.recovery-delay-ms` | `30000` | Delay between stale-PROCESSING recovery runs |
| `app.sms-outbox.lease` | `2m` (env `SMS_OUTBOX_LEASE`) | How long a worker owns a claimed message before recovery may take it back. Must be positive, or the app won't start |
| `app.sms-outbox.retry-base-delay` | `5s` | First retry wait. Each retry doubles it, with jitter |
| `app.sms-outbox.retry-max-delay` | `60s` | Longest retry wait. Must be ≥ base delay |
| `app.cors.allowed-origins` | `http://localhost:5173` (env `CORS_ALLOWED_ORIGINS`, comma-separated) | Front-end origins allowed to call the API from a browser. `*` is rejected at startup |
| `spring.jpa.open-in-view` | `false` | DB connections are held only during service calls, not the whole request (G-26) |

Security logging uses Spring's default (`INFO`). To see the security filter details for one run: `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG ./scripts/run-local.sh`.

Hard-coded values that should become configuration are listed in [docs/design/09-gaps-and-next-actions.md](docs/design/09-gaps-and-next-actions.md).

## Project layout

```
milaavo/
├── backend/              Spring Boot API (Gradle project: build.gradle, gradlew, src/, scripts/, .env)
├── frontend/             React + TypeScript web app (Vite)
└── docs/                 product, design, runbook and SDE3 docs
```

Backend packages (`backend/src/main/java/com/milaavo`):

```
├── auth/                 register, login, verify mobile, resend OTP, JWT issuing
├── otp/                  OTP challenge issue/verify, HMAC hashing, resend limits
├── notification/sms/     outbox entity, enqueue, claim, process, recover, SmsSender, message encryption
├── report/               report CRUD (Lost / Found / Swapped), owner check, type filter, pagination
├── common/dto/           ApiErrorResponse
├── common/security/      AuthUser, @CurrentUser, JWT → AuthUser converter, AES-GCM cipher
├── config/               security filter chain, CORS, JWT, password encoder, 401/403 JSON, demo profile seeder
├── exception/            GlobalExceptionHandler and shared exceptions
└── health/               /api/health
```

Liquibase: `backend/src/main/resources/db/changelog` (master + changesets 001–014).

## Documentation

**Product**

- [Vision](docs/product/vision.md): problem, personas, goals, constraints (zero cost, web first), phases
- [Requirements](docs/product/requirements.md): functional and non-functional requirements, target domain model, AI and deployment requirements, acceptance criteria

**Design**

1. [Architecture](docs/design/01-architecture.md): modular monolith, modules, dependency rules
2. [Data model](docs/design/02-data-model.md): tables, constraints, indexes, persistence rules
3. [API](docs/design/03-api.md): endpoints, status codes, error format
4. [Authentication, OTP and security](docs/design/04-auth-otp-security.md)
5. [SMS outbox](docs/design/05-sms-outbox.md): current flow, failure modes, target design
6. [Transactions and concurrency](docs/design/06-transactions-concurrency.md)
7. [Observability and operations](docs/design/07-observability-operations.md)
8. [Configuration and deployment](docs/design/08-configuration-deployment.md)
9. [Gaps and next actions](docs/design/09-gaps-and-next-actions.md): prioritized list of fixes found while documenting
10. [Frontend](docs/design/10-frontend.md): web app decisions, token storage, folder structure, build plan

**Runbook**

- [SMS outbox runbook](docs/runbook/sms-outbox.md): diagnostic SQL and recovery steps

**SDE3 preparation**

- [12-week roadmap](docs/sde3/roadmap.md)
- [Concept checklist](docs/sde3/concept-checklist.md)
- [Interview prompts, answered with Milaavo](docs/sde3/interview-prompts.md)
- [Weekly evidence log](docs/sde3/evidence-log.md)
