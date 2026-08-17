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

/** Row of `GET /admin/users` (`AdminUserDto`). */
export interface AdminUser {
  id: string
  username: string
  email: string
  /** Nullable: accounts predating the column have no value. */
  banned: boolean | null
}

/** `GET /admin/users/{id}` (`AdminUserDetailDto`). */
export interface AdminUserDetail {
  id: string
  username: string
  email: string
  bio: string | null
  picture: string | null
  isPrivate: boolean | null
  banned: boolean | null
  xpPoint: number | null
  level: number | null
  createdAt: string
  reviewCount: number
  reportCount: number
}

/**
 * Body of `PUT /admin/users/{id}` (`AdminUserEditDto`). The endpoint only
 * supports clearing moderatable profile fields and forcing an account private —
 * it cannot change a username, an email or a role.
 */
export interface AdminUserEdit {
  clearBio?: boolean
  clearPicture?: boolean
  isPrivate?: boolean
}

export type AdminUserStatus = 'all' | 'active' | 'banned'

export interface AdminUsersSearchParams {
  /** 1-based. */
  page: number
  q?: string
  status: AdminUserStatus
}
