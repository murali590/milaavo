import type { ComponentProps } from 'react'
import { buttonClasses, type ButtonVariant } from './buttonStyles'
import { Spinner } from './Spinner'

type ButtonProps = ComponentProps<'button'> & {
  variant?: ButtonVariant
  // Shows a spinner and blocks clicks, e.g. while a request is running.
  loading?: boolean
  fullWidth?: boolean
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
      className={buttonClasses(variant, fullWidth, className)}
      {...rest}
    >
      {loading && <Spinner className="size-4" decorative />}
      {children}
    </button>
  )
}