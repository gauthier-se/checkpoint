import { useQuery } from '@tanstack/react-query'
import { ReviewCard } from '@/components/reviews/review-card'
import { DiscoverySection } from '@/components/games/discovery-section'
import { popularGameReviewsQueryOptions } from '@/queries/game-social'

interface PopularGameReviewsSectionProps {
  gameId: string
  size?: number
}

export function PopularGameReviewsSection({
  gameId,
  size = 6,
}: PopularGameReviewsSectionProps) {
  const { data } = useQuery(popularGameReviewsQueryOptions(gameId, size))

  if (!data || data.length === 0) {
    return null
  }

  return (
    <DiscoverySection title="Popular reviews">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 pt-4">
        {data.map((review) => (
          <ReviewCard key={review.id} review={review} showCover={false} />
        ))}
      </div>
    </DiscoverySection>
  )
}
