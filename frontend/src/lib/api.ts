import { clearToken, getToken } from './token'

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')

/** A non-2xx response. `status` is the HTTP code; `message` is the best human-readable reason. */
export class ApiError extends Error {
  status: number
  body: unknown

  constructor(status: number, message: string, body: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  body?: unknown
  // Query params; undefined/null values are skipped.
  params?: Record<string, string | number | boolean | undefined | null>
  signal?: AbortSignal
}

function buildUrl(path: string, params?: RequestOptions['params']): string {
  const url = new URL(BASE_URL + path)
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null) url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

/** Pull the most useful message out of a Spring ProblemDetail (RFC 9457) or plain error body. */
function messageFromBody(body: unknown, status: number): string {
  if (body && typeof body === 'object') {
    const pd = body as Record<string, unknown>
    if (typeof pd.detail === 'string' && pd.detail) return pd.detail
    if (typeof pd.title === 'string' && pd.title) return pd.title
    if (typeof pd.message === 'string' && pd.message) return pd.message
  }
  return `Request failed (${status})`
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, params, signal } = options
  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const response = await fetch(buildUrl(path, params), {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal,
  })

  // A 401 means the token is missing/expired/invalid — drop it and let the app route to login.
  if (response.status === 401) {
    clearToken()
    throw new ApiError(401, 'Your session has expired. Please sign in again.', null)
  }

  if (response.status === 204) return undefined as T

  const text = await response.text()
  const parsed = text ? safeJson(text) : null

  if (!response.ok) {
    throw new ApiError(response.status, messageFromBody(parsed, response.status), parsed)
  }

  return parsed as T
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

/** Start the backend's OAuth flow for a provider by navigating the whole browser to it. */
export function oauthLoginUrl(provider: 'google' | 'github'): string {
  return `${BASE_URL}/oauth2/authorization/${provider}`
}

export const apiBaseUrl = BASE_URL
