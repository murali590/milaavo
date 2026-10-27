import { useSyncExternalStore } from 'react'
import { sessionStore } from './sessionStore'

export function useAuth() {
  const { session, endReason } = useSyncExternalStore(sessionStore.subscribe, sessionStore.getState)

  return {
    session,
    user: session?.user ?? null,
    isAuthenticated: session !== null,
    endReason,
    signIn: sessionStore.signIn,
    signOut: sessionStore.signOut,
  }
}