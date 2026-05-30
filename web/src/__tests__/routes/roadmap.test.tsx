import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { RoadmapPage } from '@/routes/_app/roadmap'

describe('RoadmapPage', () => {
  it('renders the page title', () => {
    render(<RoadmapPage />)
    expect(
      screen.getByRole('heading', { level: 1, name: 'Roadmap' }),
    ).toBeInTheDocument()
  })

  it('renders the three roadmap phases', () => {
    render(<RoadmapPage />)
    expect(screen.getByText('Short term')).toBeInTheDocument()
    expect(screen.getByText('Medium term')).toBeInTheDocument()
    expect(screen.getByText('Long term')).toBeInTheDocument()
  })

  it('renders an upcoming feature from each phase', () => {
    render(<RoadmapPage />)
    expect(
      screen.getByText('Translating the app into more languages'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Collaborative lists with friends'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Native mobile app (iOS & Android)'),
    ).toBeInTheDocument()
  })
})
