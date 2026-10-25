# 4. Authentication, OTP and security

## User lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING_VERIFICATION : register (OTP sent)
    PENDING_VERIFICATION --> PENDING_VERIFICATION : resend OTP
    PENDING_VERIFICATION --> ACTIVE : verify-mobile succeeds
    ACTIVE --> DISABLED : (no code path yet)
```

Login is allowed only when `status = ACTIVE` and `mobile_verified = true`.

## OTP challenge lifecycle (Implemented)

A challenge has no `status` column. Its state is derived from its columns:

| Derived state | Condition |
|---|---|
| Pending | `consumed_at IS NULL AND superseded_at IS NULL AND now < expires_at AND failed_attempts < 5` |
| Verified | `consumed_at` set by a successful verification |
| Superseded | `superseded_at` set by `invalidateActiveChallenges` when a new OTP is issued |
| Expired | `now ≥ expires_at` |
| Locked | `failed_attempts ≥ 5` |

```mermaid
stateDiagram-v2
    [*] --> Pending : issueOtp
    Pending --> Pending : wrong OTP (failed_attempts + 1)
    Pending --> Verified : correct OTP (consumed_at = now)
    Pending --> Superseded : new OTP issued for same user + purpose
    Pending --> Expired : 5 minutes pass
    Pending --> Locked : 5th wrong attempt
