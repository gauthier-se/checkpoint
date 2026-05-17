import { Link } from '@tanstack/react-router'
import type { Game } from '@/types/game'
import { GameCardHoverActions } from '@/components/games/game-card-hover-actions'

interface GameCardProps {
  game: Game
}

export function GameCard({ game }: GameCardProps) {
  return (
    <Link
      to="/games/$gameId"
      params={{ gameId: game.id }}
      className="group relative flex flex-col gap-2"
    >
      <img className="rounded-sm w-full" src={game.coverUrl} alt={game.title} />
      <div className="pointer-events-none absolute inset-0 rounded-sm bg-black/70 opacity-0 transition-opacity duration-200 group-hover:opacity-100" />
      <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-1 px-2 text-center opacity-0 transition-opacity duration-200 group-hover:opacity-100">
        <span className="text-sm font-semibold">{game.title}</span>
        {game.releaseDate && (
          <span className="text-xs text-white/80">
            {new Date(game.releaseDate).getFullYear()}
          </span>
        )}
      </div>
      <GameCardHoverActions gameId={game.id} />
    </Link>
  )
}
