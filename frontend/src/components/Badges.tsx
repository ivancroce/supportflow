import { ArrowUp } from 'lucide-react'
import { PRIORITY_LABEL, STATUS_LABEL, TYPE_LABEL } from '@/lib/format'
import type { TicketPriority, TicketStatus, TicketType } from '@/lib/types'
import { cn } from '@/lib/utils'

type Size = 'sm' | 'md'

// ── Status pill (rounded-full, colored dot + label) ──────────────────────────
const STATUS_STYLE: Record<TicketStatus, { wrap: string; dot: string }> = {
  OPEN: { wrap: 'bg-info-soft text-info-ink', dot: 'bg-info' },
  IN_PROGRESS: { wrap: 'bg-warning-soft text-warning-ink', dot: 'bg-warning' },
  // PENDING isn't in the handoff palette; give it a distinct indigo so it reads
  // apart from CLOSED's neutral grey.
  PENDING: { wrap: 'bg-[#eeeaf8] text-[#5b3fa8]', dot: 'bg-[#5b3fa8]' },
  RESOLVED: { wrap: 'bg-success-soft text-success-ink', dot: 'bg-success' },
  CLOSED: { wrap: 'bg-canvas-deep text-ink-3', dot: 'bg-ink-4' },
}

export function StatusBadge({ status, size = 'md' }: { status: TicketStatus; size?: Size }) {
  const s = STATUS_STYLE[status]
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
      {STATUS_LABEL[status]}
    </span>
  )
}

// ── Priority badge (7px radius; high/urgent show an up-arrow) ─────────────────
const PRIORITY_STYLE: Record<TicketPriority, { wrap: string; dot: string }> = {
  LOW: { wrap: 'bg-canvas-deep text-ink-2', dot: 'bg-ink-4' },
  MEDIUM: { wrap: 'bg-info-soft text-info-ink', dot: 'bg-info' },
  HIGH: { wrap: 'bg-warning-soft text-warning-ink', dot: 'bg-warning' },
  URGENT: { wrap: 'bg-danger-soft text-danger-ink', dot: 'bg-danger' },
}

export function PriorityBadge({ priority, size = 'md' }: { priority: TicketPriority; size?: Size }) {
  const p = PRIORITY_STYLE[priority]
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
      {PRIORITY_LABEL[priority]}
    </span>
  )
}

// ── Type label (sidebar labels + ticket meta): colored dot + text ─────────────
const TYPE_DOT: Record<TicketType, string> = {
  BUG: 'bg-danger',
  QUESTION: 'bg-info',
  FEATURE_REQUEST: 'bg-success',
}

export function TypeDot({ type }: { type: TicketType }) {
  return <span className={cn('size-2 rounded-full', TYPE_DOT[type])} />
}

export function TypeBadge({ type }: { type: TicketType }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-[12.5px] font-medium text-ink-2">
      <TypeDot type={type} />
      {TYPE_LABEL[type]}
    </span>
  )
}
