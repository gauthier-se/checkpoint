import { Link } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ListsGrid } from '@/components/lists/lists-grid'
import { listsContainingGameQueryOptions } from '@/queries/lists'
import { DiscoverySection } from '@/components/games/discovery-section'

interface GameListsSectionProps {
  gameId: string
}

export function GameListsSection({ gameId }: GameListsSectionProps) {
  const { data } = useSuspenseQuery(
    listsContainingGameQueryOptions(gameId, 0, 4),
  )
  const total = data.metadata.totalElements

  if (total === 0) {
    return null
  }

  return (
    <DiscoverySection
      title={`Appears in ${total} ${total === 1 ? 'list' : 'lists'}`}
      action={
        total > 4 && (
          <Link
            to="/games/$gameId/lists"
            params={{ gameId }}
            search={{ page: 1 }}
            className="text-sm font-medium text-primary hover:underline"
          >
            See all →
          </Link>
        )
      }
    >
      <ListsGrid lists={data.content} />
    </DiscoverySection>
  )
}
