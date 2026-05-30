import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Flag, Gamepad2, MessageSquare, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { Link } from '@tanstack/react-router'

import type { PlayStatus } from '@/types/interaction'
import type { ReviewsResponse } from '@/types/review'
import type { gameReviewsQueryOptions } from '@/queries/review'
import { CommentSection } from '@/components/comments/comment-section'
import { ReportReviewDialog } from '@/components/reviews/report-review-dialog'
import { LikeButton } from '@/components/shared/like-button'
import { MentionText } from '@/components/shared/mention-text'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { resolvePictureUrl } from '@/lib/picture'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useAuth } from '@/hooks/use-auth'
import { toggleReviewLike } from '@/queries/review'

// Shape returned by `queryOptions(...)` for a reviews page. We reuse the
// existing helper for the type so both this component and the friend-reviews
// factory accept it without manual generics gymnastics.
type ReviewsQueryOptions = ReturnType<typeof gameReviewsQueryOptions>

interface ReviewListProps {
  title: string
  queryOptionsFactory: (page: number, size: number) => ReviewsQueryOptions
  hideWhenEmpty?: boolean
  emptyMessage?: string
}

const PLAY_STATUS_LABELS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'Playing',
  PLAYED: 'Played',
  COMPLETED: 'Completed',
  RETIRED: 'Retired',
  SHELVED: 'Shelved',
  ABANDONED: 'Abandoned',
}

const PLAY_STATUS_COLORS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  PLAYED: 'bg-violet-500/15 text-violet-400 border-violet-500/20',
  COMPLETED: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  RETIRED: 'bg-slate-500/15 text-slate-400 border-slate-500/20',
  SHELVED: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
  ABANDONED: 'bg-red-500/15 text-red-400 border-red-500/20',
}

