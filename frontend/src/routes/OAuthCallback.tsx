import { Loader2 } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/lib/auth-context'

/**
 * Landing page for the backend's OAuth success redirect, which arrives as
 * `/oauth/callback#token=<jwt>`. We read the token from the URL fragment (never sent to a server),
 * persist it, scrub it from the address bar, and route into the app.
 */
export function OAuthCallback() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const handled = useRef(false)

  useEffect(() => {
    if (handled.current) return
    handled.current = true

    const token = new URLSearchParams(window.location.hash.slice(1)).get('token')
    if (!token) {
      navigate('/login?error=oauth_failed', { replace: true })
      return
    }

    void signIn(token).then(() => {
      window.history.replaceState(null, '', window.location.pathname)
      navigate('/', { replace: true })
    })
  }, [signIn, navigate])

  return (
    <div className="flex min-h-svh items-center justify-center bg-canvas">
      <div className="flex items-center gap-2.5 text-ink-3">
        <Loader2 className="size-5 animate-spin" />
        <span className="text-sm">Signing you in…</span>
      </div>
    </div>
  )
}
