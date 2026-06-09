import type { TicketPriority, TicketStatus, TicketType } from './types'

/** Compact relative age like the design's "32m", "5h", "3d", "2w". */
export function relativeAge(iso: string): string {
  const then = new Date(iso).getTime()
  const seconds = Math.max(0, Math.floor((Date.now() - then) / 1000))
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d`
  const weeks = Math.floor(days / 7)
  if (weeks < 52) return `${weeks}w`
  return `${Math.floor(days / 365)}y`
}

export function fullDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

export const STATUS_LABEL: Record<TicketStatus, string> = {
  OPEN: 'Open',
  IN_PROGRESS: 'In Progress',
  PENDING: 'Pending',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
}

export const PRIORITY_LABEL: Record<TicketPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  URGENT: 'Urgent',
}

export const TYPE_LABEL: Record<TicketType, string> = {
  QUESTION: 'Question',
  BUG: 'Bug',
  FEATURE_REQUEST: 'Feature Request',
}

/** A short, stable display key for a ticket (UUIDs have no human key). e.g. "a1b2c3d4". */
export function shortId(id: string): string {
  return id.replace(/-/g, '').slice(0, 8).toUpperCase()
}

/**
 * The Advisor returns free-text suggested fields. Map them back to our enums (best-effort) so
 * "Apply" can PATCH the ticket. Returns null when the suggestion doesn't match a known value.
 * (Status is never suggested by the Advisor, so there is no parser for it.)
 */
export function parsePriorityLike(value: string | null | undefined): TicketPriority | null {
  if (!value) return null
  const v = value.trim().toUpperCase()
  return (['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as TicketPriority[]).find((p) => p === v) ?? null
}

export function parseTypeLike(value: string | null | undefined): TicketType | null {
  if (!value) return null
  const v = value.trim().toUpperCase().replace(/[\s-]+/g, '_')
  if (v === 'QUESTION') return 'QUESTION'
  if (v === 'BUG') return 'BUG'
  if (v === 'FEATURE_REQUEST' || v === 'FEATURE') return 'FEATURE_REQUEST'
  return null
}
