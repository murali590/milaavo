import type { ComponentProps } from 'react'
import { cn } from './cn'
import { Spinner } from './Spinner'

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'

type ButtonProps = ComponentProps<'button'> & {
  variant?: ButtonVariant
  // Shows a spinner and blocks clicks, e.g. while a request is running.
  loading?: boolean
  fullWidth?: boolean
}

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-brand text-on-brand hover:bg-brand-hover',
  secondary: 'border border-line bg-surface text-ink hover:bg-surface-muted',
  danger: 'bg-danger text-on-danger hover:opacity-90',
  ghost: 'text-brand hover:bg-brand-soft',
}

// Defaults to type="button" so it never submits a form by accident; pass type="submit" in forms.
export function Button({
  variant = 'primary',
  loading = false,
  fullWidth = false,
  type = 'button',
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(
        'inline-flex h-11 items-center justify-center gap-2 rounded-lg px-4 font-medium transition-colors',
        'disabled:cursor-not-allowed disabled:opacity-60',
        variantClasses[variant],
        fullWidth && 'w-full',
        className,
      )}
      {...rest}
    >
      {loading && <Spinner className="size-4" decorative />}
      {children}
    </button>
  )
}