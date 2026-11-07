import { Card, LinkButton } from '../../../shared/ui'

export function ReportNotFound() {
  return (
    <Card className="space-y-3 py-10 text-center">
      <h1 className="text-xl font-semibold">Report not found</h1>
      <p className="text-sm text-ink-muted">It may have been deleted, or the link is wrong.</p>
      <LinkButton to="/reports" variant="secondary">
        Go to my reports
      </LinkButton>
    </Card>
  )
}