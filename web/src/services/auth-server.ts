import { createServerFn } from '@tanstack/react-start'
import { getRequestHeader } from '@tanstack/react-start/server'
import type { User } from '@/types/user'

// Throws when no auth cookie is present so the loader leaves the cache empty
// and the client refetches after hydration. Returns null only when the cookie
// is present but the API rejects it (truly anonymous session).
export const fetchCurrentUserServerFn = createServerFn({
  method: 'GET',
}).handler(async (): Promise<User | null> => {
  const cookie = getRequestHeader('cookie')
  if (!cookie?.includes('checkpoint_token=')) {
    throw new Error('auth_cookie_unavailable')
  }

  const apiUrl = process.env.API_INTERNAL_URL ?? 'http://localhost:8080'
  const res = await fetch(`${apiUrl}/api/auth/me`, {
    headers: { Cookie: cookie },
  })
  if (!res.ok) return null
  return (await res.json()) as User
})
