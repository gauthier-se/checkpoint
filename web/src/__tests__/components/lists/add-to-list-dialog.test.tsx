import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import type { GameListDetail, GameListsResponse } from '@/types/list'
import { AddToListDialog } from '@/components/lists/add-to-list-dialog'

const addGameToListMock = vi.fn()
const removeGameFromListMock = vi.fn()
const createListMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()

const GAME_ID = 'game-1'

const MY_LISTS: GameListsResponse = {
  content: [
    {
      id: 'list-1',
      title: 'Favorites',
      description: null,
      isPrivate: false,
      videoGamesCount: 3,
      likesCount: 0,
      commentsCount: 0,
      authorPseudo: 'me',
      authorPicture: null,
      coverUrls: [],
      createdAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 'list-2',
      title: 'To finish',
      description: null,
      isPrivate: true,
      videoGamesCount: 1,
      likesCount: 0,
      commentsCount: 0,
      authorPseudo: 'me',
      authorPicture: null,
      coverUrls: [],
      createdAt: '2026-01-02T00:00:00Z',
    },
  ],
  metadata: { totalElements: 2, totalPages: 1, page: 0, size: 100 },
}

function detailFor(listId: string): GameListDetail {
  // list-1 already contains the game, list-2 does not.
  const entries =
    listId === 'list-1'
      ? [
          {
            videoGameId: GAME_ID,
            title: 'A Game',
            coverUrl: '',
            releaseDate: '2026-01-01',
            position: 0,
            addedAt: '2026-01-01T00:00:00Z',
          },
        ]
      : []
  return {
    id: listId,
    title: listId,
    description: null,
    isPrivate: false,
    videoGamesCount: entries.length,
    likesCount: 0,
    commentsCount: 0,
    authorPseudo: 'me',
    authorPicture: null,
    entries,
    isOwner: true,
    hasLiked: false,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  }
}

let emptyLists = false

vi.mock('@/queries/lists', () => ({
  myListsQueryOptions: () => ({
    queryKey: ['lists', 'mine', 0, 100],
    queryFn: () =>
      Promise.resolve(
        emptyLists
          ? {
              ...MY_LISTS,
              content: [],
              metadata: { ...MY_LISTS.metadata, totalElements: 0 },
            }
          : MY_LISTS,
      ),
  }),
  listDetailQueryOptions: (listId: string) => ({
    queryKey: ['lists', listId, 'detail'],
    queryFn: () => Promise.resolve(detailFor(listId)),
  }),
  addGameToList: (...args: Array<unknown>) => addGameToListMock(...args),
  removeGameFromList: (...args: Array<unknown>) =>
    removeGameFromListMock(...args),
  createList: (...args: Array<unknown>) => createListMock(...args),
}))

vi.mock('@/services/api', () => ({
  isApiError: () => false,
}))

vi.mock('sonner', () => ({
  toast: {
    success: (...args: Array<unknown>) => toastSuccessMock(...args),
    error: (...args: Array<unknown>) => toastErrorMock(...args),
  },
}))

function renderDialog() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AddToListDialog
        gameId={GAME_ID}
        gameTitle="A Game"
        open
        onOpenChange={() => {}}
      />
    </QueryClientProvider>,
  )
}

describe('AddToListDialog', () => {
  beforeEach(() => {
    emptyLists = false
    addGameToListMock.mockResolvedValue(detailFor('list-2'))
    removeGameFromListMock.mockResolvedValue(undefined)
    createListMock.mockResolvedValue(detailFor('list-new'))
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders the user lists with membership reflected', async () => {
    renderDialog()

    await screen.findByText('Favorites')
    expect(screen.getByText('To finish')).toBeInTheDocument()

    const checkboxes = await screen.findAllByRole('checkbox')
    await waitFor(() => {
      expect(checkboxes[0]).toHaveAttribute('aria-checked', 'true')
    })
    expect(checkboxes[1]).toHaveAttribute('aria-checked', 'false')
  })

  it('adds the game when checking an unchecked list', async () => {
    renderDialog()

    const checkboxes = await screen.findAllByRole('checkbox')
    await waitFor(() => {
      expect(checkboxes[1]).not.toBeDisabled()
    })

    fireEvent.click(checkboxes[1])

    await waitFor(() => {
      expect(addGameToListMock).toHaveBeenCalledWith('list-2', GAME_ID)
    })
  })

  it('removes the game when unchecking a checked list', async () => {
    renderDialog()

    const checkboxes = await screen.findAllByRole('checkbox')
    await waitFor(() => {
      expect(checkboxes[0]).toHaveAttribute('aria-checked', 'true')
    })

    fireEvent.click(checkboxes[0])

    await waitFor(() => {
      expect(removeGameFromListMock).toHaveBeenCalledWith('list-1', GAME_ID)
    })
  })

  it('filters lists by title', async () => {
    renderDialog()

    await screen.findByText('Favorites')
    const filterInput = screen.getByPlaceholderText('Filter your lists...')
    fireEvent.change(filterInput, { target: { value: 'finish' } })

    expect(screen.queryByText('Favorites')).not.toBeInTheDocument()
    expect(screen.getByText('To finish')).toBeInTheDocument()
  })

  it('creates a new list and adds the game to it', async () => {
    renderDialog()

    await screen.findByText('Favorites')
    fireEvent.click(screen.getByRole('button', { name: /create a new list/i }))

    const titleInput = screen.getByLabelText('New list title')
    fireEvent.change(titleInput, { target: { value: 'Fresh list' } })

    fireEvent.click(screen.getByRole('button', { name: /create & add/i }))

    await waitFor(() => {
      expect(createListMock).toHaveBeenCalledWith({
        title: 'Fresh list',
        isPrivate: false,
      })
    })
    await waitFor(() => {
      expect(addGameToListMock).toHaveBeenCalledWith('list-new', GAME_ID)
    })
  })

  it('shows an empty state when the user has no lists', async () => {
    emptyLists = true
    renderDialog()

    expect(
      await screen.findByText(/you don't have any lists yet/i),
    ).toBeInTheDocument()
  })
})
