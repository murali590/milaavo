import { Link, Outlet } from 'react-router'
import { useAuth } from '../features/auth/useAuth'
import { Button } from '../shared/ui'

export function AppLayout() {
  const { user, isAuthenticated, signOut } = useAuth()

  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex h-14 max-w-2xl items-center justify-between px-4">
          <Link to="/" className="text-lg font-semibold text-brand">
            Milaavo
          </Link>

          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <span className="hidden text-sm text-ink-muted sm:inline">Hi, {user?.name}</span>
              <Button variant="ghost" onClick={() => signOut('manual')}>
                Log out
              </Button>
            </div>
          ) : (
            <nav className="flex items-center gap-1">
              <Link to="/login" className="rounded-lg px-3 py-2 text-sm font-medium text-ink hover:bg-surface-muted">
                Log in
              </Link>
              <Link to="/register" className="rounded-lg px-3 py-2 text-sm font-medium text-brand hover:bg-brand-soft">
                Register
              </Link>
            </nav>
          )}
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}