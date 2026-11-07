import { useNavigate, useParams } from 'react-router'
import { ReportForm } from '../components/ReportForm'
import { ReportNotFound } from '../components/ReportNotFound'
import { useReport, useUpdateReport } from '../queries'
import type { ReportFormValues } from '../schemas'
import { isNotFound } from '../reportErrors'
import { Alert, Button, Card, Spinner } from '../../../shared/ui'

export function EditReportPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { data: report, isPending, isPlaceholderData, isError, error, refetch, isFetching } = useReport(id)
  const updateReport = useUpdateReport()

  async function handleSubmit(values: ReportFormValues) {
    await updateReport.mutateAsync({ id, input: values })
    navigate(`/reports/${id}`, { replace: true, state: { notice: 'Report updated.' } })
  }

  // Wait for fresh data: never start editing a possibly stale copy from the list cache.
  if (isPending || isPlaceholderData) {
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

  return (
    <Card className="space-y-5">
      <h1 className="text-xl font-semibold">Edit report</h1>
      <ReportForm
        defaultValues={{
          type: report.type,
          itemName: report.itemName,
          description: report.description,
          city: report.city,
        }}
        submitLabel="Save changes"
        cancelTo={`/reports/${id}`}
        onSubmit={handleSubmit}
      />
    </Card>
  )
}