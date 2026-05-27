import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { Library, Pencil } from 'lucide-react'
import { useState } from 'react'
import type {
  GameStatus,
  LibraryResponse,
  UserGameResponse,
} from '@/types/library'
import { CollectionGameCard } from '@/components/collection/collection-game-card'
import { CollectionPagination } from '@/components/collection/collection-pagination'
import { EmptyState } from '@/components/collection/empty-state'
import { NotesDialog } from '@/components/collection/notes-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ButtonGroup } from '@/components/ui/button-group'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { apiFetch } from '@/services/api'

const PAGE_SIZE = 20

export function libraryQuery(page: number) {
  return queryOptions({
    queryKey: ['library', 'me', page],
    queryFn: async (): Promise<LibraryResponse> => {
      const apiPage = page - 1
      const res = await apiFetch(
        `/api/me/library?page=${apiPage}&size=${PAGE_SIZE}`,
      )
      return res.json()
    },
  })
}

const STATUS_LABELS: Record<GameStatus, string> = {
  PLAYING: 'Playing',
  COMPLETED: 'Completed',
  DROPPED: 'Dropped',
}

const STATUS_COLORS: Record<GameStatus, string> = {
  PLAYING: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  COMPLETED: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  DROPPED: 'bg-red-500/15 text-red-400 border-red-500/20',
}

const ALL_STATUSES: Array<GameStatus | 'ALL'> = [
  'ALL',
  'PLAYING',
  'COMPLETED',
  'DROPPED',
]

interface LibraryTabProps {
  page: number
}

export function LibraryTab({ page }: LibraryTabProps) {
  const { data, isLoading, isError } = useQuery(libraryQuery(page))
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState<GameStatus | 'ALL'>('ALL')
  const [notesGame, setNotesGame] = useState<UserGameResponse | null>(null)

  const filteredGames = (data?.content ?? []).filter(
    (game) => filter === 'ALL' || game.status === filter,
  )

  const updateStatusMutation = useMutation({
    mutationFn: async ({
      gameId,
      status,
      notes,
    }: {
      gameId: string
      status: GameStatus
      notes: string | null
    }) => {
      await apiFetch(`/api/me/library/${gameId}`, {
        method: 'PUT',
        body: JSON.stringify({ videoGameId: gameId, status, notes }),
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
      {/* Status filter */}
      <div className="mb-6 flex items-center gap-3">
        <span className="text-sm font-medium text-muted-foreground">
          Filter:
        </span>
        <ButtonGroup>
          {ALL_STATUSES.map((status) => (
            <Button
              key={status}
              variant={filter === status ? 'default' : 'outline'}
              size="sm"
              onClick={() => setFilter(status)}
            >
              {status === 'ALL' ? 'All' : STATUS_LABELS[status]}
            </Button>
          ))}
        </ButtonGroup>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {Array.from({ length: 10 }).map((_, i) => (
            <div key={i} className="flex flex-col gap-2 rounded-lg border p-3">
              <div className="aspect-[3/4] animate-pulse rounded-md bg-muted" />
              <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
            </div>
          ))}
        </div>
      )}

      {/* Error */}
      {(isError || (!isLoading && !data)) && (
        <EmptyState
          icon={<Library className="size-12" />}
          title="Unable to load library"
          description="Something went wrong loading your library. Please try again later."
        />
      )}

      {/* Games grid */}
      {data && filteredGames.length === 0 && (
        <EmptyState
          icon={<Library className="size-12" />}
          title="No games found"
          description={
            filter === 'ALL'
              ? 'Your library is empty. Browse games to start building your collection!'
              : `No games with status "${STATUS_LABELS[filter]}".`
          }
          actionLabel={filter === 'ALL' ? 'Browse Games' : undefined}
          actionTo={filter === 'ALL' ? '/games' : undefined}
        />
      )}

      {data && filteredGames.length > 0 && (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {filteredGames.map((game) => (
              <CollectionGameCard
                key={game.id}
                videoGameId={game.videoGameId}
                title={game.title}
                coverUrl={game.coverUrl}
                releaseDate={game.releaseDate}
              >
                <Badge
                  className={`${STATUS_COLORS[game.status]} mt-1 text-[11px]`}
                >
                  {STATUS_LABELS[game.status]}
                </Badge>
                <div className="mt-auto flex flex-wrap gap-1 pt-2">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 text-xs"
                        disabled={updateStatusMutation.isPending}
                      >
                        Status
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent>
                      {(['PLAYING', 'COMPLETED', 'DROPPED'] as const)
                        .filter((s) => s !== game.status)
                        .map((s) => (
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
                            {STATUS_LABELS[s]}
                          </DropdownMenuItem>
                        ))}
                    </DropdownMenuContent>
                  </DropdownMenu>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 gap-1 text-xs"
                    onClick={() => setNotesGame(game)}
                  >
                    Notes
                    {game.notes && game.notes.trim() !== '' && (
                      <Pencil className="size-3" aria-label="Has notes" />
                    )}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs text-destructive hover:text-destructive"
                    disabled={removeGameMutation.isPending}
                    onClick={() => removeGameMutation.mutate(game.videoGameId)}
                  >
                    Remove
                  </Button>
                </div>
              </CollectionGameCard>
            ))}
          </div>
          <CollectionPagination
            tab="library"
            page={page}
            totalPages={data.metadata.totalPages}
            hasNext={data.metadata.hasNext}
            hasPrevious={data.metadata.hasPrevious}
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
