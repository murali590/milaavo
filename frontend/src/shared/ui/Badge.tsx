import type { ReactNode } from 'react'
import { cn } from './cn'

type BadgeTone = 'lost' | 'found' | 'brand' | 'neutral'

const toneClasses: Record<BadgeTone, string> = {
  lost: 'bg-lost-soft text-lost',
  found: 'bg-found-soft text-found',
  brand: 'bg-brand-soft text-brand',
  neutral: 'bg-surface-muted text-ink-muted',
}

type BadgeProps = {
  tone?: BadgeTone
  children: ReactNode
}

export function Badge({ tone = 'neutral', children }: BadgeProps) {
  return (
    <span className={cn('inline-flex items-center rounded-full px-3 py-1 text-sm font-medium', toneClasses[tone])}>
      {children}
    </span>
  )
}