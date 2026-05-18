import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { LeaderboardEntry } from '@/types/leaderboard'
import { LeaderboardTable } from '@/components/leaderboard/leaderboard-table'

vi.mock('@tanstack/react-router', () => ({
  Link: ({
    children,
    to,
    params,
  }: {
    children: React.ReactNode
    to: string
    params?: Record<string, string>
  }) => {
    const href = params
      ? to.replace(/\$(\w+)/g, (_, key: string) => params[key] ?? '')
      : to
    return <a href={href}>{children}</a>
  },
}))

const entries: Array<LeaderboardEntry> = [
  {
    rank: 1,
    id: 'user-1',
    pseudo: 'alpha',
    picture: null,
    level: 10,
    xpPoint: 9000,
  },
  {
    rank: 2,
    id: 'user-2',
    pseudo: 'bravo',
    picture: null,
    level: 8,
    xpPoint: 7000,
  },
]

describe('LeaderboardTable', () => {
  it('renders one row per entry with rank, pseudo, level, and XP', () => {
    render(<LeaderboardTable entries={entries} sortBy="xp" />)

    const rows = screen.getAllByRole('listitem')
    expect(rows).toHaveLength(2)

    expect(within(rows[0]).getByText('1')).toBeInTheDocument()
    expect(within(rows[0]).getByText('alpha')).toBeInTheDocument()
    expect(within(rows[0]).getByText('10')).toBeInTheDocument()
    expect(within(rows[0]).getByText('9,000')).toBeInTheDocument()

    expect(within(rows[1]).getByText('2')).toBeInTheDocument()
    expect(within(rows[1]).getByText('bravo')).toBeInTheDocument()
  })

  it('links each row to the user profile by pseudo', () => {
    render(<LeaderboardTable entries={entries} sortBy="xp" />)

    const link = screen.getByRole('link', { name: /alpha/ })
    expect(link).toHaveAttribute('href', '/profile/alpha')
  })

  it('renders an empty state when there are no entries', () => {
    render(<LeaderboardTable entries={[]} sortBy="xp" />)
    expect(screen.getByText(/no players to show yet/i)).toBeInTheDocument()
    expect(screen.queryByRole('listitem')).not.toBeInTheDocument()
  })
})
