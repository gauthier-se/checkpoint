import { Link } from '@tanstack/react-router'
import {
  AlignLeft,
  Calendar,
  Clock,
  Gamepad2,
  RefreshCw,
  Tag,
} from 'lucide-react'
import type { PlayLogResponse } from '@/types/collection'
import {
  PLAY_STATUS_ICONS,
  PLAY_STATUS_ICON_COLORS,
  PLAY_STATUS_LABELS,
} from '@/lib/play-status'
import { Badge } from '@/components/ui/badge'
import { ScoreStars } from '@/components/games/score-stars'
import { cn } from '@/lib/utils'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'

function formatTimePlayed(minutes: number): string {
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

interface JournalEntryProps {
  entry: PlayLogResponse
  /** Optional action buttons shown on hover (e.g. delete) — owner only. */
  actions?: React.ReactNode
}

/**
 * A single journal (play log) entry row. The cover and title link to the
 * play detail page `/plays/$id`. Shared between the My Games journal tab
 * (with owner actions) and the read-only profile journal tab.
 */
export function JournalEntry({ entry, actions }: JournalEntryProps) {
  const StatusIcon = PLAY_STATUS_ICONS[entry.status]

  return (
    <div className="group flex items-start gap-4 py-3 border-b border-border/40 last:border-0">
      {/* Cover */}
      <Link to="/plays/$id" params={{ id: entry.id }} className="shrink-0">
        <div className="h-24 w-16 overflow-hidden rounded-md bg-muted">
          {entry.coverUrl ? (
            <img
              src={entry.coverUrl}
              alt={entry.title}
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-secondary">
              <Gamepad2 className="size-4 text-muted-foreground" />
            </div>
          )}
        </div>
      </Link>

      {/* Info */}
      <div className="flex min-w-0 flex-col gap-1.5">
        <div className="flex flex-wrap items-center gap-3">
          <span className="flex min-w-0 items-baseline gap-1.5">
            <Link
              to="/plays/$id"
              params={{ id: entry.id }}
              className="font-semibold leading-tight hover:underline line-clamp-1"
            >
              {entry.title}
            </Link>
            {entry.releaseDate && (
              <span className="shrink-0 text-xs text-muted-foreground">
                ({new Date(entry.releaseDate).getFullYear()})
              </span>
            )}
          </span>
          <div className="flex shrink-0 items-center gap-4">
            {entry.score != null ? (
              <ScoreStars score={entry.score} starClassName="size-3.5" />
            ) : null}

            <div className="flex items-center gap-1.5">
              <Tooltip>
                <TooltipTrigger asChild>
                  <StatusIcon
                    aria-label={PLAY_STATUS_LABELS[entry.status]}
                    className={cn(
                      'size-4',
                      PLAY_STATUS_ICON_COLORS[entry.status],
                    )}
                  />
                </TooltipTrigger>
                <TooltipContent>
                  <p>{PLAY_STATUS_LABELS[entry.status]}</p>
                </TooltipContent>
              </Tooltip>
              {entry.isReplay && (
                <Tooltip>
                  <TooltipTrigger asChild>
                    <RefreshCw
                      aria-label="Replay"
                      className="text-muted-foreground size-3.5"
                    />
                  </TooltipTrigger>
                  <TooltipContent>
                    <p>Replay</p>
                  </TooltipContent>
                </Tooltip>
              )}
            </div>
          </div>
        </div>

        {/* Metadata row */}
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
          {entry.platformName && (
            <span className="flex items-center gap-1">
              <Gamepad2 className="size-3" />
              {entry.platformName}
            </span>
          )}
          {entry.timePlayed != null && entry.timePlayed > 0 && (
            <span className="flex items-center gap-1">
              <Clock className="size-3" />
              {formatTimePlayed(entry.timePlayed)}
            </span>
          )}
          {entry.startDate && (
            <span className="flex items-center gap-1">
              <Calendar className="size-3" />
              {formatDate(entry.startDate)}
              {entry.endDate && ` — ${formatDate(entry.endDate)}`}
            </span>
          )}
        </div>

        {/* Tag chips */}
        {entry.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-1">
            {entry.tags.map((tag) => (
              <Badge
                key={tag.id}
                variant="outline"
                className="gap-1 text-[11px] text-muted-foreground"
              >
                <Tag className="size-2.5" />
                {tag.name}
              </Badge>
            ))}
          </div>
        )}

        {/* Review preview */}
        {entry.hasReview && entry.reviewPreview && (
          <div className="mt-1.5 flex items-start gap-1.5 text-xs text-muted-foreground italic bg-muted/30 p-2 rounded-md border border-muted">
            <AlignLeft className="size-3.5 mt-0.5 shrink-0" />
            <span className="line-clamp-2">"{entry.reviewPreview}"</span>
          </div>
        )}
      </div>

      {/* Hover actions */}
      {actions && (
        <div className="flex shrink-0 flex-col gap-1 opacity-0 transition-opacity group-hover:opacity-100">
          {actions}
        </div>
      )}
    </div>
  )
}
