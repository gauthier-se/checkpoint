import { Link, createFileRoute } from '@tanstack/react-router'
import { Users } from 'lucide-react'
import type { GamesResponse } from '@/types/game'
import { GameGrid } from '@/components/games/game-grid'
import { PopularWithFriendsPagination } from '@/components/games/popular-with-friends-pagination'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { friendsPopularGamesPagedQueryOptions } from '@/queries/feed'

import { seo } from '@/lib/seo'

type PopularWithFriendsSearchParams = {
  page: number
}

const PAGE_SIZE = 32

export const Route = createFileRoute(
  '/_app/_protected/games/popular-with-friends',
)({
  head: () => ({
    meta: seo({ title: 'Popular with friends — Checkpoint' }),
  }),
  validateSearch: (
    search: Record<string, unknown>,
  ): PopularWithFriendsSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
  }),
  loaderDeps: ({ search }) => search,
  loader: async ({ deps, context }): Promise<GamesResponse> => {
    return context.queryClient.ensureQueryData(
      friendsPopularGamesPagedQueryOptions(deps.page - 1, PAGE_SIZE),
    )
  },
  component: RouteComponent,
})

function RouteComponent() {
  const data = Route.useLoaderData()
  const { page } = Route.useSearch()

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mt-10">
        <h1 className="text-xl font-bold">Popular with friends</h1>
      </div>

      <div className="my-8">
        <div className="py-2">
          <h2 className="text-muted-foreground font-semibold">
            {data.metadata.totalElements === 0
              ? 'No games to show yet'
              : `${data.metadata.totalElements} game${data.metadata.totalElements > 1 ? 's' : ''} trending among people you follow`}
          </h2>
        </div>
        <Separator />

        {data.content.length > 0 ? (
          <>
            <GameGrid games={data.content} />
            <PopularWithFriendsPagination
              page={page}
              totalPages={data.metadata.totalPages}
              hasNext={data.metadata.hasNext}
              hasPrevious={data.metadata.hasPrevious}
            />
          </>
        ) : (
          <div className="flex flex-col items-center gap-4 py-16 text-center">
            <Users className="text-muted-foreground size-12" />
            <p className="text-muted-foreground text-lg">
              No games to show yet
            </p>
            <p className="text-muted-foreground text-sm">
              Follow other players to see what they are playing, rating, and
              reviewing here.
            </p>
            <Button asChild>
              <Link to="/members/all" search={{ page: 1 }}>
                Find people to follow
              </Link>
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