export function ReviewList({
  title,
  queryOptionsFactory,
  hideWhenEmpty = false,
  emptyMessage = 'No reviews yet. Be the first to leave one!',
}: ReviewListProps) {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const [revealedSpoilers, setRevealedSpoilers] = useState<
    Record<string, boolean>
  >({})
  const [reportingReviewId, setReportingReviewId] = useState<string | null>(
    null,
  )
  const size = 10

  const queryClient = useQueryClient()
  const currentOptions = queryOptionsFactory(page, size)

  const { data, isLoading, isError } = useQuery(currentOptions)

  const likeMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (reviewId: string) => toggleReviewLike(reviewId),
    onMutate: async (reviewId) => {
      const queryKey = currentOptions.queryKey
      await queryClient.cancelQueries({ queryKey })
      const previous = queryClient.getQueryData<ReviewsResponse>(queryKey)
      queryClient.setQueryData<ReviewsResponse>(queryKey, (old) => {
        if (!old) return old
        return {
          ...old,
          content: old.content.map((r) =>
            r.id === reviewId
              ? {
                  ...r,
                  hasLiked: !r.hasLiked,
                  likesCount: r.hasLiked ? r.likesCount - 1 : r.likesCount + 1,
                }
              : r,
          ),
        }
      })
      return { previous }
    },
    onError: (_err, _reviewId, context) => {
      toast.error('Failed to update like')
      if (context?.previous) {
        queryClient.setQueryData(currentOptions.queryKey, context.previous)
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({
        queryKey: currentOptions.queryKey,
      })
    },
  })

  const toggleSpoilers = (reviewId: string) => {
    setRevealedSpoilers((prev) => ({
      ...prev,
      [reviewId]: !prev[reviewId],
    }))
  }

  if (isLoading) {
    if (hideWhenEmpty) return null
    return (
      <div className="py-8 text-center text-muted-foreground">
        Loading reviews...
      </div>
    )
  }

  if (isError || !data) {
    if (hideWhenEmpty) return null
    return (
      <div className="py-8 text-center text-red-500">
        Error loading reviews.
      </div>
    )
  }

  const { content: reviews, metadata } = data

  if (reviews.length === 0) {
    if (hideWhenEmpty) return null
    return (
      <div className="py-8 text-center text-muted-foreground">
        {emptyMessage}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <h2 className="text-xl font-semibold">{title}</h2>

      <div className="grid grid-cols-1 gap-4">
        {reviews.map((review) => (
          <Card key={review.id} className="py-4">
            <CardHeader className="py-0 pb-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Avatar className="h-10 w-10">
                    <AvatarImage
                      src={resolvePictureUrl(review.user.picture)}
                      alt={review.user.pseudo}
                    />
                    <AvatarFallback>
                      {review.user.pseudo.substring(0, 2).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <CardTitle className="text-base flex items-center gap-2">
                      {review.user.pseudo}
                      {review.playStatus && (
                        <Badge
                          className={`${PLAY_STATUS_COLORS[review.playStatus]} text-[10px] h-5 px-1.5`}
                        >
                          {PLAY_STATUS_LABELS[review.playStatus]}
                        </Badge>
                      )}
                      {review.isReplay && (
                        <Badge
                          variant="outline"
                          className="text-[10px] h-5 px-1.5 gap-1"
                        >
                          <RefreshCw className="size-2.5" />
                          Replay
                        </Badge>
                      )}
                    </CardTitle>
                    <CardDescription className="flex items-center gap-2 mt-0.5">
                      {review.playLogId ? (
                        <Link
                          to="/plays/$id"
                          params={{ id: review.playLogId }}
                          className="hover:underline"
                        >
                          {new Date(review.createdAt).toLocaleDateString(
                            'en-US',
                            {
                              year: 'numeric',
                              month: 'long',
                              day: 'numeric',
                            },
                          )}
                        </Link>
                      ) : (
                        new Date(review.createdAt).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric',
                        })
                      )}
                      {review.platformName && (
                        <>
                          <span className="text-muted-foreground/30">•</span>
                          <span className="flex items-center gap-1 text-xs">
                            <Gamepad2 className="size-3" />
                            {review.platformName}
                          </span>
                        </>
                      )}
                    </CardDescription>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <LikeButton
                    liked={review.hasLiked}
                    likesCount={review.likesCount}
                    onToggle={() => likeMutation.mutate(review.id)}
                    disabled={!user}
                    isPending={likeMutation.isPending}
                  />
                  {review.playLogId ? (
                    <Link
                      to="/plays/$id"
                      params={{ id: review.playLogId }}
                      className="inline-flex items-center gap-1 h-8 px-3 text-xs rounded-md hover:bg-accent hover:text-accent-foreground"
                    >
                      <MessageSquare className="size-4" />
                      {review.commentsCount}
                    </Link>
                  ) : (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="gap-1 pointer-events-none"
                    >
                      <MessageSquare className="size-4" />
                      {review.commentsCount}
                    </Button>
                  )}
                  {user && user.id !== review.user.id && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-muted-foreground hover:text-destructive"
                      onClick={() => setReportingReviewId(review.id)}
                    >
                      <Flag className="size-4" />
                    </Button>
                  )}
                </div>
              </div>
            </CardHeader>
            <CardContent className="py-0">
              {review.haveSpoilers && !revealedSpoilers[review.id] ? (
                <div className="mt-3 p-4 border rounded-md bg-muted/50 flex flex-col items-center justify-center gap-2">
                  <p className="text-sm font-medium text-muted-foreground">
                    This review contains spoilers
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => toggleSpoilers(review.id)}
                  >
                    Show Spoilers
                  </Button>
                </div>
              ) : (
                <div className="mt-3">
                  <p className="text-sm leading-relaxed whitespace-pre-line">
                    {review.playLogId ? (
                      <Link
                        to="/plays/$id"
                        params={{ id: review.playLogId }}
                        className="hover:underline"
                      >
                        <MentionText content={review.content} />
                      </Link>
                    ) : (
                      <MentionText content={review.content} />
                    )}
                  </p>
                  {review.haveSpoilers && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="mt-2 h-7 px-2 text-xs text-muted-foreground"
                      onClick={() => toggleSpoilers(review.id)}
                    >
                      Hide Spoilers
                    </Button>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {reportingReviewId && (
        <ReportReviewDialog
          reviewId={reportingReviewId}
          open={true}
          onOpenChange={(open) => {
            if (!open) setReportingReviewId(null)
          }}
        />
      )}

      {/* Pagination Controls */}
      {metadata.totalPages > 1 && (
        <div className="flex items-center justify-center gap-4 mt-4">
          <Button
            variant="outline"
            disabled={!metadata.hasPrevious}
            onClick={() => setPage((old) => Math.max(old - 1, 0))}
          >
            Previous
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {metadata.page + 1} of {metadata.totalPages}
          </span>
          <Button
            variant="outline"
            disabled={!metadata.hasNext}
            onClick={() => setPage((old) => old + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}
