import { cn } from './cn'

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-brand text-on-brand hover:bg-brand-hover',
  secondary: 'border border-line bg-surface text-ink hover:bg-surface-muted',
  danger: 'bg-danger text-on-danger hover:opacity-90',
  ghost: 'text-brand hover:bg-brand-soft',
}

// Shared by Button and LinkButton, so a link can look exactly like a button.
export function buttonClasses(variant: ButtonVariant = 'primary', fullWidth = false, className?: string): string {
  return cn(
    'inline-flex h-11 items-center justify-center gap-2 rounded-lg px-4 font-medium transition-colors',
    'disabled:cursor-not-allowed disabled:opacity-60',
    variantClasses[variant],
    fullWidth && 'w-full',
    className,
  )
}