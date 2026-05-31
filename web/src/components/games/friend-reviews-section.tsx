import { useQuery } from '@tanstack/react-query'
import { ReviewCard } from '@/components/reviews/review-card'
import { DiscoverySection } from '@/components/games/discovery-section'
import { friendReviewsQueryOptions } from '@/queries/game-social'

interface FriendReviewsSectionProps {
  gameId: string
  size?: number
}

export function FriendReviewsSection({
  gameId,
  size = 6,
}: FriendReviewsSectionProps) {
  const { data } = useQuery(friendReviewsQueryOptions(gameId, 0, size))

  if (!data || data.content.length === 0) {
    return null
  }

  return (
    <DiscoverySection title="Reviews from friends">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 pt-4">
        {data.content.map((review) => (
          <ReviewCard key={review.id} review={review} showCover={false} />
        ))}
      </div>
    </DiscoverySection>
  )
}
