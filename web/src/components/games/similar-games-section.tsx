import { useSuspenseQuery } from '@tanstack/react-query'
import { DiscoverySection } from '@/components/games/discovery-section'
import { GameGrid } from '@/components/games/game-grid'
import { similarGamesQueryOptions } from '@/queries/catalog'

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
