import { useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { http } from '../../../shared/api/http'
import { isApiError } from '../../../shared/api/ApiError'
import { Alert, Button, Card } from '../../../shared/ui'

type PageSummary = {
  totalElements: number
}

export function ReportsPage() {
  const { user } = useAuth()
  const [message, setMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function loadReports() {
    setLoading(true)
    setMessage(null)
    try {
      const { data } = await http.get<PageSummary>('/api/reports')
      setMessage(`You have ${data.totalElements} report(s).`)
    } catch (error) {
      setMessage(isApiError(error) ? `${error.code}: ${error.message}` : 'Unexpected error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card className="space-y-4">
      <h1 className="text-xl font-semibold">My reports</h1>
      <p className="text-sm text-ink-muted">Hi {user?.name}. The real list comes in step F6.</p>
      <Button onClick={loadReports} loading={loading}>
        Load my reports
      </Button>
      {message && <Alert>{message}</Alert>}
    </Card>
  )
}