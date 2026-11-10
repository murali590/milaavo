import { logoutSession, refreshSession } from './api'
import { sessionStore } from './sessionStore'

// Only one refresh at a time. Each refresh token works once (rotation), so two parallel
// refreshes with the same cookie look like reuse, and the backend logs the user out.
let inFlight: Promise<string | null> | null = null


// Proactive refresh at 80% of the token's life, so active users never hit a 401.
// The 401 + retry in http.ts stays as the safety net (sleep, throttled timers, revoked tokens).
const REFRESH_AT = 0.8
let timer: ReturnType<typeof setTimeout> | null = null
let scheduledToken: string | null = null
let dueAt = Number.POSITIVE_INFINITY



// Returns the new access token, or null if there is no valid session.
export function refreshAccessToken(): Promise<string | null> {
  if (inFlight === null) {
    inFlight = refreshSession()
      .then((login) => {
        sessionStore.signIn(login)
        return login.accessToken
      })
      .catch(() => null)
      .finally(() => {
        inFlight = null
      })
  }
  return inFlight
}

// Called once on page load.
export async function restoreSession(): Promise<void> {
  await refreshAccessToken()
  sessionStore.finishRestoring()
}

// Tell the server first (it revokes the token and clears the cookie), then clear memory.
// Wait for the server, otherwise a quick reload could restore the session we just left.
export async function logout(): Promise<void> {
  try {
    await logoutSession()
  } catch {
    // Server unreachable: still log out on this device.
  } finally {
    sessionStore.signOut('manual')
  }
}

// Refresh only for a visible tab, so an unattended tab doesn't keep the session alive forever.
function refreshIfDue(): void {
  if (
    sessionStore.getAccessToken() !== null &&
    document.visibilityState === 'visible' &&
    Date.now() >= dueAt
  ) {
    void refreshAccessToken()
  }
}

export function startProactiveRefresh(): void {
  sessionStore.subscribe(() => {
    const session = sessionStore.getState().session
    const token = session?.accessToken ?? null

    // Reschedule only when the token changes (sign in, refresh, sign out).
    if (token === scheduledToken) {
      return
    }
    scheduledToken = token

    if (timer !== null) {
      clearTimeout(timer)
      timer = null
    }
    if (session === null) {
      dueAt = Number.POSITIVE_INFINITY
      return
    }

    const delay = Math.max(0, (session.expiresAt - Date.now()) * REFRESH_AT)
    dueAt = Date.now() + delay
    timer = setTimeout(refreshIfDue, delay)
  })

  // The tab was hidden when the timer fired: refresh as soon as the user comes back.
  document.addEventListener('visibilitychange', refreshIfDue)
}