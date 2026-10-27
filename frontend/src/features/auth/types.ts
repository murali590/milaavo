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

// Why the last session ended, so the login page can explain it.
export type SignOutReason = 'manual' | 'expired'