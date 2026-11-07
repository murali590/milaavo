import { useNavigate, useSearchParams } from 'react-router'
import { ReportForm } from '../components/ReportForm'
import { useCreateReport } from '../queries'
import { parseReportType } from '../reportTypes'
import type { ReportFormValues } from '../schemas'
import { Card } from '../../../shared/ui'

export function NewReportPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const createReport = useCreateReport()

  // Opened from the Found tab → start with "Found" selected.
  const initialType = parseReportType(searchParams.get('type')) ?? 'LOST'

  async function handleSubmit(values: ReportFormValues) {
    await createReport.mutateAsync(values)
    // Newest first, so the new report is at the top of page 1.
    navigate('/reports', { state: { notice: 'Report created.' } })
  }

  return (
    <Card className="space-y-5">
      <div>
        <h1 className="text-xl font-semibold">New report</h1>
        <p className="mt-1 text-sm text-ink-muted">The more detail you add, the easier it is to match.</p>
      </div>
      <ReportForm
        defaultValues={{ type: initialType, itemName: '', description: '', city: '' }}
        submitLabel="Create report"
        cancelTo="/reports"
        onSubmit={handleSubmit}
      />
    </Card>
  )
}