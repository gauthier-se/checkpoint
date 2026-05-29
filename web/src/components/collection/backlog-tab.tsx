import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { Archive, ArrowRightLeft, Trash2 } from 'lucide-react'
import { useState } from 'react'
import type { BacklogListResponse, Priority } from '@/types/collection'
import { CollectionGameCard } from '@/components/collection/collection-game-card'
import { CollectionPagination } from '@/components/collection/collection-pagination'
import { EmptyState } from '@/components/collection/empty-state'
import { PriorityBadge } from '@/components/collection/priority-badge'
import { PrioritySelect } from '@/components/collection/priority-select'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { updateBacklogPriority } from '@/queries/games'
import { apiFetch } from '@/services/api'

const PAGE_SIZE = 20

type BacklogSort = 'addedAt' | 'priority'

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
}

export function BacklogTab({ page }: BacklogTabProps) {
  const [sort, setSort] = useState<BacklogSort>('addedAt')
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
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {Array.from({ length: 10 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-2 rounded-lg border p-3">
            <div className="aspect-[3/4] animate-pulse rounded-md bg-muted" />
            <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
          </div>
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <EmptyState
        icon={<Archive className="size-12" />}
        title="Unable to load backlog"
        description="The backlog feature is not available yet. Check back soon!"
      />
    )
  }

  if (data.content.length === 0) {
    return (
      <EmptyState
        icon={<Archive className="size-12" />}
        title="Your backlog is empty"
        description="Found a game you want to play later? Add it to your backlog to keep track!"
        actionLabel="Browse Games"
        actionTo="/games"
      />
    )
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-end gap-2">
        <span className="text-xs text-muted-foreground">Sort by</span>
        <Select
          value={sort}
          onValueChange={(value) => setSort(value as BacklogSort)}
        >
          <SelectTrigger size="sm" className="h-8 w-[160px] text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="addedAt">Date added</SelectItem>
            <SelectItem value="priority">Priority</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {data.content.map((game) => (
          <CollectionGameCard
            key={game.id}
            videoGameId={game.videoGameId}
            title={game.title}
            coverUrl={game.coverUrl}
            releaseDate={game.releaseDate}
          >
            <div className="flex flex-wrap items-center gap-1.5">
              {game.releaseDate && (
                <p className="text-xs text-muted-foreground">
                  {new Date(game.releaseDate).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                  })}
                </p>
              )}
              <PriorityBadge priority={game.priority} />
            </div>
            <div className="mt-auto flex flex-col gap-1 pt-2">
              <PrioritySelect
                value={game.priority}
                disabled={priorityMutation.isPending}
                onChange={(priority) =>
                  priorityMutation.mutate({
                    videoGameId: game.videoGameId,
                    priority,
                  })
                }
              />
              <Button
                variant="outline"
                size="sm"
                className="h-7 w-full gap-1.5 text-xs"
                disabled={moveToLibraryMutation.isPending}
                onClick={() => moveToLibraryMutation.mutate(game.videoGameId)}
              >
                <ArrowRightLeft className="size-3" />
                Move to Library
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 w-full gap-1.5 text-xs text-destructive hover:text-destructive"
                disabled={removeMutation.isPending}
                onClick={() => removeMutation.mutate(game.videoGameId)}
              >
                <Trash2 className="size-3" />
                Remove
              </Button>
            </div>
          </CollectionGameCard>
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
