import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router'
import { registerUser } from '../api'
import { normalizeIndianMobile, registerSchema, type RegisterFormValues } from '../schemas'
import type { VerifyMobileState } from '../types'
import { isApiError } from '../../../shared/api/ApiError'
import { ErrorCode } from '../../../shared/api/errorCodes'
import { applyServerFieldErrors } from '../../../shared/forms/applyServerFieldErrors'
import { Alert, Button, Card, TextField } from '../../../shared/ui'

// Fields the backend may report errors for.
const SERVER_FIELDS = ['name', 'mobileNumber', 'password'] as const

export function RegisterPage() {
  const navigate = useNavigate()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched',
  })

  async function onSubmit(values: RegisterFormValues) {
    setFormError(null)

    try {
      const response = await registerUser({
        name: values.name,
        mobileNumber: normalizeIndianMobile(values.mobileNumber) ?? values.mobileNumber,
        password: values.password,
      })

      const state: VerifyMobileState = {
        userId: response.userId,
        challengeId: response.challengeId,
        mobileNumber: response.mobileNumber,
      }
      navigate('/verify-mobile', { state })
    } catch (error) {
      if (!isApiError(error)) {
        setFormError('Something went wrong. Please try again.')
        return
      }

      if (error.code === ErrorCode.MOBILE_NUMBER_ALREADY_REGISTERED) {
        setError('mobileNumber', {
          type: 'server',
          message: 'This number is already registered. Please log in instead.',
        })
        return
      }

      if (error.code === ErrorCode.VALIDATION_ERROR && applyServerFieldErrors(error, setError, SERVER_FIELDS)) {
        return
      }

      setFormError(error.message)
    }
  }

  return (
    <Card className="mx-auto max-w-sm space-y-5">
      <div>
        <h1 className="text-xl font-semibold">Create your account</h1>
        <p className="mt-1 text-sm text-ink-muted">We'll send an OTP to verify your mobile number.</p>
      </div>

      {formError && <Alert tone="danger">{formError}</Alert>}

      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Full name"
          autoComplete="name"
          error={errors.name?.message}
          {...register('name')}
        />
        <TextField
          label="Mobile number"
          type="tel"
          inputMode="tel"
          autoComplete="tel"
          placeholder="98765 43210"
          hint="Indian mobile number. We'll add +91 for you."
          error={errors.mobileNumber?.message}
          {...register('mobileNumber')}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          hint="At least 12 characters"
          error={errors.password?.message}
          {...register('password')}
        />
        <TextField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />

        <Button type="submit" fullWidth loading={isSubmitting}>
          Create account
        </Button>
      </form>

      <p className="text-center text-sm text-ink-muted">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-brand hover:underline">
          Log in
        </Link>
      </p>
    </Card>
  )
}