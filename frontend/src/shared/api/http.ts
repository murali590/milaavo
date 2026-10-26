import axios from 'axios'
import { ApiError, type ApiErrorBody } from './ApiError'
import { ErrorCode } from './errorCodes'

// Set once by the auth layer (F3), so this file doesn't depend on React.
type HttpConfig = {
  getAccessToken: () => string | null
  onUnauthorized: () => void
}

let config: HttpConfig = {
  getAccessToken: () => null,
  onUnauthorized: () => {},
}

export function configureHttp(next: Partial<HttpConfig>): void {
  config = { ...config, ...next }
}

// The only axios instance the app uses. Feature code calls http.get / http.post etc.
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15_000,
  headers: { Accept: 'application/json' },
})

// Before every request: attach the token, if we have one.
http.interceptors.request.use((request) => {
  const token = config.getAccessToken()
  if (token) {
    request.headers.set('Authorization', `Bearer ${token}`)
  }
  return request
})

// After every response: turn any failure into an ApiError.
http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    // A cancelled request is not a failure: pass it through for the caller to ignore.
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    const apiError = toApiError(error)

    // Only an expired or invalid token should log the user out, not a wrong password on login.
    const sentToken = axios.isAxiosError(error) && Boolean(error.config?.headers?.has('Authorization'))
    if (apiError.status === 401 && sentToken) {
      config.onUnauthorized()
    }

    return Promise.reject(apiError)
  },
)

function toApiError(error: unknown): ApiError {
  if (!axios.isAxiosError(error)) {
    return new ApiError(0, ErrorCode.UNKNOWN_ERROR, 'Something went wrong. Please try again.')
  }

  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return new ApiError(0, ErrorCode.TIMEOUT, 'The server is taking too long. Please try again.')
  }

  // No response at all: server down, CORS blocked or offline. The browser hides which one.
  if (!error.response) {
    return new ApiError(0, ErrorCode.NETWORK_ERROR, 'Cannot reach the server. Please check your connection and try again.')
  }

  const { status, data } = error.response
  if (isApiErrorBody(data)) {
    return new ApiError(status, data.error, data.message, data.fieldErrors ?? null)
  }

  // Not our JSON, e.g. an HTML error page from a proxy.
  return new ApiError(status, ErrorCode.UNKNOWN_ERROR, `Request failed with status ${status}`)
}

function isApiErrorBody(data: unknown): data is ApiErrorBody {
  if (typeof data !== 'object' || data === null) {
    return false
  }
  const body = data as Partial<ApiErrorBody>
  return typeof body.error === 'string' && typeof body.message === 'string'
}