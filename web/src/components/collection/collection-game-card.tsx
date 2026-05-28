import { Link } from '@tanstack/react-router'
import { Star } from 'lucide-react'
import { GameCardHoverActions } from '@/components/games/game-card-hover-actions'
import { Card, CardContent, CardFooter } from '@/components/ui/card'

interface CollectionGameCardProps {
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate?: string | null
  /** Optional half-star rating (0.5–5.0). When set, renders next to the title. */
  userRating?: number | null
  children?: React.ReactNode
}

export function CollectionGameCard({
  videoGameId,
  title,
  coverUrl,
  releaseDate,
  userRating,
  children,
}: CollectionGameCardProps) {
  return (
    <Card className="group relative gap-2 p-3 transition-shadow hover:shadow-md">
      <Link
        to="/games/$gameId"
        params={{ gameId: videoGameId }}
        className="block"
      >
        <CardContent className="relative aspect-[3/4] overflow-hidden rounded-md bg-muted p-0">
          {coverUrl ? (
            <img
              src={coverUrl}
              alt={title}
              className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-secondary">
              <span className="text-xs text-muted-foreground">No Cover</span>
            </div>
          )}
          <div className="pointer-events-none absolute inset-0 bg-black/70 opacity-0 transition-opacity duration-200 group-hover:opacity-100" />
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-1 px-2 text-center text-white opacity-0 transition-opacity duration-200 group-hover:opacity-100">
            <span className="text-sm font-semibold">{title}</span>
            {releaseDate && (
              <span className="text-xs text-white/80">
                {new Date(releaseDate).getFullYear()}
              </span>
            )}
          </div>
          <GameCardHoverActions gameId={videoGameId} />
        </CardContent>
      </Link>
      <CardFooter className="flex-1 flex-col items-stretch gap-2 p-0">
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-sm font-semibold leading-tight line-clamp-2">
            {title}
          </h3>
          {userRating != null && (
            <span
              className="inline-flex shrink-0 items-center gap-0.5 text-xs font-medium text-amber-400"
              aria-label={`Your rating: ${userRating} out of 5`}
            >
              {userRating}
              <Star className="size-3 fill-amber-400" />
            </span>
          )}
        </div>
        {children}
      </CardFooter>
    </Card>
  )
}
