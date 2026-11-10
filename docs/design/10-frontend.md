# 10. Frontend (web app)

*Started 25 Oct 2026. Status: F1–F7 done (7 Nov 2026), refresh-token session done (10 Nov 2026); next is deployment, then F8. Product scope: [requirements](../product/requirements.md).*

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
| HTTP client | axios | Interceptors for the token and for 401, a base URL and timeout in one place, and errors for every non-2xx response |
| Server data | TanStack Query | Caching, loading and error states, retries, and refetch after changes. Avoids hand-written `useEffect` fetching everywhere |
| Forms | react-hook-form + zod | Form state without re-render storms; zod rules mirror the backend limits (item name 200, city 100, description 2000, password 12–72) |
| API address | `VITE_API_BASE_URL` (dev: `http://localhost:8091`) | Calls go cross-origin on purpose, so the CORS allow-list (G-27) is exercised from day one. In production the value changes, not the code |

## UI theme

*Decided 26 Oct 2026.* Tokens live in `frontend/src/index.css`.

| Choice | Value | Why |
|---|---|---|
| Brand colour | Teal. Buttons and links use **teal-700 `#0f766e`** (hover teal-800 `#115e59`); soft highlight teal-50 | Calm and trustworthy, and different from the usual blue apps. Teal-600 with white text is only 3.7:1 contrast, below WCAG AA (4.5:1); teal-700 is 5.5:1 |
| Lost / found | Lost = amber (`#b45309` on `#fffbeb`), found = emerald (`#047857` on `#ecfdf5`) | Instantly tells the two report types apart. Both pairs pass AA |
| Status | Danger red-600, success emerald-700, warning amber-700, each with a soft background | Standard meanings |
| Neutrals | Page slate-50, cards white, borders slate-200, text slate-900, muted text slate-500 (4.8:1) | Quiet background so the brand colour stands out |
| Font | Inter (variable), self-hosted with `@fontsource-variable/inter` | Very readable on phones. Self-hosting means no request to Google, works offline, and no tracking |
| Dark mode | Not yet, but **dark-ready** | Components use semantic tokens (`bg-surface`, `text-ink`), never raw colours. Dark mode later = new token values only |

### Rules for every component

1. **Use tokens, never raw colours.** Write `bg-brand`, `text-ink-muted`, `border-line`, not `bg-teal-700` or `text-slate-500`.
2. **Mobile first.** Base classes are for phones; add `sm:`/`md:` for bigger screens.
3. **Tap targets at least 44 px high** (`h-11`) for buttons and inputs.
4. **Visible keyboard focus** on everything clickable (a global `:focus-visible` outline).
5. **Text contrast at least 4.5:1** (WCAG AA). Check before adding a new colour.
6. **One radius for cards** (`rounded-card`, 1 rem) and the default spacing scale (`p-4`, `gap-3`…).

## Login token in the browser

**Decision: keep the access token in memory only** (an external store), never in `localStorage`. **The refresh token is an `HttpOnly` cookie** (done 10 Nov 2026), so a reload no longer logs the user out.

| Option | Survives reload | Risk |
|---|---|---|
| **Memory** (chosen) | No, reload logs you out | An XSS bug can use the token only while the page is open; it can't be read from storage later |
| `localStorage` | Yes | Any injected script can read and send it anywhere. It stays there until removed |
| `HttpOnly` cookie | Yes | JavaScript can't read it at all. Needs CSRF care and `allowCredentials` in CORS |

How the session works now (`features/auth/sessionRefresh.ts`, `shared/api/http.ts`):

| When | What happens |
|---|---|
| Page load | `restoreSession()` runs once from `main.tsx` (outside React, so StrictMode can't run it twice). `App` shows a spinner until it finishes, so route guards don't bounce a logged-in user to `/login` |
| At 80% of the access token's life | A timer refreshes, only if the tab is visible. If the tab was hidden, it refreshes as soon as the tab becomes visible again. A hidden tab doesn't keep the session alive |
| A call gets 401 | The response interceptor refreshes **once** (`_retried` marker) and retries the call. If the refresh fails → sign out with "session expired". Safety net for laptop sleep, throttled timers and revoked tokens |
| Many calls need a refresh at once | All wait on **one** shared promise (single-flight). Two refreshes with the same cookie would look like reuse and end the session |
| Log out | `POST /api/auth/logout` first (revokes and clears the cookie), then clear memory |

Two rules in `http.ts`: `withCredentials: true` (needed to send and store the cookie cross-origin), and **no `Authorization` header on `/api/auth/*`**, because Spring rejects an expired Bearer token even on public endpoints.

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
| **F1** | ✅ Create the project: Vite + React + TS + Tailwind, `VITE_API_BASE_URL`, a health-check page | Tooling, env vars, a real CORS call from the browser |
| F1.5 | ✅ UI theme: colour tokens, Inter font, base styles | Design tokens, contrast, dark-ready CSS |
| F2 | ✅ API client on axios: instance, interceptors, `ApiError`, attach the token | One place for HTTP and errors |
| F3 | ✅ Routing, layout, auth state in memory, protected routes | Context, route guards |
| F4 | ✅ Register → verify OTP → resend (countdown, error codes) | Forms, validation, UX for error codes |
| F5 | ✅ Login, logout, handle 401 | Session flow |
| F6 | ✅ My reports list with paging | TanStack Query, loading and empty states |
| F7 | ✅ Create, edit, delete report | Mutations, cache invalidation, zod rules matching the backend |
| R | ✅ Refresh-token session: restore on reload, proactive + on-401 refresh, server logout | HttpOnly cookies, CORS credentials, single-flight |
| F8 | Polish: mobile layout, accessibility, error and loading states | Responsive design, a11y basics |

Notes from building it:

- F3: the session is an external store (`sessionStore` + `useSyncExternalStore`), not React Context, because the axios interceptor also needs the token.
- F5: after login the page only calls `signIn`; `RedirectIfAuthenticated` does the redirect to `from` (path + query), so there is one owner and no race.
- F6/F7: query keys come from `reportKeys`. Create and delete invalidate the lists; update writes the PUT response into the detail cache. The cache is cleared on sign-out.
- F7: reports have a type (Lost, Found, Swapped) with metadata in `REPORT_TYPE_INFO`; delete uses a native `<dialog>` and treats 404 as already deleted.
- Adding or changing routes needs a full page reload in dev: `RouterProvider` keeps the router it started with.

Next: deployment (the cookie needs the frontend and API on the same site, see [08](08-configuration-deployment.md#deployment-checklist)), then F8. Later: found-item details, photos, matching and claims.
