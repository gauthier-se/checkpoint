import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AdminReport, AdminReportsSearchParams } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'
import { AdminReportsTable } from '@/components/admin/moderation/admin-reports-table'

vi.mock('@tanstack/react-router', () => ({
  Link: ({
    children,
    to,
    params,
    search,
  }: {
    children: React.ReactNode
    to: string
    params?: Record<string, string>
    search?: Record<string, unknown>
  }) => {
    const path = params
      ? to.replace(/\$(\w+)/g, (_, key: string) => params[key] ?? '')
      : to
    const query = search?.page
      ? `?page=${String(search.page)}&type=${String(search.type)}`
      : ''
    return <a href={`${path}${query}`}>{children}</a>
  },
}))

const rows: Array<AdminReport> = [
  {
    id: 'report-1',
    reporterUsername: 'alpha',
    reason: 'Spoilers',
    type: 'review',
    contentPreview: 'The ending is…',
    createdAt: '2026-03-04T18:30:00',
  },
  {
    id: 'report-2',
    reporterUsername: null,
    reason: null,
    type: 'comment',
    contentPreview: null,
    createdAt: '2026-03-05T09:00:00',
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

const search: AdminReportsSearchParams = { page: 1, type: 'all' }

function renderTable(
  overrides: Partial<Parameters<typeof AdminReportsTable>[0]> = {},
) {
  return render(
    <AdminReportsTable
      rows={rows}
      metadata={metadata}
      search={search}
      {...overrides}
    />,
  )
}

describe('AdminReportsTable', () => {
  it('labels what each report targets', () => {
    renderTable()

    const first = screen.getByText('The ending is…').closest('tr')!
    expect(within(first).getByText('Review')).toBeInTheDocument()
    expect(screen.getByText('Comment')).toBeInTheDocument()
  })

  it('links each row to its report detail', () => {
    renderTable()

    const links = screen.getAllByRole('link', { name: 'Open' })
    expect(links[0]).toHaveAttribute(
      'href',
      '/admin/moderation/reports/report-1',
    )
  })

  it('degrades gracefully when the reporter or the content is gone', () => {
    renderTable()

    expect(screen.getByText('Content unavailable')).toBeInTheDocument()
    // Missing reporter and missing reason both render as a dash.
    expect(screen.getAllByText('-').length).toBeGreaterThanOrEqual(2)
  })

  it('keeps the type filter when paginating', () => {
    renderTable({ search: { page: 1, type: 'comment' } })

    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: /next/i })).toHaveAttribute(
      'href',
      '/admin/moderation/reports?page=2&type=comment',
    )
  })

  it('says the queue is empty only when nothing is filtered out', () => {
    const empty = {
      rows: [],
      metadata: {
        ...metadata,
        totalElements: 0,
        totalPages: 1,
        hasNext: false,
        last: true,
      },
    }

    const { unmount } = renderTable(empty)
    expect(
      screen.getByText('The queue is empty. Nothing to moderate.'),
    ).toBeInTheDocument()
    unmount()

    renderTable({ ...empty, search: { page: 1, type: 'review' } })
    expect(screen.getByText('No reports of this kind.')).toBeInTheDocument()
  })
})
