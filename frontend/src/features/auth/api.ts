import { http } from '../../shared/api/http'
import type {
  RegisterRequest,
  RegisterResponse,
  ResendOtpResponse,
  VerifyMobileRequest,
} from './types'

export async function registerUser(body: RegisterRequest): Promise<RegisterResponse> {
  const { data } = await http.post<RegisterResponse>('/api/auth/register', body)
  return data
}

export async function verifyMobile(body: VerifyMobileRequest): Promise<void> {
  await http.post('/api/auth/verify-mobile', body)
}

export async function resendVerificationOtp(userId: string): Promise<ResendOtpResponse> {
  const { data } = await http.post<ResendOtpResponse>('/api/auth/verify/resend-otp', { userId })
  return data
}