import type { ComponentProps } from 'react'
import { cn } from './cn'

export function Card({ className, ...rest }: ComponentProps<'div'>) {
  return (
    <div
      className={cn('rounded-card border border-line bg-surface p-4 shadow-sm sm:p-6', className)}
      {...rest}
    />
  )
}