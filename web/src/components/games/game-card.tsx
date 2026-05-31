import { useState } from 'react'
import { Link } from '@tanstack/react-router'
import { Gamepad2 } from 'lucide-react'
import { GameCardHoverActions } from '@/components/games/game-card-hover-actions'

interface GameCardItem {
  id: string
  title: string
  coverUrl: string | null
  releaseDate?: string | null
}

interface GameCardProps {
  game: GameCardItem
  reason?: string
}

export function GameCard({ game, reason }: GameCardProps) {
  const [isHovered, setIsHovered] = useState(false)

  return (
    <div
      className="group relative flex flex-col gap-1"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <Link
        to="/games/$gameId"
        params={{ gameId: game.id }}
        className="absolute inset-0 z-0"
      >
        <span className="sr-only">{game.title}</span>
      </Link>
      <div className="relative pointer-events-none">
        {game.coverUrl ? (
          <img
            className="rounded-sm w-full"
            src={game.coverUrl}
            alt={game.title}
          />
        ) : (
          <div className="bg-muted flex aspect-[3/4] w-full items-center justify-center rounded-sm">
            <Gamepad2 className="text-muted-foreground size-8" />
          </div>
        )}
        <div className="absolute inset-0 rounded-sm bg-black/70 opacity-0 transition-opacity duration-200 group-hover:opacity-100" />
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 px-2 text-center text-white opacity-0 transition-opacity duration-200 group-hover:opacity-100">
          <span className="text-sm font-semibold">{game.title}</span>
          {game.releaseDate && (
            <span className="text-xs text-white/80">
              {new Date(game.releaseDate).getFullYear()}
            </span>
          )}
        </div>
        <GameCardHoverActions
          gameId={game.id}
          className="z-10 pointer-events-auto"
          isHovered={isHovered}
        />
      </div>
      {reason && (
        <span
          className="text-xs text-muted-foreground line-clamp-2 pointer-events-none relative z-0"
          title={reason}
        >
          {reason}
        </span>
      )}
    </div>
  )
}
