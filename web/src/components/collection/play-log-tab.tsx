import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { BookOpen, Trash2 } from 'lucide-react'
import type { PlayLogListResponse } from '@/types/collection'
import { CollectionPagination } from '@/components/collection/collection-pagination'
import { EmptyState } from '@/components/collection/empty-state'
import { JournalTimeline } from '@/components/collection/journal-timeline'
import { Button } from '@/components/ui/button'
import { apiFetch } from '@/services/api'

const PAGE_SIZE = 20

export function playLogQuery(page: number) {
  return queryOptions({
    queryKey: ['plays', 'me', page],
    queryFn: async (): Promise<PlayLogListResponse> => {
      const apiPage = page - 1
      const res = await apiFetch(
        `/api/me/plays?page=${apiPage}&size=${PAGE_SIZE}&sort=updatedAt,desc`,
      )
      return res.json()
    },
  })
}

interface PlayLogTabProps {
  page: number
}

export function PlayLogTab({ page }: PlayLogTabProps) {
  const { data, isLoading, isError } = useQuery(playLogQuery(page))
  const queryClient = useQueryClient()

  const deleteMutation = useMutation({
    mutationFn: async (playId: string) => {
      await apiFetch(`/api/me/plays/${playId}`, {
        method: 'DELETE',
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['plays', 'me'] })
    },
  })

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex items-start gap-4 rounded-lg border p-4">
            <div className="h-24 w-16 animate-pulse rounded-md bg-muted" />
            <div className="flex-1 space-y-2">
              <div className="h-5 w-1/3 animate-pulse rounded bg-muted" />
              <div className="h-4 w-1/2 animate-pulse rounded bg-muted" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <EmptyState
        icon={<BookOpen className="size-12" />}
        title="Unable to load journal"
        description="The journal feature is not available yet. Check back soon!"
      />
    )
  }

  if (data.content.length === 0) {
    return (
      <EmptyState
        icon={<BookOpen className="size-12" />}
        title="Your journal is empty"
        description="Start logging your play sessions to build a diary of your gaming journey!"
        actionLabel="Browse Games"
        actionTo="/games"
      />
    )
  }

  return (
    <div>
      <JournalTimeline
        entries={data.content}
        renderActions={(entry) => (
          <Button
            variant="ghost"
            size="sm"
            className="h-7 gap-1 text-xs text-destructive hover:text-destructive"
            disabled={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate(entry.id)}
          >
            <Trash2 className="size-3" />
            Delete
          </Button>
        )}
      />
      <CollectionPagination
        tab="journal"
        page={page}
        totalPages={data.metadata.totalPages}
        hasNext={data.metadata.hasNext}
        hasPrevious={data.metadata.hasPrevious}
      />
    </div>
  )
}
