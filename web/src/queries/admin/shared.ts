import { ADMIN_PAGE_SIZE } from '@/types/admin'
import { apiFetch } from '@/services/api'

/**
 * Root of every admin query key. Keeping a single root lets a role change or a
 * logout drop the whole panel cache with one invalidation.
 */
export const ADMIN_QUERY_KEY = 'admin' as const

interface AdminPageParams {
  /** 1-based, as shown in the UI and in the URL. */
  page: number
  size?: number
  /** Spring sort expression, e.g. `createdAt,desc`. */
  sort?: string
}

/**
 * Builds an `/api/admin/...` URL with the pagination contract the admin
 * endpoints share, converting the 1-based UI page to the 0-based `page` the API
 * expects. Extra params are appended when set to a non-empty value, so callers
 * can pass optional filters straight through.
 *
 * `apiFetch` rewrites the `/api` prefix to the versioned one, so paths here stay
 * unversioned like the rest of the query layer.
 */
export function buildAdminPageUrl(
  path: string,
  { page, size = ADMIN_PAGE_SIZE, sort }: AdminPageParams,
  extra?: Record<string, string | number | boolean | undefined>,
): string {
  const params = new URLSearchParams()

  // Guard against a non-finite page reaching the API: validateSearch normally
  // prevents it, but an unvalidated navigation could still feed in NaN.
  const uiPage = Number.isFinite(page) ? Math.floor(page) : 1
  params.set('page', String(Math.max(0, uiPage - 1)))
  params.set('size', String(size))

  if (sort) {
    params.set('sort', sort)
  }

  for (const [key, value] of Object.entries(extra ?? {})) {
    if (value !== undefined && value !== '') {
      params.set(key, String(value))
    }
  }

  return `${path}?${params.toString()}`
}

/** Fetches and parses an admin endpoint, letting `ApiError` propagate. */
export async function adminFetchJson<T>(url: string): Promise<T> {
  const res = await apiFetch(url)
  return res.json() as Promise<T>
}
