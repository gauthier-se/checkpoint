import { getRequestCookieServerFn } from '@/services/auth-server'
import { API_PREFIX } from '@/services/api-config'

/**
 * Typed error thrown by `apiFetch` whenever the server returns a non-2xx
 * response, or the fetch itself fails (network error).
 *
 * The brand (`__isApiError`) lets {@link isApiError} recognize instances that
 * crossed the SSR boundary and lost their class identity during dehydration.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  // Brand for cross-realm / cross-SSR-boundary detection.
  readonly __isApiError = true as const

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export function isApiError(error: unknown): error is ApiError {
  if (error instanceof ApiError) return true
  return (
    typeof error === 'object' &&
    error !== null &&
    (error as { __isApiError?: unknown }).__isApiError === true
  )
}

interface ServerErrorResponse {
  status?: number
  error?: string
  message?: string
  timestamp?: string
}

/**
 * Wrapper around fetch that automatically includes credentials (cookies)
 * for session-based authentication.
 *
 * Client-side: uses relative paths so requests hit the web origin and are
 * proxied to the API (see `nitro.routeRules` in vite.config.ts). This keeps
 * auth cookies scoped to the web origin, which is required for SSR auth.
 *
 * Server-side: uses `API_INTERNAL_URL` for direct API calls (Node fetch
 * requires absolute URLs) and forwards the incoming SSR request's auth cookie —
 * Node's fetch has no browser cookie jar, so `credentials: 'include'` alone
 * sends nothing and any protected endpoint hit from a loader would 401.
 *
 * On `!res.ok`, parses the API's `ErrorResponse` JSON body and throws an
 * {@link ApiError}. On network failure, throws `ApiError { status: 0 }`.
 * Successful responses are returned unchanged so callers can call
 * `res.json()` or inspect `res.status` (e.g. 204) themselves.
 */
export async function apiFetch(
  path: string,
  init?: RequestInit,
): Promise<Response> {
  const isServer = typeof window === 'undefined'
  const base = isServer
    ? (process.env.API_INTERNAL_URL ?? 'http://localhost:8080')
    : (import.meta.env.VITE_API_URL ?? '')

  const headers = new Headers(init?.headers)
  if (isServer && !headers.has('cookie') && !headers.has('Cookie')) {
    const cookie = await getRequestCookieServerFn()
    if (cookie) headers.set('Cookie', cookie)
  }

  // Rewrite the legacy `/api` segment to the versioned prefix so call sites can
  // keep using `/api/...` paths while the version lives in one place.
  const versionedPath = path.startsWith('/api/')
    ? `${API_PREFIX}${path.slice('/api'.length)}`
    : path

  let res: Response
  try {
    res = await fetch(`${base}${versionedPath}`, {
      ...init,
      headers,
      credentials: 'include',
    })
  } catch {
    throw new ApiError(0, 'NetworkError', 'Unable to reach the server.')
  }

  if (!res.ok) {
    let body: ServerErrorResponse | null = null
    try {
      body = (await res.clone().json()) as ServerErrorResponse
    } catch {
      body = null
    }
    throw new ApiError(
      body?.status ?? res.status,
      body?.error ?? res.statusText,
      body?.message ?? 'An unexpected error occurred.',
    )
  }

  return res
}
