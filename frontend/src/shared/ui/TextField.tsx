import { useId, type ComponentProps } from 'react'
import { cn } from './cn'

type TextFieldProps = ComponentProps<'input'> & {
  label: string
  // Shown in red below the input. Also marks the input as invalid for screen readers.
  error?: string
  // Grey help text below the input. Hidden while there is an error.
  hint?: string
}

export function TextField({ label, error, hint, id, className, ...rest }: TextFieldProps) {
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
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : showHint ? hintId : undefined}
        className={cn(
          'h-11 w-full rounded-lg border bg-surface px-3 text-ink placeholder:text-ink-muted',
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