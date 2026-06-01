import { useQuery } from '@tanstack/react-query'
import { Heart, Lock } from 'lucide-react'
import type { UserProfile } from '@/types/profile'
import { PriorityBadge } from '@/components/collection/priority-badge'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { userWishlistQueryOptions } from '@/queries/profile'

interface ProfileWishlistTabProps {
  profile: UserProfile
  page: number
}

export function ProfileWishlistTab({ profile, page }: ProfileWishlistTabProps) {
  const apiPage = Math.max(0, page - 1)
  const { data, isLoading, isError } = useQuery(
    userWishlistQueryOptions(profile.username, apiPage),
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
          <div
            key={i}
            className="bg-muted aspect-[3/4] animate-pulse rounded-md"
          />
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Heart className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">Unable to load wishlist</p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Heart className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">No games in wishlist</p>
      </div>
    )
  }

  return (
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
        />
      ))}
    </div>
  )
}
