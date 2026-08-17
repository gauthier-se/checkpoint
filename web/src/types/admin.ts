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

/** What a report targets. Derived server-side from the populated relation. */
export type AdminReportType = 'review' | 'comment'

/** Row of `GET /admin/reports` (`AdminReportDto`). */
export interface AdminReport {
  id: string
  /** Null when the reporting account has since been deleted. */
  reporterUsername: string | null
  /** The reporter's message, not a canned category. */
  reason: string | null
  type: AdminReportType
  contentPreview: string | null
  createdAt: string
}

/** `GET /admin/reports/{id}` (`AdminReportDetailDto`). */
export interface AdminReportDetail {
  id: string
  reporterUsername: string | null
  reason: string | null
  type: AdminReportType
  /** Id of the reported review or comment — the handle for removing it. */
  targetId: string | null
  targetAuthorUsername: string | null
  targetFullContent: string | null
  createdAt: string
}

/** Row of `GET /admin/reviews` (`AdminReviewDto`). */
export interface AdminReview {
  id: string
  content: string
  haveSpoilers: boolean | null
  authorUsername: string | null
  gameTitle: string | null
  createdAt: string
}

/** Row of `GET /admin/reviews/reported` (`AdminReportedReviewDto`). */
export interface AdminReportedReview {
  id: string
  content: string
  authorId: string
  authorUsername: string | null
  gameTitle: string | null
  reportCount: number
  createdAt: string
}

/** Row of `GET /admin/reviews/{id}/reports` (`AdminReviewReportDto`). */
export interface AdminReviewReport {
  id: string
  reporterUsername: string | null
  reason: string | null
  createdAt: string
}

export type AdminReportTypeFilter = 'all' | AdminReportType

export interface AdminReportsSearchParams {
  /** 1-based. */
  page: number
  type: AdminReportTypeFilter
}

export interface AdminReviewsSearchParams {
  /** 1-based. */
  page: number
  /** Switches to the reported-only endpoint, which carries a report count. */
  reported: boolean
}
