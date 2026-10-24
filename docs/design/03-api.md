# 3. API

Base URL (local): `http://localhost:8091`. All bodies are JSON. Routes are taken from the controllers. There is no OpenAPI spec yet (Proposed: add `springdoc-openapi` and treat the generated spec as the contract).

## Error format

**Implemented.** Every handled error uses `ApiErrorResponse`:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": { "mobileNumber": "must not be blank" }
}
```

`fieldErrors` is `null` (or `{}` for `INVALID_OTP`) when no fields are involved. 401 and 403 raised by the security filter chain are written by `ApiSecurityErrorHandler` in the same shape.

| `error` | HTTP | Raised when |
|---|---|---|
| `VALIDATION_ERROR` | 400 | `@Valid` body fails, or a `@Min`/`@Max` query parameter fails (message is always "Invalid pagination parameters", Gap G-22) |
| `INVALID_REQUEST_BODY` | 400 | Missing or malformed JSON, wrong JSON types |
| `INVALID_PARAMETER` | 400 | Path or query parameter cannot be converted, for example a non-UUID `{id}` |
| `INVALID_OTP` | 400 | OTP wrong, expired, already used, superseded, or locked |
| `INVALID_VERIFICATION_REQUEST` | 400 | Verify or resend with a `userId` that doesn't exist |
| `UNAUTHORIZED` | 401 | No token, or an invalid or expired token |
| `INVALID_CREDENTIALS` | 401 | Login with an unknown mobile number or a wrong password (same message for both) |
| `FORBIDDEN` | 403 | Login before verification, or a security access denial |
| `REPORT_NOT_FOUND` | 404 | Report id does not exist |
| `MOBILE_NUMBER_ALREADY_REGISTERED` | 409 | Register with an existing number |
| `USER_ALREADY_ACTIVE` | 409 | Verify or resend for an already-verified user |
| `TOO_MANY_REQUESTS` | 429 | Resend cooldown or window limit hit |
| `INTERNAL_SERVER_ERROR` | 500 | Anything else. Logged with a stack trace, never returned to the client |

> **Gap G-22:** some client errors end up as 500 because the catch-all `Exception` handler catches them. Examples: a register race on the unique constraint (`DataIntegrityViolationException`, G-08), and an over-length report field. Spring's own 404 (no route) and 405 (wrong method) exceptions are probably caught by the catch-all too. Verify this and map them explicitly.

## Auth endpoints (public)

### `POST /api/auth/register` → 201

```json
{ "name": "Asha", "mobileNumber": "+919800000001", "password": "at-least-12-chars" }
```

Validation: `name` 1–100, `mobileNumber` 1–20, `password` 12–72 characters. The 72 limit matches BCrypt's input limit.

Response:

```json
{
  "userId": "…", "name": "Asha", "mobileNumber": "+919800000001",
  "mobileVerified": false, "status": "PENDING_VERIFICATION",
  "createdAt": "2026-10-24T05:00:00Z", "challengeId": "…"
}
```

In one transaction: insert the user, supersede any open challenges, insert the challenge, insert the outbox row. The SMS is sent later by the worker.

Errors: 400, 409 `MOBILE_NUMBER_ALREADY_REGISTERED`.

### `POST /api/auth/verify-mobile` → 204

```json
{ "userId": "…", "challengeId": "…", "otp": "123456" }
```

`otp` must match `\d{6}`. On success the user becomes `ACTIVE` with `mobileVerified = true`.

Errors: 400 `INVALID_OTP` (the message says which: invalid, expired, already used, max attempts), 400 `INVALID_VERIFICATION_REQUEST` for an unknown `userId`, 409 `USER_ALREADY_ACTIVE`.

### `POST /api/auth/verify/resend-otp` → 200

```json
{ "userId": "…" }
```

Response: `{ "challengeId": "…" }`. The previous challenge is superseded, so only the newest OTP works.

Limits (configurable): at least 60 s since the last resend, and at most 3 resends in 15 minutes. The initial registration OTP does not count as a resend.

Errors: 400 `INVALID_VERIFICATION_REQUEST` for an unknown `userId`, 409 `USER_ALREADY_ACTIVE`, 429 `TOO_MANY_REQUESTS`.

### `POST /api/auth/login` → 200

```json
{ "mobileNumber": "+919800000001", "password": "…" }
```

Response:

```json
{
  "userId": "…", "name": "Asha", "mobileNumber": "+919800000001",
  "mobileVerified": true, "status": "ACTIVE",
  "accessToken": "eyJ…", "tokenType": "Bearer", "expiresIn": 900
}
```

The password is checked **before** the account status, so the 403 for an unverified account is only returned to someone who knows the password.

Errors: 401 `INVALID_CREDENTIALS`, 403 `FORBIDDEN` (not active or not verified).

## Report endpoints (Bearer token required)

Send `Authorization: Bearer <accessToken>`. Requests without a token, or with an expired or invalid one, get 401 `UNAUTHORIZED`.

**Ownership:** every report belongs to the user who created it. A user can only see, edit and delete their own reports. Someone else's report returns **404 `REPORT_NOT_FOUND`**, the same as a report that doesn't exist (see [04](04-auth-otp-security.md#authorization)).

Report body (`CreateReportRequest`, used for both create and update):

```json
{ "itemName": "Black wallet", "description": "Lost near MG Road metro", "city": "Bengaluru" }
```

All three fields are `@NotBlank`. There are no length limits yet (G-11).

Report representation (`CreateReportResponse`):

```json
{ "reportId": "…", "itemName": "…", "description": "…", "city": "…", "status": "REPORTED" }
```

`createdAt` is stored but not returned.

| Method | Path | Success | Notes |
|---|---|---|---|
| POST | `/api/reports` | 201 + report | `status` is always `REPORTED` |
| GET | `/api/reports?page=0&size=10` | 200 + page | **Only my reports.** `page ≥ 0`, `1 ≤ size ≤ 100`. Sorted by `createdAt DESC`. The client cannot choose the sort (no SQL-injection surface) |
| GET | `/api/reports/{id}` | 200 + report | 404 `REPORT_NOT_FOUND` if missing or not mine. 400 `INVALID_PARAMETER` for a non-UUID |
| PUT | `/api/reports/{id}` | 200 + report | Full replacement of the three fields. 404 if missing or not mine |
| DELETE | `/api/reports/{id}` | 204 | Hard delete. 404 if missing or not mine |

Page shape (`PagedResponse`):

```json
{ "content": [ … ], "page": 0, "size": 10, "totalElements": 42, "totalPages": 5, "first": true, "last": false }
```

## Health

`GET /api/health` → `{"status":"UP","application":"Milaavo"}`. Public. It does not check the database, so it is a liveness signal only. Readiness is in [07](07-observability-operations.md).

## API design principles

Already followed:

- Correct status codes: 201 for create, 204 for no body, 409 for state conflict, 429 for rate limit.
- Bean Validation at the boundary, business rules in the service.
- One error shape, no stack traces, no internal messages for 5xx.
- Pagination has a hard maximum page size, and the sort order is fixed by the server.
- Login does not reveal which part of the credentials was wrong.

Proposed:

- **Versioning.** Move to `/api/v1/...` before the first external client depends on the routes. That is cheap now and expensive later.
- **Correlation id.** Add a `correlationId` field to `ApiErrorResponse` and an `X-Request-Id` response header ([07](07-observability-operations.md)).
- **Enumeration.** Register's 409 tells anyone whether a number has an account. If that matters for the product, return 202 for every register request and send an SMS of the form "someone tried to register your number" to existing numbers.
- **Resend and verify by `challengeId` only.** Today the client must keep `userId` and `challengeId`. A single opaque verification token is a simpler contract.
- **`PATCH` for partial report updates**, plus `createdAt`/`updatedAt` in the response.
- **Idempotency key** (`Idempotency-Key` header) on `POST /api/reports`, so a mobile client that retries after a timeout does not create duplicates.
