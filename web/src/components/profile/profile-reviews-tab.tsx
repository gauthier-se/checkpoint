import { useQuery } from '@tanstack/react-query'
import { AlignLeft, Lock } from 'lucide-react'
import type { UserProfile } from '@/types/profile'
import { userReviewsQueryOptions } from '@/queries/profile'
import { ReviewCard } from '@/components/reviews/review-card'
import { PaginationNav } from '@/components/shared/pagination-nav'

interface ProfileReviewsTabProps {
  profile: UserProfile
  page: number
}

export function ProfileReviewsTab({ profile, page }: ProfileReviewsTabProps) {
  const apiPage = Math.max(0, page - 1)
  const { data, isLoading, isError } = useQuery(
    userReviewsQueryOptions(profile.username, apiPage),
  )

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
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="bg-muted h-32 animate-pulse rounded-lg" />
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
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {data.content.map((review) => (
          <ReviewCard key={review.id} review={review} showCover />
        ))}
      </div>
      <PaginationNav
        page={page}
        totalPages={data.metadata.totalPages}
        hasNext={data.metadata.hasNext}
        hasPrevious={data.metadata.hasPrevious}
        hideWhenSinglePage
        className="pt-6 pb-4"
        linkProps={(target) => ({
          to: '.',
          search: { tab: 'reviews', page: target },
        })}
      />
    </div>
  )
}
