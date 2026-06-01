import { GameCard } from './game-card'
import type { Game } from '@/types/game'

interface GameGridProps {
  games: Array<Game>
  columns?: number
}

export function GameGrid({ games, columns = 8 }: GameGridProps) {
  let gridClass =
    'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-8 gap-x-2 gap-y-1'
  if (columns === 7) {
    gridClass =
      'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7 gap-x-3 gap-y-1'
  } else if (columns === 9) {
    gridClass =
      'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 lg:grid-cols-9 gap-x-2 gap-y-1'
  }

  return (
    <div className={`${gridClass} py-2`}>
      {games.map((game) => (
        <div key={game.id}>
          <GameCard game={game} />
        </div>
      ))}
    </div>
  )
}
