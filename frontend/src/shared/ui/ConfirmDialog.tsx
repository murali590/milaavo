import { useEffect, useId, useRef, type ReactNode } from 'react'
import { Button } from './Button'

type ConfirmDialogProps = {
  open: boolean
  title: string
  children: ReactNode
  confirmLabel: string
  loading?: boolean
  onConfirm: () => void
  onCancel: () => void
}

// Native <dialog> with showModal(): focus stays inside, Esc closes, and the page behind is inert.
export function ConfirmDialog({ open, title, children, confirmLabel, loading = false, onConfirm, onCancel }: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) {
      return
    }
    if (open && !dialog.open) {
      dialog.showModal()
    }
    if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      // Esc: let React state decide (and don't close while the request is running).
      onCancel={(event) => {
        event.preventDefault()
        if (!loading) {
          onCancel()
        }
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-sm rounded-card border border-line bg-surface p-0 text-ink shadow-lg backdrop:bg-ink/40"
    >
      <div className="space-y-4 p-6">
        <h2 id={titleId} className="text-lg font-semibold">
          {title}
        </h2>
        <div className="text-sm text-ink-muted wrap-anywhere">{children}</div>
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          {/* Focus starts on Cancel: the safe choice for a destructive action. */}
          <Button variant="secondary" onClick={onCancel} disabled={loading} autoFocus>
            Cancel
          </Button>
          <Button variant="danger" onClick={onConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </dialog>
  )
}