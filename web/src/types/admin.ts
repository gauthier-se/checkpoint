import type { PaginationMetadata } from './game'

/**
 * Envelope returned by every paginated `/admin/**` endpoint. Mirrors the API's
 * `PagedResponseDto<T>`, whose metadata is field-for-field identical to the one
 * the public catalog endpoints already return.
 */
export interface AdminPagedResponse<T> {
  content: Array<T>
  metadata: PaginationMetadata
}

/**
 * Search params shared by the paginated admin listings. `page` is **1-based**
 * here, as everywhere in the UI; `buildAdminPageUrl` converts it to the API's
 * 0-based `page` query parameter.
 */
export interface AdminPageSearchParams {
  page: number
  sort?: string
}

/** Rows per page across the admin tables. */
export const ADMIN_PAGE_SIZE = 20
