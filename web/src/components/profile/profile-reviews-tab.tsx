import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { AlignLeft, Lock, MessageCircleWarning } from 'lucide-react'
import type { UserProfile } from '@/types/profile'
import { userReviewsQueryOptions } from '@/queries/profile'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

interface ProfileReviewsTabProps {
  profile: UserProfile
  page: number
}

export function ProfileReviewsTab({ profile, page }: ProfileReviewsTabProps) {
  const [revealedSpoilers, setRevealedSpoilers] = useState<
    Record<string, boolean>
  >({})
  const apiPage = Math.max(0, page - 1)
  const { data, isLoading, isError } = useQuery(
    userReviewsQueryOptions(profile.username, apiPage),
  )

  const toggleSpoilers = (id: string) => {
    setRevealedSpoilers((prev) => ({ ...prev, [id]: !prev[id] }))
  }

  if (profile.isPrivate && !profile.isOwner) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Lock className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">This profile is private</p>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="bg-muted h-24 animate-pulse rounded-lg" />
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <AlignLeft className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">Unable to load reviews</p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <AlignLeft className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">No reviews yet</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {data.content.map((review) => (
        <Card key={review.id}>
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">
                {review.platformName && (
                  <span className="text-muted-foreground mr-2 text-sm">
                    on {review.platformName}
                  </span>
                )}
              </CardTitle>
              <span className="text-muted-foreground text-sm">
                {new Date(review.createdAt).toLocaleDateString('en-US', {
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric',
                })}
              </span>
            </div>
          </CardHeader>
          <CardContent>
            {review.haveSpoilers &&
            !revealedSpoilers[review.id] &&
            !profile.isOwner ? (
              <p className="text-sm italic flex items-center gap-2">
                <MessageCircleWarning className="size-4" />
                This review contains spoilers
                <Button
                  variant="outline"
                  size="sm"
                  className="h-6 text-[10px] px-2 not-italic"
                  onClick={() => toggleSpoilers(review.id)}
                >
                  Show
                </Button>
              </p>
            ) : (
              <p className="text-sm leading-relaxed whitespace-pre-wrap">
                {review.content}
              </p>
            )}
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
