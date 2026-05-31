import { useQuery } from '@tanstack/react-query'
import type { PlayStatus } from '@/types/interaction'
import { FriendAvatarGrid } from '@/components/games/friend-avatar-grid'
import { FriendAvatarTile } from '@/components/games/friend-avatar-tile'
import { Badge } from '@/components/ui/badge'
import { DiscoverySection } from '@/components/games/discovery-section'
import { friendsActivityQueryOptions } from '@/queries/game-social'

interface FriendActivitySectionProps {
  gameId: string
}

const PLAY_STATUS_LABELS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'Playing',
  PLAYED: 'Played',
  COMPLETED: 'Completed',
  RETIRED: 'Retired',
  SHELVED: 'Shelved',
  ABANDONED: 'Abandoned',
}

const PLAY_STATUS_COLORS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  PLAYED: 'bg-violet-500/15 text-violet-400 border-violet-500/20',
  COMPLETED: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  RETIRED: 'bg-slate-500/15 text-slate-400 border-slate-500/20',
  SHELVED: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
  ABANDONED: 'bg-red-500/15 text-red-400 border-red-500/20',
}

const STATUS_DISPLAY_ORDER: ReadonlyArray<PlayStatus> = [
  'COMPLETED',
  'ARE_PLAYING',
  'PLAYED',
  'SHELVED',
  'ABANDONED',
  'RETIRED',
]

export function FriendActivitySection({ gameId }: FriendActivitySectionProps) {
  const { data } = useQuery(friendsActivityQueryOptions(gameId))

  if (!data || data.totalCount === 0) {
    return null
  }

  return (
    <DiscoverySection
      title="Friend activity"
      action={
        <div className="flex flex-wrap items-center justify-end text-sm text-muted-foreground font-medium">
          {STATUS_DISPLAY_ORDER.map((status) => {
            const count = data.countsByPlayStatus[status]
            if (!count) return null
            return `${count} ${PLAY_STATUS_LABELS[status]}`
          })
            .filter(Boolean)
            .join(' • ')}
        </div>
      }
    >
      <div className="pt-4">
        <FriendAvatarGrid
          title="Friend activity"
          totalCount={data.friends.length}
          renderItem={(i) => {
            const f = data.friends[i]
            return (
              <FriendAvatarTile
                key={f.userId}
                pseudo={f.pseudo}
                picture={f.picture}
                rating={f.rating}
                hasReview={f.hasReview}
                href={f.latestPlayId ? `/plays/${f.latestPlayId}` : null}
                tooltip={
                  f.primaryPlayStatus
                    ? `${f.pseudo} — ${PLAY_STATUS_LABELS[f.primaryPlayStatus]}`
                    : f.pseudo
                }
              />
            )
          }}
        />
      </div>
    </DiscoverySection>
  )
}
