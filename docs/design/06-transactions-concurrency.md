# 6. Transactions and concurrency

This page lists every transaction boundary in the code, what it protects, and the Spring and PostgreSQL behavior behind it. It is written to be explained out loud in an interview.

## Transaction inventory (Implemented)

| Method | Annotation | Protects | Notes |
|---|---|---|---|
| `UserServiceImpl.register` | `@Transactional` | User + challenge + outbox row commit together | Joins `OtpServiceImpl.issueOtp` and `SmsOutboxService.enqueue` (`REQUIRED`) |
| `UserServiceImpl.login` | `@Transactional(readOnly = true)` | Consistent read | `readOnly` lets Hibernate skip dirty checking |
| `UserServiceImpl.verifyMobileOtp` | `@Transactional` | User activation | Calls `OtpVerificationTransaction.verify` (`REQUIRES_NEW`) |
| `UserServiceImpl.resendVerificationOtp` | `@Transactional` | Rate-limit check + attempt insert + new OTP, atomically | `findByIdForUpdate` locks the user row |
| `OtpServiceImpl.issueOtp` | `@jakarta.transaction.Transactional` | Supersede + insert + enqueue | Uses the JTA annotation, unlike the rest of the code (G-19). Spring honors it, but it lacks `readOnly`, isolation and similar attributes. Standardize on Spring's |
| `OtpVerificationTransaction.verify` | `@Transactional(REQUIRES_NEW)` | Failed-attempt counter survives the caller's rollback | See [04](04-auth-otp-security.md#verify-otpverificationtransactionverify) |
| `SmsOutboxClaimService.claimPendingMessages` | `@Transactional` | `SKIP LOCKED` select + mark `PROCESSING` | Row locks are held only for this short transaction |
| `SmsOutboxProcessor.process` | `@Transactional` | Outcome write | **Wraps the network call** (G-02) |
| `SmsOutboxRecoveryService.recoverStaleMessages` | `@Transactional` | Bulk reset | A single `UPDATE` |
| `ReportServiceImpl.create/update/delete` | `@Transactional` | Single-entity writes | |
| `ReportServiceImpl.getAll/getById` | `@Transactional(readOnly = true)` | Consistent read | Added with G-06. `readOnly` lets Hibernate skip dirty checking |

## Spring concepts this code depends on

### Proxies and self-invocation

`@Transactional` works through a proxy around the bean. A call like `this.someTransactionalMethod()` from inside the same class bypasses the proxy, so **no transaction starts and nothing errors**. Milaavo avoids this by putting each transaction boundary in its own bean:

- `OtpVerificationTransaction` is a separate class only so that `REQUIRES_NEW` actually applies.
- `SmsOutboxClaimService`, `SmsOutboxProcessor` and `SmsOutboxRecoveryService` are separate from `SmsOutboxWorker`.

The same rule applies to `@Async`, `@Cacheable` and `@PreAuthorize`.

### Propagation

- `REQUIRED` (default): join the existing transaction or start one. That is why the outbox insert is atomic with registration.
- `REQUIRES_NEW`: suspend the outer transaction and run an independent one **on another connection**. Commit or rollback is independent. Cost: two connections per request (G-20).
- `MANDATORY` would be a good guard on `SmsOutboxService.enqueue`. It must never run outside a business transaction, otherwise the outbox loses its point. (Proposed)

### Rollback rules

By default Spring rolls back on unchecked exceptions (`RuntimeException`, `Error`), **not** on checked exceptions. All Milaavo domain exceptions extend `RuntimeException`. Note the `verifyMobileOtp` flow: `InvalidOtpException` rolls back the outer transaction, which contains nothing yet, while the inner `REQUIRES_NEW` already committed the counter. That is the intended behavior.

In `SmsOutboxProcessor`, the provider exception is **caught**, so the transaction commits the retry state. If the exception were rethrown, the retry bookkeeping would roll back.

### Isolation

PostgreSQL defaults to `READ COMMITTED`: each statement sees data committed before it started. Milaavo uses explicit row locks instead of raising isolation:

- `SELECT … FOR UPDATE` (`findByIdForUpdate`, `findForVerification`) for check-then-act on one row.
- `FOR UPDATE SKIP LOCKED` for queue claiming.

`SERIALIZABLE` would also prevent these races, but it aborts transactions with serialization failures that the app must then retry.

### Persistence context and bulk updates

`@Modifying` JPQL or native updates go straight to the database. Entities already loaded in the same persistence context are **not** refreshed. Today this is safe because `invalidateActiveChallenges` runs before any challenge is loaded, and recovery loads nothing. Use `@Modifying(clearAutomatically = true, flushAutomatically = true)` when that ordering is not guaranteed.

### Dirty checking

In `SmsOutboxClaimService`, the rows returned by the native query are managed entities. Changing their fields is enough, because Hibernate flushes `UPDATE`s at commit. The explicit `saveAll` is harmless but redundant.

## Concurrency patterns used

| Problem | Where | Technique | Alternative |
|---|---|---|---|
| Duplicate registration | `register` | Unique constraint `uk_app_user_mobile_number`. `existsBy…` is only for a friendly error | Catch `DataIntegrityViolationException` → 409. **Missing today** (G-08): the race returns 500 |
| Double OTP verification | `verify` | Pessimistic lock + `consumed_at` check | Conditional update: `UPDATE … SET consumed_at=now WHERE id=? AND consumed_at IS NULL`, then check the row count |
| Resend rate-limit race | `resendVerificationOtp` | Lock the parent user row to serialize | Advisory lock, or a unique `(user_id, time_bucket)` |
| Queue claiming | outbox claim | `FOR UPDATE SKIP LOCKED` | Broker with visibility timeouts (SQS), or Kafka partitions |
| Stale worker overwrite | outbox outcome | **None** (G-03) | Claim token or `@Version` compare-and-set |
| Concurrent report edits | `updateReport` | **None**, last write wins | `@Version` on `LostReport` → 409 on conflict (P-2) |

### Pessimistic or optimistic locking?

- **Pessimistic** (`FOR UPDATE`): when conflicts are likely and the critical section is short and entirely in the database. OTP verification and resend fit.
- **Optimistic** (`@Version`): when conflicts are rare and you can tell the user "someone else changed this". Report edits fit.
- **Queue / single writer**: when work should be processed once by whoever is free. The outbox fits.

Never hold a pessimistic lock across a network call. That is the same mistake as G-02, applied to locks.

### Deadlock notes

- All current locking transactions lock a single row, or a set of rows taken in `created_at` order with `SKIP LOCKED`, so lock-ordering deadlocks are unlikely.
- If a future transaction locks both a user and a challenge, always lock in the same order (user, then challenge) everywhere.
- PostgreSQL detects deadlocks and aborts one transaction (`40P01`). Catch it and retry idempotent operations.

## PostgreSQL topics to practice on this schema

- `EXPLAIN (ANALYZE, BUFFERS)` the claim query with about 1 M `SENT` rows and a few hundred `PENDING` rows. Compare the existing `(status, created_at)` index with the partial index P-3.
- MVCC: why `UPDATE`-heavy `sms_outbox` rows create dead tuples, and what autovacuum does about them.
- Watch locks while a verify is blocked: `pg_locks` joined to `pg_stat_activity` (query in the runbook).
- Offset pagination cost: `GET /api/reports?page=10000` scans and discards rows. Keyset pagination (`WHERE created_at < :cursor ORDER BY created_at DESC LIMIT n`) avoids that.
