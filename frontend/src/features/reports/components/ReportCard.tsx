import { REPORT_TYPE_INFO } from '../reportTypes'
import type { ItemReport } from '../types'
import { formatDate } from '../../../shared/format/date'
import { Badge, Card } from '../../../shared/ui'

export function ReportCard({ report }: { report: ItemReport }) {
  const typeInfo = REPORT_TYPE_INFO[report.type]

  return (
    <Card className="space-y-2">
      <div className="flex items-start justify-between gap-3">
        <h2 className="min-w-0 font-medium wrap-anywhere">{report.itemName}</h2>
        <Badge tone={typeInfo.badgeTone}>{typeInfo.label}</Badge>
      </div>
      <p className="line-clamp-2 text-sm text-ink-muted wrap-anywhere">{report.description}</p>
      <p className="text-xs text-ink-muted wrap-anywhere">
        {report.city} · {formatDate(report.createdAt)}
      </p>
    </Card>
  )
}