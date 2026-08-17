import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import { AdminTopBarChart } from '@/components/admin/analytics/admin-top-bar-chart'

// The chart itself is client-only and measures its container, neither of which
// jsdom provides; these tests cover the card, the empty state and the
// client-only boundary rather than the rendered SVG.
vi.mock('@tanstack/react-router', () => ({
  ClientOnly: ({ fallback }: { children: ReactNode; fallback: ReactNode }) => (
    <div data-testid="client-only">{fallback}</div>
  ),
}))

const data = [
  { id: 'game-1', label: 'A Game', value: 12 },
  { id: 'game-2', label: 'Another Game', value: 5 },
]

describe('AdminTopBarChart', () => {
  it('titles and describes the ranking', () => {
    render(
      <AdminTopBarChart
        title="Most reviewed games"
        description="Top five by number of reviews."
        data={data}
        valueNoun="review"
        emptyMessage="No reviews yet."
      />,
    )

    expect(screen.getByText('Most reviewed games')).toBeInTheDocument()
    expect(
      screen.getByText('Top five by number of reviews.'),
    ).toBeInTheDocument()
  })

  it('defers the plot to the client rather than rendering it on the server', () => {
    render(
      <AdminTopBarChart
        title="Most reviewed games"
        description="Top five."
        data={data}
        valueNoun="review"
        emptyMessage="No reviews yet."
      />,
    )

    expect(screen.getByTestId('client-only')).toBeInTheDocument()
  })

  it('shows an empty message instead of an empty plot on a fresh database', () => {
    render(
      <AdminTopBarChart
        title="Most reviewed games"
        description="Top five."
        data={[]}
        valueNoun="review"
        emptyMessage="No reviews yet."
      />,
    )

    expect(screen.getByText('No reviews yet.')).toBeInTheDocument()
    expect(screen.queryByTestId('client-only')).not.toBeInTheDocument()
  })
})
