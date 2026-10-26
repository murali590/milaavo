import { useId, type ComponentProps } from 'react'
import { cn } from './cn'

type TextAreaFieldProps = ComponentProps<'textarea'> & {
  label: string
  error?: string
  hint?: string
}

export function TextAreaField({ label, error, hint, id, className, rows = 4, ...rest }: TextAreaFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const hintId = `${inputId}-hint`
  const errorId = `${inputId}-error`
  const showHint = Boolean(hint) && !error

  return (
    <div className={cn('space-y-1.5', className)}>
      <label htmlFor={inputId} className="block text-sm font-medium text-ink">
        {label}
      </label>
      <textarea
        id={inputId}
        rows={rows}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : showHint ? hintId : undefined}
        className={cn(
          'w-full rounded-lg border bg-surface px-3 py-2 text-ink placeholder:text-ink-muted',
          error ? 'border-danger' : 'border-line',
        )}
        {...rest}
      />
      {showHint && (
        <p id={hintId} className="text-sm text-ink-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}