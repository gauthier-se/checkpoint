import type { AdminUser, AdminUserStatus } from '@/types/admin'
import type { ClientPage } from '@/lib/admin-pagination'
import { paginateClientSide } from '@/lib/admin-pagination'

/**
 * `banned` is nullable on the API side (accounts predating the column), so
 * "not banned" and "unknown" both read as active.
 */
export function isBanned(user: Pick<AdminUser, 'banned'>): boolean {
  return user.banned === true
}

interface AdminUserFilters {
  q?: string
  status: AdminUserStatus
}

/** Case-insensitive match on username or email, plus the ban-state filter. */
export function filterAdminUsers(
  users: Array<AdminUser>,
  { q, status }: AdminUserFilters,
): Array<AdminUser> {
  const needle = q?.trim().toLowerCase()

  return users.filter((user) => {
    if (status === 'banned' && !isBanned(user)) return false
    if (status === 'active' && isBanned(user)) return false

    if (!needle) return true
    return (
      user.username.toLowerCase().includes(needle) ||
      user.email.toLowerCase().includes(needle)
    )
  })
}

export type AdminUserPage = ClientPage<AdminUser>

/** Client-side paging for the unpaginated `GET /admin/users` listing. */
export function paginateAdminUsers(
  users: Array<AdminUser>,
  page: number,
  size: number,
): AdminUserPage {
  return paginateClientSide(users, page, size)
}
