import { Link } from '@tanstack/react-router'
import { Heart, MessageSquare } from 'lucide-react'
import type { ReviewCard as ReviewCardType } from '@/types/review'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Card, CardContent } from '@/components/ui/card'

interface ReviewCardProps {
  review: ReviewCardType
}

export function ReviewCard({ review }: ReviewCardProps) {
  return (
    <Link
      to="/games/$gameId"
      params={{ gameId: review.gameId }}
      className="group flex h-full"
    >
      <Card className="flex h-full w-full flex-col gap-3 overflow-hidden p-3 transition-colors group-hover:border-foreground/40">
        <div className="flex items-start gap-3">
          <img
            src={review.gameCoverUrl}
            alt={review.gameTitle}
            className="h-20 w-14 shrink-0 rounded-sm object-cover"
          />
          <div className="min-w-0 flex-1">
            <p className="line-clamp-2 text-sm font-semibold leading-snug">
              {review.gameTitle}
            </p>
            <div className="mt-1 flex items-center gap-2">
              <Avatar className="h-5 w-5">
                <AvatarImage
                  src={review.user.picture ?? undefined}
                  alt={review.user.pseudo}
                />
                <AvatarFallback className="text-[10px]">
                  {review.user.pseudo.substring(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <span className="truncate text-xs text-muted-foreground">
                {review.user.pseudo}
              </span>
            </div>
          </div>
        </div>
        <CardContent className="flex flex-1 flex-col justify-between gap-2 p-0">
          {review.haveSpoilers ? (
            <p className="line-clamp-3 text-xs italic text-muted-foreground">
              This review contains spoilers.
            </p>
          ) : (
            <p className="line-clamp-3 text-xs leading-relaxed text-muted-foreground">
              {review.content}
            </p>
          )}
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span className="flex items-center gap-1">
              <Heart
                className={`size-3 ${review.hasLiked ? 'fill-current text-red-500' : ''}`}
              />
              {review.likesCount}
            </span>
            <span className="flex items-center gap-1">
              <MessageSquare className="size-3" />
              {review.commentsCount}
            </span>
          </div>
        </CardContent>
      </Card>
    </Link>
  )
}
