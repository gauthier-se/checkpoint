import { useState } from 'react'
import { Link } from '@tanstack/react-router'
import { Heart, MessageSquare } from 'lucide-react'
import type { ReviewCard as ReviewCardType } from '@/types/review'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { resolvePictureUrl } from '@/lib/picture'
import { useAuth } from '@/hooks/use-auth'

interface ReviewCardProps {
  review: ReviewCardType
  showCover?: boolean
}

export function ReviewCard({ review, showCover = true }: ReviewCardProps) {
  const { user } = useAuth()
  const isOwner = user?.id === review.user.id
  const [showSpoilers, setShowSpoilers] = useState(false)

  const contentNode =
    review.haveSpoilers && !showSpoilers && !isOwner ? (
      <div className="flex flex-col items-center justify-center bg-muted/20 border border-border/5 rounded-md p-3 h-20 gap-1.5">
        <p className="text-xs italic text-muted-foreground">
          This review contains spoilers.
        </p>
        <Button
          variant="outline"
          size="sm"
          className="h-6 text-[10px] px-2"
          onClick={(e) => {
            e.preventDefault()
            e.stopPropagation()
            setShowSpoilers(true)
          }}
        >
          Show
        </Button>
      </div>
    ) : (
      <p className="line-clamp-4 text-sm leading-relaxed text-foreground/80 whitespace-pre-wrap">
        {review.content}
      </p>
    )

  const footerNode = (
    <div className="flex items-center justify-end gap-4 text-xs font-medium text-muted-foreground">
      <span className="flex items-center gap-1.5 hover:text-red-400 transition-colors">
        <Heart
          className={`size-3.5 ${review.hasLiked ? 'fill-red-500 text-red-500' : ''}`}
        />
        {review.likesCount}
      </span>
      <span className="flex items-center gap-1.5 hover:text-blue-400 transition-colors">
        <MessageSquare className="size-3.5" />
        {review.commentsCount}
      </span>
    </div>
  )

  const formattedDate = new Date(review.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  return (
    <Link
      to={review.playLogId ? '/plays/$id' : '/games/$gameId'}
      params={
        review.playLogId ? { id: review.playLogId } : { gameId: review.gameId }
      }
      className="group flex h-full"
    >
      <Card className="flex h-full w-full flex-col gap-2 overflow-hidden p-3.5 sm:p-4 bg-card border border-border/10 hover:border-border/30 transition-all hover:bg-card/80 shadow-sm hover:shadow">
        {showCover ? (
          <>
            <div className="flex items-start gap-3 border-b border-border/10 pb-2">
              <img
                src={review.gameCoverUrl}
                alt={review.gameTitle}
                className="h-14 w-10 shrink-0 rounded-sm object-cover shadow-sm"
              />
              <div className="min-w-0 flex-1 flex flex-col justify-center h-14">
                <p className="line-clamp-1 text-sm font-bold text-foreground/90">
                  {review.gameTitle}
                </p>
                <div className="mt-1 flex items-center gap-2">
                  <Avatar className="h-5 w-5 border border-border/20">
                    <AvatarImage
                      src={resolvePictureUrl(review.user.picture)}
                      alt={review.user.pseudo}
                    />
                    <AvatarFallback className="text-[10px] bg-muted">
                      {review.user.pseudo.substring(0, 2).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  <span className="truncate text-xs font-medium text-muted-foreground">
                    <span className="font-normal opacity-70">Review by</span>{' '}
                    {review.user.pseudo}
                  </span>
                </div>
              </div>
            </div>
            <CardContent className="flex flex-1 flex-col justify-between gap-2 p-0 pt-1">
              {contentNode}
              <div className="pt-2 border-t border-border/5">{footerNode}</div>
            </CardContent>
          </>
        ) : (
          <div className="flex items-start gap-3 h-full">
            <Avatar className="h-10 w-10 shrink-0 border border-border/20 mt-0.5">
              <AvatarImage
                src={resolvePictureUrl(review.user.picture)}
                alt={review.user.pseudo}
              />
              <AvatarFallback className="bg-muted">
                {review.user.pseudo.substring(0, 2).toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <div className="min-w-0 flex-1 flex flex-col h-full">
              <div className="flex items-center gap-2">
                <p className="line-clamp-1 text-sm font-bold text-foreground/90">
                  {review.user.pseudo}
                </p>
                <span className="truncate text-xs text-muted-foreground">
                  {formattedDate}
                </span>
              </div>
              <div className="mt-1.5 flex-1">{contentNode}</div>
              <div className="mt-3">{footerNode}</div>
            </div>
          </div>
        )}
      </Card>
    </Link>
  )
}
