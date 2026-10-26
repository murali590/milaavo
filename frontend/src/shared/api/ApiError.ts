// Shape of every error body from our backend (ApiErrorResponse in Java).
export type ApiErrorBody = {
  status: number
  error: string
  message: string
  fieldErrors: Record<string, string> | null
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Record<string, string>

  constructor(
    status: number,
    code: string,
    message: string,
    fieldErrors: Record<string, string> | null = null,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors ?? {}
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}