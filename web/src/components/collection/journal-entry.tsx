import { Link } from '@tanstack/react-router'
import {
  Calendar,
  Clock,
  Gamepad2,
  MessageSquare,
  RefreshCw,
  Star,
  Tag,
} from 'lucide-react'
import type { PlayLogResponse, PlayStatus } from '@/types/collection'
import { Badge } from '@/components/ui/badge'

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
  return (
    <div className="group flex items-start gap-4 rounded-lg border bg-card p-4 shadow-sm transition-shadow hover:shadow-md">
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
      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        <div className="flex items-start justify-between gap-2">
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
          <div className="flex shrink-0 items-center gap-1.5">
            <Badge
              className={`${PLAY_STATUS_COLORS[entry.status]} text-[11px]`}
            >
              {PLAY_STATUS_LABELS[entry.status]}
            </Badge>
            {entry.isReplay && (
              <Badge variant="outline" className="gap-1 text-[11px]">
                <RefreshCw className="size-2.5" />
                Replay
              </Badge>
            )}
            {entry.score != null && (
              <Badge
                variant="secondary"
                className="gap-1 text-[11px] bg-yellow-500/10 text-yellow-600 hover:bg-yellow-500/20"
              >
                <Star className="size-2.5 fill-current" />
                {(entry.score / 2).toFixed(1)}
              </Badge>
            )}
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
            <MessageSquare className="size-3.5 mt-0.5 shrink-0" />
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
