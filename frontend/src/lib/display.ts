// Single source of truth for how each ticket enum renders: human label + Tailwind classes. Keying
// each map by the enum type means adding a new enum value is a compile error until it's filled in
// here, instead of silently missing from a badge or the sidebar.
import type { TicketPriority, TicketStatus, TicketType } from './types'

export const STATUS_DISPLAY: Record<TicketStatus, { label: string; wrap: string; dot: string }> = {
  OPEN: { label: 'Open', wrap: 'bg-info-soft text-info-ink', dot: 'bg-info' },
  IN_PROGRESS: { label: 'In Progress', wrap: 'bg-warning-soft text-warning-ink', dot: 'bg-warning' },
  // PENDING isn't in the handoff palette; give it a distinct indigo so it reads apart from CLOSED's
  // neutral grey.
  PENDING: { label: 'Pending', wrap: 'bg-[#eeeaf8] text-[#5b3fa8]', dot: 'bg-[#5b3fa8]' },
  RESOLVED: { label: 'Resolved', wrap: 'bg-success-soft text-success-ink', dot: 'bg-success' },
  CLOSED: { label: 'Closed', wrap: 'bg-canvas-deep text-ink-3', dot: 'bg-ink-4' },
}

export const PRIORITY_DISPLAY: Record<TicketPriority, { label: string; wrap: string; dot: string }> =
  {
    LOW: { label: 'Low', wrap: 'bg-canvas-deep text-ink-2', dot: 'bg-ink-4' },
    MEDIUM: { label: 'Medium', wrap: 'bg-info-soft text-info-ink', dot: 'bg-info' },
    HIGH: { label: 'High', wrap: 'bg-warning-soft text-warning-ink', dot: 'bg-warning' },
    URGENT: { label: 'Urgent', wrap: 'bg-danger-soft text-danger-ink', dot: 'bg-danger' },
  }

// `label` is the singular form used in ticket meta/badges; `navLabel` is the plural sidebar form.
export const TYPE_DISPLAY: Record<TicketType, { label: string; navLabel: string; dot: string }> = {
  QUESTION: { label: 'Question', navLabel: 'Questions', dot: 'bg-info' },
  BUG: { label: 'Bug', navLabel: 'Bugs', dot: 'bg-danger' },
  FEATURE_REQUEST: { label: 'Feature Request', navLabel: 'Feature requests', dot: 'bg-success' },
}
