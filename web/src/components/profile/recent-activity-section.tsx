import { Gamepad2, Heart, MessageSquare, RefreshCw } from 'lucide-react'
import { Link } from '@tanstack/react-router'
import type { RecentPlay } from '@/types/profile'
import { ScoreStars } from '@/components/games/score-stars'
import { Separator } from '@/components/ui/separator'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'

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
        <h2 className="text-lg font-semibold">Recent activity</h2>
        <p className="text-muted-foreground text-sm">
          Log your first play to see it here.
        </p>
        <Separator className="mt-6" />
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-semibold">Recent activity</h2>
      <div className="grid grid-cols-5 gap-2 sm:gap-3">
        {recentPlays.map((play) => (
          <RecentPlayCard key={play.id} play={play} />
        ))}
      </div>
      <Separator className="mt-6" />
    </div>
  )
}

function RecentPlayCard({ play }: { play: RecentPlay }) {
  return (
    <Link
      to="/plays/$id"
      params={{ id: play.id }}
      title={play.title}
      className="group flex flex-col gap-1.5 rounded-md transition-opacity hover:opacity-90"
    >
      {play.coverUrl ? (
        <img
          src={play.coverUrl}
          alt={play.title}
          className="aspect-[3/4] w-full rounded-md object-cover"
        />
      ) : (
        <div className="bg-muted flex aspect-[3/4] w-full items-center justify-center rounded-md">
          <Gamepad2 className="text-muted-foreground size-8" />
        </div>
      )}

      <div className="flex items-center justify-between gap-1">
        {play.score != null ? (
          <ScoreStars score={play.score} starClassName="h-3 w-3" />
        ) : (
          <span aria-hidden className="h-3" />
        )}

        <div className="flex items-center gap-1">
          {play.hasReview && (
            <Tooltip>
              <TooltipTrigger asChild>
                <MessageSquare
                  aria-label="Has review"
                  className="text-muted-foreground size-3"
                />
              </TooltipTrigger>
              <TooltipContent>
                <p>Has review</p>
              </TooltipContent>
            </Tooltip>
          )}
          {play.isLiked && (
            <Tooltip>
              <TooltipTrigger asChild>
                <Heart
                  aria-label="Liked this game"
                  className="size-3 fill-red-500 text-red-500"
                />
              </TooltipTrigger>
              <TooltipContent>
                <p>Liked this game</p>
              </TooltipContent>
            </Tooltip>
          )}
          {play.isReplay && (
            <Tooltip>
              <TooltipTrigger asChild>
                <RefreshCw
                  aria-label="Replay"
                  className="text-muted-foreground size-3"
                />
              </TooltipTrigger>
              <TooltipContent>
                <p>Replay</p>
              </TooltipContent>
            </Tooltip>
          )}
        </div>
      </div>
    </Link>
  )
}
