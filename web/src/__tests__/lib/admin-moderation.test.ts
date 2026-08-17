import { describe, expect, it } from 'vitest'
import type { AdminReportedReview, AdminReview } from '@/types/admin'
import {
  toModeratedReviewRow,
  toReportedReviewRow,
} from '@/lib/admin-moderation'
import { formatAdminDateTime } from '@/lib/admin-format'

const review: AdminReview = {
  id: 'review-1',
  content: 'Solid game.',
  haveSpoilers: true,
  authorUsername: 'alpha',
  gameTitle: 'A Game',
  createdAt: '2026-03-04T18:30:00',
}

const reportedReview: AdminReportedReview = {
  id: 'review-2',
  content: 'Spoiler-heavy rant.',
  authorId: 'user-9',
  authorUsername: 'bravo',
  gameTitle: 'Another Game',
  reportCount: 3,
  createdAt: '2026-03-05T09:00:00',
}

describe('review row normalisation', () => {
  it('keeps the spoiler flag and reports no count for a plain review', () => {
    expect(toModeratedReviewRow(review)).toEqual({
      id: 'review-1',
      content: 'Solid game.',
      authorUsername: 'alpha',
      gameTitle: 'A Game',
      createdAt: '2026-03-04T18:30:00',
      reportCount: null,
      haveSpoilers: true,
    })
  })

  it('carries the report count and no spoiler flag for a reported review', () => {
    expect(toReportedReviewRow(reportedReview)).toEqual({
      id: 'review-2',
      content: 'Spoiler-heavy rant.',
      authorUsername: 'bravo',
      gameTitle: 'Another Game',
      createdAt: '2026-03-05T09:00:00',
      reportCount: 3,
      haveSpoilers: null,
    })
  })

  it('produces the same shape from both listings', () => {
    expect(Object.keys(toModeratedReviewRow(review)).sort()).toEqual(
      Object.keys(toReportedReviewRow(reportedReview)).sort(),
    )
  })
})

describe('formatAdminDateTime', () => {
  it('includes the time of day, which the queue order depends on', () => {
    const formatted = formatAdminDateTime('2026-03-04T18:30:00')

    expect(formatted).toContain('2026')
    expect(formatted).toMatch(/\d{1,2}:\d{2}/)
  })
})
