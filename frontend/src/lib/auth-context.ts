import { createContext, use } from 'react'
import type { Me } from './types'

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export interface AuthContextValue {
  me: Me | null
  status: AuthStatus
  /** Persist a freshly-minted JWT (from OAuth callback or dev login) and load the profile. */
  signIn: (token: string) => Promise<void>
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const ctx = use(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
