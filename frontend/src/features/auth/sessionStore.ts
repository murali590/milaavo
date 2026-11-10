import type { LoginResponse, Session, SignOutReason } from './types'

type SessionState = {
  session: Session | null
  endReason: SignOutReason | null
  // True until the first refresh attempt on page load has finished.
  restoring: boolean
}

type Listener = () => void

// The access token stays in memory only. After a reload it comes back through the refresh cookie.
let state: SessionState = { session: null, endReason: null, restoring: true }
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
      restoring: false,
    })
  },

  signOut(reason: SignOutReason): void {
    if (state.session === null) {
      return
    }
    setState({ session: null, endReason: reason, restoring: false })
  },

  finishRestoring(): void {
    if (state.restoring) {
      setState({ ...state, restoring: false })
    }
  },
}