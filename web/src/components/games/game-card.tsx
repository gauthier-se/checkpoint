import { Link } from '@tanstack/react-router'
import { GameCardHoverActions } from '@/components/games/game-card-hover-actions'

interface GameCardItem {
  id: string
  title: string
  coverUrl: string
  releaseDate: string
}

interface GameCardProps {
  game: GameCardItem
  reason?: string
}

export function GameCard({ game, reason }: GameCardProps) {
  return (
    <Link
      to="/games/$gameId"
      params={{ gameId: game.id }}
      className="group relative flex flex-col gap-1"
    >
      <div className="relative">
        <img
          className="rounded-sm w-full"
          src={game.coverUrl}
          alt={game.title}
        />
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
      </div>
      {reason && (
        <span className="text-xs text-muted-foreground line-clamp-1">
          {reason}
        </span>
      )}
    </Link>
  )
}
