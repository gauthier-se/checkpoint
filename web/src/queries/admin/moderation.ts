import { keepPreviousData, queryOptions } from '@tanstack/react-query'
import { ADMIN_QUERY_KEY, adminFetchJson, buildAdminPageUrl } from './shared'
import type { QueryClient } from '@tanstack/react-query'
import type {
  AdminPagedResponse,
  AdminReport,
  AdminReportDetail,
  AdminReportTypeFilter,
  AdminReportedReview,
  AdminReview,
  AdminReviewReport,
} from '@/types/admin'
import { apiFetch } from '@/services/api'

const REPORTS_PATH = '/api/admin/reports'
const REVIEWS_PATH = '/api/admin/reviews'
const COMMENTS_PATH = '/api/admin/comments'

/** Matches the controllers' DEFAULT_SORT, so page 1 looks the same either way. */
const NEWEST_FIRST = 'createdAt,desc'

export const adminModerationQueryKey = [ADMIN_QUERY_KEY, 'moderation'] as const

export function adminReportsQueryOptions(params: {
  page: number
  type: AdminReportTypeFilter
}) {
  return queryOptions({
    queryKey: [...adminModerationQueryKey, 'reports', params],
    queryFn: () =>
      adminFetchJson<AdminPagedResponse<AdminReport>>(
        buildAdminPageUrl(
          REPORTS_PATH,
          { page: params.page, sort: NEWEST_FIRST },
          // "all" is the absence of the filter, not a value the API knows.
          { type: params.type === 'all' ? undefined : params.type },
        ),
      ),
    // Keeps the previous page on screen while the next one loads, so working
    // through the queue does not flash an empty table on every click.
    placeholderData: keepPreviousData,
  })
}

export function adminReportQueryOptions(reportId: string) {
  return queryOptions({
    queryKey: [...adminModerationQueryKey, 'reports', 'detail', reportId],
    queryFn: () =>
      adminFetchJson<AdminReportDetail>(`${REPORTS_PATH}/${reportId}`),
  })
}

export function adminReviewsQueryOptions(params: { page: number }) {
  return queryOptions({
    queryKey: [...adminModerationQueryKey, 'reviews', params],
    queryFn: () =>
      adminFetchJson<AdminPagedResponse<AdminReview>>(
        buildAdminPageUrl(REVIEWS_PATH, {
          page: params.page,
          sort: NEWEST_FIRST,
        }),
      ),
    placeholderData: keepPreviousData,
  })
}

export function adminReportedReviewsQueryOptions(params: { page: number }) {
  return queryOptions({
    queryKey: [...adminModerationQueryKey, 'reviews', 'reported', params],
    queryFn: () =>
      adminFetchJson<AdminPagedResponse<AdminReportedReview>>(
        buildAdminPageUrl(`${REVIEWS_PATH}/reported`, {
          page: params.page,
          sort: NEWEST_FIRST,
        }),
      ),
    placeholderData: keepPreviousData,
  })
}

export function adminReviewReportsQueryOptions(reviewId: string, page: number) {
  return queryOptions({
    queryKey: [
      ...adminModerationQueryKey,
      'reviews',
      reviewId,
      'reports',
      page,
    ],
    queryFn: () =>
      adminFetchJson<AdminPagedResponse<AdminReviewReport>>(
        buildAdminPageUrl(`${REVIEWS_PATH}/${reviewId}/reports`, {
          page,
          sort: NEWEST_FIRST,
        }),
      ),
    placeholderData: keepPreviousData,
  })
}

/** Drops the report and leaves the reported content in place. */
export async function dismissAdminReport(reportId: string): Promise<void> {
  await apiFetch(`${REPORTS_PATH}/${reportId}`, { method: 'DELETE' })
}

export async function deleteAdminReview(reviewId: string): Promise<void> {
  await apiFetch(`${REVIEWS_PATH}/${reviewId}`, { method: 'DELETE' })
}

export async function deleteAdminComment(commentId: string): Promise<void> {
  await apiFetch(`${COMMENTS_PATH}/${commentId}`, { method: 'DELETE' })
}

/**
 * Removing a review or a comment changes what the public pages show, but the
 * admin panel has no way to know which game or list cached that content. Rather
 * than guess, mark every content root stale: nothing public is mounted while an
 * admin is in the panel, so this costs no refetch now and guarantees the next
 * visit to a game or review page is correct.
 */
export async function invalidateModeratedContent(
  queryClient: QueryClient,
): Promise<void> {
  await Promise.all(
    [[ADMIN_QUERY_KEY], ['reviews'], ['comments'], ['games']].map((queryKey) =>
      queryClient.invalidateQueries({ queryKey }),
    ),
  )
}
