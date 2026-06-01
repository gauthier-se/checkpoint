import { Link } from '@tanstack/react-router'
import type { GameListCard } from '@/types/list'
import { ListsGrid } from '@/components/lists/lists-grid'
import { SectionHeader } from '@/components/home/section-header'

export function PopularListsSection({
  lists,
  isLoading,
}: {
  lists: Array<GameListCard> | undefined
  isLoading: boolean
}) {
  if (isLoading || !lists || lists.length === 0) return null

  return (
    <section className="my-12">
      <SectionHeader
        title="Popular lists"
        action={
          <Link
            to="/lists"
            search={{ page: 1 }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            See all
          </Link>
        }
      />
      <ListsGrid lists={lists} />
    </section>
  )
}
