# 9. Gaps and next actions

Found while checking the design document against the code on 24 Oct 2026 (`main` plus uncommitted OTP-resend and outbox work). Ids are referenced throughout the other docs.

Priority: **P0** = security or correctness that would block production, **P1** = should fix in the current milestone, **P2** = cleanup or hardening.

## Gap register

| Id | Pri | Area | Gap | Fix |
|---|---|---|---|---|
| ~~G-01~~ | ✅ Done | OTP / SMS | ~~Plaintext OTP stored in `sms_outbox.message` and never removed~~ | **Fixed (25 Oct 2026):** AES-256-GCM via a reusable `common.security.crypto.AesGcmCipher` (random IV per message), wrapped by `SmsMessageCipher` with its own key `SMS_OUTBOX_ENCRYPTION_KEY`. Message redacted to `[redacted]` once SENT or FAILED. Changeset 012 redacted old plaintext rows. Tamper test gave `AEADBadTagException` as expected. `failExpiredMessages` redacts too |
| G-28 | P2 | Outbox | All send errors are retried the same way, even permanent ones (decrypt failure, invalid number) | Classify errors as retryable or permanent; fail permanent ones immediately. Do it when adding a real SMS provider |
| ~~G-06~~ | ✅ Done | Reports | ~~No `owner_id`. Any authenticated user can read, update or delete any report (BOLA)~~ | **Fixed (24 Oct 2026):** changeset 008 adds `owner_id`. `AuthUser` + `@CurrentUser` give the logged-in user to controllers. All report queries filter by owner, and other users' reports return 404 |
| ~~G-02~~ | ✅ Done | Outbox | ~~`smsSender.send()` runs inside `@Transactional` `process()`~~ | **Fixed (24 Oct 2026):** `SmsOutboxProcessor` has no transaction. `SmsOutboxStateService` (separate bean) has short `loadForSending` / `markSent` / `recordFailure` transactions. Proved with `pg_stat_activity`: no more "idle in transaction" during a slow send |
| ~~G-03~~ | ✅ Done | Outbox | ~~No claim token or version. A stale worker overwrites a reclaimed row~~ | **Fixed (25 Oct 2026):** changeset 010 adds `claim_token`. Claim sets a new token, recovery clears it, and `markSent`/`recordFailure` lock the row by `(id, token, PROCESSING)`. A stale worker logs "Lease lost" and writes nothing. Verified with two instances |
| ~~G-04~~ | ✅ Done | Outbox | ~~Recovery cutoff hard-coded at 2 min, unrelated to any provider timeout~~ | **Fixed (25 Oct 2026):** `app.sms-outbox.lease` (`SMS_OUTBOX_LEASE`, default `2m`) in `SmsOutboxProperties`, validated at startup (zero or negative stops the app). Rule: lease much longer than the provider timeout |
| ~~G-05~~ | ✅ Done | Outbox | ~~Poison message loops forever (attempt increment rolled back, recovery does not count)~~ | **Fixed (24 Oct 2026):** attempt counted in the claim transaction (`attempt_count` = attempts started). Recovery sets `FAILED` once `attempt_count >= MAX_ATTEMPTS`. Accepted trade-off: a crash before the send still uses up an attempt (same as SQS receive count) |
| ~~G-07~~ | ✅ Done | API | ~~Unknown `userId` on verify/resend → `IllegalArgumentException` → 500~~ | **Fixed (24 Oct 2026):** dedicated `InvalidVerificationRequestException` → 400 `INVALID_VERIFICATION_REQUEST` with a generic message |
| ~~G-08~~ | ✅ Done | API | ~~Concurrent register with the same number → unique violation → 500~~ | **Fixed (24 Oct 2026):** `saveAndFlush` inside try/catch. Only `uk_app_user_mobile_number` is mapped to 409; other integrity errors still give 500. Verified with two parallel curls (201 + 409) |
| G-09 | P1 | Config | Only `LocalSmsSender` (`@Profile("local")`) exists, so the app cannot start in any other profile | Real provider sender, or a `@ConditionalOnMissingBean` no-op/fail-closed sender |
| G-15 | P1 | Security | No per-IP, per-prefix or global limits on `register` and resend. SMS pumping risk | Rate limiter (Bucket4j / gateway), CAPTCHA after N attempts, prefix allow-list |
| ~~G-17~~ | ✅ Done | Outbox | ~~Retries finish in ~30 s and ignore OTP expiry~~ | **Fixed (25 Oct 2026):** changeset 011 adds `expires_at` (set to the OTP expiry). Capped exponential backoff with jitter (`retry-base-delay` 5s, `retry-max-delay` 60s, validated at startup). Expired messages are never claimed or retried |
| ~~G-11~~ | ✅ Done | API | ~~`CreateReportRequest` has no `@Size`. Over-length values → DB error → 500~~ | **Fixed (24 Oct 2026):** `@Size` on the request DTO: item name 200, description 2000, city 100 (same as the DB columns). Over-length now gives 400 `VALIDATION_ERROR`. Validation is on the DTO, not the entity, because entity checks run too late (at save) |
| ~~G-12~~ | ✅ Done | Security | ~~`org.springframework.security: TRACE` in the base config~~ | **Fixed (25 Oct 2026):** removed; default `INFO`. Raise per run with `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG` |
| ~~G-13~~ | ✅ Done | Security | ~~JWT issuer not validated on decode~~ | **Fixed (25 Oct 2026):** `JwtValidators.createDefaultWithIssuer(JwtConfig.ISSUER)`; `JwtService` uses the same constant. Hand-made wrong-issuer token → 401 |
| G-14 | P2 | Data | Mobile number only trimmed. The same number in different formats becomes different accounts | Normalize to E.164 |
| G-16 | P2 | Data | Outbox index does not match the claim query, no CHECK constraints, no retention | P-3, P-6, P-7 |
| G-18 | P2 | Config | OTP validity, attempt limits, JWT TTL, outbox limits, lease are hard-coded | `@ConfigurationProperties` per module ([08](08-configuration-deployment.md)) |
| G-19 | P2 | Code | `OtpServiceImpl` uses `jakarta.transaction.Transactional`, everything else uses Spring's | Use `org.springframework.transaction.annotation.Transactional` |
| G-20 | P2 | Perf | `REQUIRES_NEW` in verify holds 2 connections per request | Size the pool for it, or restructure: commit the counter with a conditional update in the outer transaction and return a result instead of throwing |
| G-21 | P2 | Ops | One scheduler thread shared by processing and recovery. No graceful shutdown | `spring.task.scheduling.pool.size: 2`, graceful shutdown |
| ~~G-22~~ | ✅ Done | API | ~~1. Catch-all turns Spring 404/405/415 into 500~~ ✅ fixed 24 Oct 2026 (405 and 404 verified; 415 and `Allow` header not checked yet). ~~2. `HandlerMethodValidationException` message always says "pagination"~~ ✅ fixed 24 Oct 2026 (now returns `fieldErrors` per parameter). ~~3. Superseded OTP reported as "already used"~~ ✅ fixed 24 Oct 2026 (new `superseded_at` column, changeset 009). ~~4. `MAX_ATTEMPTS` returned as 400~~ ✅ decided 24 Oct 2026: keep 400 (429 means "retry later", but a locked OTP never unlocks), and give each OTP failure its own error code | All 4 parts done |
| G-10 | P2 | Code | Dead code: `OtpSmsEvent`/`OtpSmsEventListener`, unused `smsSender`/`eventPublisher` in `OtpServiceImpl`, unused `findTop50…`, stale TODO | Delete |
| G-23 | P2 | Ops | No correlation id, metrics, Actuator or readiness | [07](07-observability-operations.md) |
| G-24 | P2 | Code | `UserAlreayActiveException` typo. `ResentVerificationOTP*` DTO names. Field injection in the report module | Rename to `UserAlreadyActiveException` / `ResendVerificationOtp*`, use constructor injection |
| G-25 | P2 | Tests | Only report tests exist. OTP, auth and outbox flows have none. `contextLoads` needs a real DB and env. `application.yml` has `${SPRING_PROFILES_ACTIVE}` with no default, so tests fail unless the variable is set | Dedicated test session: Testcontainers, OTP concurrency tests, outbox scenarios ([05](05-sms-outbox.md#verification-scenarios)). Default the profile to `local` |
| ~~G-26~~ | ✅ Done | Perf | ~~`spring.jpa.open-in-view` is on by default: a DB connection stays open for the whole HTTP request~~ | **Fixed (25 Oct 2026):** `spring.jpa.open-in-view: false`. Safe because controllers return DTOs only |
| G-27 | P1 | Security | No CORS configuration. A browser front end on another origin (for example the React dev server) will be blocked | Explicit allow-list of front-end origins in `SecurityConfig`. Needed before the frontend |

## Status of the original design document's next actions

| # | Action from the design PDF | Status after code review |
|---|---|---|
| 1 | Confirm the native claim query no longer has `@Lock` | **Done.** `SmsOutboxRepository.claimPendingMessages` uses SQL `FOR UPDATE SKIP LOCKED` with no `@Lock`. (`@Lock` remains on the JPQL queries `findByIdForUpdate` and `findForVerification`, where it is correct) |
| 2 | Move provider calls outside DB transactions | **Done** (G-02) |
| 3 | Configurable lease, prevent stale-worker updates | **Done** (G-03, G-04) |
| 4 | Define retry semantics, timeouts, backoff, idempotency, ambiguous outcomes | **Done** (G-05, G-17). Provider idempotency key still open, for when a real provider is added |
| 5 | Verify OTP expiry, one-time use, attempt limits, rate limits, secure storage | **Verified** for `otp_challenge` ([04](04-auth-otp-security.md)). Open: G-01 (plaintext in outbox), G-15 (IP/global limits) |
| 6 | Run the manual verification scenarios | Open. Scenarios in [05](05-sms-outbox.md#verification-scenarios) |
| 7 | README with architecture, env vars, migrations, run commands, endpoints, recovery | **Done.** [README](../../README.md) + [runbook](../runbook/sms-outbox.md) |

## Suggested order of work

1. ~~**G-06 report ownership.**~~ ✅ Done. A strong interview example of "authentication is not authorization".
2. **Outbox refactor**, one step at a time: ~~G-02~~ ✅, ~~G-05~~ ✅, ~~G-04~~ ✅, ~~G-03~~ ✅, ~~G-17~~ ✅. Next: G-01. It is the core distributed-systems story of this project.
3. **G-01 OTP at rest**, together with the outbox refactor, since it touches the same table.
4. **Error mapping:** ~~G-07~~ ✅, ~~G-08~~ ✅, ~~G-11~~ ✅, ~~G-22~~ ✅ all done.
5. **G-15 rate limiting**, then **G-23 observability**.
6. **G-25 test session**, which also locks in everything above.
7. The remaining P2 cleanup items.

Record each fix in the [evidence log](../sde3/evidence-log.md): what broke, how you proved it, and how you fixed it.
