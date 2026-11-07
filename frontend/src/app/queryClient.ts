import { QueryClient } from '@tanstack/react-query'
import { isApiError } from '../shared/api/ApiError'

const MAX_RETRIES = 2

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Data counts as fresh for 30 s: no refetch when returning to it in that time.
      staleTime: 30_000,
      // Retry only what may be temporary: network failures (status 0) and server errors (5xx).
      retry: (failureCount, error) => {
        if (isApiError(error) && error.status >= 400 && error.status < 500) {
          return false
        }
        return failureCount < MAX_RETRIES
      },
    },
  },
})