import { queryOptions, useQuery } from '@tanstack/react-query'
import { ThumbsUp } from 'lucide-react'
import type { LikedGameListResponse } from '@/types/collection'
import { CollectionGameCard } from '@/components/collection/collection-game-card'
import { CollectionPagination } from '@/components/collection/collection-pagination'
import { EmptyState } from '@/components/collection/empty-state'
import { apiFetch } from '@/services/api'

const PAGE_SIZE = 20

export function likedGamesQuery(page: number) {
  return queryOptions({
    queryKey: ['likes', 'me', page],
    queryFn: async (): Promise<LikedGameListResponse> => {
      const apiPage = page - 1
      const res = await apiFetch(
        `/api/me/likes?page=${apiPage}&size=${PAGE_SIZE}&sort=createdAt,desc`,
      )
      return res.json()
    },
  })
}

interface LikedTabProps {
  page: number
}

export function LikedTab({ page }: LikedTabProps) {
  const { data, isLoading, isError } = useQuery(likedGamesQuery(page))

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
        icon={<ThumbsUp className="size-12" />}
        title="Unable to load liked games"
        description="Something went wrong loading your liked games. Please try again later."
      />
    )
  }

  if (data.content.length === 0) {
    return (
      <EmptyState
        icon={<ThumbsUp className="size-12" />}
        title="No liked games yet"
        description="Like games to keep track of your favourites!"
        actionLabel="Browse Games"
        actionTo="/games"
      />
    )
  }

  return (
    <>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {data.content.map((game) => (
          <CollectionGameCard
            key={game.id}
            videoGameId={game.videoGameId}
            title={game.title}
            coverUrl={game.coverUrl}
            releaseDate={game.releaseDate}
          />
        ))}
      </div>
      <CollectionPagination
        tab="liked"
        page={page}
        totalPages={data.metadata.totalPages}
        hasNext={data.metadata.hasNext}
        hasPrevious={data.metadata.hasPrevious}
      />
    </>
  )
}
