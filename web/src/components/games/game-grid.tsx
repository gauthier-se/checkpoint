import { GameCard } from './game-card'
import type { Game } from '@/types/game'

interface GameGridProps {
  games: Array<Game>
  columns?: number
  /**
   * For the fixed 7-game showcase rows: below `md` the grid is only 2 or 3
   * columns wide, so the 7th game ends up alone on its own row. Hide it there
   * and show the whole set again from `md` up. Never enable this on a grid
   * whose games are actual results (search, catalog pages) — it would hide one.
   */
  hideLastOnMobile?: boolean
}

export function GameGrid({
  games,
  columns = 8,
  hideLastOnMobile = false,
}: GameGridProps) {
  let gridClass =
    'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-8 gap-x-2 gap-y-1'
  if (columns === 7) {
    gridClass =
      'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7 gap-3'
  } else if (columns === 9) {
    gridClass =
      'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 lg:grid-cols-9 gap-x-2 gap-y-1'
  }

  const trimClass = hideLastOnMobile ? ' max-md:[&>*:nth-child(7)]:hidden' : ''

  return (
    <div className={`${gridClass}${trimClass} py-2`}>
      {games.map((game) => (
        <div key={game.id}>
          <GameCard game={game} />
        </div>
      ))}
    </div>
  )
}
