import { useQuery } from '@tanstack/react-query'
import { Library, Lock } from 'lucide-react'
import type { PlayStatus } from '@/types/interaction'
import type { UserProfile } from '@/types/profile'
import type { ProfileGamesTabKey } from '@/components/profile/profile-tab-bar'
import { userLibraryQueryOptions } from '@/queries/profile'
import { STATUS_TABS } from '@/components/profile/profile-status-bar'
import { PLAY_STATUS_LABELS } from '@/lib/play-status'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { PaginationNav } from '@/components/shared/pagination-nav'

interface ProfileLibraryTabProps {
  profile: UserProfile
  page: number
  status?: PlayStatus
  tabKey: ProfileGamesTabKey
}

/**
 * Read-only library grid for the public {@code /profile/$username/games} page,
 * scoped to one status (or all statuses when {@code status} is undefined).
 * Respects {@code isPrivate} for non-owners.
 */
export function ProfileLibraryTab({
  profile,
  page,
  status,
  tabKey,
}: ProfileLibraryTabProps) {
  const apiPage = Math.max(0, page - 1)
  const { data, isLoading, isError } = useQuery({
    ...userLibraryQueryOptions(profile.username, apiPage, 20, status),
    enabled: !(profile.isPrivate && !profile.isOwner),
  })

  const TabIcon = STATUS_TABS.find((t) => t.value === tabKey)?.icon || Library

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
        <TabIcon className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">Unable to load library</p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <TabIcon className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">
          {status
            ? `No ${PLAY_STATUS_LABELS[status].toLowerCase()} games`
            : 'No games in library'}
        </p>
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
            score={game.userRating != null ? game.userRating * 2 : null}
            status={game.status}
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
          search: { tab: tabKey, page: target },
        })}
      />
    </>
  )
}
