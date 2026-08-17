import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeleteGameButton } from '@/components/admin/games/delete-game-button'
import { ApiError } from '@/services/api'

const deleteMock = vi.fn((_gameId: string) => Promise.resolve())
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()

vi.mock('@/queries/admin/games', () => ({
  adminGamesQueryKey: ['admin', 'games'],
  deleteAdminGame: (gameId: string) => deleteMock(gameId),
}))

vi.mock('sonner', () => ({
  toast: {
    success: (...args: Array<unknown>) => toastSuccessMock(...args),
    error: (...args: Array<unknown>) => toastErrorMock(...args),
  },
}))

function renderButton() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <DeleteGameButton game={{ id: 'game-1', title: 'A Game' }} />
    </QueryClientProvider>,
  )
}

function confirm() {
  fireEvent.click(screen.getByRole('button', { name: 'Delete' }))
  fireEvent.click(screen.getAllByRole('button', { name: 'Delete' }).at(-1)!)
}

beforeEach(() => {
  deleteMock.mockReset()
  deleteMock.mockResolvedValue(undefined)
  toastSuccessMock.mockClear()
  toastErrorMock.mockClear()
})

describe('DeleteGameButton', () => {
  it('deletes after confirmation', async () => {
    renderButton()
    confirm()

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('game-1'))
    await waitFor(() => expect(toastSuccessMock).toHaveBeenCalled())
  })

  it('spells out what still references the game on a 409', async () => {
    deleteMock.mockRejectedValue(
      new ApiError(409, 'Conflict', 'Game cannot be deleted', {
        blockingReferences: { library: 3, reviews: 1 },
      }),
    )

    renderButton()
    confirm()

    await waitFor(() => expect(toastErrorMock).toHaveBeenCalled())
    const [title, options] = toastErrorMock.mock.calls[0] as [
      string,
      { description: string },
    ]
    expect(title).toContain('still in use')
    expect(options.description).toContain('3 library entries, 1 review')
    expect(toastSuccessMock).not.toHaveBeenCalled()
  })

  it('falls back to a plain message for any other failure', async () => {
    deleteMock.mockRejectedValue(new ApiError(500, 'Error', 'Boom'))

    renderButton()
    confirm()

    await waitFor(() => expect(toastErrorMock).toHaveBeenCalled())
    expect(toastErrorMock.mock.calls[0][0]).toContain('Could not delete')
  })

  it('does not delete before confirmation', () => {
    renderButton()
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(deleteMock).not.toHaveBeenCalled()
  })
})
