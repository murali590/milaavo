import { useEffect, useState } from 'react'
import { http } from './shared/api/http'
import { isApiError } from './shared/api/ApiError'
import { Alert, Button, Card } from './shared/ui'

type Health = {
  status: string
  application: string
}

export function ApiPlayground() {
  const [health, setHealth] = useState<Health | null>(null)
  const [result, setResult] = useState<string>('')

  useEffect(() => {
    http.get<Health>('/api/health')
      .then((response) => setHealth(response.data))
      .catch((error: unknown) => setResult(describe(error)))
  }, [])

  async function run(label: string, call: () => Promise<unknown>) {
    try {
      await call()
      setResult(`${label}: success`)
    } catch (error) {
      setResult(`${label}: ${describe(error)}`)
    }
  }

  return (
    <main className="min-h-screen p-4 sm:p-8">
      <div className="mx-auto max-w-md space-y-4">
        <h1 className="text-2xl font-semibold">API playground</h1>

        <Alert tone={health ? 'success' : 'info'}>
          {health ? `${health.application} API is ${health.status}` : 'Checking API…'}
        </Alert>

        <Card className="space-y-3">
          <Button
            fullWidth
            onClick={() => run('Wrong password', () =>
              http.post('/api/auth/login', { mobileNumber: '+919800000001', password: 'wrong-password-123' }))}
          >
            Login with wrong password
          </Button>
          <Button
            fullWidth
            variant="secondary"
            onClick={() => run('Invalid fields', () =>
              http.post('/api/auth/login', { mobileNumber: '', password: 'short' }))}
          >
            Login with invalid fields
          </Button>
          <Button
            fullWidth
            variant="secondary"
            onClick={() => run('No token', () => http.get('/api/reports'))}
          >
            Get reports without a token
          </Button>
        </Card>

        {result && (
          <pre className="whitespace-pre-wrap break-words rounded-lg bg-surface-muted p-3 text-xs">{result}</pre>
        )}
      </div>
    </main>
  )
}

function describe(error: unknown): string {
  if (isApiError(error)) {
    return `status=${error.status} code=${error.code}\nmessage="${error.message}"\nfieldErrors=${JSON.stringify(error.fieldErrors)}`
  }
  return `unexpected: ${String(error)}`
}