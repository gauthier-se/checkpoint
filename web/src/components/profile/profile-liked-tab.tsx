import { useQuery } from '@tanstack/react-query'
import { Lock, ThumbsUp } from 'lucide-react'
import type { UserProfile } from '@/types/profile'
import { userLikedGamesQueryOptions } from '@/queries/profile'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { PaginationNav } from '@/components/shared/pagination-nav'

interface ProfileLikedTabProps {
  profile: UserProfile
  page: number
}

export function ProfileLikedTab({ profile, page }: ProfileLikedTabProps) {
  const apiPage = Math.max(0, page - 1)
  const { data, isLoading, isError } = useQuery(
    userLikedGamesQueryOptions(profile.username, apiPage),
  )

  if (profile.isPrivate && !profile.isOwner) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Lock className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">This profile is private</p>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
        {Array.from({ length: 12 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-1.5">
            <div className="bg-muted aspect-[3/4] animate-pulse rounded-md" />
            <div className="bg-muted h-3 w-3/4 animate-pulse rounded" />
          </div>
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <ThumbsUp className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">
          Unable to load liked games
        </p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <ThumbsUp className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">No liked games yet</p>
      </div>
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
      <PaginationNav
        page={page}
        totalPages={data.metadata.totalPages}
        hasNext={data.metadata.hasNext}
        hasPrevious={data.metadata.hasPrevious}
        hideWhenSinglePage
        className="pt-6 pb-4"
        linkProps={(target) => ({
          to: '.',
          search: { tab: 'liked', page: target },
        })}
      />
    </>
  )
}
