import { ArrowUp } from 'lucide-react'
import { PRIORITY_DISPLAY, STATUS_DISPLAY, TYPE_DISPLAY } from '@/lib/display'
import type { TicketPriority, TicketStatus, TicketType } from '@/lib/types'
import { cn } from '@/lib/utils'

type Size = 'sm' | 'md'

// ── Status pill (rounded-full, colored dot + label) ──────────────────────────
export function StatusBadge({ status, size = 'md' }: { status: TicketStatus; size?: Size }) {
  const s = STATUS_DISPLAY[status]
  const sm = size === 'sm'
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full font-semibold tracking-[-0.005em]',
        sm ? 'h-[22px] px-2 text-[11.5px]' : 'h-6 px-2.5 text-xs',
        s.wrap,
      )}
    >
      <span className={cn('size-1.5 rounded-full', s.dot)} />
      {s.label}
    </span>
  )
}

// ── Priority badge (7px radius; high/urgent show an up-arrow) ─────────────────
export function PriorityBadge({ priority, size = 'md' }: { priority: TicketPriority; size?: Size }) {
  const p = PRIORITY_DISPLAY[priority]
  const sm = size === 'sm'
  const arrow = priority === 'HIGH' || priority === 'URGENT'
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-[7px] font-semibold',
        sm ? 'h-[22px] px-2 text-[11.5px]' : 'h-6 px-2.5 text-xs',
        p.wrap,
      )}
    >
      {arrow ? (
        <ArrowUp size={11} strokeWidth={2.6} />
      ) : (
        <span className={cn('size-1.5 rounded-[2px]', p.dot)} />
      )}
      {p.label}
    </span>
  )
}

// ── Type label (sidebar labels + ticket meta): colored dot + text ─────────────
export function TypeDot({ type }: { type: TicketType }) {
  return <span className={cn('size-2 rounded-full', TYPE_DISPLAY[type].dot)} />
}

export function TypeBadge({ type }: { type: TicketType }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-[12.5px] font-medium text-ink-2">
      <TypeDot type={type} />
      {TYPE_DISPLAY[type].label}
    </span>
  )
}
