import { useLocation, useParams } from 'react-router'
import { ReportNotFound } from '../components/ReportNotFound'
import { isNotFound } from '../reportErrors'
import { useReport } from '../queries'
import { REPORT_TYPE_INFO } from '../reportTypes'
import { formatDate } from '../../../shared/format/date'
import { Alert, Badge, Button, Card, LinkButton, Spinner } from '../../../shared/ui'



export function ReportDetailPage() {
  const { id = '' } = useParams()
  const location = useLocation()
  const navState = location.state as { notice?: string; listSearch?: string } | null
  const { data: report, isPending, isError, error, refetch, isFetching } = useReport(id)

  const backToList = `/reports${navState?.listSearch ?? ''}`

  if (isPending) {
    return (
      <div className="flex justify-center py-10 text-brand">
        <Spinner label="Loading report" />
      </div>
    )
  }

  if (isError && isNotFound(error)) {
    return <ReportNotFound />
  }

  if (isError) {
    return (
      <div className="space-y-3">
        <Alert tone="danger">{error.message}</Alert>
        <Button variant="secondary" onClick={() => refetch()} loading={isFetching}>
          Try again
        </Button>
      </div>
    )
  }

  const typeInfo = REPORT_TYPE_INFO[report.type]

  return (
    <div className="space-y-4">
      {navState?.notice && <Alert tone="success">{navState.notice}</Alert>}

      <Card className="space-y-4">
        <div className="flex items-start justify-between gap-3">
          <h1 className="min-w-0 text-xl font-semibold wrap-anywhere">{report.itemName}</h1>
          <Badge tone={typeInfo.badgeTone}>{typeInfo.label}</Badge>
        </div>

        <p className="whitespace-pre-wrap text-ink wrap-anywhere">{report.description}</p>

        <dl className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <dt className="text-ink-muted">City</dt>
            <dd className="font-medium wrap-anywhere">{report.city}</dd>
          </div>
          <div>
            <dt className="text-ink-muted">Reported on</dt>
            <dd className="font-medium">{formatDate(report.createdAt)}</dd>
          </div>
        </dl>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <LinkButton to={backToList} variant="secondary">
            Back to my reports
          </LinkButton>
          <LinkButton to={`/reports/${report.reportId}/edit`}>Edit</LinkButton>
        </div>
      </Card>
    </div>
  )
}