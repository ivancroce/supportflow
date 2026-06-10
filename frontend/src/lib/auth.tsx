import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from './api'
import { AuthContext, type AuthStatus } from './auth-context'
import { AUTH_LOGOUT_EVENT, clearToken, getToken, setToken } from './token'
import type { Me } from './types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  // Token presence is state, not a plain read: after signIn stores the token we must re-render so the
  // disabled `/api/me` query flips to enabled and fetches (invalidateQueries alone won't run a
  // disabled query). The logout handler flips it back to false.
  const [hasToken, setHasToken] = useState(() => !!getToken())
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
      setHasToken(true)
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
      setHasToken(false)
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
