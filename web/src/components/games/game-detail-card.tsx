import { Link } from '@tanstack/react-router'
import { AlignLeft, Gamepad2, Heart, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'
import type { PlayStatus } from '@/types/interaction'
import { ScoreStars } from '@/components/games/score-stars'
import { GameCardHoverActions } from '@/components/games/game-card-hover-actions'
import {
  PLAY_STATUS_ICONS,
  PLAY_STATUS_ICON_COLORS,
  PLAY_STATUS_LABELS,
} from '@/lib/play-status'
import { cn } from '@/lib/utils'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'

/** Where the card links: a game detail page or a single play log. */
type GameDetailLink =
  | { type: 'game'; gameId: string }
  | { type: 'play'; playId: string }

interface GameDetailCardProps {
  title: string
  coverUrl: string | null
  link: GameDetailLink
  /** Release date — its year is shown in the hover overlay, like the catalog. */
  releaseDate?: string | null
  /** Half-star rating on the 1–10 scale (e.g. 7 = 3.5 stars). */
  score?: number | null
  /** Play status — rendered as a coloured icon next to the rating. */
  status?: PlayStatus
  /** Extra badge rendered below the cover (e.g. wishlist priority). */
  statusBadge?: ReactNode
  hasReview?: boolean
  isLiked?: boolean
  isReplay?: boolean
  /**
   * Owner-only controls (e.g. a kebab menu) rendered on hover in the top-right
   * corner. When omitted, game-linked cards fall back to the shared
   * {@link GameCardHoverActions} quick buttons, matching the catalog.
   */
  actions?: ReactNode
}

/**
 * Unified "detailed" game card: a cover with a hover overlay (title + year) and
 * a metadata row underneath (rating stars + review / liked / replay icons). On
 * hover it shows either the provided owner controls or the shared quick actions.
 * Shared by the recent-activity preview and every profile collection grid so
 * each game surface looks the same.
 */
export function GameDetailCard({
  title,
  coverUrl,
  link,
  releaseDate,
  score,
  status,
  statusBadge,
  hasReview,
  isLiked,
  isReplay,
  actions,
}: GameDetailCardProps) {
  const [isHovered, setIsHovered] = useState(false)
  const StatusIcon = status ? PLAY_STATUS_ICONS[status] : null
  const cover = coverUrl ? (
    <img
      src={coverUrl}
      alt={title}
      className="aspect-[3/4] w-full rounded-md object-cover"
    />
  ) : (
    <div className="bg-muted flex aspect-[3/4] w-full items-center justify-center rounded-md">
      <Gamepad2 className="text-muted-foreground size-8" />
    </div>
  )

  return (
    <div
      className="group relative flex flex-col gap-1.5"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <Link
        {...(link.type === 'game'
          ? { to: '/games/$gameId', params: { gameId: link.gameId } }
          : { to: '/plays/$id', params: { id: link.playId } })}
        title={title}
        className="relative block overflow-hidden rounded-md"
      >
        {cover}
        <div className="pointer-events-none absolute inset-0 rounded-md bg-black/70 opacity-0 transition-opacity duration-200 group-hover:opacity-100" />
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-1 px-2 text-center text-white opacity-0 transition-opacity duration-200 group-hover:opacity-100">
          <span className="text-sm font-semibold line-clamp-3">{title}</span>
          {releaseDate && (
            <span className="text-xs text-white/80">
              {new Date(releaseDate).getFullYear()}
            </span>
          )}
        </div>
      </Link>

      {actions ? (
        <div className="absolute top-2 right-2 opacity-0 transition-opacity duration-200 group-hover:opacity-100 focus-within:opacity-100">
          {actions}
        </div>
      ) : link.type === 'game' ? (
        <GameCardHoverActions gameId={link.gameId} isHovered={isHovered} />
      ) : null}

      {(score != null || status || hasReview || isLiked || isReplay) && (
        <div className="flex min-h-4 items-center justify-between gap-1">
          {score != null ? (
            <ScoreStars score={score} starClassName="h-3 w-3" />
          ) : (
            <span aria-hidden className="h-3" />
          )}

          <div className="flex items-center gap-1">
            {StatusIcon && status && (
              <Tooltip>
                <TooltipTrigger asChild>
                  <StatusIcon
                    aria-label={PLAY_STATUS_LABELS[status]}
                    className={cn('size-3.5', PLAY_STATUS_ICON_COLORS[status])}
                  />
                </TooltipTrigger>
                <TooltipContent>
                  <p>{PLAY_STATUS_LABELS[status]}</p>
                </TooltipContent>
              </Tooltip>
            )}
            {hasReview && (
              <Tooltip>
                <TooltipTrigger asChild>
                  <AlignLeft
                    aria-label="Has review"
                    className="text-muted-foreground size-3"
                  />
                </TooltipTrigger>
                <TooltipContent>
                  <p>Has review</p>
                </TooltipContent>
              </Tooltip>
            )}
            {isLiked && (
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
            {isReplay && (
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
      )}

      {statusBadge}
    </div>
  )
}
