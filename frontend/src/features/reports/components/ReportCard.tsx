import { formatDate } from '../../../shared/format/date'
import { Badge, Card } from '../../../shared/ui'
import type { LostReport } from '../types'

export function ReportCard({ report }: { report: LostReport }) {
  return (
    <Card className="space-y-2">
      <div className="flex items-start justify-between gap-3">
        <h2 className="min-w-0 font-medium wrap-anywhere">{report.itemName}</h2>
        <Badge tone="lost">Lost</Badge>
      </div>
      <p className="line-clamp-2 text-sm text-ink-muted wrap-anywhere">{report.description}</p>
      <p className="text-xs text-ink-muted wrap-anywhere">
        {report.city} · {formatDate(report.createdAt)}
      </p>
    </Card>
  )
}