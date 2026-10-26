import type { ReactNode } from 'react'
import { cn } from './cn'

type AlertTone = 'success' | 'warning' | 'danger' | 'info'

const toneClasses: Record<AlertTone, string> = {
  success: 'bg-success-soft text-success',
  warning: 'bg-warning-soft text-warning',
  danger: 'bg-danger-soft text-danger',
  info: 'bg-brand-soft text-brand',
}

type AlertProps = {
  tone?: AlertTone
  title?: string
  children: ReactNode
}

// Danger alerts use role="alert" so screen readers announce them at once; others are polite.
export function Alert({ tone = 'info', title, children }: AlertProps) {
  return (
    <div role={tone === 'danger' ? 'alert' : 'status'} className={cn('rounded-lg p-3 text-sm', toneClasses[tone])}>
      {title && <p className="font-semibold">{title}</p>}
      <div>{children}</div>
    </div>
  )
}