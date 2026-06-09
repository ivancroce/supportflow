import { X } from 'lucide-react'
import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { cn } from '@/lib/utils'

interface DialogProps {
  open: boolean
  onClose: () => void
  title?: string
  description?: string
  /** Max width of the panel. */
  width?: number
  /** Hide the default close (×) button — e.g. while a modal is mid-flight and must not be dismissed. */
  hideClose?: boolean
  /** When false, clicking the backdrop / pressing Escape won't close (used during in-flight actions). */
  dismissable?: boolean
  children: React.ReactNode
}

/**
 * A small modal primitive: centered card over a dimmed backdrop, rendered in a portal so it escapes
 * any overflow/stacking context. Closes on Escape and backdrop click (unless `dismissable` is off).
 * Shared by the New Ticket form and the Escalate flow.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  width = 460,
  hideClose = false,
  dismissable = true,
  children,
}: DialogProps) {
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape' && dismissable) onClose()
    }
    window.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [open, dismissable, onClose])

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 animate-fade-in bg-[#04162e]/45 backdrop-blur-[1px]"
        onClick={() => dismissable && onClose()}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="relative w-full animate-scale-in rounded-xl border border-hairline bg-card shadow-pop"
        style={{ maxWidth: width }}
      >
        {(title || !hideClose) && (
          <div className="flex items-start gap-3 px-6 pt-5">
            <div className="min-w-0 flex-1">
              {title && (
                <h2 className="text-[17px] font-bold tracking-[-0.02em] text-ink">{title}</h2>
              )}
              {description && <p className="mt-1 text-[13px] text-ink-2">{description}</p>}
            </div>
            {!hideClose && (
              <button
                onClick={onClose}
                className={cn(
                  'mt-0.5 rounded-md p-1 text-ink-3 transition-colors hover:bg-canvas-deep hover:text-ink',
                )}
                aria-label="Close"
              >
                <X size={18} />
              </button>
            )}
          </div>
        )}
        {children}
      </div>
    </div>,
    document.body,
  )
}
