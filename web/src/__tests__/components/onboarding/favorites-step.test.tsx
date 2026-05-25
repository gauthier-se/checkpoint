import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { FavoritesStep } from '@/components/onboarding/steps/favorites-step'
import { updateOnboardingStep } from '@/queries/onboarding'

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({
    user: { id: 'u-1', username: 'alice' },
    isLoading: false,
  }),
  authQueryOptions: { queryKey: ['auth', 'me'] },
}))

vi.mock('@/queries/onboarding', () => ({
  updateOnboardingStep: vi.fn().mockResolvedValue(undefined),
}))

let profilePromise: Promise<unknown>

vi.mock('@/queries/profile', () => ({
  userProfileQueryOptions: (username: string) => ({
    queryKey: ['users', username, 'profile'],
    queryFn: () => profilePromise,
  }),
}))

// The real editor pulls in DnD + game search; stub it so the test focuses on the
// step's own loading / skip behaviour and the favorites it forwards.
vi.mock('@/components/settings/favorite-games-editor', () => ({
  FavoriteGamesEditor: ({
    initialFavorites,
  }: {
    initialFavorites: Array<unknown>
  }) => <div data-testid="fav-editor">editor:{initialFavorites.length}</div>,
}))

function renderStep(onNext = vi.fn()) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <FavoritesStep onNext={onNext} />
    </QueryClientProvider>,
  )
  return { onNext }
}

describe('FavoritesStep', () => {
  it('shows a loading state while the profile is being fetched', () => {
    // Never-resolving promise keeps the query in the loading state.
    profilePromise = new Promise(() => {})
    renderStep()
    expect(screen.getByText('Loading your library...')).toBeInTheDocument()
    expect(screen.queryByTestId('fav-editor')).not.toBeInTheDocument()
  })

  it('renders the favorites editor with the loaded favorites', async () => {
    profilePromise = Promise.resolve({
      favorites: [
        { gameId: 'g1', title: 'Game 1', coverUrl: null, displayOrder: 0 },
        { gameId: 'g2', title: 'Game 2', coverUrl: null, displayOrder: 1 },
      ],
    })
    renderStep()
    const editor = await screen.findByTestId('fav-editor')
    expect(editor).toHaveTextContent('editor:2')
  })

  it('advances and records a skip when "Skip for now" is clicked', () => {
    profilePromise = new Promise(() => {})
    const { onNext } = renderStep()

    fireEvent.click(screen.getByRole('button', { name: 'Skip for now' }))

    expect(onNext).toHaveBeenCalledTimes(1)
    expect(updateOnboardingStep).toHaveBeenCalledWith('favorites', false)
  })
})
