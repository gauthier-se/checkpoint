import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AdminUser, AdminUsersSearchParams } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'
import { AdminUsersTable } from '@/components/admin/users/admin-users-table'

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
    const query = search?.page ? `?page=${String(search.page)}` : ''
    return <a href={`${path}${query}`}>{children}</a>
  },
}))

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({ user: { id: 'admin-1' }, isLoading: false }),
}))

vi.mock('@/queries/admin/users', () => ({
  adminUsersQueryKey: ['admin', 'users'],
  banAdminUser: vi.fn(),
  unbanAdminUser: vi.fn(),
}))

const rows: Array<AdminUser> = [
  {
    id: 'user-1',
    username: 'alpha',
    email: 'alpha@example.test',
    banned: false,
  },
  {
    id: 'user-2',
    username: 'bravo',
    email: 'bravo@example.test',
    banned: true,
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

const search: AdminUsersSearchParams = { page: 1, status: 'all' }

function renderTable(
  overrides: Partial<Parameters<typeof AdminUsersTable>[0]> = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminUsersTable
        rows={rows}
        metadata={metadata}
        search={search}
        {...overrides}
      />
    </QueryClientProvider>,
  )
}

describe('AdminUsersTable', () => {
  it('links each username to its detail page', () => {
    renderTable()

    expect(screen.getByRole('link', { name: 'alpha' })).toHaveAttribute(
      'href',
      '/admin/users/user-1',
    )
  })

  it('shows the ban state of each account', () => {
    renderTable()

    const bravoRow = screen.getByRole('link', { name: 'bravo' }).closest('tr')!
    expect(within(bravoRow).getByText('Banned')).toBeInTheDocument()
    expect(
      within(bravoRow).getByRole('button', { name: 'Unban' }),
    ).toBeInTheDocument()

    const alphaRow = screen.getByRole('link', { name: 'alpha' }).closest('tr')!
    expect(within(alphaRow).getByText('Active')).toBeInTheDocument()
    expect(
      within(alphaRow).getByRole('button', { name: 'Ban' }),
    ).toBeInTheDocument()
  })

  it('paginates while preserving the active filters', () => {
    renderTable({ search: { page: 1, q: 'alpha', status: 'banned' } })

    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: /next/i })).toHaveAttribute(
      'href',
      '/admin/users?page=2',
    )
  })

  it('explains an empty result differently when filters are active', () => {
    renderTable({
      rows: [],
      search: { page: 1, q: 'nobody', status: 'all' },
      metadata: {
        ...metadata,
        totalElements: 0,
        totalPages: 1,
        hasNext: false,
        last: true,
      },
    })

    expect(
      screen.getByText('No accounts match these filters.'),
    ).toBeInTheDocument()
  })

  it('uses the neutral empty message when nothing is filtered', () => {
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

    expect(screen.getByText('No accounts yet.')).toBeInTheDocument()
  })
})
