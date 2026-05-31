import { Link } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ListCard } from '@/components/lists/list-card'
import { listsContainingGameQueryOptions } from '@/queries/lists'
import { DiscoverySection } from '@/components/games/discovery-section'

interface GameListsSectionProps {
  gameId: string
}

export function GameListsSection({ gameId }: GameListsSectionProps) {
  const { data } = useSuspenseQuery(
    listsContainingGameQueryOptions(gameId, 0, 6),
  )
  const total = data.metadata.totalElements

  if (total === 0) {
    return null
  }

  return (
    <DiscoverySection
      title={`Appears in ${total} ${total === 1 ? 'list' : 'lists'}`}
      action={
        total > 6 && (
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
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 pt-4">
        {data.content.map((list) => (
          <ListCard key={list.id} list={list} />
        ))}
      </div>
    </DiscoverySection>
  )
}
