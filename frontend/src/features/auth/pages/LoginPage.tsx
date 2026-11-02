import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useLocation, useNavigate } from 'react-router'
import { login, startVerification } from '../api'
import { loginSchema, normalizeIndianMobile, type LoginFormValues } from '../schemas'
import type { LoginRequest } from '../types'
import { useAuth } from '../useAuth'
import { isApiError } from '../../../shared/api/ApiError'
import { ErrorCode } from '../../../shared/api/errorCodes'
import { Alert, Button, Card, TextField } from '../../../shared/ui'

function loginErrorMessage(error: unknown): string {
  if (!isApiError(error)) {
    return 'Something went wrong. Please try again.'
  }

  switch (error.code) {
    // A password too short to be valid is simply a wrong password: don't reveal the rules.
    case ErrorCode.INVALID_CREDENTIALS:
    case ErrorCode.VALIDATION_ERROR:
      return 'Invalid mobile number or password.'
    case ErrorCode.ACCOUNT_DISABLED:
      return 'This account has been disabled.'
    default:
      return error.message
  }
}

export function LoginPage() {
  const { endReason, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const navState = location.state as { verifiedMobile?: string } | null
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    mode: 'onTouched',
    defaultValues: { mobileNumber: navState?.verifiedMobile ?? '', password: '' },
  })

  async function resumeVerification(credentials: LoginRequest) {
    try {
      const state = await startVerification(credentials)
      navigate('/verify-mobile', { state })
    } catch (error) {
      setFormError(loginErrorMessage(error))
    }
  }

  async function onSubmit(values: LoginFormValues) {
    setFormError(null)

    const credentials: LoginRequest = {
      mobileNumber: normalizeIndianMobile(values.mobileNumber) ?? values.mobileNumber,
      password: values.password,
    }

    try {
      // RedirectIfAuthenticated sees the new session and redirects: no navigate() here.
      signIn(await login(credentials))
    } catch (error) {
      if (isApiError(error) && error.code === ErrorCode.MOBILE_NOT_VERIFIED) {
        await resumeVerification(credentials)
        return
      }
      setFormError(loginErrorMessage(error))
    }
  }

  return (
    <Card className="mx-auto max-w-sm space-y-5">
      <h1 className="text-xl font-semibold">Log in</h1>

      {navState?.verifiedMobile && (
        <Alert tone="success">{navState.verifiedMobile} is verified. Please log in.</Alert>
      )}

      {endReason === 'expired' && (
        <Alert tone="warning">Your session has expired. Please log in again.</Alert>
      )}

      {formError && <Alert tone="danger">{formError}</Alert>}

      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Mobile number"
          type="tel"
          inputMode="tel"
          autoComplete="username"
          placeholder="98765 43210"
          error={errors.mobileNumber?.message}
          {...register('mobileNumber')}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />

        <Button type="submit" fullWidth loading={isSubmitting}>
          Log in
        </Button>
      </form>

      <p className="text-center text-sm text-ink-muted">
        New to Milaavo?{' '}
        <Link to="/register" className="font-medium text-brand hover:underline">
          Create an account
        </Link>
      </p>
    </Card>
  )
}