```

Verified and Superseded use **separate columns** (G-22 part 3, fixed). Earlier both set `consumed_at`, so an old OTP was wrongly reported as "already used". Now a superseded OTP returns: "This OTP is no longer valid. Please use the latest OTP sent to your mobile". It is checked before expiry and before the hash, so it doesn't count as a failed attempt.

### Issue (`OtpServiceImpl.issueOtp`)

1. Load the user.
2. Supersede open challenges for the same user and purpose (bulk update `superseded_at = now`).
3. Generate a 6-digit code with `SecureRandom.nextInt(1_000_000)`, zero-padded.
4. Store `HMAC-SHA256(key, "userId:purpose:otp")`, Base64-encoded.
5. Set `expires_at = now + 5 min`.
6. Enqueue the SMS in `sms_outbox` **in the same transaction**. Either the user, the challenge and the SMS all commit, or none do.

### Verify (`OtpVerificationTransaction.verify`)

Runs in `REQUIRES_NEW`, separate from the caller's transaction:

1. `SELECT … FOR UPDATE` on the challenge (`id`, `user_id` and `purpose` must all match). This serializes concurrent verifications of the same challenge.
2. Not found → `CHALLENGE_NOT_FOUND`. Consumed → `ALREADY_USED`. Superseded → `SUPERSEDED`. Expired → `EXPIRED`. `failed_attempts ≥ 5` → `MAX_ATTEMPTS_EXCEEDED`.
3. Hash mismatch → `failed_attempts + 1`, commit, `INVALID`.
4. Match → `consumed_at = now`, commit, `VERIFIED`.

**Why `REQUIRES_NEW`:** the caller throws `InvalidOtpException` on failure, which would roll back its transaction. If the failed-attempt counter were in the same transaction, it would be rolled back too, and brute-force protection would never count anything. The separate transaction commits the counter whatever the caller does.

Trade-offs of this choice:

- The OTP is consumed in its own commit **before** the user is activated in the outer commit. If the outer commit fails, the OTP is spent but the user is still `PENDING_VERIFICATION`. That is recoverable: the user requests a resend.
- `REQUIRES_NEW` suspends the outer transaction and takes a **second pool connection** while the first is still held. Under heavy concurrent verification this can exhaust the Hikari pool. The pool would need to allow 2 connections per in-flight verify (G-20).

Concurrency: two requests with the same correct OTP both reach `FOR UPDATE`. The first commits `consumed_at`, and the second then reads it and gets `ALREADY_USED`. Exactly one verification succeeds.

### Resend (`UserServiceImpl.resendVerificationOtp`)

1. `SELECT … FOR UPDATE` on `app_user`, which serializes resends for one user. Without the lock, two parallel requests could both pass the cooldown check (check-then-act race).
2. Reject if the user is already active.
3. Cooldown: `MAX(requested_at) + 60 s > now` → 429.
4. Window: `COUNT(requested_at > now − 15 min) ≥ 3` → 429.
5. Insert a resend attempt, then issue a new OTP (which supersedes the old one).

### OTP security properties

| Property | Status | How |
|---|---|---|
| Unpredictable code | Implemented | `SecureRandom` |
| Not stored in plaintext in `otp_challenge` | Implemented | Keyed HMAC. A plain SHA-256 of a 6-digit code can be brute-forced offline in milliseconds. HMAC needs the server key |
| Bound to user and purpose | Implemented | Both are inside the HMAC input and the lookup query |
| Constant-time comparison | Implemented | `MessageDigest.isEqual` |
| Short validity | Implemented | 5 minutes (hard-coded, G-18) |
| Attempt limit | Implemented | 5 per challenge, counted in a separate transaction |
| One-time use | Implemented | Row lock + `consumed_at` |
| Only the latest OTP works | Implemented | Supersede on issue |
| Resend throttling | Implemented | Per user: 60 s cooldown, 3 per 15 min |
| OTP not stored in plaintext anywhere | Implemented (G-01) | The outbox message is AES-256-GCM encrypted with its own key and redacted once sent or failed. Hash vs encryption: `otp_challenge` only needs to **compare** (HMAC, one-way); the outbox must **send the real text** (encryption, two-way) |
| Per-IP / per-number-prefix / global SMS limits | **Gap G-15** | `register` has no limit at all, so a script can register many numbers and trigger SMS (toll fraud / SMS pumping) |
| Attempt limit across challenges | **Gap** | 5 tries per challenge × (1 + 3 resends) = 20 guesses per 15 minutes per user. Out of 10⁶ codes that is low risk, but monitor it |
| OTP never logged | Partly | `LocalSmsSender` logs the full message. Acceptable for `local` only. A real sender must never log message bodies |

## Passwords

- BCrypt, cost 12 (`PasswordConfig`).
- Length 12–72. BCrypt ignores input after 72 bytes, so the upper limit avoids a silent truncation surprise. Multi-byte characters can still exceed 72 bytes inside 72 characters.
- `@ToString.Exclude` keeps passwords out of Lombok `toString` and logs.

## JWT

**Implemented** (`JwtService`, `JwtConfig`, `SecurityConfig`):

| Aspect | Value |
|---|---|
| Algorithm | HS256 (symmetric). The decoder is pinned to HS256, so `alg: none` or switching algorithms is rejected |
| Key | `JWT_SECRET`, Base64, ≥ 32 bytes, checked at startup |
| Claims | `iss=milaavo`, `sub=<userId>`, `iat`, `exp` |
| Lifetime | 15 minutes, no refresh token |
| Validation | Signature, `exp`/`nbf` (60 s clock skew) and **issuer** `iss = milaavo` (G-13, `JwtValidators.createDefaultWithIssuer(JwtConfig.ISSUER)`) |
| Session | `STATELESS`, CSRF disabled (correct for bearer tokens sent in a header, not cookies) |

Gaps and proposals:

- ✅ **Issuer validated (G-13, fixed 25 Oct 2026).** A token signed with our key but with another `iss` now gets 401. This prevents token confusion if the key is ever used for other token types. Tested with a hand-made token: `iss: someone-else` → 401, `iss: milaavo` → 200. Add an `aud` claim if more than one service ever shares a key.
- **No revocation.** Logout, password change or a disabled account do not invalidate issued tokens for up to 15 minutes. That is acceptable at this lifetime. If it ever matters: add a `token_version` on the user and put it in a claim, or keep a short deny-list.
- **No authorities.** Every authenticated user has the same rights. Add roles (`ROLE_USER`, `ROLE_ADMIN`) when admin features appear.
- **Refresh tokens** (Proposed, only if the product needs long sessions): opaque, stored hashed, rotated on every use, with reuse detection that revokes the whole token family.
- **Key rotation.** Moving to RS256/ES256 with a `kid` header lets keys rotate without downtime and lets other services verify without the signing secret.

## Authorization

**Report ownership (G-06, fixed). Implemented.**

Before this fix, any logged-in user could read, edit or delete any report. This is called BOLA (Broken Object Level Authorization), and it is No. 1 in the OWASP API Security list. Now:

- Every report has an `owner_id` (changeset 008), taken from the logged-in user when the report is created.
- All report queries filter by owner: `findByIdAndOwnerId` for get, update and delete, `findByOwnerId` for the list.
- Someone else's report returns **404, not 403**. 403 would tell an attacker "this id exists". 404 tells them nothing.
- The owner is **never** taken from the request body. Only from the token.

### How the logged-in user reaches the controller (`AuthUser` context)

Think of it like a visitor badge in an office:

1. **Gate check:** Spring Security checks the JWT (signature, algorithm, expiry).
2. **Badge:** `AuthUserJwtConverter` reads the token's `sub` claim and writes `AuthUser(userId)` on the "badge" (`AuthUserAuthenticationToken`). If `sub` is missing or not a UUID, it throws `InvalidBearerTokenException`, so the user gets **401**, not 500.
3. **Reading the badge:** in the controller, `@CurrentUser AuthUser currentUser` gives the logged-in user. `@CurrentUser` is our short name for Spring's `@AuthenticationPrincipal`.

| Class (`com.milaavo.common.security`) | Job |
|---|---|
| `AuthUser` | Our own "logged-in user" record. Only `userId` for now; roles later |
| `AuthUserAuthenticationToken` | Holds `AuthUser` as the principal and the `Jwt` as credentials |
| `AuthUserJwtConverter` | Turns a verified `Jwt` into the token above. The only class that knows the user id is in `sub` |
| `CurrentUser` | Annotation for controller parameters |

It is wired in `SecurityConfig` with `.jwt(jwt -> jwt.jwtAuthenticationConverter(authUserJwtConverter))`.

**Rule:** controllers read the user, and services receive it as a parameter (`AuthUser currentUser`). Services do not read `SecurityContextHolder`, because it is stored per thread and is empty in `@Async` code, scheduled jobs and other threads.

## Other security review items

| Item | Status |
|---|---|
| Secrets in env vars, `.env` gitignored | Implemented |
| Separate keys for JWT and OTP HMAC | Implemented (two variables). Document that they must differ |
| Security logging (G-12) | Fixed: default `INFO`. For one debugging run use `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG` |
| CORS | Not configured. Fine for non-browser clients. Add an explicit allow-list before a web front end |
| Security headers | Spring Security defaults (HSTS on HTTPS, `X-Content-Type-Options`, frame options) |
| TLS | Expected to terminate at the load balancer or ingress (Proposed, [08](08-configuration-deployment.md)) |
| Mobile number normalization (G-14) | Only `trim()`. `+91 98…`, `098…` and `98…` become different accounts. Normalize to E.164 (for example with libphonenumber) before the uniqueness check |
| Account enumeration | Register 409 reveals existing numbers (see [03](03-api.md)) |
| Unknown `userId` on verify/resend (G-07) | Fixed: returns 400 `INVALID_VERIFICATION_REQUEST` with a generic message, so user ids can't be probed through a 500 |
