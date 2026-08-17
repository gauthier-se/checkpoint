import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { PaginationMetadata } from '@/types/game'
import type { AdminDataTableColumn } from '@/components/admin/admin-data-table'
import { AdminDataTable } from '@/components/admin/admin-data-table'

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
    <a href={search ? `${to}?page=${String(search.page)}` : to}>{children}</a>
  ),
}))

interface Row {
  id: string
  username: string
}

const columns: Array<AdminDataTableColumn<Row>> = [
  { id: 'username', header: 'Username', cell: (row) => row.username },
]

const rows: Array<Row> = [
  { id: 'a', username: 'alpha' },
  { id: 'b', username: 'bravo' },
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

describe('AdminDataTable', () => {
  it('renders a row per entry', () => {
    render(
      <AdminDataTable columns={columns} rows={rows} rowKey={(row) => row.id} />,
    )

    expect(
      screen.getByRole('columnheader', { name: 'Username' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'alpha' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'bravo' })).toBeInTheDocument()
  })

  it('shows the empty message instead of rows when there is nothing to list', () => {
    render(
      <AdminDataTable
        columns={columns}
        rows={[]}
        rowKey={(row) => row.id}
        emptyMessage="No accounts match this filter."
      />,
    )

    expect(
      screen.getByText('No accounts match this filter.'),
    ).toBeInTheDocument()
    expect(screen.queryByText('alpha')).not.toBeInTheDocument()
  })

  it('renders placeholder rows while loading rather than the empty state', () => {
    render(
      <AdminDataTable
        columns={columns}
        rows={[]}
        rowKey={(row) => row.id}
        isLoading
        skeletonRows={3}
        emptyMessage="No accounts match this filter."
      />,
    )

    expect(
      screen.queryByText('No accounts match this filter.'),
    ).not.toBeInTheDocument()
    // One header row plus the placeholder rows.
    expect(screen.getAllByRole('row')).toHaveLength(4)
  })

  it('paginates when a page and metadata are supplied', () => {
    render(
      <AdminDataTable
        columns={columns}
        rows={rows}
        rowKey={(row) => row.id}
        page={1}
        metadata={metadata}
        linkProps={(page) => ({ to: '/admin/users', search: { page } })}
      />,
    )

    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: /next/i })).toHaveAttribute(
      'href',
      '/admin/users?page=2',
    )
  })

  it('omits pagination when the endpoint is not paginated', () => {
    render(
      <AdminDataTable columns={columns} rows={rows} rowKey={(row) => row.id} />,
    )

    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
  })
})
