import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { ArrowRight } from 'lucide-react'
import { userLibraryQueryOptions } from '@/queries/profile'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { Separator } from '@/components/ui/separator'

interface RecentGamesSectionProps {
  username: string
  isPrivate: boolean
  isOwner: boolean
}

/**
 * Full-width row of the profile owner's 7 most recently added library games,
 * rendered with the shared {@link GameDetailCard} so it matches every other
 * collection grid. Hidden when the profile is private (and the viewer isn't the
 * owner) or when the library is empty. Links to the full games page.
 */
export function RecentGamesSection({
  username,
  isPrivate,
  isOwner,
}: RecentGamesSectionProps) {
  const enabled = !(isPrivate && !isOwner)
  const { data } = useQuery({
    ...userLibraryQueryOptions(username, 0, 7),
    enabled,
  })

  if (!enabled) return null
  if (!data || data.content.length === 0) return null

  return (
    <div className="space-y-3">
      <div>
        <div className="flex items-center justify-between py-2">
          <h2 className="text-muted-foreground font-semibold">Recent games</h2>
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
        <Separator />
      </div>
      <div className="grid grid-cols-4 gap-3 sm:grid-cols-7">
        {data.content.map((game) => (
          <GameDetailCard
            key={game.id}
            title={game.title}
            coverUrl={game.coverUrl}
            releaseDate={game.releaseDate}
            link={{ type: 'game', gameId: game.videoGameId }}
            score={game.userRating != null ? game.userRating * 2 : null}
            status={game.status}
          />
        ))}
      </div>
    </div>
  )
}
