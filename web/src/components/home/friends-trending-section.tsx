import { Link } from '@tanstack/react-router'
import type { Game } from '@/types/game'
import { GameGrid } from '@/components/games/game-grid'
import { SectionHeader } from '@/components/home/section-header'

export function FriendsTrendingSection({
  games,
  isLoading,
}: {
  games: Array<Game> | undefined
  isLoading: boolean
}) {
  if (isLoading || !games || games.length === 0) return null

  return (
    <section className="my-12">
      <SectionHeader
        title="Popular with friends"
        action={
          <Link
            to="/games/popular-with-friends"
            search={{ page: 1 }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            More
          </Link>
        }
      />
      <GameGrid games={games} columns={7} />
    </section>
  )
}
