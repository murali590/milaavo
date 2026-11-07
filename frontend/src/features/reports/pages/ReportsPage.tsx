import { useSearchParams } from 'react-router'
import { ReportCard } from '../components/ReportCard'
import { useReports } from '../queries'
import { Alert, Button, Card, Spinner } from '../../../shared/ui'

// Anything that isn't a whole number from 1 upward (?page=abc, ?page=-1) means page 1.
function parsePage(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page >= 1 ? page : 1
}

export function ReportsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = parsePage(searchParams.get('page'))
  const { data, isPending, isError, error, refetch, isFetching, isPlaceholderData } = useReports(page)

  function goToPage(next: number) {
    setSearchParams(next === 1 ? {} : { page: String(next) })
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">My reports</h1>

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
          <p className="font-medium">No reports yet</p>
          <p className="mt-1 text-sm text-ink-muted">Lost something? Your reports will show up here.</p>
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
                <ReportCard report={report} />
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