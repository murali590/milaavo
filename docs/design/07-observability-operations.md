# 7. Observability and operations

## Current state (Implemented)

- SLF4J logging through Spring Boot's default Logback.
- `GlobalExceptionHandler` logs unexpected exceptions with a stack trace and returns a generic 500.
- The outbox logs failed attempts (`id`, attempt n of 5), recovered row counts, and per-message processor crashes.
- `GET /api/health` returns a static `UP`.
- No metrics, no tracing, no correlation id, no Actuator.

## Logging (Proposed)

- **Correlation id.** A servlet filter reads `X-Request-Id`, or generates one, puts it in the MDC, returns it as a response header, and adds it to `ApiErrorResponse`. Scheduled jobs set their own id per batch, and per message using the outbox id.
- **Structured JSON logs.** Spring Boot has built-in structured logging (`logging.structured.format.console=ecs` or `logstash`), so fields can be searched.
- **What to log for the outbox**: outbox id, attempt, transition (`PENDING→PROCESSING`), latency, error category. **Never** the message body, the OTP, or the full phone number. Mask it as `+91******0001`.
- **Never log**: passwords, OTPs, tokens, `Authorization` headers, the HMAC/JWT secrets. Security logging is back at `INFO` (G-12 fixed).
- `OtpSmsEventListener` logs an error without the exception. If the class is kept, pass `ex` to the logger. (It is dead code, G-10.)

## Metrics (Proposed: Actuator + Micrometer + Prometheus)

| Metric | Type | Why |
|---|---|---|
| `sms_outbox_pending` (by due/not due) | gauge | Backlog size |
| `sms_outbox_oldest_due_age_seconds` | gauge | **Main SLO signal.** OTP SMS must leave within seconds |
| `sms_outbox_processing` and oldest processing age | gauge | Stuck workers |
| `sms_send_total{outcome=sent|retry|failed}` | counter | Provider health |
| `sms_send_duration_seconds` | histogram | Provider latency, p50/p95/p99 |
| `sms_outbox_recovered_total` | counter | Crashes and lease expiries. Should be about 0 |
| `otp_issued_total{purpose}`, `otp_verify_total{result}` | counter | Abuse detection: a spike in `INVALID` or `MAX_ATTEMPTS_EXCEEDED` |
| `otp_resend_rejected_total{reason}` | counter | Rate-limit pressure |
| `http.server.requests` | timer (built-in) | Latency and error rate per route |
| `hikaricp.connections.*` | built-in | Pool saturation (REQUIRES_NEW doubles usage, G-20) |
| `jvm.*` | built-in | GC pauses, heap, threads |

## Alerts (Proposed starting thresholds)

| Alert | Condition | First check |
|---|---|---|
| OTP SMS delayed | oldest due age > 60 s for 5 min | Worker running? DB locks? Provider latency? |
| SMS failures high | failed / total > 10 % for 10 min | Provider status, `last_error` |
| Stuck processing | any `PROCESSING` older than the lease | Instance health, recent deploys |
| OTP brute force | `verify{result=INVALID}` far above baseline | Source IPs, the affected users |
| SMS pumping | `otp_issued_total` far above the registration baseline | Number prefixes, IPs. Enable stricter limits |
| DB pool exhaustion | pending connections > 0 for 1 min | Slow queries, long transactions |
| 5xx rate | > 1 % for 5 min | Logs by correlation id |

## Health, liveness and readiness (Proposed)

- **Liveness** (`/actuator/health/liveness`): the process can serve. No dependency checks, otherwise a DB blip would make the orchestrator restart every pod.
- **Readiness** (`/actuator/health/readiness`): includes the database. Take the instance out of the load balancer if it cannot reach PostgreSQL.
- **Do not put the SMS provider in readiness.** A provider outage should not stop logins or reports. The outbox absorbs it.
- Expose `/actuator/health` publicly with `show-details: never`. Keep `/actuator/prometheus` and the other endpoints on a management port or behind auth.
- Keep `/api/health` for compatibility, or make it an alias of liveness.

## Graceful shutdown (Proposed)

- `server.shutdown: graceful` with a timeout. In-flight HTTP requests finish.
- The scheduler stops claiming new outbox batches when shutdown begins. In-flight sends get up to the lease to finish. Anything left `PROCESSING` is recovered by another instance after the lease.
- `spring.task.scheduling.shutdown.await-termination: true` with a period shorter than the platform's kill timeout.

## Data retention (Proposed)

| Data | Keep | Then |
|---|---|---|
| `sms_outbox.message` | Until `SENT`/`FAILED` | Set to `NULL`/redacted (G-01) |
| `sms_outbox` rows | 30 days | Delete (metrics keep the history) |
| `otp_challenge` | 7 days after expiry | Delete |
| `otp_resend_attempt` | Longer than the rate-limit window, for example 1 day | Delete |
| Application logs | Per policy, for example 30 days | Phone numbers already masked |

Run retention as a scheduled job that deletes in small batches (`DELETE … WHERE id IN (SELECT id … LIMIT 1000)`), to avoid long locks and WAL spikes.

## Incident practice

Use the [SMS outbox runbook](../runbook/sms-outbox.md). For each real or simulated incident, write down: impact, timeline, detection, diagnosis, mitigation, root cause, and prevention with an owner. These notes become strong behavioral-interview stories.
