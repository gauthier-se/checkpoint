import type { AdminUser, AdminUserStatus } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'

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

export interface AdminUserPage {
  rows: Array<AdminUser>
  /**
   * Same shape the paginated admin endpoints return, so `AdminDataTable` and
   * `PaginationNav` work unchanged against a client-side slice.
   */
  metadata: PaginationMetadata
}

/**
 * Slices the filtered list into a page. `page` is 1-based and clamped into
 * range, so a stale deep link (e.g. page 5 after a filter narrows the results)
 * lands on the last page instead of an empty table.
 */
export function paginateAdminUsers(
  users: Array<AdminUser>,
  page: number,
  size: number,
): AdminUserPage {
  const totalElements = users.length
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  const requested = Number.isFinite(page) ? Math.floor(page) : 1
  const current = Math.min(Math.max(1, requested), totalPages)
  const start = (current - 1) * size

  return {
    rows: users.slice(start, start + size),
    metadata: {
      page: current - 1,
      size,
      totalElements,
      totalPages,
      first: current === 1,
      last: current === totalPages,
      hasNext: current < totalPages,
      hasPrevious: current > 1,
    },
  }
}
