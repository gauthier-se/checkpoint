import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { MemberCard } from '@/types/member'
import { FollowStep } from '@/components/onboarding/steps/follow-step'

const suggested: Array<MemberCard> = [
  {
    id: 's-1',
    pseudo: 'SuggestedSally',
    picture: null,
    level: 3,
    followerCount: 10,
    reviewCount: 2,
    isFollowing: false,
  },
]

const searchResults: Array<MemberCard> = [
  {
    id: 'r-1',
    pseudo: 'SearchedSam',
    picture: null,
    level: 7,
    followerCount: 1,
    reviewCount: 0,
    isFollowing: false,
  },
]

vi.mock('@/hooks/use-auth', () => ({
  authQueryOptions: { queryKey: ['auth', 'me'] },
}))

vi.mock('@/queries/onboarding', () => ({
  updateOnboardingStep: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/queries/profile', () => ({
  toggleFollowMutation: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/queries/members', () => ({
  suggestedMembersQueryOptions: () => ({
    queryKey: ['members', 'suggested', 6],
    queryFn: () => Promise.resolve(suggested),
  }),
  searchMembersQueryOptions: (query: string) => ({
    queryKey: ['members', 'search', query],
    queryFn: () => Promise.resolve({ content: searchResults, metadata: {} }),
    enabled: query.length >= 2,
  }),
}))

function renderStep() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <FollowStep onNext={vi.fn()} />
    </QueryClientProvider>,
  )
}

describe('FollowStep', () => {
  it('shows suggested members by default', async () => {
    renderStep()
    expect(await screen.findByText('SuggestedSally')).toBeInTheDocument()
    expect(screen.queryByText('SearchedSam')).not.toBeInTheDocument()
  })

  it('switches to search results when typing 2+ characters', async () => {
    renderStep()
    await screen.findByText('SuggestedSally')

    fireEvent.change(screen.getByLabelText('Search people to follow'), {
      target: { value: 'sa' },
    })

    expect(await screen.findByText('SearchedSam')).toBeInTheDocument()
    expect(screen.queryByText('SuggestedSally')).not.toBeInTheDocument()
  })

  it('keeps showing suggestions for a single-character query', async () => {
    renderStep()
    await screen.findByText('SuggestedSally')

    fireEvent.change(screen.getByLabelText('Search people to follow'), {
      target: { value: 's' },
    })

    expect(await screen.findByText('SuggestedSally')).toBeInTheDocument()
    expect(screen.queryByText('SearchedSam')).not.toBeInTheDocument()
  })
})
