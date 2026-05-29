import { useQuery } from '@tanstack/react-query'
import { ReviewList } from '@/components/reviews/review-list'
import { friendReviewsQueryOptions } from '@/queries/game-social'

interface FriendReviewsSectionProps {
  gameId: string
}

export function FriendReviewsSection({ gameId }: FriendReviewsSectionProps) {
  // Probe the first page so we can hide the entire section when empty without
  // letting `ReviewList` flash a header during loading.
  const { data, isLoading } = useQuery(friendReviewsQueryOptions(gameId, 0, 10))

  if (isLoading) return null
  if (!data || data.content.length === 0) return null

  return (
    <section className="mt-8">
      <ReviewList
        title="Reviews from friends"
        queryOptionsFactory={(page, size) =>
          friendReviewsQueryOptions(gameId, page, size)
        }
        hideWhenEmpty
      />
    </section>
  )
}
