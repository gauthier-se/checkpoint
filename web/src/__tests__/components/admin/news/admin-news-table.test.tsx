import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AdminNews, AdminNewsSearchParams } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'
import { AdminNewsTable } from '@/components/admin/news/admin-news-table'

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
    <a href={search?.page ? `${to}?page=${String(search.page)}` : to}>
      {children}
    </a>
  ),
}))

vi.mock('@/queries/admin/news', () => ({
  adminNewsQueryKey: ['admin', 'news'],
  publishAdminNews: vi.fn(),
  unpublishAdminNews: vi.fn(),
  deleteAdminNews: vi.fn(),
  createAdminNews: vi.fn(),
  updateAdminNews: vi.fn(),
  invalidateNews: vi.fn(),
}))

vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

const manualDraft: AdminNews = {
  id: 'news-1',
  title: 'A manual draft',
  description: 'Body text',
  createdAt: '2026-03-04T18:30:00',
  updatedAt: '2026-03-04T18:30:00',
  source: 'MANUAL',
}

const importedPublished: AdminNews = {
  id: 'news-2',
  title: 'An imported article',
  createdAt: '2026-03-05T09:00:00',
  updatedAt: '2026-03-05T09:00:00',
  publishedAt: '2026-03-05T09:00:00',
  source: 'RSS',
  feedName: 'Eurogamer',
}

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

const search: AdminNewsSearchParams = { page: 1 }

function renderTable(
  overrides: Partial<Parameters<typeof AdminNewsTable>[0]> = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminNewsTable
        rows={[manualDraft, importedPublished]}
        metadata={metadata}
        search={search}
        {...overrides}
      />
    </QueryClientProvider>,
  )
}

describe('AdminNewsTable', () => {
  it('distinguishes a draft from a published article', () => {
    renderTable()

    const draftRow = screen.getByText('A manual draft').closest('tr')!
    expect(within(draftRow).getByText('Draft')).toBeInTheDocument()
    expect(
      within(draftRow).getByRole('button', { name: 'Publish' }),
    ).toBeInTheDocument()

    const liveRow = screen.getByText('An imported article').closest('tr')!
    expect(within(liveRow).getByText('Published')).toBeInTheDocument()
    expect(
      within(liveRow).getByRole('button', { name: 'Unpublish' }),
    ).toBeInTheDocument()
  })

  it('only offers the editor for manual articles', () => {
    renderTable()

    const draftRow = screen.getByText('A manual draft').closest('tr')!
    expect(
      within(draftRow).getByRole('button', { name: 'Edit' }),
    ).toBeInTheDocument()

    const importedRow = screen.getByText('An imported article').closest('tr')!
    expect(
      within(importedRow).queryByRole('button', { name: 'Edit' }),
    ).not.toBeInTheDocument()
  })

  it('shows the source and its feed name', () => {
    renderTable()

    const importedRow = screen.getByText('An imported article').closest('tr')!
    expect(within(importedRow).getByText('RSS')).toBeInTheDocument()
    expect(within(importedRow).getByText('Eurogamer')).toBeInTheDocument()
  })

  it('offers delete on every article', () => {
    renderTable()

    expect(screen.getAllByRole('button', { name: 'Delete' })).toHaveLength(2)
  })

  it('paginates', () => {
    renderTable()

    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: /next/i })).toHaveAttribute(
      'href',
      '/admin/news?page=2',
    )
  })

  it('explains an empty listing', () => {
    renderTable({
      rows: [],
      metadata: {
        ...metadata,
        totalElements: 0,
        totalPages: 1,
        hasNext: false,
        last: true,
      },
    })

    expect(
      screen.getByText('No articles yet. Write one or import a feed.'),
    ).toBeInTheDocument()
  })
})
