import type { RecentPlay } from '@/types/profile'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { Separator } from '@/components/ui/separator'

interface RecentActivitySectionProps {
  recentPlays: Array<RecentPlay>
  isOwner: boolean
}

export function RecentActivitySection({
  recentPlays,
  isOwner,
}: RecentActivitySectionProps) {
  if (recentPlays.length === 0) {
    if (!isOwner) {
      return null
    }
    return (
      <div className="space-y-3">
        <div>
          <div className="flex items-center justify-between py-2">
            <h2 className="text-muted-foreground font-semibold">
              Recent activity
            </h2>
          </div>
          <Separator />
        </div>
        <p className="text-muted-foreground text-sm">
          Log your first play to see it here.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <div>
        <div className="flex items-center justify-between py-2">
          <h2 className="text-muted-foreground font-semibold">
            Recent activity
          </h2>
        </div>
        <Separator />
      </div>
      <div className="grid grid-cols-5 gap-2 sm:gap-3">
        {recentPlays.map((play) => (
          <GameDetailCard
            key={play.id}
            title={play.title}
            coverUrl={play.coverUrl}
            link={{ type: 'play', playId: play.id }}
            score={play.score}
            hasReview={play.hasReview}
            isLiked={play.isLiked}
            isReplay={play.isReplay}
          />
        ))}
      </div>
    </div>
  )
}
