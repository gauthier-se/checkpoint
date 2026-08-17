import type { AdminReportedReview, AdminReview } from '@/types/admin'

/**
 * Common row shape for the two review listings. `GET /admin/reviews` and
 * `GET /admin/reviews/reported` return different records, so they are
 * normalised here and the table stays a single component.
 */
export interface ModeratedReviewRow {
  id: string
  content: string
  authorUsername: string | null
  gameTitle: string | null
  createdAt: string
  /** Null in the all-reviews view, which does not carry a count. */
  reportCount: number | null
  /** Only the reported listing exposes spoiler state; null otherwise. */
  haveSpoilers: boolean | null
}

export function toModeratedReviewRow(review: AdminReview): ModeratedReviewRow {
  return {
    id: review.id,
    content: review.content,
    authorUsername: review.authorUsername,
    gameTitle: review.gameTitle,
    createdAt: review.createdAt,
    reportCount: null,
    haveSpoilers: review.haveSpoilers,
  }
}

export function toReportedReviewRow(
  review: AdminReportedReview,
): ModeratedReviewRow {
  return {
    id: review.id,
    content: review.content,
    authorUsername: review.authorUsername,
    gameTitle: review.gameTitle,
    createdAt: review.createdAt,
    reportCount: review.reportCount,
    haveSpoilers: null,
  }
}
