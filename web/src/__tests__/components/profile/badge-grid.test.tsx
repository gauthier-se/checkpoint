import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import type { BadgeDto } from '@/types/profile'
import { BadgeGrid, hasMoreBadges } from '@/components/profile/badge-grid'

function makeBadge(overrides: Partial<BadgeDto> & { id: string }): BadgeDto {
  return {
    id: overrides.id,
    code: overrides.code ?? `code-${overrides.id}`,
    name: overrides.name ?? `Badge ${overrides.id}`,
    picture: overrides.picture ?? null,
    description: overrides.description ?? null,
    hidden: overrides.hidden ?? false,
    earned: overrides.earned ?? false,
  }
}

describe('BadgeGrid', () => {
  describe('without limit (full grid)', () => {
    it('renders visible badges, hidden silhouettes, and the hidden footer', () => {
      const badges: Array<BadgeDto> = [
        makeBadge({ id: '1', name: 'Visible Earned', earned: true }),
        makeBadge({ id: '2', name: 'Visible Locked' }),
        makeBadge({ id: '3', name: 'Hidden Locked', hidden: true }),
        makeBadge({
          id: '4',
          name: 'Hidden Earned',
          hidden: true,
          earned: true,
        }),
      ]

      render(<BadgeGrid badges={badges} />)

      // 2 non-hidden + 1 hidden earned = 3 named badges; the locked hidden one
      // renders as "???".
      expect(screen.getByText('Visible Earned')).toBeInTheDocument()
      expect(screen.getByText('Visible Locked')).toBeInTheDocument()
      expect(screen.getByText('Hidden Earned')).toBeInTheDocument()
      expect(screen.getByText('???')).toBeInTheDocument()
      expect(screen.getByText('1 of 2 hidden discovered')).toBeInTheDocument()
    })

    it('renders an empty message when there are no badges at all', () => {
      render(<BadgeGrid badges={[]} />)
      expect(screen.getByText('No badges to display.')).toBeInTheDocument()
    })
  })

  describe('with limit (preview)', () => {
    it('shows earned badges first, slices to limit, and hides silhouettes + footer', () => {
      const badges: Array<BadgeDto> = [
        makeBadge({ id: '1', name: 'A Locked' }),
        makeBadge({ id: '2', name: 'B Earned', earned: true }),
        makeBadge({ id: '3', name: 'C Locked' }),
        makeBadge({ id: '4', name: 'D Earned', earned: true }),
        makeBadge({ id: '5', name: 'Hidden Locked', hidden: true }),
      ]

      render(<BadgeGrid badges={badges} limit={3} />)

      const names = ['B Earned', 'D Earned', 'A Locked']
      names.forEach((n) => expect(screen.getByText(n)).toBeInTheDocument())
      expect(screen.queryByText('C Locked')).not.toBeInTheDocument()
      expect(screen.queryByText('???')).not.toBeInTheDocument()
      expect(screen.queryByText(/hidden discovered/)).not.toBeInTheDocument()
    })

    it('renders all visible badges when the count is below the limit', () => {
      const badges: Array<BadgeDto> = [
        makeBadge({ id: '1', name: 'One', earned: true }),
        makeBadge({ id: '2', name: 'Two' }),
      ]

      render(<BadgeGrid badges={badges} limit={8} />)

      expect(screen.getByText('One')).toBeInTheDocument()
      expect(screen.getByText('Two')).toBeInTheDocument()
    })
  })
})

describe('hasMoreBadges', () => {
  it('returns true when visible badges exceed the limit', () => {
    const badges = Array.from({ length: 10 }, (_, i) =>
      makeBadge({ id: String(i) }),
    )
    expect(hasMoreBadges(badges, 8)).toBe(true)
  })

  it('returns true when hidden badges exist even if visible fits in the limit', () => {
    const badges: Array<BadgeDto> = [
      makeBadge({ id: '1', earned: true }),
      makeBadge({ id: '2', hidden: true }),
    ]
    expect(hasMoreBadges(badges, 8)).toBe(true)
  })

  it('returns false when visible fits in the limit and no hidden badges exist', () => {
    const badges: Array<BadgeDto> = [
      makeBadge({ id: '1', earned: true }),
      makeBadge({ id: '2' }),
    ]
    expect(hasMoreBadges(badges, 8)).toBe(false)
  })
})
