import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { Library, MoreVertical, Pencil, Trash2 } from 'lucide-react'
import { useState } from 'react'
import type { CollectionTab } from '@/types/collection'
import type { PlayStatus } from '@/types/interaction'
import type { LibraryResponse, UserGameResponse } from '@/types/library'
import { PLAY_STATUS_LABELS, PLAY_STATUS_ORDER } from '@/lib/play-status'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { EmptyState } from '@/components/collection/empty-state'
import { STATUS_TABS } from '@/components/profile/profile-status-bar'
import { NotesDialog } from '@/components/collection/notes-dialog'
import { PaginationNav } from '@/components/shared/pagination-nav'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { apiFetch } from '@/services/api'

const PAGE_SIZE = 20

export type LibrarySort = 'rating' | 'addedAt' | 'updatedAt' | 'title'

const LIBRARY_SORT_PARAM: Record<LibrarySort, string> = {
  rating: 'rating,desc',
  addedAt: 'createdAt,desc',
  updatedAt: 'updatedAt,desc',
  title: 'title,asc',
}

export const LIBRARY_SORT_LABELS: Record<LibrarySort, string> = {
  rating: 'Rating',
  addedAt: 'Date added',
  updatedAt: 'Last updated',
  title: 'Title A→Z',
}

export function libraryQuery(
  page: number,
  status?: PlayStatus,
  sort: LibrarySort = 'addedAt',
) {
  return queryOptions({
    queryKey: ['library', 'me', page, status ?? null, sort],
    queryFn: async (): Promise<LibraryResponse> => {
      const apiPage = page - 1
      const params = new URLSearchParams({
        page: String(apiPage),
        size: String(PAGE_SIZE),
        sort: LIBRARY_SORT_PARAM[sort],
      })
      if (status) params.set('status', status)
      const res = await apiFetch(`/api/me/library?${params.toString()}`)
      return res.json()
    },
  })
}

interface LibraryTabProps {
  page: number
  status?: PlayStatus
  sort: LibrarySort
  tabKey: Extract<
    CollectionTab,
    | 'games'
    | 'playing'
    | 'played'
    | 'completed'
    | 'retired'
    | 'shelved'
    | 'abandoned'
  >
}

export function LibraryTab({ page, status, sort, tabKey }: LibraryTabProps) {
  const { data, isLoading, isError } = useQuery(
    libraryQuery(page, status, sort),
  )
  const queryClient = useQueryClient()
  const [notesGame, setNotesGame] = useState<UserGameResponse | null>(null)

  const TabIcon = STATUS_TABS.find((t) => t.value === tabKey)?.icon || Library

  const updateStatusMutation = useMutation({
    mutationFn: async ({
      gameId,
      status: newStatus,
      notes,
    }: {
      gameId: string
      status: PlayStatus
      notes: string | null
    }) => {
      await apiFetch(`/api/me/library/${gameId}`, {
        method: 'PUT',
        body: JSON.stringify({ videoGameId: gameId, status: newStatus, notes }),
        headers: { 'Content-Type': 'application/json' },
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['library', 'me'] })
    },
  })

  const removeGameMutation = useMutation({
    mutationFn: async (gameId: string) => {
      await apiFetch(`/api/me/library/${gameId}`, {
        method: 'DELETE',
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['library', 'me'] })
    },
  })

  return (
    <div>
      {isLoading && (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} className="flex flex-col gap-1.5">
              <div className="aspect-[3/4] animate-pulse rounded-md bg-muted" />
              <div className="h-3 w-3/4 animate-pulse rounded bg-muted" />
            </div>
          ))}
        </div>
      )}

      {(isError || (!isLoading && !data)) && (
        <EmptyState
          icon={<TabIcon className="size-12" />}
          title="Unable to load library"
          description="Something went wrong loading your library. Please try again later."
        />
      )}

      {data && data.content.length === 0 && (
        <EmptyState
          icon={<TabIcon className="size-12" />}
          title="No games found"
          description={
            status
              ? `No games with status "${PLAY_STATUS_LABELS[status]}".`
              : 'Your library is empty. Browse games to start building your collection!'
          }
          actionLabel={!status ? 'Browse Games' : undefined}
          actionTo={!status ? '/games' : undefined}
        />
      )}

      {data && data.content.length > 0 && (
        <>
          <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
            {data.content.map((game) => (
              <GameDetailCard
                key={game.id}
                title={game.title}
                coverUrl={game.coverUrl}
                releaseDate={game.releaseDate}
                link={{ type: 'game', gameId: game.videoGameId }}
                score={game.userRating != null ? game.userRating * 2 : null}
                status={game.status}
                actions={
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="secondary"
                        size="icon"
                        className="size-7 bg-background/80 backdrop-blur-sm hover:bg-background"
                        aria-label="Manage game"
                        disabled={
                          updateStatusMutation.isPending ||
                          removeGameMutation.isPending
                        }
                      >
                        <MoreVertical className="size-3.5" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuLabel>Change status</DropdownMenuLabel>
                      {PLAY_STATUS_ORDER.filter((s) => s !== game.status).map(
                        (s) => (
                          <DropdownMenuItem
                            key={s}
                            onClick={() =>
                              updateStatusMutation.mutate({
                                gameId: game.videoGameId,
                                status: s,
                                notes: game.notes,
                              })
                            }
                          >
                            {PLAY_STATUS_LABELS[s]}
                          </DropdownMenuItem>
                        ),
                      )}
                      <DropdownMenuSeparator />
                      <DropdownMenuItem onClick={() => setNotesGame(game)}>
                        <Pencil className="size-3.5" />
                        {game.notes && game.notes.trim() !== ''
                          ? 'Edit notes'
                          : 'Add notes'}
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        variant="destructive"
                        onClick={() =>
                          removeGameMutation.mutate(game.videoGameId)
                        }
                      >
                        <Trash2 className="size-3.5" />
                        Remove
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                }
              />
            ))}
          </div>
          <PaginationNav
            page={page}
            totalPages={data.metadata.totalPages}
            hasNext={data.metadata.hasNext}
            hasPrevious={data.metadata.hasPrevious}
            hideWhenSinglePage
            className="pt-6 pb-4"
            linkProps={(target) => ({
              to: '.',
              search: { tab: tabKey, page: target, sort },
            })}
          />
        </>
      )}

      {notesGame && (
        <NotesDialog
          open
          onOpenChange={(open) => {
            if (!open) setNotesGame(null)
          }}
          videoGameId={notesGame.videoGameId}
          gameTitle={notesGame.title}
          status={notesGame.status}
          initialNotes={notesGame.notes}
        />
      )}
    </div>
  )
}
