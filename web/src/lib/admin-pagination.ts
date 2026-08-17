import type { PaginationMetadata } from '@/types/game'

export interface ClientPage<T> {
  rows: Array<T>
  /**
   * Same shape the paginated admin endpoints return, so `AdminDataTable` and
   * `PaginationNav` work unchanged against a client-side slice.
   */
  metadata: PaginationMetadata
}

/**
 * Slices an in-memory list into a page, for the admin endpoints that answer
 * with everything at once (`GET /admin/users`, `GET /games/search`).
 *
 * `page` is 1-based and clamped into range, so a stale deep link — page 5 after
 * a filter narrowed the results to two pages — lands on the last page instead
 * of an empty table.
 */
export function paginateClientSide<T>(
  items: Array<T>,
  page: number,
  size: number,
): ClientPage<T> {
  const totalElements = items.length
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  const requested = Number.isFinite(page) ? Math.floor(page) : 1
  const current = Math.min(Math.max(1, requested), totalPages)
  const start = (current - 1) * size

  return {
    rows: items.slice(start, start + size),
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
