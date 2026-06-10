// The app JWT lives in localStorage so a refresh keeps you signed in. This is a single-user
// portfolio app served to one Owner; the XSS exposure of localStorage is an accepted trade-off
// (see the auth notes in docs). The token is sent as a Bearer header, never as a cookie.

const TOKEN_KEY = 'supportflow.jwt'

// Fired when the token is cleared (logout or a 401) so the AuthProvider can react and route to login.
export const AUTH_LOGOUT_EVENT = 'supportflow:logout'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  window.dispatchEvent(new Event(AUTH_LOGOUT_EVENT))
}
