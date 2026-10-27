// Matches LoginResponse in the backend.
export type LoginResponse = {
  userId: string
  name: string
  mobileNumber: string
  mobileVerified: boolean
  status: string
  accessToken: string
  tokenType: string
  expiresIn: number
}

export type SessionUser = {
  userId: string
  name: string
  mobileNumber: string
}

export type Session = {
  accessToken: string
  // Epoch millis when the token expires.
  expiresAt: number
  user: SessionUser
}

export type RegisterRequest = {
  name: string
  mobileNumber: string
  password: string
}

// Matches RegisterResponse in the backend.
export type RegisterResponse = {
  userId: string
  name: string
  mobileNumber: string
  mobileVerified: boolean
  status: string
  createdAt: string
  challengeId: string
}

export type VerifyMobileRequest = {
  userId: string
  challengeId: string
  otp: string
}

export type ResendOtpResponse = {
  challengeId: string
}

// Passed from the register page to the OTP page through navigation state.
export type VerifyMobileState = {
  userId: string
  challengeId: string
  mobileNumber: string
}

// Why the last session ended, so the login page can explain it.
export type SignOutReason = 'manual' | 'expired'