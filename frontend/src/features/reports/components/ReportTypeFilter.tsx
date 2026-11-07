import { REPORT_TYPES, REPORT_TYPE_INFO, type ReportType } from '../reportTypes'
import { cn } from '../../../shared/ui/cn'

type ReportTypeFilterProps = {
  value: ReportType | undefined
  onChange: (value: ReportType | undefined) => void
}

const OPTIONS: Array<{ value: ReportType | undefined; label: string }> = [
  { value: undefined, label: 'All' },
  ...REPORT_TYPES.map((type) => ({ value: type, label: REPORT_TYPE_INFO[type].label })),
]

export function ReportTypeFilter({ value, onChange }: ReportTypeFilterProps) {
  return (
    <div role="group" aria-label="Filter by type" className="flex gap-2 overflow-x-auto">
      {OPTIONS.map((option) => {
        const active = option.value === value
        return (
          <button
            key={option.label}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(option.value)}
            className={cn(
              'h-9 shrink-0 rounded-full border px-4 text-sm font-medium transition-colors',
              active ? 'border-brand bg-brand text-on-brand' : 'border-line bg-surface text-ink hover:bg-surface-muted',
            )}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}