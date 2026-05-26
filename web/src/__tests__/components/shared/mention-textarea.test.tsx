import { fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'

import type { MemberCard } from '@/types/member'
import type { ActiveMention } from '@/hooks/use-mention-autocomplete'
import { MentionTextarea } from '@/components/shared/mention-textarea'

interface MentionModule {
  detectActiveMention: (value: string, caret: number) => ActiveMention | null
  applyMention: (
    value: string,
    mention: ActiveMention,
    pseudo: string,
  ) => { value: string; caret: number }
  useMentionAutocomplete: (
    value: string,
    caret: number,
  ) => { mention: ActiveMention | null; suggestions: Array<MemberCard> }
}

// Replace the data-fetching hook with a synchronous stub so the test needs no
// QueryClientProvider, while keeping the real token detection/insertion helpers.
vi.mock('@/hooks/use-mention-autocomplete', async (importOriginal) => {
  const actual = await importOriginal<MentionModule>()

  const members: Array<MemberCard> = [
    {
      id: '1',
      pseudo: 'alice',
      picture: null,
      level: 1,
      followerCount: 0,
      reviewCount: 0,
      isFollowing: null,
    },
    {
      id: '2',
      pseudo: 'albert',
      picture: null,
      level: 1,
      followerCount: 0,
      reviewCount: 0,
      isFollowing: null,
    },
  ]

  return {
    ...actual,
    useMentionAutocomplete: (value: string, caret: number) => {
      const mention = actual.detectActiveMention(value, caret)
      const suggestions =
        mention && mention.query.length >= 2
          ? members.filter((m) => m.pseudo.startsWith(mention.query))
          : []
      return { mention, suggestions }
    },
  }
})

function Harness() {
  const [value, setValue] = useState('')
  return (
    <MentionTextarea
      value={value}
      onChange={setValue}
      placeholder="Write a comment..."
    />
  )
}

function typeMention(textarea: HTMLElement, text: string) {
  fireEvent.change(textarea, {
    target: { value: text, selectionStart: text.length },
  })
}

describe('MentionTextarea', () => {
  it('shows matching member suggestions while typing a mention', () => {
    render(<Harness />)
    const textarea = screen.getByPlaceholderText('Write a comment...')

    typeMention(textarea, 'hi @al')

    expect(screen.getByText('@alice')).toBeInTheDocument()
    expect(screen.getByText('@albert')).toBeInTheDocument()
  })

  it('inserts the highlighted suggestion on Enter', () => {
    render(<Harness />)
    const textarea = screen.getByPlaceholderText('Write a comment...')

    typeMention(textarea, 'hi @al')
    fireEvent.keyDown(textarea, { key: 'Enter' })

    expect(textarea).toHaveValue('hi @alice ')
  })

  it('inserts the suggestion clicked with the mouse', () => {
    render(<Harness />)
    const textarea = screen.getByPlaceholderText('Write a comment...')

    typeMention(textarea, 'hi @al')
    fireEvent.mouseDown(screen.getByText('@albert'))

    expect(textarea).toHaveValue('hi @albert ')
  })

  it('moves the highlight with the arrow keys before inserting', () => {
    render(<Harness />)
    const textarea = screen.getByPlaceholderText('Write a comment...')

    typeMention(textarea, 'hi @al')
    fireEvent.keyDown(textarea, { key: 'ArrowDown' })
    fireEvent.keyDown(textarea, { key: 'Enter' })

    expect(textarea).toHaveValue('hi @albert ')
  })

  it('closes the popover on Escape', () => {
    render(<Harness />)
    const textarea = screen.getByPlaceholderText('Write a comment...')

    typeMention(textarea, 'hi @al')
    expect(screen.getByText('@alice')).toBeInTheDocument()

    fireEvent.keyDown(textarea, { key: 'Escape' })

    expect(screen.queryByText('@alice')).toBeNull()
  })

  it('does not suggest anything for a plain email address', () => {
    render(<Harness />)
    const textarea = screen.getByPlaceholderText('Write a comment...')

    typeMention(textarea, 'mail bob@al')

    expect(screen.queryByText('@alice')).toBeNull()
  })
})
