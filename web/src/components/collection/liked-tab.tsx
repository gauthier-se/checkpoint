import { queryOptions, useQuery } from '@tanstack/react-query'
import { ThumbsUp } from 'lucide-react'
import type { LikedGameListResponse } from '@/types/collection'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { CollectionPagination } from '@/components/collection/collection-pagination'
import { EmptyState } from '@/components/collection/empty-state'
import { apiFetch } from '@/services/api'

const PAGE_SIZE = 21

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
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
        {Array.from({ length: 21 }).map((_, i) => (
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
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
        {data.content.map((game) => (
          <GameDetailCard
            key={game.id}
            title={game.title}
            coverUrl={game.coverUrl}
            releaseDate={game.releaseDate}
            link={{ type: 'game', gameId: game.videoGameId }}
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
