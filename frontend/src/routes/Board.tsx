import { Clock, Inbox, Plus, Search, X, Zap } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { PriorityBadge, StatusBadge, TypeDot } from '@/components/Badges'
import { Button } from '@/components/Button'
import { NewTicketDialog } from '@/components/NewTicketDialog'
import { STATUS_DISPLAY, TYPE_DISPLAY } from '@/lib/display'
import { relativeAge, shortId } from '@/lib/format'
import { useProject, useTickets } from '@/lib/queries'
import { TICKET_STATUSES } from '@/lib/types'
import type { Ticket, TicketStatus, TicketType } from '@/lib/types'
import { cn } from '@/lib/utils'

type Tab = 'all' | TicketStatus

const GRID = 'grid grid-cols-[104px_104px_minmax(0,1fr)_72px] items-center gap-4'
const TICKET_TYPES_SET = new Set<string>(['QUESTION', 'BUG', 'FEATURE_REQUEST'])

export function Board() {
  const { projectId } = useParams<{ projectId: string }>()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const { data: project } = useProject(projectId)
  const { data: tickets, isLoading, isError } = useTickets(projectId)

  const [tab, setTab] = useState<Tab>('all')
  const [query, setQuery] = useState('')
  const [createOpen, setCreateOpen] = useState(false)

  // The sidebar's type labels drive the board via `?type=`.
  const rawType = params.get('type')
  const typeFilter = (TICKET_TYPES_SET.has(rawType ?? '') ? rawType : null) as TicketType | null

  // Base = all tickets narrowed by the active type label. Tab counts derive from this (stable as you
  // type a search); the search box then narrows what's visible inside the chosen tab.
  const base = useMemo(
    () => (typeFilter ? (tickets ?? []).filter((t) => t.type === typeFilter) : tickets ?? []),
    [tickets, typeFilter],
  )

  const counts = useMemo(() => {
    const byStatus = {} as Record<TicketStatus, number>
    for (const s of TICKET_STATUSES) byStatus[s] = 0
    for (const t of base) byStatus[t.status]++
    return byStatus
  }, [base])

  const visible = useMemo(() => {
    let rows = tab === 'all' ? base : base.filter((t) => t.status === tab)
    const q = query.trim().toLowerCase()
    if (q) {
      rows = rows.filter(
        (t) => t.subject.toLowerCase().includes(q) || shortId(t.id).toLowerCase().includes(q),
      )
    }
    return rows
  }, [base, tab, query])

  function clearType() {
    params.delete('type')
    setParams(params, { replace: true })
  }

  const tabs: { id: Tab; label: string; count: number }[] = [
    { id: 'all', label: 'All', count: base.length },
    ...TICKET_STATUSES.map((s) => ({ id: s, label: STATUS_DISPLAY[s].label, count: counts[s] })),
  ]

  return (
    <div className="flex h-full flex-col">
      {/* Top bar */}
      <header className="flex items-center gap-4 border-b border-hairline px-7 pb-3.5 pt-4">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2.5">
            <h1 className="text-xl font-bold tracking-[-0.03em] text-ink">Tickets</h1>
            {typeFilter && (
              <button
                onClick={clearType}
                className="inline-flex items-center gap-1.5 rounded-full bg-brand-soft py-1 pl-2.5 pr-2 text-[12px] font-semibold text-brand-ink transition-colors hover:bg-[#d9ecfb]"
              >
                <TypeDot type={typeFilter} />
                {TYPE_DISPLAY[typeFilter].label}
                <X size={13} />
              </button>
            )}
          </div>
          <p className="mt-0.5 text-[13px] text-ink-2">
            {visible.length} {visible.length === 1 ? 'ticket' : 'tickets'}
            {project && <> · {project.name}</>}
          </p>
        </div>

        <div className="relative">
          <Search
            size={16}
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-3"
          />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search tickets…"
            className="h-[38px] w-[230px] rounded-[9px] border border-hairline-strong bg-card pl-9 pr-3 text-[13.5px] text-ink shadow-soft outline-none placeholder:text-ink-4 focus:border-brand"
          />
        </div>
        <Button icon={Plus} onClick={() => setCreateOpen(true)}>
          New Ticket
        </Button>
      </header>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-hairline px-7 pt-3">
        {tabs.map((t) => {
          const on = t.id === tab
          return (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={cn(
                'relative flex items-center gap-2 px-3 pb-3 pt-1 text-[13.5px] transition-colors',
                on ? 'font-semibold text-ink' : 'font-medium text-ink-2 hover:text-ink',
              )}
            >
              {t.label}
              <span
                className={cn(
                  'rounded-full px-1.5 py-px text-[11px] font-semibold tabular-nums',
                  on ? 'bg-brand-soft text-brand-ink' : 'bg-canvas-deep text-ink-3',
                )}
              >
                {t.count}
              </span>
              {on && (
                <span className="absolute inset-x-2.5 -bottom-px h-0.5 rounded-full bg-brand" />
              )}
            </button>
          )
        })}
      </div>

      {/* Column header */}
      <div
        className={cn(
          GRID,
          'px-7 py-2.5 text-[11px] font-bold uppercase tracking-[0.05em] text-ink-3',
        )}
      >
        <span>Status</span>
        <span>Priority</span>
        <span>Subject</span>
        <span className="text-right">Age</span>
      </div>

      {/* List */}
      <div className="flex-1 overflow-y-auto px-4 pb-6">
        {isLoading && <BoardSkeleton />}

        {isError && (
          <div className="px-3 py-16 text-center text-sm text-ink-3">
            Couldn't load tickets. Check your connection and try again.
          </div>
        )}

        {!isLoading && !isError && visible.length === 0 && (
          <EmptyState
            hasAnyTickets={(tickets?.length ?? 0) > 0}
            onNew={() => setCreateOpen(true)}
            onClearFilters={() => {
              setQuery('')
              setTab('all')
              clearType()
            }}
          />
        )}

        {!isLoading &&
          !isError &&
          visible.map((t, i) => (
            <TicketRow
              key={t.id}
              ticket={t}
              delay={Math.min(i, 12) * 0.025}
              onClick={() => navigate(`/projects/${projectId}/tickets/${t.id}`)}
            />
          ))}
      </div>

      {projectId && (
        <NewTicketDialog
          projectId={projectId}
          open={createOpen}
          onClose={() => setCreateOpen(false)}
        />
      )}
    </div>
  )
}

