import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { ArrowRight } from 'lucide-react'
import { userLibraryQueryOptions } from '@/queries/profile'

interface RecentGamesSectionProps {
  username: string
  isPrivate: boolean
  isOwner: boolean
}

/**
 * Compact preview of the profile owner's 8 most recently added library games.
 * Hidden when the profile is private (and the viewer isn't the owner) or when
 * the library is empty. Links to the full /profile/$username/games page.
 */
export function RecentGamesSection({
  username,
  isPrivate,
  isOwner,
}: RecentGamesSectionProps) {
  const enabled = !(isPrivate && !isOwner)
  const { data } = useQuery({
    ...userLibraryQueryOptions(username, 0, 8),
    enabled,
  })

  if (!enabled) return null
  if (!data || data.content.length === 0) return null

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Recent games</h2>
        <Link
          to="/profile/$username/games"
          params={{ username }}
          search={{ tab: 'games', page: 1, sort: 'addedAt' }}
          className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
        >
          See all
          <ArrowRight className="size-3.5" />
        </Link>
      </div>
      <div className="grid grid-cols-4 gap-3 sm:grid-cols-8">
        {data.content.map((game) => (
          <Link
            key={game.id}
            to="/games/$gameId"
            params={{ gameId: game.videoGameId }}
            className="group block"
            title={game.title}
          >
            <div className="bg-muted aspect-[3/4] overflow-hidden rounded-md">
              {game.coverUrl ? (
                <img
                  src={game.coverUrl}
                  alt={game.title}
                  className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
                />
              ) : (
                <div className="bg-secondary flex h-full w-full items-center justify-center">
                  <span className="text-muted-foreground text-xs">
                    No Cover
                  </span>
                </div>
              )}
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
