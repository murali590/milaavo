import { useState } from 'react' // NEW
import { useQueryClient } from '@tanstack/react-query' // NEW
import { useLocation, useNavigate, useParams } from 'react-router'
import { ReportNotFound } from '../components/ReportNotFound'
import { isNotFound } from '../reportErrors'
import { reportKeys, useDeleteReport, useReport } from '../queries'
import { REPORT_TYPE_INFO } from '../reportTypes'
import { formatDate } from '../../../shared/format/date'
import { Alert, Badge, Button, Card, ConfirmDialog, LinkButton, Spinner } from '../../../shared/ui'

export function ReportDetailPage() {
  const { id = '' } = useParams()
  const location = useLocation()
  const navigate = useNavigate() // NEW
  const queryClient = useQueryClient() // NEW
  const navState = location.state as { notice?: string; listSearch?: string } | null
  const { data: report, isPending, isError, error, refetch, isFetching } = useReport(id)
  const deleteReport = useDeleteReport() // NEW
  const [confirmOpen, setConfirmOpen] = useState(false) // NEW
  const [deleteError, setDeleteError] = useState<string | null>(null) // NEW

  const backToList = `/reports${navState?.listSearch ?? ''}`

  // NEW
  async function handleDelete() {
    setDeleteError(null)
    try {
      await deleteReport.mutateAsync(id)
    } catch (error) {
      // Already deleted (e.g. in another tab): the user's goal is reached, so carry on as success.
      if (!isNotFound(error)) {
        setConfirmOpen(false)
        setDeleteError(error instanceof Error ? error.message : 'Could not delete the report. Please try again.')
        return
      }
      await queryClient.invalidateQueries({ queryKey: reportKeys.lists() })
    }

    navigate(backToList, { replace: true, state: { notice: 'Report deleted.' } })
    // Navigate first, then forget the cached detail: removing it while this page is still
    // on screen would make it refetch a report that no longer exists.
    queryClient.removeQueries({ queryKey: reportKeys.detail(id), exact: true })
  }

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
      {deleteError && <Alert tone="danger">{deleteError}</Alert>} {/* NEW */}

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

        {/* NEW: Delete on the left (on wider screens), away from Edit, so it's hard to hit by mistake */}
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center">
          <Button variant="danger" onClick={() => setConfirmOpen(true)} className="sm:mr-auto">
            Delete
          </Button>
          <LinkButton to={backToList} variant="secondary">
            Back to my reports
          </LinkButton>
          <LinkButton to={`/reports/${report.reportId}/edit`}>Edit</LinkButton>
        </div>
      </Card>

      {/* NEW */}
      <ConfirmDialog
        open={confirmOpen}
        title="Delete this report?"
        confirmLabel="Delete report"
        loading={deleteReport.isPending}
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
      >
        “{report.itemName}” will be removed permanently. This can't be undone.
      </ConfirmDialog>
    </div>
  )
}