function TicketRow({
  ticket,
  onClick,
  delay,
}: {
  ticket: Ticket
  onClick: () => void
  delay: number
}) {
  return (
    <button
      onClick={onClick}
      style={{ animationDelay: `${delay}s` }}
      className={cn(
        GRID,
        'w-full animate-rise rounded-xl border border-transparent px-3 py-3 text-left transition-[background,border-color,box-shadow] hover:border-hairline hover:bg-card hover:shadow-soft',
      )}
    >
      <span>
        <StatusBadge status={ticket.status} size="sm" />
      </span>
      <span>
        <PriorityBadge priority={ticket.priority} size="sm" />
      </span>
      <span className="min-w-0">
        <span className="flex items-center gap-2">
          <span className="shrink-0 font-mono text-[11.5px] font-medium text-ink-3">
            {shortId(ticket.id)}
          </span>
          <span className="truncate text-sm font-semibold tracking-[-0.01em] text-ink">
            {ticket.subject}
          </span>
        </span>
        <span className="mt-1 flex items-center gap-2.5 text-[12px] text-ink-3">
          <span className="inline-flex items-center gap-1.5">
            <TypeDot type={ticket.type} />
            {TYPE_DISPLAY[ticket.type].label}
          </span>
          {ticket.category && (
            <>
              <span className="text-ink-4">·</span>
              <span className="truncate">{ticket.category}</span>
            </>
          )}
          {ticket.escalated && (
            <span className="inline-flex items-center gap-1 rounded-full bg-warning-soft px-1.5 py-px text-[11px] font-semibold text-warning-ink">
              <Zap size={11} strokeWidth={2.4} />
              Escalated
            </span>
          )}
        </span>
      </span>
      <span className="flex items-center justify-end gap-1.5 text-[12.5px] tabular-nums text-ink-3">
        <Clock size={13} className="opacity-70" />
        {relativeAge(ticket.createdAt)}
      </span>
    </button>
  )
}

function EmptyState({
  hasAnyTickets,
  onNew,
  onClearFilters,
}: {
  hasAnyTickets: boolean
  onNew: () => void
  onClearFilters: () => void
}) {
  if (hasAnyTickets) {
    return (
      <div className="flex flex-col items-center px-3 py-16 text-center">
        <Search size={26} className="text-ink-4" />
        <p className="mt-3 text-sm font-semibold text-ink-2">No tickets match your filters</p>
        <p className="mt-1 text-[13px] text-ink-3">Try a different search, tab, or label.</p>
        <Button variant="secondary" size="sm" className="mt-4" onClick={onClearFilters}>
          Clear filters
        </Button>
      </div>
    )
  }
  return (
    <div className="flex flex-col items-center px-3 py-16 text-center">
      <span className="flex size-12 items-center justify-center rounded-2xl bg-brand-soft text-brand">
        <Inbox size={24} />
      </span>
      <p className="mt-4 text-[15px] font-semibold text-ink">No tickets yet</p>
      <p className="mt-1 max-w-[280px] text-[13px] text-ink-2">
        Open your first ticket to start tracking customer issues in this project.
      </p>
      <Button icon={Plus} className="mt-5" onClick={onNew}>
        New Ticket
      </Button>
    </div>
  )
}

function BoardSkeleton() {
  const shimmer =
    'animate-shimmer rounded-md bg-[linear-gradient(90deg,#ececec,#f5f5f5,#ececec)] bg-[length:200%_100%]'
  return (
    <div className="pt-1">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className={cn(GRID, 'px-3 py-3.5')}>
          <span className={cn(shimmer, 'h-[22px] w-[88px]')} />
          <span className={cn(shimmer, 'h-[22px] w-[76px]')} />
          <span className="flex flex-col gap-2">
            <span className={cn(shimmer, 'h-3.5 w-2/3')} />
            <span className={cn(shimmer, 'h-3 w-1/3')} />
          </span>
          <span className={cn(shimmer, 'ml-auto h-3 w-9')} />
        </div>
      ))}
    </div>
  )
}
