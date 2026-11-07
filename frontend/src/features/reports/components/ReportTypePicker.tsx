import type { UseFormRegisterReturn } from 'react-hook-form'
import { REPORT_TYPES, REPORT_TYPE_INFO } from '../reportTypes'

type ReportTypePickerProps = {
  registration: UseFormRegisterReturn<'type'>
  error?: string
}

// Real radio buttons (keyboard and screen-reader friendly), styled as cards.
export function ReportTypePicker({ registration, error }: ReportTypePickerProps) {
  return (
    <fieldset className="space-y-2">
      <legend className="text-sm font-medium text-ink">Report type</legend>
      <div className="grid gap-2 sm:grid-cols-3">
        {REPORT_TYPES.map((type) => (
          <label
            key={type}
            className="flex cursor-pointer flex-col rounded-lg border border-line bg-surface p-3 has-[:checked]:border-brand has-[:checked]:bg-brand-soft has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus"
          >
            <input type="radio" value={type} className="sr-only" {...registration} />
            <span className="font-medium text-ink">{REPORT_TYPE_INFO[type].label}</span>
            <span className="text-sm text-ink-muted">{REPORT_TYPE_INFO[type].summary}</span>
          </label>
        ))}
      </div>
      {error && <p className="text-sm text-danger">{error}</p>}
    </fieldset>
  )
}