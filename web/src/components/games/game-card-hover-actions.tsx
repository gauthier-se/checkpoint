import { Bookmark, Heart } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { useAuth } from '@/hooks/use-auth'
import { useWishlistBacklogActions } from '@/hooks/use-wishlist-backlog-actions'

interface GameCardHoverActionsProps {
  gameId: string
  className?: string
}

export function GameCardHoverActions({
  gameId,
  className,
}: GameCardHoverActionsProps) {
  const { user } = useAuth()
  const {
    inWishlist,
    inBacklog,
    toggleWishlist,
    toggleBacklog,
    wishlistPending,
    backlogPending,
  } = useWishlistBacklogActions(gameId)

  if (!user) return null

  const stop = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
  }

  return (
    <div
      className={cn(
        'pointer-events-auto absolute right-2 top-2 flex gap-1 opacity-0 transition-opacity duration-200 group-hover:opacity-100 focus-within:opacity-100',
        className,
      )}
      onClick={stop}
    >
      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            type="button"
            variant="secondary"
            size="icon"
            className="size-7 bg-background/80 backdrop-blur-sm hover:bg-background"
            disabled={wishlistPending}
            aria-label={inWishlist ? 'Remove from wishlist' : 'Add to wishlist'}
            onClick={(e) => {
              stop(e)
              toggleWishlist()
            }}
          >
            <Heart className={cn('size-3.5', inWishlist && 'fill-current')} />
          </Button>
        </TooltipTrigger>
        <TooltipContent>
          {inWishlist ? 'Remove from wishlist' : 'Add to wishlist'}
        </TooltipContent>
      </Tooltip>

      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            type="button"
            variant="secondary"
            size="icon"
            className="size-7 bg-background/80 backdrop-blur-sm hover:bg-background"
            disabled={backlogPending}
            aria-label={inBacklog ? 'Remove from backlog' : 'Add to backlog'}
            onClick={(e) => {
              stop(e)
              toggleBacklog()
            }}
          >
            <Bookmark className={cn('size-3.5', inBacklog && 'fill-current')} />
          </Button>
        </TooltipTrigger>
        <TooltipContent>
          {inBacklog ? 'Remove from backlog' : 'Add to backlog'}
        </TooltipContent>
      </Tooltip>
    </div>
  )
}
