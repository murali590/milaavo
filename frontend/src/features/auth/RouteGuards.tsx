import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './useAuth'

// Only for logged-in users. Remembers where they wanted to go, for after login.
export function RequireAuth() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

// Login and register pages: a logged-in user has no reason to see them.
export function RedirectIfAuthenticated() {
  const { isAuthenticated } = useAuth()

  if (isAuthenticated) {
    return <Navigate to="/reports" replace />
  }
  return <Outlet />
}

export function HomeRedirect() {
  const { isAuthenticated } = useAuth()
  return <Navigate to={isAuthenticated ? '/reports' : '/login'} replace />
}