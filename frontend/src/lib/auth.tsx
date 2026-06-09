import { createContext, use, useCallback, useEffect, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from './api'
import { AUTH_LOGOUT_EVENT, clearToken, getToken, setToken } from './token'
import type { Me } from './types'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

interface AuthContextValue {
  me: Me | null
  status: AuthStatus
  /** Persist a freshly-minted JWT (from OAuth callback or dev login) and load the profile. */
  signIn: (token: string) => Promise<void>
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const hasToken = !!getToken()
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: () => apiFetch<Me>('/api/me'),
    enabled: hasToken,
    retry: false,
    staleTime: Infinity,
  })

  const signIn = useCallback(
    async (token: string) => {
      setToken(token)
      await queryClient.invalidateQueries({ queryKey: ['me'] })
    },
    [queryClient],
  )

  const signOut = useCallback(() => {
    clearToken() // dispatches AUTH_LOGOUT_EVENT, handled below
  }, [])

  // A 401 anywhere (or an explicit signOut) clears the token and dispatches this event; wipe cached
  // server state and route to login so no stale data lingers behind a dead session.
  useEffect(() => {
    const onLogout = () => {
      queryClient.clear()
      navigate('/login', { replace: true })
    }
    window.addEventListener(AUTH_LOGOUT_EVENT, onLogout)
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, onLogout)
  }, [queryClient, navigate])

  let status: AuthStatus
  if (!hasToken) status = 'unauthenticated'
  else if (meQuery.isSuccess) status = 'authenticated'
  else if (meQuery.isError) status = 'unauthenticated'
  else status = 'loading'

  return (
    <AuthContext value={{ me: meQuery.data ?? null, status, signIn, signOut }}>
      {children}
    </AuthContext>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = use(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
