import { useQuery } from '@tanstack/react-query'
import { Library, Lock } from 'lucide-react'
import type { GameStatus } from '@/types/library'
import type { UserProfile } from '@/types/profile'
import { userLibraryQueryOptions } from '@/queries/profile'
import { CollectionGameCard } from '@/components/collection/collection-game-card'
import { PaginationNav } from '@/components/shared/pagination-nav'
import { Badge } from '@/components/ui/badge'

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

interface ProfileLibraryTabProps {
  profile: UserProfile
  page: number
}

export function ProfileLibraryTab({ profile, page }: ProfileLibraryTabProps) {
  const apiPage = Math.max(0, page - 1)
  const { data, isLoading, isError } = useQuery(
    userLibraryQueryOptions(profile.username, apiPage),
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
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {Array.from({ length: 10 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-2 rounded-lg border p-3">
            <div className="bg-muted aspect-[3/4] animate-pulse rounded-md" />
            <div className="bg-muted h-4 w-3/4 animate-pulse rounded" />
          </div>
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Library className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">Unable to load library</p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Library className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">No games in library</p>
      </div>
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
          >
            <Badge className={`${STATUS_COLORS[game.status]} mt-1 text-[11px]`}>
              {STATUS_LABELS[game.status]}
            </Badge>
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
          search: { tab: 'library', page: target },
        })}
      />
    </>
  )
}
