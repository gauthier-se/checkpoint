import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { Library, Pencil } from 'lucide-react'
import { useState } from 'react'
import type { CollectionTab } from '@/types/collection'
import type {
  GameStatus,
  LibraryResponse,
  UserGameResponse,
} from '@/types/library'
import { CollectionGameCard } from '@/components/collection/collection-game-card'
import { EmptyState } from '@/components/collection/empty-state'
import { NotesDialog } from '@/components/collection/notes-dialog'
import { PaginationNav } from '@/components/shared/pagination-nav'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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
  status?: GameStatus,
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

interface LibraryTabProps {
  page: number
  status?: GameStatus
  sort: LibrarySort
  tabKey: Extract<CollectionTab, 'games' | 'playing' | 'completed' | 'dropped'>
  onSortChange: (sort: LibrarySort) => void
}

export function LibraryTab({
  page,
  status,
  sort,
  tabKey,
  onSortChange,
}: LibraryTabProps) {
  const { data, isLoading, isError } = useQuery(
    libraryQuery(page, status, sort),
  )
  const queryClient = useQueryClient()
  const [notesGame, setNotesGame] = useState<UserGameResponse | null>(null)

  const updateStatusMutation = useMutation({
    mutationFn: async ({
      gameId,
      status: newStatus,
      notes,
    }: {
      gameId: string
      status: GameStatus
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

  const sortControl = (
    <div className="mb-4 flex items-center justify-end gap-2">
      <span className="text-xs text-muted-foreground">Sort by</span>
      <Select
        value={sort}
        onValueChange={(value) => onSortChange(value as LibrarySort)}
      >
        <SelectTrigger size="sm" className="h-8 w-[160px] text-xs">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {(Object.keys(LIBRARY_SORT_LABELS) as Array<LibrarySort>).map(
            (key) => (
              <SelectItem key={key} value={key}>
                {LIBRARY_SORT_LABELS[key]}
              </SelectItem>
            ),
          )}
        </SelectContent>
      </Select>
    </div>
  )

  return (
    <div>
      {sortControl}

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

      {(isError || (!isLoading && !data)) && (
        <EmptyState
          icon={<Library className="size-12" />}
          title="Unable to load library"
          description="Something went wrong loading your library. Please try again later."
        />
      )}

      {data && data.content.length === 0 && (
        <EmptyState
          icon={<Library className="size-12" />}
          title="No games found"
          description={
            status
              ? `No games with status "${STATUS_LABELS[status]}".`
              : 'Your library is empty. Browse games to start building your collection!'
          }
          actionLabel={!status ? 'Browse Games' : undefined}
          actionTo={!status ? '/games' : undefined}
        />
      )}

      {data && data.content.length > 0 && (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {data.content.map((game) => (
              <CollectionGameCard
                key={game.id}
                videoGameId={game.videoGameId}
                title={game.title}
                coverUrl={game.coverUrl}
                releaseDate={game.releaseDate}
                userRating={game.userRating}
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
