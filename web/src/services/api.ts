/**
 * Wrapper around fetch that automatically includes credentials (cookies)
 * for session-based authentication.
 *
 * Client-side: uses relative paths so requests hit the web origin and are
 * proxied to the API (see `nitro.routeRules` in vite.config.ts). This keeps
 * auth cookies scoped to the web origin, which is required for SSR auth.
 *
 * Server-side: uses `API_INTERNAL_URL` for direct API calls (Node fetch
 * requires absolute URLs).
 */
export async function apiFetch(
  path: string,
  init?: RequestInit,
): Promise<Response> {
  const base =
    typeof window === 'undefined' ? process.env.API_INTERNAL_URL ?? '' : ''
  return fetch(`${base}${path}`, {
    ...init,
    credentials: 'include',
  })
}
