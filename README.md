# Milaavo

Milaavo reunites people with what they lost. Owners report lost items, finders report found items, and Milaavo matches the two using rules and, later, AI image understanding. It then connects both people privately so the item can be returned. See the [vision](docs/product/vision.md) and [requirements](docs/product/requirements.md).

**What exists today** (phase 0): users register with a mobile number, verify it with an SMS OTP, log in to get a JWT, and create, read, update and delete lost-item reports. Found reports, matching, claims, the web UI, cloud deployment and AI are planned.

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
| `SPRING_PROFILES_ACTIVE` | yes | Use `local`. Today the only `SmsSender` implementation is `LocalSmsSender` (`@Profile("local")`), so any other profile fails at startup with a missing bean. |

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

### 5. Tests

```bash
./gradlew test
```

`ReportControllerTest` and `ReportServiceImplTest` are unit/slice tests. `MilaavoApplicationTests.contextLoads` starts the full context, so it needs the database and the environment variables above (run it through `./scripts/run-local.sh test`).

## API summary

Full contract, status codes and error codes: [docs/design/03-api.md](docs/design/03-api.md).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/health` | public | Liveness check |
| POST | `/api/auth/register` | public | Create user (`PENDING_VERIFICATION`) and send OTP |
| POST | `/api/auth/verify-mobile` | public | Verify OTP, activate the user |
| POST | `/api/auth/verify/resend-otp` | public | Issue a new OTP (cooldown and window limits) |
| POST | `/api/auth/login` | public | Exchange mobile and password for a JWT |
| POST | `/api/reports` | Bearer | Create a lost-item report |
| GET | `/api/reports?page=&size=` | Bearer | Paged list, newest first |
| GET | `/api/reports/{id}` | Bearer | Report detail |
| PUT | `/api/reports/{id}` | Bearer | Replace report fields |
| DELETE | `/api/reports/{id}` | Bearer | Delete report |

## Configuration reference

From `src/main/resources/application.yml`:

| Key | Default | Meaning |
|---|---|---|
| `server.port` | `8091` | HTTP port |
| `app.otp.resend.cooldown-seconds` | `60` | Minimum gap between resends |
| `app.otp.resend.max-resends` | `3` | Max resends per window |
| `app.otp.resend.window-minutes` | `15` | Rolling window for `max-resends` |
| `app.sms-outbox.poll-delay-ms` | `5000` | Delay between outbox claim runs |
| `app.sms-outbox.recovery-delay-ms` | `30000` | Delay between stale-PROCESSING recovery runs |
| `logging.level.org.springframework.security` | `TRACE` | Very noisy. Lower it outside debugging (see Gap G-12) |

Hard-coded values that should become configuration are listed in [docs/design/09-gaps-and-next-actions.md](docs/design/09-gaps-and-next-actions.md).

## Project layout

```
src/main/java/com/milaavo
├── auth/                 register, login, verify mobile, resend OTP, JWT issuing
├── otp/                  OTP challenge issue/verify, HMAC hashing, resend limits
├── notification/sms/     outbox entity, enqueue, claim, process, recover, SmsSender
├── report/               lost-item report CRUD and pagination
├── common/dto/           ApiErrorResponse
├── config/               security filter chain, JWT, password encoder, 401/403 JSON
├── exception/            GlobalExceptionHandler and shared exceptions
└── health/               /api/health
src/main/resources/db/changelog   Liquibase master + changesets 001–007
```

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

**Runbook**

- [SMS outbox runbook](docs/runbook/sms-outbox.md): diagnostic SQL and recovery steps

**SDE3 preparation**

- [12-week roadmap](docs/sde3/roadmap.md)
- [Concept checklist](docs/sde3/concept-checklist.md)
- [Interview prompts, answered with Milaavo](docs/sde3/interview-prompts.md)
- [Weekly evidence log](docs/sde3/evidence-log.md)
