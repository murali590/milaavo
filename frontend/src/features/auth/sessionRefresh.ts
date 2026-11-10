import { logoutSession, refreshSession } from './api'
import { sessionStore } from './sessionStore'

// Only one refresh at a time. Each refresh token works once (rotation), so two parallel
// refreshes with the same cookie look like reuse, and the backend logs the user out.
let inFlight: Promise<string | null> | null = null

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