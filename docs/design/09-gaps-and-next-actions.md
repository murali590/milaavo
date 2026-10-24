# 9. Gaps and next actions

Found while checking the design document against the code on 24 Oct 2026 (`main` plus uncommitted OTP-resend and outbox work). Ids are referenced throughout the other docs.

Priority: **P0** = security or correctness that would block production, **P1** = should fix in the current milestone, **P2** = cleanup or hardening.

## Gap register

| Id | Pri | Area | Gap | Fix |
|---|---|---|---|---|
| G-01 | P0 | OTP / SMS | Plaintext OTP stored in `sms_outbox.message` and never removed | Encrypt or template the message, null it after completion, add retention ([05](05-sms-outbox.md#changes-in-order) item 10) |
| ~~G-06~~ | ✅ Done | Reports | ~~No `owner_id`. Any authenticated user can read, update or delete any report (BOLA)~~ | **Fixed (24 Oct 2026):** changeset 008 adds `owner_id`. `AuthUser` + `@CurrentUser` give the logged-in user to controllers. All report queries filter by owner, and other users' reports return 404 |
| G-02 | P1 | Outbox | `smsSender.send()` runs inside `@Transactional` `process()` | Non-transactional orchestrator + short state transactions |
| G-03 | P1 | Outbox | No claim token or version. A stale worker overwrites a reclaimed row | P-4, conditional `UPDATE … WHERE claim_token = ?` |
| G-04 | P1 | Outbox | Recovery cutoff hard-coded at 2 min, unrelated to any provider timeout | Configurable lease, much larger than the provider timeout |
| G-05 | P1 | Outbox | Poison message loops forever (attempt increment rolled back, recovery does not count) | Count the attempt at claim time, cap recoveries |
| G-07 | P1 | API | Unknown `userId` on verify/resend → `IllegalArgumentException` → 500 | Map it to 400 with a generic message (or a dedicated exception) |
| G-08 | P1 | API | Concurrent register with the same number → unique violation → 500 | Catch `DataIntegrityViolationException` on that constraint → 409 |
| G-09 | P1 | Config | Only `LocalSmsSender` (`@Profile("local")`) exists, so the app cannot start in any other profile | Real provider sender, or a `@ConditionalOnMissingBean` no-op/fail-closed sender |
| G-15 | P1 | Security | No per-IP, per-prefix or global limits on `register` and resend. SMS pumping risk | Rate limiter (Bucket4j / gateway), CAPTCHA after N attempts, prefix allow-list |
| G-17 | P1 | Outbox | Retries finish in ~30 s and ignore OTP expiry | `expires_at` on outbox rows, backoff with jitter and a cap |
| G-11 | P2 | API | `CreateReportRequest` has no `@Size`. Over-length values → DB error → 500 | `@Size(max = 200)` / `@Size(max = 100)`, cap `description` |
| G-12 | P2 | Security | `org.springframework.security: TRACE` in the base config | `INFO` by default, `DEBUG` in `application-local.yml` |
| G-13 | P2 | Security | JWT issuer not validated on decode | `JwtValidators.createDefaultWithIssuer("milaavo")` |
| G-14 | P2 | Data | Mobile number only trimmed. The same number in different formats becomes different accounts | Normalize to E.164 |
| G-16 | P2 | Data | Outbox index does not match the claim query, no CHECK constraints, no retention | P-3, P-6, P-7 |
| G-18 | P2 | Config | OTP validity, attempt limits, JWT TTL, outbox limits, lease are hard-coded | `@ConfigurationProperties` per module ([08](08-configuration-deployment.md)) |
| G-19 | P2 | Code | `OtpServiceImpl` uses `jakarta.transaction.Transactional`, everything else uses Spring's | Use `org.springframework.transaction.annotation.Transactional` |
| G-20 | P2 | Perf | `REQUIRES_NEW` in verify holds 2 connections per request | Size the pool for it, or restructure: commit the counter with a conditional update in the outer transaction and return a result instead of throwing |
| G-21 | P2 | Ops | One scheduler thread shared by processing and recovery. No graceful shutdown | `spring.task.scheduling.pool.size: 2`, graceful shutdown |
| G-22 | P2 | API | Catch-all handler likely turns Spring 404/405 into 500. `HandlerMethodValidationException` message always says "pagination". Superseded OTP reported as "already used". `MAX_ATTEMPTS` returned as 400 | Extend `ResponseEntityExceptionHandler` or map explicitly, use a generic message, add a `superseded` state |
| G-10 | P2 | Code | Dead code: `OtpSmsEvent`/`OtpSmsEventListener`, unused `smsSender`/`eventPublisher` in `OtpServiceImpl`, unused `findTop50…`, stale TODO | Delete |
| G-23 | P2 | Ops | No correlation id, metrics, Actuator or readiness | [07](07-observability-operations.md) |
| G-24 | P2 | Code | `UserAlreayActiveException` typo. `ResentVerificationOTP*` DTO names. Field injection in the report module | Rename to `UserAlreadyActiveException` / `ResendVerificationOtp*`, use constructor injection |
| G-25 | P2 | Tests | Only report tests exist. OTP, auth and outbox flows have none. `contextLoads` needs a real DB and env | Dedicated test session: Testcontainers, OTP concurrency tests, outbox scenarios ([05](05-sms-outbox.md#verification-scenarios)) |

## Status of the original design document's next actions

| # | Action from the design PDF | Status after code review |
|---|---|---|
| 1 | Confirm the native claim query no longer has `@Lock` | **Done.** `SmsOutboxRepository.claimPendingMessages` uses SQL `FOR UPDATE SKIP LOCKED` with no `@Lock`. (`@Lock` remains on the JPQL queries `findByIdForUpdate` and `findForVerification`, where it is correct) |
| 2 | Move provider calls outside DB transactions | Open: G-02 |
| 3 | Configurable lease, prevent stale-worker updates | Open: G-03, G-04 |
| 4 | Define retry semantics, timeouts, backoff, idempotency, ambiguous outcomes | Designed in [05](05-sms-outbox.md). Implementation open: G-05, G-17 |
| 5 | Verify OTP expiry, one-time use, attempt limits, rate limits, secure storage | **Verified** for `otp_challenge` ([04](04-auth-otp-security.md)). Open: G-01 (plaintext in outbox), G-15 (IP/global limits) |
| 6 | Run the manual verification scenarios | Open. Scenarios in [05](05-sms-outbox.md#verification-scenarios) |
| 7 | README with architecture, env vars, migrations, run commands, endpoints, recovery | **Done.** [README](../../README.md) + [runbook](../runbook/sms-outbox.md) |

## Suggested order of work

1. ~~**G-06 report ownership.**~~ ✅ Done. A strong interview example of "authentication is not authorization".
2. **G-02, G-05, G-03, G-04 outbox refactor**, as one change. It is the core distributed-systems story of this project.
3. **G-01 OTP at rest**, together with the outbox refactor, since it touches the same table.
4. **G-07, G-08, G-11, G-22 error mapping.** Quick wins, a few lines each.
5. **G-15 rate limiting**, then **G-23 observability**.
6. **G-25 test session**, which also locks in everything above.
7. The remaining P2 cleanup items.

Record each fix in the [evidence log](../sde3/evidence-log.md): what broke, how you proved it, and how you fixed it.
