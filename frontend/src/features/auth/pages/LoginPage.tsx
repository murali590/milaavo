import { useLocation, useNavigate } from 'react-router'
import { Alert, Button, Card } from '../../../shared/ui'
import { useAuth } from '../useAuth'

export function LoginPage() {
  const { endReason, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/reports'

  // TEMP (F3 only): pretend to log in with a fake token, to test the guards and the 401 flow.
  function simulateSignIn() {
    signIn({
      userId: 'dev-user',
      name: 'Dev User',
      mobileNumber: '+910000000000',
      mobileVerified: true,
      status: 'ACTIVE',
      accessToken: 'fake-token-for-testing',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    navigate(from, { replace: true })
  }

  return (
    <Card className="mx-auto max-w-sm space-y-4">
      <h1 className="text-xl font-semibold">Log in</h1>

      {endReason === 'expired' && (
        <Alert tone="warning">Your session has expired. Please log in again.</Alert>
      )}

      <p className="text-sm text-ink-muted">The real login form comes in step F5.</p>

      <Button fullWidth onClick={simulateSignIn}>
        Simulate sign-in (fake token)
      </Button>
    </Card>
  )
}