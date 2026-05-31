import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  ArrowRightLeft,
  Flag,
  Library,
  MoreVertical,
  Trash2,
} from 'lucide-react'
import type { BacklogListResponse, Priority } from '@/types/collection'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { CollectionPagination } from '@/components/collection/collection-pagination'
import { EmptyState } from '@/components/collection/empty-state'
import { PriorityBadge } from '@/components/collection/priority-badge'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { updateBacklogPriority } from '@/queries/games'
import { apiFetch } from '@/services/api'

const PRIORITY_OPTIONS: ReadonlyArray<{
  value: Priority | null
  label: string
}> = [
  { value: 'HIGH', label: 'High priority' },
  { value: 'MEDIUM', label: 'Medium priority' },
  { value: 'LOW', label: 'Low priority' },
  { value: null, label: 'No priority' },
]

const PAGE_SIZE = 20

export type BacklogSort = 'addedAt' | 'priority'

const SORT_PARAM: Record<BacklogSort, string> = {
  addedAt: 'createdAt,desc',
  priority: 'priority,desc',
}

export function backlogQuery(page: number, sort: BacklogSort = 'addedAt') {
  return queryOptions({
    queryKey: ['backlog', 'me', page, sort],
    queryFn: async (): Promise<BacklogListResponse> => {
      const apiPage = page - 1
      const res = await apiFetch(
        `/api/me/backlog?page=${apiPage}&size=${PAGE_SIZE}&sort=${SORT_PARAM[sort]}`,
      )
      return res.json()
    },
  })
}

interface BacklogTabProps {
  page: number
  sort: BacklogSort
}

export function BacklogTab({ page, sort }: BacklogTabProps) {
  const { data, isLoading, isError } = useQuery(backlogQuery(page, sort))
  const queryClient = useQueryClient()

  const removeMutation = useMutation({
    mutationFn: async (videoGameId: string) => {
      await apiFetch(`/api/me/backlog/${videoGameId}`, {
        method: 'DELETE',
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['backlog', 'me'] })
    },
  })

  const moveToLibraryMutation = useMutation({
    mutationFn: async (videoGameId: string) => {
      // Add to library with ARE_PLAYING status
      await apiFetch('/api/me/library', {
        method: 'POST',
        body: JSON.stringify({ videoGameId, status: 'ARE_PLAYING' }),
        headers: { 'Content-Type': 'application/json' },
      })
      // Remove from backlog
      await apiFetch(`/api/me/backlog/${videoGameId}`, { method: 'DELETE' })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['backlog', 'me'] })
      void queryClient.invalidateQueries({ queryKey: ['library', 'me'] })
    },
  })

  const priorityMutation = useMutation({
    mutationFn: ({
      videoGameId,
      priority,
    }: {
      videoGameId: string
      priority: Priority | null
    }) => updateBacklogPriority(videoGameId, priority),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['backlog', 'me'] })
    },
  })

  if (isLoading) {
    return (
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
        {Array.from({ length: 14 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-1.5">
            <div className="aspect-[3/4] animate-pulse rounded-md bg-muted" />
            <div className="h-3 w-3/4 animate-pulse rounded bg-muted" />
          </div>
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <EmptyState
        icon={<Library className="size-12" />}
        title="Unable to load backlog"
        description="The backlog feature is not available yet. Check back soon!"
      />
    )
  }

  if (data.content.length === 0) {
    return (
      <EmptyState
        icon={<Library className="size-12" />}
        title="Your backlog is empty"
        description="Found a game you want to play later? Add it to your backlog to keep track!"
        actionLabel="Browse Games"
        actionTo="/games"
      />
    )
  }

  return (
    <div>
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
        {data.content.map((game) => (
          <GameDetailCard
            key={game.id}
            title={game.title}
            coverUrl={game.coverUrl}
            releaseDate={game.releaseDate}
            link={{ type: 'game', gameId: game.videoGameId }}
            statusBadge={
              game.priority ? (
                <PriorityBadge priority={game.priority} />
              ) : undefined
            }
            actions={
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="secondary"
                    size="icon"
                    className="size-7 bg-background/80 backdrop-blur-sm hover:bg-background"
                    aria-label="Manage backlog game"
                    disabled={
                      priorityMutation.isPending ||
                      removeMutation.isPending ||
                      moveToLibraryMutation.isPending
                    }
                  >
                    <MoreVertical className="size-3.5" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem
                    onClick={() =>
                      moveToLibraryMutation.mutate(game.videoGameId)
                    }
                  >
                    <ArrowRightLeft className="size-3.5" />
                    Move to library
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuLabel>Priority</DropdownMenuLabel>
                  {PRIORITY_OPTIONS.map((option) => (
                    <DropdownMenuItem
                      key={option.label}
                      disabled={game.priority === option.value}
                      onClick={() =>
                        priorityMutation.mutate({
                          videoGameId: game.videoGameId,
                          priority: option.value,
                        })
                      }
                    >
                      <Flag className="size-3.5" />
                      {option.label}
                    </DropdownMenuItem>
                  ))}
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    variant="destructive"
                    onClick={() => removeMutation.mutate(game.videoGameId)}
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
      <CollectionPagination
        tab="backlog"
        page={page}
        totalPages={data.metadata.totalPages}
        hasNext={data.metadata.hasNext}
        hasPrevious={data.metadata.hasPrevious}
      />
    </div>
  )
}
