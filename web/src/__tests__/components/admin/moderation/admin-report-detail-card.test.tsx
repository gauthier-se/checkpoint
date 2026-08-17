import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AdminReportDetail } from '@/types/admin'
import { AdminReportDetailCard } from '@/components/admin/moderation/admin-report-detail-card'

const dismissMock = vi.fn((_id: string) => Promise.resolve())
const deleteReviewMock = vi.fn((_id: string) => Promise.resolve())
const deleteCommentMock = vi.fn((_id: string) => Promise.resolve())
const invalidateMock = vi.fn(() => Promise.resolve())
const navigateMock = vi.fn(() => Promise.resolve())

vi.mock('@/queries/admin/moderation', () => ({
  dismissAdminReport: (id: string) => dismissMock(id),
  deleteAdminReview: (id: string) => deleteReviewMock(id),
  deleteAdminComment: (id: string) => deleteCommentMock(id),
  invalidateModeratedContent: () => invalidateMock(),
}))

vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => navigateMock,
}))

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}))

const reviewReport: AdminReportDetail = {
  id: 'report-1',
  reporterUsername: 'alpha',
  reason: 'Spoilers without a warning',
  type: 'review',
  targetId: 'review-1',
  targetAuthorUsername: 'bravo',
  targetFullContent: 'The ending is ruined for you now.',
  createdAt: '2026-03-04T18:30:00',
}

function renderCard(report: AdminReportDetail = reviewReport) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminReportDetailCard report={report} />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  dismissMock.mockClear()
  deleteReviewMock.mockClear()
  deleteCommentMock.mockClear()
  invalidateMock.mockClear()
  navigateMock.mockClear()
})

describe('AdminReportDetailCard', () => {
  it('shows the reported content in full, not a preview', () => {
    renderCard()

    expect(
      screen.getByText('The ending is ruined for you now.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Spoilers without a warning')).toBeInTheDocument()
    expect(screen.getByText(/by bravo/)).toBeInTheDocument()
  })

  it('dismisses the report without touching the content', async () => {
    renderCard()

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss report' }))
    fireEvent.click(
      screen.getAllByRole('button', { name: 'Dismiss report' }).at(-1)!,
    )

    await waitFor(() => expect(dismissMock).toHaveBeenCalledWith('report-1'))
    expect(deleteReviewMock).not.toHaveBeenCalled()
    await waitFor(() => expect(navigateMock).toHaveBeenCalled())
  })

  it('deletes the review behind a review report', async () => {
    renderCard()

    fireEvent.click(screen.getByRole('button', { name: 'Remove review' }))
    fireEvent.click(
      screen.getAllByRole('button', { name: 'Remove review' }).at(-1)!,
    )

    await waitFor(() =>
      expect(deleteReviewMock).toHaveBeenCalledWith('review-1'),
    )
    expect(deleteCommentMock).not.toHaveBeenCalled()
    // The public pages cached this content, so they must be invalidated too.
    await waitFor(() => expect(invalidateMock).toHaveBeenCalled())
  })

  it('deletes the comment behind a comment report', async () => {
    renderCard({
      ...reviewReport,
      type: 'comment',
      targetId: 'comment-1',
    })

    fireEvent.click(screen.getByRole('button', { name: 'Remove comment' }))
    fireEvent.click(
      screen.getAllByRole('button', { name: 'Remove comment' }).at(-1)!,
    )

    await waitFor(() =>
      expect(deleteCommentMock).toHaveBeenCalledWith('comment-1'),
    )
    expect(deleteReviewMock).not.toHaveBeenCalled()
  })

  it('cannot remove content that is already gone', () => {
    renderCard({
      ...reviewReport,
      targetId: null,
      targetFullContent: null,
    })

    const remove = screen.getByRole('button', { name: 'Remove review' })
    expect(remove).toBeDisabled()
    expect(remove).toHaveAttribute(
      'title',
      'This content has already been removed',
    )
    expect(
      screen.getByText('This content has already been removed.'),
    ).toBeInTheDocument()
  })

  it('requires confirmation before any destructive action', () => {
    renderCard()

    fireEvent.click(screen.getByRole('button', { name: 'Remove review' }))

    expect(deleteReviewMock).not.toHaveBeenCalled()
  })
})
