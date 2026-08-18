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
 * supports clearing moderatable profile fields and forcing an account private:
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
  /** Id of the reported review or comment: the handle for removing it. */
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

/** Result of `GET /admin/external-games/search` (`ExternalGameDto`, IGDB). */
export interface ExternalGame {
  externalId: number
  title: string
  releaseYear: number | null
  coverUrl: string | null
}

export type ImportJobState = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

/** `ImportJobStatusDto`: returned when starting a bulk import and when polling. */
export interface ImportJobStatus {
  jobId: string
  type: string
  state: ImportJobState
  requestedLimit: number
  minRatingCount: number
  totalFetched: number
  processed: number
  imported: number
  skipped: number
  failed: number
  errors: Array<string>
  errorMessage: string | null
  startedAt: string
  finishedAt: string | null
}

/**
 * Body of `POST /admin/games` and `PUT /admin/games/{id}`. The create and
 * update DTOs are field-for-field identical on the API side, so one type covers
 * both.
 */
export interface AdminGamePayload {
  title: string
  description?: string | null
  coverUrl?: string | null
  artworkUrl?: string | null
  trailerYoutubeId?: string | null
  timeToBeatNormally?: number | null
  timeToBeatHastily?: number | null
  timeToBeatCompletely?: number | null
  releaseDate?: string | null
  genreIds?: Array<string>
  platformIds?: Array<string>
  companyIds?: Array<string>
}

export interface AdminCatalogSearchParams {
  /** 1-based. */
  page: number
  q?: string
}

export type AdminNewsSource = 'MANUAL' | 'STEAM' | 'RSS'

/** Sources that can actually be imported: the API rejects MANUAL. */
export const IMPORTABLE_NEWS_SOURCES = ['STEAM', 'RSS'] as const
export type ImportableNewsSource = (typeof IMPORTABLE_NEWS_SOURCES)[number]

/**
 * `NewsResponseDto`. The record is serialised with `@JsonInclude(NON_NULL)`, so
 * every nullable field is **absent** rather than null: most importantly
 * `publishedAt`, whose absence is what makes an article a draft.
 */
export interface AdminNews {
  id: string
  title: string
  description?: string
  picture?: string
  publishedAt?: string
  createdAt: string
  updatedAt: string
  // The nested author DTO is not `NON_NULL`, so its picture really can be null.
  author?: { id: string; pseudo: string; picture?: string | null }
  source: AdminNewsSource
  externalUrl?: string
  feedName?: string
  videoGameId?: string
}

/**
 * Body of `POST /admin/news` and `PUT /admin/news/{newsId}` (`NewsRequestDto`).
 * The endpoint takes these three fields and nothing else: there is no way to
 * set the source, the publication date or a game association from the editor.
 */
export interface AdminNewsPayload {
  title: string
  description: string | null
  picture: string | null
}

export interface AdminNewsSearchParams {
  /** 1-based. */
  page: number
}

export interface AdminTopGame {
  id: string
  title: string
  reviewCount: number
}

export interface AdminTopReviewer {
  id: string
  username: string
  reviewCount: number
}

/**
 * `GET /admin/analytics` (`AdminAnalyticsDto`). Five running totals plus two
 * top-five rankings: there is no time series in the payload, so nothing here
 * is plotted over time.
 */
export interface AdminAnalytics {
  totalUsers: number
  /** Not banned. `totalUsers - activeUsers` is the banned count. */
  activeUsers: number
  totalGames: number
  totalReviews: number
  /** Every report on file; the API keeps no dismissed/open distinction. */
  openReports: number
  topReviewedGames: Array<AdminTopGame>
  topReviewers: Array<AdminTopReviewer>
}

/**
 * `GET /admin/news/import-settings` (`NewsImportSettingsDto`).
 *
 * The two `*Enabled` flags gate the **scheduled** passes only: the panel's
 * Import buttons are an explicit admin action and run whatever the flags say.
 * The daily ceiling, on the other hand, binds both paths.
 */
export interface AdminNewsImportSettings {
  steamEnabled: boolean
  rssEnabled: boolean
  /** Null means no ceiling at all. */
  maxArticlesPerDay: number | null
  steamNewsPerGame: number
  /** Articles the importers inserted since midnight, server time. */
  importedToday: number
  /** Null whenever `maxArticlesPerDay` is null. */
  remainingToday: number | null
  /** Absent until an admin first saves the form. */
  updatedAt?: string
  updatedBy?: string
}

/**
 * Body of `PUT /admin/news/import-settings`. Every field is optional and an
 * omitted one is left untouched, which is why removing the ceiling needs the
 * explicit `unlimited` flag rather than a null `maxArticlesPerDay`.
 */
export interface AdminNewsImportSettingsPayload {
  steamEnabled?: boolean
  rssEnabled?: boolean
  maxArticlesPerDay?: number
  steamNewsPerGame?: number
  unlimited?: boolean
}
