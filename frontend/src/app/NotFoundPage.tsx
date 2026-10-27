import { Link } from 'react-router'
import { Card } from '../shared/ui'

export function NotFoundPage() {
  return (
    <Card className="space-y-2 text-center">
      <h1 className="text-xl font-semibold">Page not found</h1>
      <p className="text-sm text-ink-muted">The page you are looking for doesn't exist.</p>
      <Link to="/" className="inline-block text-sm font-medium text-brand hover:underline">
        Go to home
      </Link>
    </Card>
  )
}