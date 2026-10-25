import { useEffect, useState } from 'react'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

type Health = {
  status: string
  application: string
}

export default function App() {
  const [health, setHealth] = useState<Health | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/health`)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }
        return response.json() as Promise<Health>
      })
      .then(setHealth)
      .catch((err: Error) => setError(err.message))
  }, [])

  return (
    <main className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow">
        <h1 className="text-2xl font-semibold text-slate-900">Milaavo</h1>
        <p className="mt-1 text-sm text-slate-500">Backend connection check</p>

        <div className="mt-6">
          {health && (
            <p className="rounded-lg bg-green-50 p-3 text-green-800">
              ✅ {health.application} API is {health.status}
            </p>
          )}
          {error && (
            <p className="rounded-lg bg-red-50 p-3 text-red-800">
              ❌ Cannot reach the API: {error}
            </p>
          )}
          {!health && !error && <p className="text-slate-500">Checking…</p>}
        </div>
      </div>
    </main>
  )
}