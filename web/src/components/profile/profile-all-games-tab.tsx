import { useQuery } from '@tanstack/react-query'
import {
  Bookmark,
  CheckCircle2,
  Flag,
  Gamepad2,
  Gift,
  Heart,
  Library,
  Lock,
  Pause,
  PlayCircle,
  XCircle,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { UserProfile } from '@/types/profile'
import type { CollectionType, UnifiedGameItem } from '@/types/collection'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { CollectionPagination } from '@/components/collection/collection-pagination'
import { userAllGamesQueryOptions } from '@/queries/profile'
import { cn } from '@/lib/utils'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'

interface ProfileAllGamesTabProps {
  profile: UserProfile
  page: number
}

interface CollectionTypeMeta {
  icon: LucideIcon
  label: string
  className: string
}

const COLLECTION_TYPE_META: Record<CollectionType, CollectionTypeMeta> = {
  LIBRARY: { icon: Gamepad2, label: 'Library', className: 'text-blue-500' },
  WISHLIST: { icon: Gift, label: 'Wishlist', className: 'text-purple-500' },
  BACKLOG: { icon: Library, label: 'Backlog', className: 'text-orange-500' },
  LIKED: { icon: Heart, label: 'Liked', className: 'text-red-500' },
}

const LIBRARY_STATUS_ICONS: Record<string, LucideIcon | undefined> = {
  ARE_PLAYING: PlayCircle,
  PLAYED: Flag,
  COMPLETED: CheckCircle2,
  RETIRED: Pause,
  SHELVED: Bookmark,
  ABANDONED: XCircle,
}

const LIBRARY_STATUS_LABELS: Record<string, string> = {
  ARE_PLAYING: 'Playing',
  PLAYED: 'Played',
  COMPLETED: 'Completed',
  RETIRED: 'Retired',
  SHELVED: 'Shelved',
  ABANDONED: 'Abandoned',
}

function CollectionIcon({
  type,
  libraryStatus,
}: {
  type: CollectionType
  libraryStatus: string | null
}) {
  if (type === 'LIBRARY' && libraryStatus) {
    const StatusIcon = LIBRARY_STATUS_ICONS[libraryStatus]
    if (!StatusIcon) return null
    const label = LIBRARY_STATUS_LABELS[libraryStatus] ?? libraryStatus
    return (
      <Tooltip>
        <TooltipTrigger asChild>
          <StatusIcon
            className={cn(
              'size-3 shrink-0',
              COLLECTION_TYPE_META.LIBRARY.className,
            )}
          />
        </TooltipTrigger>
        <TooltipContent>
          <p>{label}</p>
        </TooltipContent>
      </Tooltip>
    )
  }

  const meta = COLLECTION_TYPE_META[type]
  const Icon = meta.icon
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Icon className={cn('size-3 shrink-0', meta.className)} />
      </TooltipTrigger>
      <TooltipContent>
        <p>{meta.label}</p>
      </TooltipContent>
    </Tooltip>
  )
}

function CollectionIcons({ item }: { item: UnifiedGameItem }) {
  return (
    <>
      {item.collectionTypes.map((type) => (
        <CollectionIcon
          key={type}
          type={type}
          libraryStatus={item.libraryStatus}
        />
      ))}
    </>
  )
}

function buildScoreFromRating(userRating: number | null): number | null {
  if (userRating === null) return null
  return Math.round(userRating * 2)
}

export function ProfileAllGamesTab({ profile, page }: ProfileAllGamesTabProps) {
  const apiPage = Math.max(0, page - 1)
  const { data, isLoading, isError } = useQuery(
    userAllGamesQueryOptions(profile.username, apiPage),
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
        {Array.from({ length: 14 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-1.5">
            <div className="bg-muted aspect-[3/4] animate-pulse rounded-md" />
            <div className="bg-muted h-3 w-1/2 animate-pulse rounded" />
          </div>
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Gamepad2 className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">Unable to load games</p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Gamepad2 className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">No games yet</p>
      </div>
    )
  }

  return (
    <div>
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
        {data.content.map((game) => (
          <GameDetailCard
            key={game.videoGameId}
            title={game.title}
            coverUrl={game.coverUrl}
            releaseDate={game.releaseDate}
            link={{ type: 'game', gameId: game.videoGameId }}
            score={buildScoreFromRating(game.userRating)}
            collectionIcons={<CollectionIcons item={game} />}
          />
        ))}
      </div>
      <CollectionPagination
        tab="games"
        page={page}
        totalPages={data.metadata.totalPages}
        hasNext={data.metadata.hasNext}
        hasPrevious={data.metadata.hasPrevious}
      />
    </div>
  )
}
