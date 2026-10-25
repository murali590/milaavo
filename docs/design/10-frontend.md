# 10. Frontend (web app)

*Started 25 Oct 2026. Status: planning. Product scope: [requirements](../product/requirements.md).*

One responsive web app that works well in phone browsers. No native apps (see [vision](../product/vision.md#non-goals-for-now)).

## Decisions

| Topic | Choice | Why |
|---|---|---|
| Location | `frontend/` next to `backend/` in this repo | One repo for API and UI: one commit can change both, and CI/deploy stay simple. Each side keeps its own build files in its own folder |
| Build tool | Vite | Fast dev server, simple config, the standard for new React apps |
| Language | TypeScript | Types for API requests and responses catch mistakes before the browser does. Expected in most full-stack roles |
| UI library | React | Existing strength; the largest ecosystem |
| Styling | Tailwind CSS | Mobile-first utility classes (`md:`, `lg:` for bigger screens), no separate CSS files to keep in sync |
| Routing | React Router | Pages and protected routes |
| Server data | TanStack Query | Caching, loading and error states, retries, and refetch after changes. Avoids hand-written `useEffect` fetching everywhere |
| Forms | react-hook-form + zod | Form state without re-render storms; zod rules mirror the backend limits (item name 200, city 100, description 2000, password 12–72) |
| API address | `VITE_API_BASE_URL` (dev: `http://localhost:8091`) | Calls go cross-origin on purpose, so the CORS allow-list (G-27) is exercised from day one. In production the value changes, not the code |

## Login token in the browser

**Decision: keep the access token in memory only** (a React context), never in `localStorage`.

| Option | Survives reload | Risk |
|---|---|---|
| **Memory** (chosen) | No, reload logs you out | An XSS bug can use the token only while the page is open; it can't be read from storage later |
| `localStorage` | Yes | Any injected script can read and send it anywhere. It stays there until removed |
| `HttpOnly` cookie | Yes | JavaScript can't read it at all. Needs CSRF care and `allowCredentials` in CORS |

Access tokens live 15 minutes, so losing them on reload is a small cost for now. **Planned improvement:** a refresh token in an `HttpOnly`, `Secure`, `SameSite=Strict` cookie, issued by the backend (FR-AUTH-4/5). The page then gets a new access token after a reload without ever seeing the refresh token.

## Folder structure (target)

Feature folders, like the backend packages:

```
frontend/
├── .env.development          VITE_API_BASE_URL (not a secret)
├── index.html
├── vite.config.ts
└── src/
    ├── main.tsx              entry point
    ├── index.css             Tailwind import
    ├── app/                  router, providers (Query client, auth), layout
    ├── features/
    │   ├── auth/             api.ts, AuthContext, Register / VerifyOtp / Login pages
    │   └── reports/          api.ts, list / create / detail / edit pages and components
    └── shared/
        ├── api/              http client, ApiError, error-code helpers
        └── ui/               Button, TextField, Spinner, ErrorMessage …
```

## How the UI uses the API contract

- Every error body is `ApiErrorResponse` (`status`, `error`, `message`, `fieldErrors`). The client turns it into one `ApiError` type.
- Screens branch on **`error` codes, never on message text** (G-22). For example: `OTP_EXPIRED` and `MAX_ATTEMPTS_EXCEEDED` → show "Resend OTP", `SUPERSEDED` → "use the latest OTP", `TOO_MANY_REQUESTS` → keep the resend button disabled.
- `fieldErrors` are shown next to the matching input.
- **401** on a protected call → clear the token and go to the login page.

## Screens for the first version (backend already supports these)

| Screen | API |
|---|---|
| Register | `POST /api/auth/register` |
| Verify mobile (OTP entry, resend with 60 s countdown) | `POST /api/auth/verify-mobile`, `POST /api/auth/verify/resend-otp` |
| Login | `POST /api/auth/login` |
| My reports (paged list) | `GET /api/reports?page&size` |
| New report | `POST /api/reports` |
| Report detail | `GET /api/reports/{id}` |
| Edit report | `PUT /api/reports/{id}` |
| Delete report (with confirm) | `DELETE /api/reports/{id}` |

## Build plan (one step at a time)

| Step | What | Learning focus |
|---|---|---|
| **F1** | Create the project: Vite + React + TS + Tailwind, `VITE_API_BASE_URL`, a health-check page | Tooling, env vars, a real CORS call from the browser |
| F2 | API client: `fetch` wrapper, `ApiError`, attach the token | One place for HTTP and errors |
| F3 | Routing, layout, auth state in memory, protected routes | Context, route guards |
| F4 | Register → verify OTP → resend (countdown, error codes) | Forms, validation, UX for error codes |
| F5 | Login, logout, handle 401 | Session flow |
| F6 | My reports list with paging | TanStack Query, loading and empty states |
| F7 | Create, edit, delete report | Mutations, cache invalidation, zod rules matching the backend |
| F8 | Polish: mobile layout, accessibility, error and loading states | Responsive design, a11y basics |

Later (with backend work): refresh-token cookie, found items, photos, matching and claims.
