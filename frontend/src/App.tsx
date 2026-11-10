import { RouterProvider } from 'react-router/dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './app/queryClient'
import { router } from './app/router'
import { useAuth } from './features/auth/useAuth'
import { Spinner } from './shared/ui'

export default function App() {
  const { isRestoring } = useAuth()

  // Wait for the refresh cookie check, so guards don't send a logged-in user to /login.
  if (isRestoring) {
    return (
      <div className="grid min-h-screen place-items-center text-brand">
        <Spinner className="size-8" label="Loading Milaavo" />
      </div>
    )
  }

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}