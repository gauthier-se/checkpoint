import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { ComponentProps, ReactNode } from 'react'

import { ErrorPage } from '@/components/errors/error-page'

vi.mock('@tanstack/react-router', () => ({
  Link: ({
    children,
    to,
    ...rest
  }: {
    children: ReactNode
    to?: string
  } & ComponentProps<'a'>) => (
    <a href={typeof to === 'string' ? to : '#'} {...rest}>
      {children}
    </a>
  ),
}))

describe('ErrorPage', () => {
  it('renders the 404 variant with no "Try again" button', () => {
    render(<ErrorPage status={404} />)
    expect(
      screen.getByRole('heading', { name: 'Page not found' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /try again/i })).toBeNull()
    expect(screen.getByRole('link', { name: /go home/i })).toHaveAttribute(
      'href',
      '/',
    )
  })

  it('renders the 403 variant', () => {
    render(<ErrorPage status={403} />)
    expect(
      screen.getByRole('heading', { name: 'Access denied' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /try again/i })).toBeNull()
  })

  it('renders the 401 variant with a Sign in link to /login', () => {
    render(<ErrorPage status={401} />)
    expect(
      screen.getByRole('heading', { name: 'Sign in required' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute(
      'href',
      '/login',
    )
  })

  it('renders the 500 variant with a working "Try again" button', () => {
    const onRetry = vi.fn()
    render(<ErrorPage status={500} onRetry={onRetry} />)
    expect(
      screen.getByRole('heading', { name: 'Server error' }),
    ).toBeInTheDocument()
    const retry = screen.getByRole('button', { name: /try again/i })
    fireEvent.click(retry)
    expect(onRetry).toHaveBeenCalledTimes(1)
  })

  it('renders the 503 variant with maintenance copy', () => {
    render(<ErrorPage status={503} onRetry={vi.fn()} />)
    expect(
      screen.getByRole('heading', { name: 'Service unavailable' }),
    ).toBeInTheDocument()
    expect(screen.getByText(/maintenance/i)).toBeInTheDocument()
  })

  it('renders the network (status 0) variant', () => {
    render(<ErrorPage status={0} onRetry={vi.fn()} />)
    expect(
      screen.getByRole('heading', { name: /can't reach the server/i }),
    ).toBeInTheDocument()
  })

  it('falls back to the generic variant when status is undefined', () => {
    render(<ErrorPage onRetry={vi.fn()} />)
    expect(
      screen.getByRole('heading', { name: 'Something went wrong' }),
    ).toBeInTheDocument()
  })

  it('lets custom title and message override the status defaults', () => {
    render(
      <ErrorPage
        status={404}
        title="Game not found"
        message="We couldn't find this game."
      />,
    )
    expect(
      screen.getByRole('heading', { name: 'Game not found' }),
    ).toBeInTheDocument()
    expect(screen.getByText("We couldn't find this game.")).toBeInTheDocument()
  })

  it('omits the "Try again" button on 5xx when no onRetry is provided', () => {
    render(<ErrorPage status={500} />)
    expect(screen.queryByRole('button', { name: /try again/i })).toBeNull()
  })

  it('sets document.title from the resolved title', () => {
    render(<ErrorPage status={404} />)
    expect(document.title).toBe('Page not found - Checkpoint')
  })
})
