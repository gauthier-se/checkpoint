import { Link } from '@tanstack/react-router'
import type { Game } from '@/types/game'
import { GameGrid } from '@/components/games/game-grid'
import { SectionHeader } from '@/components/home/section-header'

export function TrendingSection({ games }: { games: Array<Game> }) {
  if (games.length === 0) return null

  return (
    <section className="my-12">
      <SectionHeader
        title="Popular games this week"
        action={
          <Link
            to="/games"
            search={{ page: 1 }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            See all
          </Link>
        }
      />
      <GameGrid games={games} columns={7} />
    </section>
  )
}
