import { Link } from '@tanstack/react-router'
import type { LeaderboardEntry, LeaderboardSortBy } from '@/types/leaderboard'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { cn } from '@/lib/utils'

interface LeaderboardTableProps {
  entries: Array<LeaderboardEntry>
  sortBy: LeaderboardSortBy
}

function rankClass(rank: number): string {
  if (rank === 1) return 'text-yellow-500'
  if (rank === 2) return 'text-zinc-400'
  if (rank === 3) return 'text-amber-700'
  return 'text-muted-foreground'
}

export function LeaderboardTable({ entries, sortBy }: LeaderboardTableProps) {
  if (entries.length === 0) {
    return (
      <p className="py-10 text-center text-muted-foreground">
        No players to show yet.
      </p>
    )
  }

  return (
    <ul className="divide-y rounded-lg border">
      {entries.map((entry) => {
        const initials = entry.pseudo.slice(0, 2).toUpperCase()
        return (
          <li
            key={entry.id}
            className="flex items-center gap-4 px-4 py-3 transition-colors hover:bg-accent"
          >
            <span
              className={cn(
                'w-10 shrink-0 text-center text-lg font-bold tabular-nums',
                rankClass(entry.rank),
              )}
            >
              {entry.rank}
            </span>
            <Link
              to="/profile/$username"
              params={{ username: entry.pseudo }}
              className="flex flex-1 items-center gap-3 min-w-0"
            >
              <Avatar size="lg">
                <AvatarImage
                  src={entry.picture ?? undefined}
                  alt={entry.pseudo}
                />
                <AvatarFallback>{initials}</AvatarFallback>
              </Avatar>
              <span className="truncate font-medium">{entry.pseudo}</span>
            </Link>
            <div className="flex items-center gap-6 text-sm tabular-nums">
              <span
                className={cn(
                  'flex flex-col items-end',
                  sortBy === 'level'
                    ? 'text-foreground font-semibold'
                    : 'text-muted-foreground',
                )}
              >
                <span className="text-xs uppercase tracking-wide">Level</span>
                <span>{entry.level}</span>
              </span>
              <span
                className={cn(
                  'flex flex-col items-end',
                  sortBy === 'xp'
                    ? 'text-foreground font-semibold'
                    : 'text-muted-foreground',
                )}
              >
                <span className="text-xs uppercase tracking-wide">XP</span>
                <span>{entry.xpPoint.toLocaleString()}</span>
              </span>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
