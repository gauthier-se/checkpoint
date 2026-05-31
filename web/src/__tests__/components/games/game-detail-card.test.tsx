import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'

import { GameDetailCard } from '@/components/games/game-detail-card'

vi.mock('@tanstack/react-router', () => ({
  Link: ({
    children,
    to,
    params,
    className,
    title,
  }: {
    children: ReactNode
    to: string
    params?: Record<string, string>
    className?: string
    title?: string
  }) => {
    const href = params
      ? to.replace(/\$(\w+)/g, (_, key: string) => params[key] ?? '')
      : to
    return (
      <a href={href} className={className} title={title}>
        {children}
      </a>
    )
  },
}))

vi.mock('@/components/ui/tooltip', () => ({
  Tooltip: ({ children }: { children: ReactNode }) => <>{children}</>,
  TooltipTrigger: ({ children }: { children: ReactNode }) => <>{children}</>,
  TooltipContent: ({ children }: { children: ReactNode }) => <>{children}</>,
}))

vi.mock('@/components/games/game-card-hover-actions', () => ({
  GameCardHoverActions: () => <div data-testid="hover-actions" />,
}))

describe('GameDetailCard', () => {
  it('links to the game detail page', () => {
    render(
      <GameDetailCard
        title="Hollow Knight"
        coverUrl="/covers/hk.jpg"
        link={{ type: 'game', gameId: 'game-1' }}
      />,
    )
    const link = screen.getByRole('link', { name: /Hollow Knight/i })
    expect(link).toHaveAttribute('href', '/games/game-1')
  })

  it('links to the play log when the link type is play', () => {
    render(
      <GameDetailCard
        title="Celeste"
        coverUrl={null}
        link={{ type: 'play', playId: 'play-9' }}
      />,
    )
    const link = screen.getByRole('link', { name: /Celeste/i })
    expect(link).toHaveAttribute('href', '/plays/play-9')
  })

  it('renders review, liked and replay markers when set', () => {
    render(
      <GameDetailCard
        title="Hades"
        coverUrl="/covers/hades.jpg"
        link={{ type: 'play', playId: 'play-1' }}
        score={9}
        hasReview
        isLiked
        isReplay
      />,
    )
    expect(screen.getByLabelText('Has review')).toBeInTheDocument()
    expect(screen.getByLabelText('Liked this game')).toBeInTheDocument()
    expect(screen.getByLabelText('Replay')).toBeInTheDocument()
  })

  it('renders the status badge and owner actions slots', () => {
    render(
      <GameDetailCard
        title="Tunic"
        coverUrl="/covers/tunic.jpg"
        link={{ type: 'game', gameId: 'game-2' }}
        statusBadge={<span>Completed</span>}
        actions={<button type="button">Manage</button>}
      />,
    )
    expect(screen.getByText('Completed')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Manage' })).toBeInTheDocument()
  })
})
