import { isApiError } from '../../shared/api/ApiError'
import { ErrorCode } from '../../shared/api/errorCodes'

// Not found, someone else's report (also 404, see G-06) or a broken id in the URL: all "not found" to the user.
export function isNotFound(error: unknown): boolean {
  return isApiError(error) && (error.code === ErrorCode.REPORT_NOT_FOUND || error.code === ErrorCode.INVALID_PARAMETER)
}