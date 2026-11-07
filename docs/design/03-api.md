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

`fieldErrors` is `null` (or `{}` for OTP errors) when no fields are involved. 401 and 403 raised by the security filter chain are written by `ApiSecurityErrorHandler` in the same shape.

| `error` | HTTP | Raised when |
|---|---|---|
| `VALIDATION_ERROR` | 400 | `@Valid` body fails, or a `@Min`/`@Max` query parameter fails. `fieldErrors` names each failed field or parameter, for example `{"page": "Page number must be at least 0"}` |
| `INVALID_REQUEST_BODY` | 400 | Missing or malformed JSON, wrong JSON types |
| `INVALID_PARAMETER` | 400 | Path or query parameter cannot be converted, for example a non-UUID `{id}` |
| `INVALID_OTP` | 400 | Wrong OTP, or the challenge doesn't match (same code for both, so challenge ids can't be probed) |
| `OTP_EXPIRED` | 400 | OTP older than 5 minutes. Client should offer "Resend" |
| `OTP_ALREADY_USED` | 400 | OTP was already verified |
| `SUPERSEDED` | 400 | A newer OTP was issued. Client should ask for the latest OTP |
| `MAX_ATTEMPTS_EXCEEDED` | 400 | 5 wrong tries. The challenge is locked for good, even for the correct OTP. Client should offer "Resend" |
| `INVALID_VERIFICATION_REQUEST` | 400 | Verify or resend with a `userId` that doesn't exist |
| `UNAUTHORIZED` | 401 | No token, or an invalid or expired token |
| `INVALID_CREDENTIALS` | 401 | Login with an unknown mobile number or a wrong password (same message for both) |
| `FORBIDDEN` | 403 | A security access denial |
| `MOBILE_NOT_VERIFIED` | 403 | Login with the correct password, but the mobile isn't verified yet. Client should call `POST /api/auth/verify/start` |
| `ACCOUNT_DISABLED` | 403 | Login with the correct password, but the account is disabled |
| `REPORT_NOT_FOUND` | 404 | Report id does not exist |
| `MOBILE_NUMBER_ALREADY_REGISTERED` | 409 | Register with an existing number |
| `USER_ALREADY_ACTIVE` | 409 | Verify or resend for an already-verified user |
| `TOO_MANY_REQUESTS` | 429 | Resend cooldown or window limit hit |
| `INTERNAL_SERVER_ERROR` | 500 | Anything else. Logged with a stack trace, never returned to the client |

**Spring's own client errors** (G-22 part 1, fixed): a wrong URL gives 404 `NOT_FOUND`, a wrong method gives 405 `METHOD_NOT_ALLOWED` (with an `Allow` header), and a wrong Content-Type gives 415 `UNSUPPORTED_MEDIA_TYPE`. The catch-all handler checks for Spring's `ErrorResponse` interface and reuses the status it carries. Only real unexpected errors become 500.

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

Errors: 400 with one of `INVALID_OTP`, `OTP_EXPIRED`, `OTP_ALREADY_USED`, `SUPERSEDED`, `MAX_ATTEMPTS_EXCEEDED` (clients should branch on this code, never on the message text), 400 `INVALID_VERIFICATION_REQUEST` for an unknown `userId`, 409 `USER_ALREADY_ACTIVE`.

### `POST /api/auth/verify/resend-otp` → 200

```json
{ "userId": "…" }
```

Response: `{ "challengeId": "…" }`. The previous challenge is superseded, so only the newest OTP works.

Limits (configurable): at least 60 s since the last resend, and at most 3 resends in 15 minutes. The initial registration OTP does not count as a resend.

Errors: 400 `INVALID_VERIFICATION_REQUEST` for an unknown `userId`, 409 `USER_ALREADY_ACTIVE`, 429 `TOO_MANY_REQUESTS`.

### `POST /api/auth/verify/start` → 200

For a user who registered but never finished verifying, and so has no `userId` or `challengeId`.

```json
{ "mobileNumber": "+919876543210", "password": "…" }
```

Response:

```json
{ "userId": "…", "challengeId": "…", "mobileNumber": "+919876543210" }
```

Sends a new OTP. It requires the password, so nobody can trigger SMS to another person's number. It shares the cooldown and window limits with resend.

Errors: 401 `INVALID_CREDENTIALS` (the same for an unknown number and a wrong password), 409 `USER_ALREADY_ACTIVE`, 429 `TOO_MANY_REQUESTS`.

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

Errors: 401 `INVALID_CREDENTIALS`, 403 `MOBILE_NOT_VERIFIED`, 403 `ACCOUNT_DISABLED`.

## Report endpoints (Bearer token required)

Send `Authorization: Bearer <accessToken>`. Requests without a token, or with an expired or invalid one, get 401 `UNAUTHORIZED`.

**Ownership:** every report belongs to the user who created it. A user can only see, edit and delete their own reports. Someone else's report returns **404 `REPORT_NOT_FOUND`**, the same as a report that doesn't exist (see [04](04-auth-otp-security.md#authorization)).

Report body (`CreateReportRequest`, used for both create and update):

```json
{ "type": "LOST", "itemName": "Black wallet", "description": "Lost near MG Road metro", "city": "Bengaluru" }
```

`type` is required: `LOST`, `FOUND` or `SWAPPED` (swapped by mistake). A missing type gives 400 `VALIDATION_ERROR`; an unknown value gives 400 `INVALID_REQUEST_BODY`. The other three fields are `@NotBlank`, with max lengths: `itemName` 200, `description` 2000, `city` 100. Longer values return 400 `VALIDATION_ERROR` with `fieldErrors`.

Report representation (`CreateReportResponse`):

```json
{ "reportId": "…", "type": "LOST", "itemName": "…", "description": "…", "city": "…", "status": "REPORTED", "createdAt": "2026-11-02T06:30:00.123Z" }
```

`createdAt` is an ISO-8601 UTC instant. Create uses `saveAndFlush`, so the INSERT runs before the response is built and `@CreationTimestamp` has already filled the value.

| Method | Path | Success | Notes |
|---|---|---|---|
| POST | `/api/reports` | 201 + report | `status` is always `REPORTED` |
| GET | `/api/reports?page=0&size=10&type=FOUND` | 200 + page | **Only my reports.** `type` is optional (`LOST`, `FOUND`, `SWAPPED`); a wrong value gives 400 `INVALID_PARAMETER`. `page ≥ 0`, `1 ≤ size ≤ 100`. Sorted by `createdAt DESC`. The client cannot choose the sort (no SQL-injection surface) |
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
