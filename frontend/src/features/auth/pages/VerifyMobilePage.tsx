import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useLocation, useNavigate } from 'react-router'
import { resendVerificationOtp, verifyMobile } from '../api'
import { describeOtpError } from '../otpErrors'
import { otpSchema, type OtpFormValues } from '../schemas'
import type { VerifyMobileState } from '../types'
import { isApiError } from '../../../shared/api/ApiError'
import { useCountdown } from '../../../shared/hooks/useCountdown'
import { Alert, Button, Card, TextField } from '../../../shared/ui'

// Must match app.otp.resend.cooldown-seconds in the backend. The server still enforces it.
const RESEND_COOLDOWN_SECONDS = 60

type PageAlert = {
  tone: 'info' | 'warning' | 'danger'
  message: string
}

export function VerifyMobilePage() {
  const location = useLocation()
  const registration = location.state as VerifyMobileState | null

  // Navigation state is lost on reload. Checked here, outside the form, so hooks are never conditional.
  if (!registration) {
    return <MissingRegistration />
  }

  return <VerifyMobileForm registration={registration} />
}

function VerifyMobileForm({ registration }: { registration: VerifyMobileState }) {
  const navigate = useNavigate()
  const [challengeId, setChallengeId] = useState(registration.challengeId)
  const [pageAlert, setPageAlert] = useState<PageAlert | null>(null)
  const [resending, setResending] = useState(false)
  const { secondsLeft, restart } = useCountdown(RESEND_COOLDOWN_SECONDS)

  const {
    register,
    handleSubmit,
    setError,
    reset,
    setFocus,
    formState: { errors, isSubmitting },
  } = useForm<OtpFormValues>({
    resolver: zodResolver(otpSchema),
    defaultValues: { otp: '' },
  })

  function goToLogin() {
    navigate('/login', { replace: true, state: { verifiedMobile: registration.mobileNumber } })
  }

  function showError(error: unknown) {
    if (!isApiError(error)) {
      setPageAlert({ tone: 'danger', message: 'Something went wrong. Please try again.' })
      return
    }

    const view = describeOtpError(error)
    switch (view.kind) {
      case 'field':
        setError('otp', { type: 'server', message: view.message })
        break
      case 'alert':
        setPageAlert({ tone: view.tone, message: view.message })
        break
      case 'alreadyVerified':
        goToLogin()
        break
    }
  }

  async function onSubmit({ otp }: OtpFormValues) {
    setPageAlert(null)
    try {
      await verifyMobile({ userId: registration.userId, challengeId, otp })
      goToLogin()
    } catch (error) {
      showError(error)
    }
  }

  async function onResend() {
    setPageAlert(null)
    setResending(true)
    try {
      const response = await resendVerificationOtp(registration.userId)
      // The old OTP no longer works: always verify against the newest challenge.
      setChallengeId(response.challengeId)
      reset({ otp: '' })
      restart(RESEND_COOLDOWN_SECONDS)
      setPageAlert({ tone: 'info', message: `We sent a new OTP to ${registration.mobileNumber}.` })
      setFocus('otp')
    } catch (error) {
      showError(error)
    } finally {
      setResending(false)
    }
  }

  return (
    <Card className="mx-auto max-w-sm space-y-5">
      <div>
        <h1 className="text-xl font-semibold">Verify your mobile</h1>
        <p className="mt-1 text-sm text-ink-muted">
          Enter the 6-digit OTP we sent to{' '}
          <span className="font-medium text-ink">{registration.mobileNumber}</span>.
        </p>
      </div>

      {pageAlert && <Alert tone={pageAlert.tone}>{pageAlert.message}</Alert>}

      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="OTP"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          placeholder="6-digit code"
          error={errors.otp?.message}
          {...register('otp')}
        />

        <Button type="submit" fullWidth loading={isSubmitting}>
          Verify
        </Button>
      </form>

      <div className="flex items-center justify-between text-sm">
        <Button variant="ghost" onClick={onResend} loading={resending} disabled={secondsLeft > 0}>
          {secondsLeft > 0 ? `Resend OTP in ${secondsLeft}s` : 'Resend OTP'}
        </Button>
        <Link to="/register" className="font-medium text-ink-muted hover:underline">
          Wrong number?
        </Link>
      </div>
    </Card>
  )
}

function MissingRegistration() {
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