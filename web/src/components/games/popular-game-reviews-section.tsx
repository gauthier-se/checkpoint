import { useQuery } from '@tanstack/react-query'
import { ReviewCard } from '@/components/reviews/review-card'
import { Separator } from '@/components/ui/separator'
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
    <section className="mt-8">
      <div className="flex items-center justify-between py-2">
        <h2 className="text-xl font-semibold">Popular reviews</h2>
      </div>
      <Separator className="my-4" />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {data.map((review) => (
          <ReviewCard key={review.id} review={review} />
        ))}
      </div>
    </section>
  )
}
