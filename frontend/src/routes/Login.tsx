import { useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/Button'
import { GithubMark, GoogleMark } from '@/components/BrandMarks'
import { Wordmark } from '@/components/Wordmark'
import { oauthLoginUrl } from '@/lib/api'
import { useAuth } from '@/lib/auth-context'
import { useDevLogin } from '@/lib/queries'
import { ApiError } from '@/lib/api'

const OAUTH_ERRORS: Record<string, string> = {
  not_allowed: "That account isn't on the allowlist.",
  email_unavailable: "Couldn't read a verified email from that account.",
  oauth_failed: 'Sign-in failed. Please try again.',
}

export function Login() {
  const { status, signIn } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [pending, setPending] = useState<'google' | 'github' | null>(null)

  const oauthError = params.get('error')

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  function startOAuth(provider: 'google' | 'github') {
    setPending(provider)
    window.location.href = oauthLoginUrl(provider)
  }

  return (
    <div className="relative flex min-h-svh items-center justify-center overflow-hidden bg-canvas px-4">
      {/* Soft radial glows */}
      <div className="glow-brand pointer-events-none absolute -right-40 -top-40 size-[520px] rounded-full" />
      <div className="glow-navy pointer-events-none absolute -bottom-40 -left-40 size-[520px] rounded-full" />

      <div className="relative w-[408px] max-w-full animate-rise">
        <div className="mb-[26px] flex justify-center">
          <Wordmark size={22} />
        </div>

        <div className="rounded-xl border border-hairline bg-card px-9 pb-[30px] pt-9 shadow-card">
          <h1 className="text-center text-[22px] font-bold tracking-[-0.03em] text-ink">Welcome back</h1>
          <p className="mt-1.5 text-center text-sm text-ink-2">
            Sign in to manage your support queue.
          </p>

          {oauthError && (
            <div className="mt-5 rounded-lg border border-danger/30 bg-danger-soft px-3.5 py-2.5 text-[13px] text-danger-ink">
              {OAUTH_ERRORS[oauthError] ?? 'Something went wrong signing in.'}
            </div>
          )}

          <div className="mt-6 flex flex-col gap-3">
            <SsoButton
              label="Continue with Google"
              mark={<GoogleMark />}
              loading={pending === 'google'}
              dim={pending !== null && pending !== 'google'}
              onClick={() => startOAuth('google')}
            />
            <SsoButton
              label="Continue with GitHub"
              mark={<GithubMark />}
              loading={pending === 'github'}
              dim={pending !== null && pending !== 'github'}
              onClick={() => startOAuth('github')}
            />
          </div>

          <div className="my-6 flex items-center gap-3">
            <span className="h-px flex-1 bg-hairline" />
            <span className="text-[11.5px] font-semibold uppercase tracking-[0.05em] text-ink-3">
              Secure SSO
            </span>
            <span className="h-px flex-1 bg-hairline" />
          </div>

          <DevLogin onToken={async (token) => {
            await signIn(token)
            navigate('/', { replace: true })
          }} />

          <p className="mt-6 text-center text-[12.5px] leading-relaxed text-ink-3">
            By continuing you agree to the{' '}
            <span className="text-brand-ink">Terms</span> and{' '}
            <span className="text-brand-ink">Privacy Policy</span>.
          </p>
        </div>
      </div>
    </div>
  )
}

function SsoButton({
  label,
  mark,
  loading,
  dim,
  onClick,
}: {
  label: string
  mark: React.ReactNode
  loading: boolean
  dim: boolean
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      disabled={loading || dim}
      className="flex h-12 items-center justify-center gap-2.5 rounded-[11px] border border-hairline-strong bg-card text-[14.5px] font-semibold text-ink transition-all hover:border-ink-4 hover:bg-card-muted disabled:opacity-55 data-[dim=true]:opacity-55"
      data-dim={dim}
    >
      {loading ? <span className="text-ink-3">Signing in…</span> : (<>{mark}{label}</>)}
    </button>
  )
}

// Local developer sign-in: hits the backend's local-profile /api/dev/login. In a deployed build the
// endpoint isn't registered, so this fails gracefully — it's a convenience for local testing.
function DevLogin({ onToken }: { onToken: (token: string) => void | Promise<void> }) {
  const [email, setEmail] = useState('')
  const devLogin = useDevLogin()

  function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!email.trim()) return
    devLogin.mutate(email.trim(), { onSuccess: (res) => onToken(res.token) })
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-2">
      <label className="text-[11px] font-semibold uppercase tracking-[0.05em] text-ink-3">
        Developer sign-in (local)
      </label>
      <div className="flex gap-2">
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com"
          className="h-[38px] flex-1 rounded-[9px] border border-hairline-strong bg-card px-3 text-sm text-ink outline-none placeholder:text-ink-4 focus:border-brand"
        />
        <Button type="submit" loading={devLogin.isPending} disabled={!email.trim()}>
          Continue
        </Button>
      </div>
      {devLogin.isError && (
        <p className="text-[12.5px] text-danger-ink">
          {devLogin.error instanceof ApiError && devLogin.error.status === 403
            ? "That email isn't on the allowlist."
            : 'Dev sign-in unavailable.'}
        </p>
      )}
    </form>
  )
}
