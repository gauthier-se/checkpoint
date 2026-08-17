import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AdminCatalogSearchParams } from '@/types/admin'
import type { Game, PaginationMetadata } from '@/types/game'
import { AdminGamesTable } from '@/components/admin/games/admin-games-table'

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
    return (
      <a href={search?.page ? `${path}?page=${String(search.page)}` : path}>
        {children}
      </a>
    )
  },
}))

vi.mock('@/queries/admin/games', () => ({
  adminGamesQueryKey: ['admin', 'games'],
  deleteAdminGame: vi.fn(),
}))

vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

const rows: Array<Game> = [
  {
    id: 'game-1',
    title: 'A Game',
    coverUrl: 'cover.png',
    releaseDate: '2026-03-04',
    averageRating: 4.25,
    ratingCount: 12,
  },
  {
    id: 'game-2',
    title: 'Unrated Game',
    coverUrl: '',
    releaseDate: '',
    averageRating: null,
    ratingCount: 0,
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

function renderTable(
  overrides: Partial<Parameters<typeof AdminGamesTable>[0]> = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const search: AdminCatalogSearchParams = { page: 1 }
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminGamesTable
        rows={rows}
        metadata={metadata}
        search={search}
        {...overrides}
      />
    </QueryClientProvider>,
  )
}

describe('AdminGamesTable', () => {
  it('links each row to its edit form', () => {
    renderTable()

    expect(screen.getAllByRole('link', { name: 'Edit' })[0]).toHaveAttribute(
      'href',
      '/admin/games/game-1/edit',
    )
  })

  it('shows the release year and the rating summary', () => {
    renderTable()

    const row = screen.getByText('A Game').closest('tr')!
    expect(within(row).getByText('2026')).toBeInTheDocument()
    expect(within(row).getByText('4.3 (12)')).toBeInTheDocument()
  })

  it('degrades to a dash when a game has no date or rating', () => {
    renderTable()

    const row = screen.getByText('Unrated Game').closest('tr')!
    expect(within(row).getAllByText('—')).toHaveLength(2)
  })

  it('offers a delete action per row', () => {
    renderTable()

    expect(screen.getAllByRole('button', { name: 'Delete' })).toHaveLength(2)
  })

  it('names the query in the empty message when searching', () => {
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

    const { unmount } = renderTable({
      ...empty,
      search: { page: 1, q: 'zelda' },
    })
    expect(screen.getByText('No game matches “zelda”.')).toBeInTheDocument()
    unmount()

    renderTable(empty)
    expect(
      screen.getByText('The catalog is empty. Import games to get started.'),
    ).toBeInTheDocument()
  })

  it('keeps the query when paginating search results', () => {
    renderTable({ search: { page: 1, q: 'zelda' } })

    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: /next/i })).toHaveAttribute(
      'href',
      '/admin/games?page=2',
    )
  })
})
