import { useSuspenseQuery } from '@tanstack/react-query'
import { DiscoverySection } from '@/components/games/discovery-section'
import { GameGrid } from '@/components/games/game-grid'
import { Skeleton } from '@/components/ui/skeleton'
import { similarGamesQueryOptions } from '@/queries/catalog'

const SIMILAR_GAMES_COUNT = 7

interface SimilarGamesSectionProps {
  gameId: string
}

export function SimilarGamesSection({ gameId }: SimilarGamesSectionProps) {
  const { data } = useSuspenseQuery(similarGamesQueryOptions(gameId))

  if (data.length === 0) {
    return null
  }

  return (
    <DiscoverySection title="You may also like...">
      <GameGrid games={data} columns={7} />
    </DiscoverySection>
  )
}

/**
 * Loading placeholder shown as the Suspense fallback for
 * {@link SimilarGamesSection}: the same titled section with the same 7-column
 * grid filled with cover-shaped skeleton tiles.
 */
export function SimilarGamesSectionSkeleton() {
  return (
    <DiscoverySection title="You may also like...">
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7 gap-3 py-4">
        {Array.from({ length: SIMILAR_GAMES_COUNT }).map((_, i) => (
          <Skeleton key={i} className="aspect-[3/4] w-full rounded-sm" />
        ))}
      </div>
    </DiscoverySection>
  )
}
