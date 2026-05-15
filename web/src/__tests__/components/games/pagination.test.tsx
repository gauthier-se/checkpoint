import { render, screen, within } from '@testing-library/react'
import type { ComponentProps, ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'

vi.mock('@tanstack/react-router', () => ({
  // Replace TanStack Router's <Link> with a plain <a> so the component can
  // render outside of a router context. We forward `disabled` to the DOM as a
  // data attribute (anchors don't support the native disabled attribute) and
  // drop router-only props (to/search/hash) before spreading.
  Link: ({
    children,
    disabled,
    to,
    search,
    hash,
    ...rest
  }: {
    children: ReactNode
    disabled?: boolean
    to?: string
    search?: Record<string, unknown>
    hash?: string
  } & ComponentProps<'a'>) => (
    <a
      href={typeof to === 'string' ? to : '#'}
      data-disabled={disabled ? 'true' : undefined}
      {...rest}
    >
      {children}
    </a>
  ),
}))

// Imported after the mock so the component picks up the mocked Link.
import { GamesPagination } from '@/components/games/pagination'

describe('GamesPagination', () => {
  it('renders one page button per page when totalPages <= 7', () => {
    render(
      <GamesPagination
        page={1}
        totalPages={5}
        hasNext
        hasPrevious={false}
        search={{}}
      />,
    )

    for (const label of ['1', '2', '3', '4', '5']) {
      expect(screen.getByRole('button', { name: label })).toBeInTheDocument()
    }
    // No ellipsis buttons.
    expect(screen.queryByRole('button', { name: '...' })).toBeNull()
  })

  it('marks only the current page button with the default (filled) variant', () => {
    render(
      <GamesPagination
        page={3}
        totalPages={5}
        hasNext
        hasPrevious
        search={{}}
      />,
    )

    const current = screen.getByRole('button', { name: '3' })
    const other = screen.getByRole('button', { name: '2' })
    // The shadcn Button maps variant="default" to "bg-primary"; outline keeps a border without bg-primary.
    expect(current.className).toContain('bg-primary')
    expect(other.className).not.toContain('bg-primary')
  })

  it('disables the Previous and Next buttons at the boundaries', () => {
    render(
      <GamesPagination
        page={1}
        totalPages={3}
        hasNext={false}
        hasPrevious={false}
        search={{}}
      />,
    )

    const prev = screen.getByRole('button', { name: /previous/i })
    const next = screen.getByRole('button', { name: /next/i })
    expect(prev).toBeDisabled()
    expect(next).toBeDisabled()

    // The wrapping anchor is also flagged via data-disabled (forwarded by the Link mock).
    const prevLink = prev.closest('a')!
    const nextLink = next.closest('a')!
    expect(prevLink.dataset.disabled).toBe('true')
    expect(nextLink.dataset.disabled).toBe('true')
    // Sanity: the surrounding link wrappers contain the expected button text.
    expect(within(prevLink).getByRole('button', { name: /previous/i })).toBe(prev)
    expect(within(nextLink).getByRole('button', { name: /next/i })).toBe(next)
  })
})
