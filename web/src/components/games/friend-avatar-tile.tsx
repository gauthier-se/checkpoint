import { Link } from '@tanstack/react-router'
import { AlignLeft } from 'lucide-react'
import { MiniStarRating } from '@/components/games/mini-star-rating'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'
import { resolvePictureUrl } from '@/lib/picture'

interface FriendAvatarTileProps {
  pseudo: string
  picture: string | null
  rating: number | null
  hasReview: boolean
  href: string | null
  tooltip?: string
}

/**
 * Single avatar tile rendered in the friend-activity / want-to-play grids.
 * A shadcn Badge with the review icon sits at the top-right corner of the
 * avatar; stars (filled portion only) sit underneath. When {@code href} is
 * null the tile is non-interactive.
 */
export function FriendAvatarTile({
  pseudo,
  picture,
  rating,
  hasReview,
  href,
  tooltip,
}: FriendAvatarTileProps) {
  const fallback = pseudo.substring(0, 2).toUpperCase()
  const inner = (
    <div className="flex w-16 flex-col items-center gap-1">
      <div className="relative">
        <Avatar
          size="2xl"
          className={cn(href && 'transition hover:opacity-90')}
        >
          <AvatarImage src={resolvePictureUrl(picture)} alt={pseudo} />
          <AvatarFallback>{fallback}</AvatarFallback>
        </Avatar>
        {hasReview && (
          <Badge
            variant="secondary"
            aria-label="has a review"
            className="absolute -top-1 -right-1 size-4 rounded-full p-0 ring-2 ring-background [&>svg]:size-2.5 bg-muted-foreground text-background"
          >
            <AlignLeft />
          </Badge>
        )}
      </div>
      <span className="max-w-[3.5rem] truncate text-[10px] text-muted-foreground">
        {pseudo}
      </span>
      {rating != null && <MiniStarRating value={rating} />}
    </div>
  )

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        {href ? (
          <Link to={href} className="block">
            {inner}
          </Link>
        ) : (
          <div>{inner}</div>
        )}
      </TooltipTrigger>
      <TooltipContent>{tooltip ?? pseudo}</TooltipContent>
    </Tooltip>
  )
}
