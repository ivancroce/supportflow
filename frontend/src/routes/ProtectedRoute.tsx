import { Loader2 } from 'lucide-react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/lib/auth-context'

/** Gate the app behind a valid session. While the profile loads, show a quiet full-screen spinner. */
export function ProtectedRoute() {
  const { status } = useAuth()

  if (status === 'loading') {
    return (
      <div className="flex min-h-svh items-center justify-center bg-canvas">
        <Loader2 className="size-6 animate-spin text-ink-3" />
      </div>
    )
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
