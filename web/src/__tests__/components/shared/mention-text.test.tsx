import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { MentionText } from '@/components/shared/mention-text'

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

describe('MentionText', () => {
  it('renders a mention as a link to the user profile', () => {
    render(<MentionText content="hello @alice!" />)

    const link = screen.getByRole('link', { name: '@alice' })
    expect(link).toHaveAttribute('href', '/profile/alice')
  })

  it('renders text without mentions verbatim and adds no links', () => {
    render(<MentionText content="just some text" />)

    expect(screen.getByText('just some text')).toBeInTheDocument()
    expect(screen.queryByRole('link')).toBeNull()
  })

  it('does not linkify email addresses', () => {
    render(<MentionText content="reach me at bob@example.com" />)

    expect(screen.queryByRole('link')).toBeNull()
  })

  it('renders multiple distinct mentions', () => {
    render(<MentionText content="@alice and @bob_42" />)

    expect(screen.getByRole('link', { name: '@alice' })).toHaveAttribute(
      'href',
      '/profile/alice',
    )
    expect(screen.getByRole('link', { name: '@bob_42' })).toHaveAttribute(
      'href',
      '/profile/bob_42',
    )
  })
})
