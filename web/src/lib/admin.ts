import { redirect } from '@tanstack/react-router'
import type { QueryClient } from '@tanstack/react-query'
import type { User } from '@/types/user'
import { authQueryOptions } from '@/hooks/use-auth'

/**
 * Role name the API returns for administrators. `UserMeDto.role` carries the
 * bare role name ("ADMIN" / "USER"), not the Spring Security `ROLE_` prefix.
 */
export const ADMIN_ROLE = 'ADMIN'

/**
 * Client-side role check used to gate the `/admin` routes and the entry point
 * in the avatar menu. It is a UX guard only: every `/admin/**` endpoint is
 * independently protected by `@PreAuthorize("hasRole('ADMIN')")` on the API.
 */
export function isAdmin(user: User | null | undefined): boolean {
  return user?.role === ADMIN_ROLE
}

/**
 * `beforeLoad` guard for the admin routes: resolves the session and turns away
 * signed-in non-admins.
 *
 * Two cases are deliberately *not* redirected here:
 *
 * - **No user.** The admin routes live under `_protected`, which already
 *   redirects anonymous visitors to `/login` with a `redirect` search param.
 *   Bouncing them to `/` instead would lose that return path.
 * - **The session lookup failed.** During SSR the auth cookie can be
 *   unreachable from the web origin, and treating that as "not an admin" would
 *   lock a legitimate admin out of the panel. The layout re-checks the role
 *   after hydration, where the cookie is available.
 */
export async function requireAdmin(queryClient: QueryClient): Promise<void> {
  let user: User | null = null

  try {
    user = await queryClient.ensureQueryData(authQueryOptions)
  } catch {
    return
  }

  if (user && !isAdmin(user)) {
    throw redirect({ to: '/' })
  }
}
