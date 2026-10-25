# Runbook: SMS outbox

Use this when OTP SMS are late, missing, duplicated, or the `sms_outbox` table looks unhealthy. Design background: [05-sms-outbox.md](../design/05-sms-outbox.md).

> `sms_outbox.message` is encrypted (AES-256-GCM) while a message is waiting, and replaced by `[redacted]` once it's SENT or FAILED (G-01). `mobile_number` is still personal data. Do not paste query results containing it into tickets, chat or screenshots.
>
> **Never change `SMS_OUTBOX_ENCRYPTION_KEY` while messages are pending.** Messages encrypted with the old key can't be decrypted (`Unable to decrypt value`, caused by `AEADBadTagException: Tag mismatch`) and will end as FAILED. Users then need to resend.

## How it works, in one paragraph

`OtpServiceImpl.issueOtp` inserts a `PENDING` row in the same transaction that creates the OTP challenge. Every `app.sms-outbox.poll-delay-ms` (5 s), `SmsOutboxWorker` claims up to 50 due rows with `FOR UPDATE SKIP LOCKED`, marks them `PROCESSING` and commits. It then calls `SmsOutboxProcessor.process(id)` for each one, which sends the SMS **inside a transaction** and writes `SENT`, `PENDING` (retry after 2/4/8/16 s) or `FAILED` (after 5 attempts). Every `app.sms-outbox.recovery-delay-ms` (30 s), rows stuck in `PROCESSING` for longer than the lease (`app.sms-outbox.lease`, default 2 min) are reset to `PENDING` (or `FAILED` if they used all 5 attempts), and their `claim_token` is cleared.

## Status meanings

| Status | Meaning | Normal lifetime |
|---|---|---|
| `PENDING` | Waiting for `next_attempt_at` | ≤ poll delay for new rows; up to 16 s for retries |
| `PROCESSING` | Claimed by a worker, send in progress | Seconds. More than 2 min means the worker died or is stuck |
| `SENT` | Provider call returned without exception | Terminal |
| `FAILED` | 5 attempts failed | Terminal. Needs a decision (see below) |

## Health check queries

Counts by status:

```sql
SELECT status, COUNT(*) AS message_count
FROM sms_outbox
GROUP BY status
ORDER BY status;
```

Oldest due-but-unsent message (main backlog signal):

```sql
SELECT now() - MIN(next_attempt_at) AS oldest_due_age, COUNT(*) AS due_count
FROM sms_outbox
WHERE status = 'PENDING'
  AND next_attempt_at <= now();
```

Stuck in processing:

```sql
SELECT id, attempt_count, processing_started_at, now() - processing_started_at AS age, last_error
FROM sms_outbox
WHERE status = 'PROCESSING'
ORDER BY processing_started_at;
```

Recent activity, without sensitive columns:

```sql
SELECT id, status, attempt_count, created_at, processing_started_at, next_attempt_at, sent_at, last_error
FROM sms_outbox
ORDER BY created_at DESC
LIMIT 50;
```

Messages recovered more than once (possible poison message or duplicate sends):

```sql
SELECT id, status, attempt_count, created_at, last_error
FROM sms_outbox
WHERE last_error = 'Recovered stale processing message'
ORDER BY created_at DESC
LIMIT 50;
```

Who holds locks on the table (useful if claims stop):

```sql
SELECT a.pid, a.state, a.query_start, now() - a.query_start AS running_for, left(a.query, 120) AS query
FROM pg_locks l
JOIN pg_stat_activity a ON a.pid = l.pid
WHERE l.relation = 'sms_outbox'::regclass;
```

## Symptoms and actions

### Users say they never received an OTP

1. Find recent rows for the time window (not by phone number in shared channels).
2. `PENDING` with `next_attempt_at` in the past and a growing `oldest_due_age`: the worker is not running. Check that the app is up, that `@EnableScheduling` is active, and look for `Unexpected error processing SMS outbox message` in the logs.
3. `PROCESSING` older than 2 min: the worker crashed or is blocked. Recovery should reset it within 30 s. If it does not, check the logs for `Recovered N stale SMS outbox messages`.
4. `FAILED`: read `last_error`. The OTP itself expires 5 minutes after issue, so **do not re-queue an old OTP message**. Ask the user to tap "resend", which creates a new challenge and a new outbox row.
5. `SENT`: the provider accepted it. The issue is downstream (carrier, DND, wrong number). With `LocalSmsSender`, "sent" only means it was logged.

### Users received the same OTP twice

Expected under the current design in two cases (see [05-sms-outbox.md](../design/05-sms-outbox.md#failure-modes)):

- The send succeeded but the transaction writing `SENT` failed or the app crashed. Recovery re-queues the row after 2 minutes.
- A send took longer than the lease (`app.sms-outbox.lease`, default 2 min). Recovery reset the row while the first send was still running. Look for `Lease lost for SMS outbox ID …` in the logs: the slow worker's late result was correctly ignored. If this happens often, the provider is slower than the lease allows, so raise the lease or lower the provider timeout.

Both are at-least-once delivery. For an OTP the second copy is harmless if it carries the same code. Track how often it happens.

### A row keeps cycling between PROCESSING and PENDING

Since the G-05 fix, each claim counts an attempt, so a message that keeps crashing the worker stops by itself: after 5 claims, recovery marks it `FAILED` with `last_error = 'Recovered stale processing message'`. If you see many of these, a message or the provider SDK is crashing the worker. Check the app log for `Error`s, not just `Exception`s.

If you need to stop one immediately, mark it failed by hand:

```sql
UPDATE sms_outbox
SET status = 'FAILED', processing_started_at = NULL, last_error = 'Manually failed: poison message'
WHERE id = '<id>' AND status IN ('PENDING', 'PROCESSING');
```

### Provider outage

Retries spread over about 75 seconds (backoff with jitter), so outages up to about a minute are absorbed. Longer outages move messages to `FAILED`, and once an OTP is 5 minutes old its message is failed with `Expired before delivery` instead of being sent late. After the provider recovers, users should request new OTPs. Do not bulk re-queue `FAILED` rows that are older than 5 minutes.

To re-queue recent failures (younger than the OTP validity) once the provider is healthy:

```sql
UPDATE sms_outbox
SET status = 'PENDING', next_attempt_at = now(), attempt_count = 0, last_error = 'Manually re-queued after outage'
WHERE status = 'FAILED'
  AND created_at > now() - interval '4 minutes';
```

## Safe manual operations checklist

- Always filter on `status` in the `WHERE` clause so you do not overwrite a row a worker just changed.
- Run updates in a transaction (`BEGIN; ... ; SELECT ...; COMMIT;`) and check the affected row count before committing.
- Never `DELETE` rows during an incident. They are the audit trail.
- Record what you changed and why in the incident notes.
