import { Link } from '@tanstack/react-router'
import { Gamepad2, Star } from 'lucide-react'
import type { CommonGameEntry } from '@/queries/profile'
import type { GameStatus } from '@/types/library'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

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

/** Rating difference (5-star scale) at or above which a disagreement is highlighted. */
const DISAGREEMENT_THRESHOLD = 2

interface CommonGameRowProps {
  entry: CommonGameEntry
  /** The compared user's username, used to label the "their" side. */
  targetUsername: string
}

export function CommonGameRow({ entry, targetUsername }: CommonGameRowProps) {
  const isDisagreement =
    entry.ratingDiff !== null && entry.ratingDiff >= DISAGREEMENT_THRESHOLD

  return (
    <div className="flex items-center gap-4 rounded-lg border p-3">
      <Link
        to="/games/$gameId"
        params={{ gameId: entry.videoGameId }}
        className="shrink-0"
      >
        {entry.coverUrl ? (
          <img
            src={entry.coverUrl}
            alt={entry.title}
            className="aspect-[3/4] w-12 rounded-md object-cover"
          />
        ) : (
          <div className="bg-muted flex aspect-[3/4] w-12 items-center justify-center rounded-md">
            <Gamepad2 className="text-muted-foreground size-5" />
          </div>
        )}
      </Link>

      <div className="min-w-0 flex-1">
        <Link
          to="/games/$gameId"
          params={{ gameId: entry.videoGameId }}
          className="line-clamp-1 font-medium hover:underline"
        >
          {entry.title}
        </Link>
        <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
          <span className="text-muted-foreground text-xs">You</span>
          <Badge
            className={cn(STATUS_COLORS[entry.viewerStatus], 'text-[11px]')}
          >
            {STATUS_LABELS[entry.viewerStatus]}
          </Badge>
          <span className="text-muted-foreground text-xs">
            · @{targetUsername}
          </span>
          <Badge
            className={cn(STATUS_COLORS[entry.targetStatus], 'text-[11px]')}
          >
            {STATUS_LABELS[entry.targetStatus]}
          </Badge>
        </div>
      </div>

      <div className="flex shrink-0 items-center gap-4 text-sm">
        <RatingValue label="You" rating={entry.viewerRating} />
        <RatingValue label={`@${targetUsername}`} rating={entry.targetRating} />
        <div className="w-12 text-center">
          <p className="text-muted-foreground text-[11px]">Δ</p>
          {entry.ratingDiff === null ? (
            <p className="text-muted-foreground">—</p>
          ) : (
            <p
              className={cn('font-semibold', isDisagreement && 'text-red-500')}
            >
              {entry.ratingDiff.toFixed(1)}
            </p>
          )}
        </div>
      </div>
    </div>
  )
}

function RatingValue({
  label,
  rating,
}: {
  label: string
  rating: number | null
}) {
  return (
    <div className="w-14 text-center">
      <p className="text-muted-foreground text-[11px]">{label}</p>
      {rating === null ? (
        <p className="text-muted-foreground">—</p>
      ) : (
        <p className="inline-flex items-center gap-1 font-medium">
          <Star className="size-3.5 fill-yellow-400 text-yellow-500" />
          {rating.toFixed(1)}
        </p>
      )}
    </div>
  )
}
