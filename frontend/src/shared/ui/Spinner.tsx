import { cn } from './cn'

type SpinnerProps = {
  className?: string
  // Screen-reader text. Ignored when decorative.
  label?: string
  // True when something else already says "loading" (e.g. a busy button).
  decorative?: boolean
}

export function Spinner({ className, label = 'Loading', decorative = false }: SpinnerProps) {
  return (
    <span
      role={decorative ? undefined : 'status'}
      aria-label={decorative ? undefined : label}
      aria-hidden={decorative || undefined}
      className={cn(
        'inline-block size-5 animate-spin rounded-full border-2 border-current border-r-transparent',
        className,
      )}
    />
  )
}