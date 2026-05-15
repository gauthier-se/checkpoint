import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { LikeButton } from '@/components/shared/like-button'

function renderButton(overrides: Partial<Parameters<typeof LikeButton>[0]> = {}) {
  const props = {
    liked: false,
    likesCount: 0,
    onToggle: vi.fn(),
    disabled: false,
    isPending: false,
    ...overrides,
  }
  render(<LikeButton {...props} />)
  return props
}

describe('LikeButton', () => {
  it('renders the likes count next to the heart', () => {
    renderButton({ likesCount: 42 })
    expect(screen.getByRole('button')).toHaveTextContent('42')
  })

  it('fills the heart in red when liked', () => {
    const { container } = render(
      <LikeButton
        liked
        likesCount={1}
        onToggle={() => {}}
        disabled={false}
        isPending={false}
      />,
    )
    // The heart icon is the only <svg> in the button.
    const heart = container.querySelector('svg')
    expect(heart).not.toBeNull()
    expect(heart!.classList.contains('fill-current')).toBe(true)
    expect(heart!.classList.contains('text-red-500')).toBe(true)
  })

  it('calls onToggle when clicked', () => {
    const props = renderButton({ likesCount: 3 })
    fireEvent.click(screen.getByRole('button'))
    expect(props.onToggle).toHaveBeenCalledTimes(1)
  })

  it('disables the button while a mutation is pending and does not fire onToggle', () => {
    const props = renderButton({ isPending: true })
    const button = screen.getByRole('button')
    expect(button).toBeDisabled()
    fireEvent.click(button)
    expect(props.onToggle).not.toHaveBeenCalled()
  })
})
