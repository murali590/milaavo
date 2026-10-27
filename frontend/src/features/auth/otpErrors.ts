import type { ApiError } from '../../shared/api/ApiError'
import { ErrorCode } from '../../shared/api/errorCodes'

// What the OTP page should do for each error. Pages switch on `kind`, never on message text.
export type OtpErrorView =
  | { kind: 'field'; message: string }
  | { kind: 'alert'; tone: 'warning' | 'danger'; message: string }
  | { kind: 'alreadyVerified' }

export function describeOtpError(error: ApiError): OtpErrorView {
  switch (error.code) {
    case ErrorCode.INVALID_OTP:
      return { kind: 'field', message: 'Incorrect OTP. Please check and try again.' }
    case ErrorCode.OTP_EXPIRED:
      return { kind: 'alert', tone: 'warning', message: 'This OTP has expired. Please request a new one.' }
    case ErrorCode.SUPERSEDED:
      return { kind: 'alert', tone: 'warning', message: 'You requested a newer OTP. Please use the latest one we sent.' }
    case ErrorCode.MAX_ATTEMPTS_EXCEEDED:
      return { kind: 'alert', tone: 'danger', message: 'Too many wrong attempts. Please request a new OTP.' }
    case ErrorCode.OTP_ALREADY_USED:
    case ErrorCode.USER_ALREADY_ACTIVE:
      return { kind: 'alreadyVerified' }
    case ErrorCode.INVALID_VERIFICATION_REQUEST:
      return { kind: 'alert', tone: 'danger', message: "We couldn't find this registration. Please register again." }
    case ErrorCode.TOO_MANY_REQUESTS:
      // Same code for "wait 60 s" and "3 per 15 min": the server's message says which one.
      return { kind: 'alert', tone: 'warning', message: error.message }
    default:
      return { kind: 'alert', tone: 'danger', message: error.message }
  }
}