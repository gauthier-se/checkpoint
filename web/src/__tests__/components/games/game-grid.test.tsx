import { render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { Game } from '@/types/game'
import { GameGrid } from '@/components/games/game-grid'

vi.mock('@tanstack/react-router', () => ({
  Link: ({
    children,
    to,
    params,
    className,
  }: {
    children: React.ReactNode
    to: string
    params?: Record<string, string>
    className?: string
  }) => {
    const href = params
      ? to.replace(/\$(\w+)/g, (_, key: string) => params[key] ?? '')
      : to
    return (
      <a href={href} className={className}>
        {children}
      </a>
    )
  },
}))

// GameCardHoverActions short-circuits when there is no user: keep tests focused
// on the grid / card layout itself.
vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({ user: null, isLoading: false }),
}))

// The hover-actions hook unconditionally calls useQueryClient, which would
// require a QueryClientProvider in the tree. Stub it with no-op values.
vi.mock('@/hooks/use-wishlist-backlog-actions', () => ({
  useWishlistBacklogActions: () => ({
    inWishlist: false,
    inBacklog: false,
    liked: false,
    toggleWishlist: () => {},
    toggleBacklog: () => {},
    toggleLike: () => {},
    wishlistPending: false,
    backlogPending: false,
    likePending: false,
  }),
}))

const makeGames = (count: number): Array<Game> =>
  Array.from({ length: count }, (_, i) => ({
    id: `game-${i + 1}`,
    title: `Game ${i + 1}`,
    coverUrl: `/covers/${i + 1}.jpg`,
    releaseDate: '2024-05-10',
    averageRating: 4.5,
    ratingCount: 10,
  }))

const TRIM_CLASS = 'max-md:[&>*:nth-child(7)]:hidden'

describe('GameGrid', () => {
  it('constrains every cover to the 3/4 tile so tall images cannot overflow', () => {
    const { container } = render(<GameGrid games={makeGames(3)} />)

    const covers = Array.from(container.querySelectorAll('img'))
    expect(covers).toHaveLength(3)
    for (const cover of covers) {
      expect(cover.className).toContain('aspect-[3/4]')
      expect(cover.className).toContain('object-cover')
    }
  })

  it('renders one card per game', () => {
    const { container } = render(<GameGrid games={makeGames(7)} columns={7} />)

    expect(container.querySelectorAll('img')).toHaveLength(7)
  })

  it('trims the lone 7th tile below md when hideLastOnMobile is set', () => {
    const { container } = render(
      <GameGrid games={makeGames(7)} columns={7} hideLastOnMobile />,
    )

    expect(container.firstElementChild?.className).toContain(TRIM_CLASS)
  })

  it('keeps every tile visible by default, so result grids never hide a game', () => {
    const { container } = render(<GameGrid games={makeGames(7)} columns={7} />)

    expect(container.firstElementChild?.className).not.toContain(TRIM_CLASS)
  })
})
