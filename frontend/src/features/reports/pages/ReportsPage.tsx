import { useLocation, useSearchParams } from 'react-router'
import { ReportCard } from '../components/ReportCard'
import { ReportTypeFilter } from '../components/ReportTypeFilter'               
import { useReports } from '../queries'
import { parseReportType, REPORT_TYPE_INFO, type ReportType } from '../reportTypes'
import { Alert, Button, Card, LinkButton, Spinner } from '../../../shared/ui'

// Anything that isn't a whole number from 1 upward (?page=abc, ?page=-1) means page 1.
function parsePage(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page >= 1 ? page : 1
}

export function ReportsPage() {
  const location = useLocation()
  const notice = (location.state as { notice?: string } | null)?.notice

  const [searchParams, setSearchParams] = useSearchParams()
  const page = parsePage(searchParams.get('page'))
  const type = parseReportType(searchParams.get('type'))
  const { data, isPending, isError, error, refetch, isFetching, isPlaceholderData } = useReports(page, type)

  // Page 1 and "All" are the defaults, so they stay out of the URL.
  function updateParams(next: { page?: number; type?: ReportType }) {
    const params = new URLSearchParams()
    if (next.type) {
      params.set('type', next.type)
    }
    if (next.page && next.page > 1) {
      params.set('page', String(next.page))
    }
    setSearchParams(params)
  }

  const goToPage = (next: number) => updateParams({ page: next, type })
  // A new filter starts again from page 1.
  const changeType = (next: ReportType | undefined) => updateParams({ type: next })

  const newReportLink = type ? `/reports/new?type=${type}` : '/reports/new'
  const emptyLabel = type ? `No ${REPORT_TYPE_INFO[type].label.toLowerCase()} reports yet` : 'No reports yet'

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-xl font-semibold">My reports</h1>
        <LinkButton to={newReportLink}>New report</LinkButton>
      </div>

      {notice && <Alert tone="success">{notice}</Alert>}

      <ReportTypeFilter value={type} onChange={changeType} />

      {isPending && (
        <div className="flex justify-center py-10 text-brand">
          <Spinner label="Loading your reports" />
        </div>
      )}

      {isError && (
        <div className="space-y-3">
          <Alert tone="danger">{error.message}</Alert>
          <Button variant="secondary" onClick={() => refetch()} loading={isFetching}>
            Try again
          </Button>
        </div>
      )}

      {data && data.totalElements === 0 && (
        <Card className="py-10 text-center">
          <p className="font-medium">{emptyLabel}</p>
          <p className="mt-1 text-sm text-ink-muted">Your reports will show up here.</p>
        </Card>
      )}

      {/* The URL points past the end, e.g. ?page=999 */}
      {data && data.totalElements > 0 && data.content.length === 0 && (
        <Card className="space-y-3 py-10 text-center">
          <p className="font-medium">This page doesn't exist</p>
          <Button variant="secondary" onClick={() => goToPage(data.totalPages)}>
            Go to the last page
          </Button>
        </Card>
      )}

      {data && data.content.length > 0 && (
        <>
          <ul className={isPlaceholderData ? 'space-y-3 opacity-60' : 'space-y-3'}>
            {data.content.map((report) => (
              <li key={report.reportId}>
                <ReportCard report={report} listSearch={location.search}/>
              </li>
            ))}
          </ul>

          {data.totalPages > 1 && (
            <nav aria-label="Pagination" className="flex items-center justify-between gap-3">
              <Button variant="secondary" disabled={data.first || isPlaceholderData} onClick={() => goToPage(page - 1)}>
                Previous
              </Button>
              <span className="text-sm text-ink-muted">
                Page {data.page + 1} of {data.totalPages}
              </span>
              <Button variant="secondary" disabled={data.last || isPlaceholderData} onClick={() => goToPage(page + 1)}>
                Next
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  )
}