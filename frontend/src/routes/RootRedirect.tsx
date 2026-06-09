import { Loader2 } from 'lucide-react'
import { Navigate } from 'react-router-dom'
import { getLastProjectId } from '@/lib/lastProject'
import { useProjects } from '@/lib/queries'

/** Land the Owner on their last-used project's board, falling back to the first project. */
export function RootRedirect() {
  const { data: projects, isLoading, isError } = useProjects()

  if (isLoading) {
    return (
      <div className="flex min-h-svh items-center justify-center bg-canvas">
        <Loader2 className="size-6 animate-spin text-ink-3" />
      </div>
    )
  }

  if (isError || !projects || projects.length === 0) {
    return <Navigate to="/projects/new" replace />
  }

  const lastId = getLastProjectId()
  const target = projects.find((p) => p.id === lastId) ?? projects[0]
  return <Navigate to={`/projects/${target.id}/tickets`} replace />
}
