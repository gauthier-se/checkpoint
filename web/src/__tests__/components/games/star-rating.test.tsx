import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import type { GameDetail } from '@/types/game'
import { StarRating } from '@/components/games/star-rating'

const rateGameMock = vi.fn()
const removeRatingMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()
const useAuthMock = vi.fn()
const routerInvalidateMock = vi.fn()

vi.mock('@/queries/games', () => ({
  rateGame: (...args: Array<unknown>) => rateGameMock(...args),
  removeRating: (...args: Array<unknown>) => removeRatingMock(...args),
  gameInteractionStatusQueryOptions: (gameId: string) => ({
    queryKey: ['games', gameId, 'interaction-status'],
    queryFn: () => Promise.resolve(null),
  }),
}))

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => useAuthMock(),
}))

vi.mock('@tanstack/react-router', () => ({
  useRouter: () => ({ invalidate: routerInvalidateMock }),
}))

vi.mock('sonner', () => ({
  toast: {
    success: (...args: Array<unknown>) => toastSuccessMock(...args),
    error: (...args: Array<unknown>) => toastErrorMock(...args),
  },
}))

vi.mock('@/components/ui/tooltip', () => ({
  Tooltip: ({ children }: { children: ReactNode }) => <>{children}</>,
  TooltipTrigger: ({ children }: { children: ReactNode }) => <>{children}</>,
  TooltipContent: ({ children }: { children: ReactNode }) => <>{children}</>,
}))

const TEST_GAME: GameDetail = {
  id: 'game-1',
  title: 'Test Game',
  description: null,
  coverUrl: '',
  artworkUrl: null,
  trailerYoutubeId: null,
  timeToBeatNormally: null,
  timeToBeatHastily: null,
  timeToBeatCompletely: null,
  releaseDate: '2025-01-01',
  averageRating: null,
  ratingCount: 0,
  ratingDistribution: [],
  genres: [],
  platforms: [],
  companies: [],
}

function renderWithClient(ui: ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
  )
}

describe('StarRating (half-star)', () => {
  beforeEach(() => {
    useAuthMock.mockReturnValue({ user: { id: 'u-1', pseudo: 'me' } })
    rateGameMock.mockResolvedValue({
      id: 'r-1',
      score: 7,
      videoGameId: TEST_GAME.id,
      createdAt: '',
      updatedAt: '',
    })
    removeRatingMock.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders 10 half-star buttons labelled 0.5 through 5.0', () => {
    renderWithClient(<StarRating game={TEST_GAME} currentRating={null} />)

    for (let half = 1; half <= 10; half++) {
      const label = `Rate ${(half / 2).toFixed(1)} stars`
      expect(screen.getByRole('button', { name: label })).toBeInTheDocument()
    }
  })

  it('calls rateGame with raw 1-10 score when clicking a half-star', async () => {
    renderWithClient(<StarRating game={TEST_GAME} currentRating={null} />)

    // 3.5 stars = score 7
    fireEvent.click(screen.getByLabelText('Rate 3.5 stars'))

    await waitFor(() =>
      expect(rateGameMock).toHaveBeenCalledWith(TEST_GAME.id, 7),
    )
    expect(removeRatingMock).not.toHaveBeenCalled()
  })

  it('calls rateGame with the right-half score when clicking a full star', async () => {
    renderWithClient(<StarRating game={TEST_GAME} currentRating={null} />)

    // 3.0 stars = score 6
    fireEvent.click(screen.getByLabelText('Rate 3.0 stars'))

    await waitFor(() =>
      expect(rateGameMock).toHaveBeenCalledWith(TEST_GAME.id, 6),
    )
  })

  it('removes the rating when clicking the current score', async () => {
    renderWithClient(<StarRating game={TEST_GAME} currentRating={5} />)

    // currentRating = 5 → clicking the 2.5★ button removes
    fireEvent.click(screen.getByLabelText('Rate 2.5 stars'))

    await waitFor(() => expect(removeRatingMock).toHaveBeenCalledTimes(1))
    expect(rateGameMock).not.toHaveBeenCalled()
  })

  it('renders a half-filled star when currentRating is an odd score', () => {
    const { container } = renderWithClient(
      <StarRating game={TEST_GAME} currentRating={5} />,
    )

    // For score 5 (2.5★): stars 1-2 are full, star 3 is half, stars 4-5 are empty.
    // The half-star uses clip-path: inset(0 50% 0 0)
    const halfFilled = container.querySelector('[style*="inset(0 50% 0 0)"]')
    expect(halfFilled).not.toBeNull()
  })

  it('disables interactions when no user is logged in', () => {
    useAuthMock.mockReturnValue({ user: null })
    renderWithClient(<StarRating game={TEST_GAME} currentRating={null} />)

    // All buttons are disabled (no auth)
    const buttons = screen.getAllByRole('button')
    buttons.forEach((btn) => expect(btn).toBeDisabled())
  })
})
