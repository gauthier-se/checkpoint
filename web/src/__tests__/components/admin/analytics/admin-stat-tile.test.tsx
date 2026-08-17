import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AdminStatTile } from '@/components/admin/analytics/admin-stat-tile'

describe('AdminStatTile', () => {
  it('groups thousands so a large count stays readable', () => {
    render(<AdminStatTile label="Reviews" value={12345} />)

    expect(screen.getByText('12,345')).toBeInTheDocument()
    expect(screen.getByText('Reviews')).toBeInTheDocument()
  })

  it('renders a meter for a ratio rather than a two-slice chart', () => {
    render(
      <AdminStatTile
        label="Active users"
        value={75}
        ratio={{ value: 75, of: 100, label: '25 banned accounts' }}
      />,
    )

    const meter = screen.getByRole('meter', { name: '25 banned accounts' })
    expect(meter).toHaveAttribute('aria-valuenow', '75')
    expect(screen.getByText('25 banned accounts')).toBeInTheDocument()
  })

  it('does not divide by zero on an empty platform', () => {
    render(
      <AdminStatTile
        label="Active users"
        value={0}
        ratio={{ value: 0, of: 0, label: 'No banned accounts' }}
      />,
    )

    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuenow', '0')
  })

  it('omits the meter when no ratio is given', () => {
    render(<AdminStatTile label="Games" value={4} />)

    expect(screen.queryByRole('meter')).not.toBeInTheDocument()
  })

  it('shows a detail line when provided', () => {
    render(
      <AdminStatTile label="Reports" value={2} detail="Dismissed or not" />,
    )

    expect(screen.getByText('Dismissed or not')).toBeInTheDocument()
  })
})
