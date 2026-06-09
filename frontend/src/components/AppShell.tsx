import { Check, ChevronsUpDown, Inbox, LogOut, Plus, Settings } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Avatar } from '@/components/Avatar'
import { Wordmark } from '@/components/Wordmark'
import { useAuth } from '@/lib/auth-context'
import { setLastProjectId } from '@/lib/lastProject'
import { useProject, useProjects, useTickets } from '@/lib/queries'
import type { Project, TicketType } from '@/lib/types'
import { cn } from '@/lib/utils'

// Deterministic project chip (no color field on the backend — derive one from the name).
const CHIP_COLORS = ['#0176D3', '#06A59A', '#5867E8', '#9050E9', '#C23934', '#0B7285']
function projectChip(project: Project): { short: string; color: string } {
  const short = project.name
    .split(/\s+/)
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
  const color = CHIP_COLORS[(project.name.charCodeAt(0) || 0) % CHIP_COLORS.length]
  return { short, color }
}

const TYPE_LABELS: { type: TicketType; label: string; dot: string }[] = [
  { type: 'BUG', label: 'Bugs', dot: 'bg-danger' },
  { type: 'QUESTION', label: 'Questions', dot: 'bg-info' },
  { type: 'FEATURE_REQUEST', label: 'Feature requests', dot: 'bg-success' },
]

export function AppShell() {
  const { projectId } = useParams<{ projectId: string }>()
  const { me, signOut } = useAuth()
  const { data: project } = useProject(projectId)
  const { data: tickets } = useTickets(projectId)

  useEffect(() => {
    if (projectId) setLastProjectId(projectId)
  }, [projectId])

  const openCount = tickets?.filter((t) => t.status === 'OPEN').length ?? 0
  const totalCount = tickets?.length ?? 0

  return (
    <div className="flex h-svh bg-canvas">
      <aside className="flex w-[248px] shrink-0 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground">
        <div className="px-[18px] pb-3.5 pt-[17px]">
          <Wordmark size={16} light />
        </div>

        <div className="px-3">
          <ProjectSwitcher activeId={projectId} active={project} openCount={openCount} />
        </div>

        <nav className="mt-2 flex flex-col gap-0.5 px-3">
          <NavLink
            to={`/projects/${projectId}/tickets`}
            end
            className={({ isActive }) =>
              cn(navItem, isActive ? navItemActive : navItemIdle)
            }
          >
            <Inbox size={16} />
            <span className="flex-1">All Tickets</span>
            <span className="text-xs text-nav-text-2">{totalCount}</span>
          </NavLink>
        </nav>

        <div className="mt-5 px-3">
          <p className="px-2.5 pb-1.5 text-[10.5px] font-bold uppercase tracking-[0.05em] text-nav-text-2">
            Labels
          </p>
          <div className="flex flex-col gap-0.5">
            {TYPE_LABELS.map((t) => (
              <TypeLabelLink key={t.type} projectId={projectId} {...t} count={
                tickets?.filter((tk) => tk.type === t.type).length
              } />
            ))}
          </div>
        </div>

        <div className="mt-auto px-3 pb-2">
          <Link
            to={`/projects/${projectId}/settings`}
            className={cn(navItem, navItemIdle)}
          >
            <Settings size={16} />
            <span className="flex-1">Project settings</span>
          </Link>
        </div>

        <div className="flex items-center gap-2.5 border-t border-sidebar-border px-4 py-3">
          <Avatar name={me?.name || me?.email || '?'} src={me?.avatarUrl} size={32} />
          <div className="min-w-0 flex-1">
            <p className="truncate text-[13px] font-semibold text-nav-text">{me?.name || 'Owner'}</p>
            <p className="truncate text-[11.5px] text-nav-text-2">{me?.email}</p>
          </div>
          <button
            onClick={signOut}
            title="Sign out"
            className="rounded-md p-1.5 text-nav-text-2 transition-colors hover:bg-white/10 hover:text-nav-text"
          >
            <LogOut size={16} />
          </button>
        </div>
      </aside>

      <main className="min-w-0 flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}

const navItem =
  'flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13.5px] font-medium transition-colors'
const navItemActive = 'bg-sidebar-accent text-nav-text [&_svg]:text-nav-accent'
const navItemIdle = 'text-nav-text-2 hover:bg-white/[0.08] hover:text-nav-text'

function TypeLabelLink({
  projectId,
  type,
  label,
  dot,
  count,
}: {
  projectId: string | undefined
  type: TicketType
  label: string
  dot: string
  count: number | undefined
}) {
  const [params] = useSearchParams()
  const active = params.get('type') === type
  return (
    <Link
      to={`/projects/${projectId}/tickets?type=${type}`}
      className={cn(navItem, active ? navItemActive : navItemIdle)}
    >
      <span className={cn('size-2 rounded-full', dot)} />
      <span className="flex-1">{label}</span>
      {count != null && count > 0 && <span className="text-xs text-nav-text-2">{count}</span>}
    </Link>
  )
}

function ProjectSwitcher({
  activeId,
  active,
  openCount,
}: {
  activeId: string | undefined
  active: Project | undefined
  openCount: number
}) {
  const { data: projects } = useProjects()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const chip = active ? projectChip(active) : null

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center gap-2.5 rounded-lg bg-white/[0.08] px-2.5 py-2 text-left transition-colors hover:bg-white/[0.12]"
      >
        <span
          className="flex size-7 shrink-0 items-center justify-center rounded-md text-[11px] font-bold text-white"
          style={{ background: chip?.color ?? '#0176D3' }}
        >
          {chip?.short ?? '··'}
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-[13.5px] font-semibold text-nav-text">
            {active?.name ?? 'Select project'}
          </span>
          <span className="block text-[11.5px] text-nav-text-2">{openCount} open tickets</span>
        </span>
        <ChevronsUpDown size={15} className="text-nav-text-2" />
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute left-0 right-0 top-[calc(100%+4px)] z-20 animate-scale-in rounded-lg border border-hairline bg-card p-1.5 text-ink shadow-float">
            {projects?.map((p) => {
              const c = projectChip(p)
              return (
                <button
                  key={p.id}
                  onClick={() => {
                    setOpen(false)
                    navigate(`/projects/${p.id}/tickets`)
                  }}
                  className={cn(
                    'flex w-full items-center gap-2.5 rounded-md px-2 py-1.5 text-left transition-colors hover:bg-canvas-deep',
                    p.id === activeId && 'bg-canvas-deep',
                  )}
                >
                  <span
                    className="flex size-6 shrink-0 items-center justify-center rounded-md text-[10px] font-bold text-white"
                    style={{ background: c.color }}
                  >
                    {c.short}
                  </span>
                  <span className="flex-1 truncate text-[13px] font-medium">{p.name}</span>
                  {p.id === activeId && <Check size={15} className="text-brand" />}
                </button>
              )
            })}
            <button
              onClick={() => {
                setOpen(false)
                navigate('/projects/new')
              }}
              className="mt-1 flex w-full items-center gap-2.5 rounded-md border border-dashed border-hairline-strong px-2 py-1.5 text-left text-[13px] font-medium text-ink-3 transition-colors hover:border-ink-4 hover:text-ink"
            >
              <Plus size={15} />
              New project
            </button>
          </div>
        </>
      )}
    </div>
  )
}
