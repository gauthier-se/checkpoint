import { Gamepad2, Gift, Heart, Library, Play } from 'lucide-react'
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
    liked,
    libraryStatus,
    toggleWishlist,
    toggleBacklog,
    toggleLike,
    toggleLibraryStatus,
    wishlistPending,
    backlogPending,
    likePending,
    libraryPending,
  } = useWishlistBacklogActions(gameId)

  if (!user) return null

  const isPlaying = libraryStatus === 'PLAYING'
  const isCompleted = libraryStatus === 'COMPLETED'

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
            disabled={likePending}
            aria-label={liked ? 'Unlike' : 'Like'}
            aria-pressed={liked}
            onClick={(e) => {
              stop(e)
              toggleLike()
            }}
          >
            <Heart className={cn('size-3.5', liked && 'fill-current')} />
          </Button>
        </TooltipTrigger>
        <TooltipContent>{liked ? 'Unlike' : 'Like'}</TooltipContent>
      </Tooltip>

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
            <Gift className={cn('size-3.5', inWishlist && 'fill-current')} />
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
            <Library className={cn('size-3.5', inBacklog && 'fill-current')} />
          </Button>
        </TooltipTrigger>
        <TooltipContent>
          {inBacklog ? 'Remove from backlog' : 'Add to backlog'}
        </TooltipContent>
      </Tooltip>

      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            type="button"
            variant="secondary"
            size="icon"
            className="size-7 bg-background/80 backdrop-blur-sm hover:bg-background"
            disabled={libraryPending}
            aria-label={isPlaying ? 'Clear playing status' : 'Mark as playing'}
            aria-pressed={isPlaying}
            onClick={(e) => {
              stop(e)
              toggleLibraryStatus('PLAYING')
            }}
          >
            <Play className={cn('size-3.5', isPlaying && 'fill-current')} />
          </Button>
        </TooltipTrigger>
        <TooltipContent>
          {isPlaying ? 'Clear playing status' : 'Mark as playing'}
        </TooltipContent>
      </Tooltip>

      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            type="button"
            variant="secondary"
            size="icon"
            className="size-7 bg-background/80 backdrop-blur-sm hover:bg-background"
            disabled={libraryPending}
            aria-label={
              isCompleted ? 'Clear completed status' : 'Mark as completed'
            }
            aria-pressed={isCompleted}
            onClick={(e) => {
              stop(e)
              toggleLibraryStatus('COMPLETED')
            }}
          >
            <Gamepad2
              className={cn('size-3.5', isCompleted && 'fill-current')}
            />
          </Button>
        </TooltipTrigger>
        <TooltipContent>
          {isCompleted ? 'Clear completed status' : 'Mark as completed'}
        </TooltipContent>
      </Tooltip>
    </div>
  )
}
