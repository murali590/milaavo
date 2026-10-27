import type { LoginResponse, Session, SignOutReason } from './types'

type SessionState = {
  session: Session | null
  endReason: SignOutReason | null
}

type Listener = () => void

// Kept in memory only (never localStorage): a page reload logs the user out.
let state: SessionState = { session: null, endReason: null }
const listeners = new Set<Listener>()

function setState(next: SessionState): void {
  state = next
  listeners.forEach((listener) => listener())
}

export const sessionStore = {
  getState(): SessionState {
    return state
  },

  subscribe(listener: Listener): () => void {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },

  getAccessToken(): string | null {
    return state.session?.accessToken ?? null
  },

  signIn(login: LoginResponse): void {
    setState({
      session: {
        accessToken: login.accessToken,
        expiresAt: Date.now() + login.expiresIn * 1000,
        user: { userId: login.userId, name: login.name, mobileNumber: login.mobileNumber },
      },
      endReason: null,
    })
  },

  signOut(reason: SignOutReason): void {
    if (state.session === null) {
      return
    }
    setState({ session: null, endReason: reason })
  },
}