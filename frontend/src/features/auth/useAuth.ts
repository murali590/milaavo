import { useSyncExternalStore } from 'react'
import { sessionStore } from './sessionStore'
import { logout } from './sessionRefresh'

export function useAuth() {
  const { session, endReason, restoring } = useSyncExternalStore(sessionStore.subscribe, sessionStore.getState)

  return {
    session,
    user: session?.user ?? null,
    isAuthenticated: session !== null,
    isRestoring: restoring,
    endReason,
    signIn: sessionStore.signIn,
    logout,
  }
}