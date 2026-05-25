import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { RecommendedGame } from '@/types/game'
import { RecommendedForYouSection } from '@/components/games/recommended-for-you-section'

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

// GameCardHoverActions short-circuits when there is no user — keep tests focused
// on the section / card rendering itself.
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

const games: Array<RecommendedGame> = [
  {
    id: 'game-1',
    title: 'Cool RPG',
    coverUrl: '/covers/1.jpg',
    releaseDate: '2024-05-10',
    averageRating: 4.5,
    reason: 'Matches your favorite genres: RPG',
  },
  {
    id: 'game-2',
    title: 'Studio B Action',
    coverUrl: '/covers/2.jpg',
    releaseDate: '2023-09-01',
    averageRating: 4.0,
    reason: 'From Studio B, like Liked RPG',
  },
]

describe('RecommendedForYouSection', () => {
  it('renders nothing while loading', () => {
    const { container } = render(
      <RecommendedForYouSection games={undefined} isLoading={true} />,
    )
    expect(container.firstChild).toBeNull()
  })

  it('renders nothing when the list is empty', () => {
    const { container } = render(
      <RecommendedForYouSection games={[]} isLoading={false} />,
    )
    expect(container.firstChild).toBeNull()
  })

  it('renders nothing when games is undefined and not loading', () => {
    const { container } = render(
      <RecommendedForYouSection games={undefined} isLoading={false} />,
    )
    expect(container.firstChild).toBeNull()
  })

  it('renders the heading and one card per game with its reason', () => {
    render(<RecommendedForYouSection games={games} isLoading={false} />)

    expect(screen.getByText('Recommended for you')).toBeInTheDocument()
    expect(
      screen.getByText('Matches your favorite genres: RPG'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('From Studio B, like Liked RPG'),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('link')).toHaveLength(2)
  })

  it('links each card to /games/$gameId', () => {
    const { container } = render(
      <RecommendedForYouSection games={games} isLoading={false} />,
    )
    const links = Array.from(container.querySelectorAll('a'))
    expect(links.map((a) => a.getAttribute('href'))).toEqual([
      '/games/game-1',
      '/games/game-2',
    ])
  })
})
