import { http } from '../../shared/api/http'
import type {
  RegisterRequest,
  RegisterResponse,
  ResendOtpResponse,
  VerifyMobileRequest,
  LoginRequest,
  LoginResponse,
  VerifyMobileState,
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

export async function login(body: LoginRequest): Promise<LoginResponse> {
  const { data } = await http.post<LoginResponse>('/api/auth/login', body)
  return data
}

// The response has exactly what the OTP page needs: userId, challengeId and mobileNumber.
export async function startVerification(body: LoginRequest): Promise<VerifyMobileState> {
  const { data } = await http.post<VerifyMobileState>('/api/auth/verify/start', body)
  return data
}

// The browser sends the HttpOnly cookie by itself. JS never sees the refresh token.
export async function refreshSession(): Promise<LoginResponse> {
  const { data } = await http.post<LoginResponse>('/api/auth/refresh')
  return data
}

export async function logoutSession(): Promise<void> {
  await http.post('/api/auth/logout')
}