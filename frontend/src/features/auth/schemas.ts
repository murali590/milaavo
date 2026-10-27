import { z } from 'zod'

// Accepts "98765 43210", "+91 98765 43210", "09876543210" ... and returns "+919876543210".
// Indian mobile numbers only for now: 10 digits starting with 6-9.
export function normalizeIndianMobile(input: string): string | null {
  const compact = input.replace(/[\s-]/g, '')
  const match = /^(?:\+91|91|0)?([6-9]\d{9})$/.exec(compact)
  return match ? `+91${match[1]}` : null
}

// Same limits as RegisterRequest in the backend (name 100, password 12-72).
export const registerSchema = z
  .object({
    name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be at most 100 characters'),
    mobileNumber: z
      .string()
      .trim()
      .min(1, 'Mobile number is required')
      .refine((value) => normalizeIndianMobile(value) !== null, 'Enter a valid 10-digit Indian mobile number'),
    password: z
      .string()
      .min(12, 'Password must be at least 12 characters')
      .max(72, 'Password must be at most 72 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

export type RegisterFormValues = z.infer<typeof registerSchema>

export const otpSchema = z.object({
  otp: z.string().trim().regex(/^\d{6}$/, 'Enter the 6-digit OTP'),
})

export type OtpFormValues = z.infer<typeof otpSchema>