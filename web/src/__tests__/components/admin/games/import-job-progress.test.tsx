import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { ImportJobStatus } from '@/types/admin'
import {
  ImportJobProgress,
  importProgressPercent,
} from '@/components/admin/games/import-job-progress'

const job: ImportJobStatus = {
  jobId: 'job-1',
  type: 'TOP_RATED',
  state: 'RUNNING',
  requestedLimit: 100,
  minRatingCount: 100,
  totalFetched: 100,
  processed: 40,
  imported: 30,
  skipped: 8,
  failed: 2,
  errors: [],
  errorMessage: null,
  startedAt: '2026-03-04T18:30:00Z',
  finishedAt: null,
}

describe('importProgressPercent', () => {
  it('reports the share of the batch already processed', () => {
    expect(importProgressPercent(job)).toBe(40)
  })

  it('reports zero before the batch size is known, rather than dividing by zero', () => {
    expect(importProgressPercent({ ...job, totalFetched: 0 })).toBe(0)
  })

  it('never exceeds 100', () => {
    expect(
      importProgressPercent({ ...job, processed: 150, totalFetched: 100 }),
    ).toBe(100)
  })
})

describe('ImportJobProgress', () => {
  it('exposes progress to assistive technology', () => {
    render(<ImportJobProgress job={job} />)

    expect(screen.getByRole('progressbar')).toHaveAttribute(
      'aria-valuenow',
      '40',
    )
  })

  it('breaks down the outcome counts', () => {
    render(<ImportJobProgress job={job} />)

    expect(screen.getByText('RUNNING')).toBeInTheDocument()
    expect(screen.getByText('40/100')).toBeInTheDocument()
    expect(screen.getByText('30')).toBeInTheDocument()
    expect(screen.getByText('8')).toBeInTheDocument()
  })

  it('reassures the admin that a running import survives navigation', () => {
    render(<ImportJobProgress job={job} />)

    expect(screen.getByText(/does not stop the import/i)).toBeInTheDocument()
  })

  it('surfaces the fatal error of a failed job and drops the running hint', () => {
    render(
      <ImportJobProgress
        job={{
          ...job,
          state: 'FAILED',
          errorMessage: 'IGDB rejected the token',
          finishedAt: '2026-03-04T18:35:00Z',
        }}
      />,
    )

    expect(screen.getByText('IGDB rejected the token')).toBeInTheDocument()
    expect(
      screen.queryByText(/does not stop the import/i),
    ).not.toBeInTheDocument()
  })

  it('collapses per-item errors behind a summary', () => {
    render(
      <ImportJobProgress
        job={{ ...job, errors: ['game 1 failed', 'game 2 failed'] }}
      />,
    )

    expect(screen.getByText('2 import errors')).toBeInTheDocument()
    expect(screen.getByText('game 1 failed')).toBeInTheDocument()
  })
})
