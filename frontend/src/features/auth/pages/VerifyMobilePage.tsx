import { Link, useLocation } from 'react-router'
import type { VerifyMobileState } from '../types'
import { Alert, Card } from '../../../shared/ui'

export function VerifyMobilePage() {
  const location = useLocation()
  const state = location.state as VerifyMobileState | null

  // Navigation state is lost on page reload.
  if (!state) {
    return (
      <Card className="mx-auto max-w-sm space-y-4">
        <h1 className="text-xl font-semibold">Verify your mobile</h1>
        <Alert tone="warning">We couldn't find your registration details. Please register again or log in.</Alert>
        <div className="flex gap-4 text-sm font-medium">
          <Link to="/register" className="text-brand hover:underline">Register</Link>
          <Link to="/login" className="text-brand hover:underline">Log in</Link>
        </div>
      </Card>
    )
  }

  return (
    <Card className="mx-auto max-w-sm space-y-2">
      <h1 className="text-xl font-semibold">Verify your mobile</h1>
      <p className="text-sm text-ink-muted">
        We sent a 6-digit OTP to <span className="font-medium text-ink">{state.mobileNumber}</span>.
      </p>
      <p className="text-sm text-ink-muted">The OTP form comes in step F4b.</p>
    </Card>
  )
}