import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AdminReviewsSearchParams } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'
import type { ModeratedReviewRow } from '@/lib/admin-moderation'
import { AdminReviewsTable } from '@/components/admin/moderation/admin-reviews-table'

vi.mock('@tanstack/react-router', () => ({
  Link: ({
    children,
    to,
    search,
  }: {
    children: React.ReactNode
    to: string
    search?: Record<string, unknown>
  }) => (
    <a
      href={
        search?.page
          ? `${to}?page=${String(search.page)}&reported=${String(search.reported)}`
          : to
      }
    >
      {children}
    </a>
  ),
}))

vi.mock('@/queries/admin/moderation', () => ({
  deleteAdminReview: vi.fn(),
  invalidateModeratedContent: vi.fn(),
  adminReviewReportsQueryOptions: () => ({
    queryKey: ['admin', 'moderation', 'reports'],
    queryFn: () => Promise.resolve({ content: [], metadata: {} }),
  }),
}))

vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

const rows: Array<ModeratedReviewRow> = [
  {
    id: 'review-1',
    content: 'A long rant about the ending.',
    authorUsername: 'alpha',
    gameTitle: 'A Game',
    createdAt: '2026-03-04T18:30:00',
    reportCount: 3,
    haveSpoilers: null,
  },
]

const metadata: PaginationMetadata = {
  page: 0,
  size: 20,
  totalElements: 40,
  totalPages: 2,
  first: true,
  last: false,
  hasNext: true,
  hasPrevious: false,
}

function renderTable(search: AdminReviewsSearchParams, rowOverride = rows) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminReviewsTable
        rows={rowOverride}
        metadata={metadata}
        search={search}
      />
    </QueryClientProvider>,
  )
}

describe('AdminReviewsTable', () => {
  it('offers the reports drill-down only where a count exists', () => {
    const { unmount } = renderTable({ page: 1, reported: true })
    expect(
      screen.getByRole('columnheader', { name: 'Reports' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: '3 reports' }),
    ).toBeInTheDocument()
    unmount()

    renderTable({ page: 1, reported: false }, [
      { ...rows[0], reportCount: null, haveSpoilers: true },
    ])
    expect(
      screen.queryByRole('columnheader', { name: 'Reports' }),
    ).not.toBeInTheDocument()
  })

  it('flags a spoiler review in the all-reviews listing', () => {
    renderTable({ page: 1, reported: false }, [
      { ...rows[0], reportCount: null, haveSpoilers: true },
    ])

    expect(screen.getByText('Spoilers')).toBeInTheDocument()
  })

  it('always offers the delete action', () => {
    renderTable({ page: 1, reported: true })

    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
  })

  it('keeps the reported toggle when paginating', () => {
    renderTable({ page: 1, reported: true })

    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: /next/i })).toHaveAttribute(
      'href',
      '/admin/moderation/reviews?page=2&reported=true',
    )
  })

  it('uses a different empty message per listing', () => {
    const { unmount } = renderTable({ page: 1, reported: true }, [])
    expect(
      screen.getByText('No reported reviews. Nothing to look at.'),
    ).toBeInTheDocument()
    unmount()

    renderTable({ page: 1, reported: false }, [])
    expect(screen.getByText('No reviews yet.')).toBeInTheDocument()
  })
})
