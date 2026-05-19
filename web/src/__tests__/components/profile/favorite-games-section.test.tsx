import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { FavoriteGame } from '@/types/profile'
import { FavoriteGamesSection } from '@/components/profile/favorite-games-section'

vi.mock('@tanstack/react-router', () => ({
  Link: ({
    children,
    to,
    params,
    hash,
    className,
    title,
  }: {
    children: React.ReactNode
    to: string
    params?: Record<string, string>
    hash?: string
    className?: string
    title?: string
  }) => {
    let href = params
      ? to.replace(/\$(\w+)/g, (_, key: string) => params[key] ?? '')
      : to
    if (hash) href += `#${hash}`
    return (
      <a href={href} className={className} title={title}>
        {children}
      </a>
    )
  },
}))

const favorites: Array<FavoriteGame> = [
  {
    gameId: 'game-1',
    title: 'Game One',
    coverUrl: '/covers/1.jpg',
    displayOrder: 0,
  },
  {
    gameId: 'game-2',
    title: 'Game Two',
    coverUrl: null,
    displayOrder: 1,
  },
]

describe('FavoriteGamesSection', () => {
  it('always renders 5 slots', () => {
    const { container } = render(
      <FavoriteGamesSection favorites={favorites} isOwner={false} />,
    )
    // 2 filled (anchors) + 3 empty placeholders (divs).
    const grid = container.querySelector('.grid')
    expect(grid?.children).toHaveLength(5)
  })

  it('renders a separator under the grid', () => {
    const { container } = render(
      <FavoriteGamesSection favorites={favorites} isOwner={false} />,
    )
    expect(container.querySelector('[data-slot="separator"]')).not.toBeNull()
  })

  it('links filled slots to /games/$gameId', () => {
    const { container } = render(
      <FavoriteGamesSection favorites={favorites} isOwner={false} />,
    )
    const links = Array.from(container.querySelectorAll('a[title]'))
    expect(links.map((a) => a.getAttribute('href'))).toEqual([
      '/games/game-1',
      '/games/game-2',
    ])
    expect(links[0]).toHaveAttribute('title', 'Game One')
    expect(links[1]).toHaveAttribute('title', 'Game Two')
  })

  it('shows an "Add favorite" CTA in empty slots when the viewer is the owner', () => {
    render(<FavoriteGamesSection favorites={favorites} isOwner={true} />)
    const ctas = screen.getAllByRole('link', { name: /Add favorite/ })
    expect(ctas).toHaveLength(3)
    expect(ctas[0]).toHaveAttribute('href', '/settings/profile#favorites')
  })

  it('does not show the CTA for visitors (non-owners)', () => {
    render(<FavoriteGamesSection favorites={favorites} isOwner={false} />)
    expect(screen.queryByText(/Add favorite/)).not.toBeInTheDocument()
  })

  it('renders 5 empty placeholders when the user has no favorites and is a visitor', () => {
    const { container } = render(
      <FavoriteGamesSection favorites={[]} isOwner={false} />,
    )
    const grid = container.querySelector('.grid')
    expect(grid?.children).toHaveLength(5)
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })
})
