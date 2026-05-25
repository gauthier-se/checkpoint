import { Link, createFileRoute } from '@tanstack/react-router'
import { Users } from 'lucide-react'
import type { FeedResponse } from '@/types/feed'
import { FeedList } from '@/components/feed/feed-list'
import { FeedPagination } from '@/components/feed/feed-pagination'
import { OnboardingChecklist } from '@/components/onboarding/onboarding-checklist'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { feedQueryOptions } from '@/queries/feed'

type FeedSearchParams = {
  page: number
}

const PAGE_SIZE = 20

export const Route = createFileRoute('/_app/_protected/feed/')({
  validateSearch: (search: Record<string, unknown>): FeedSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
  }),
  loaderDeps: ({ search }) => search,
  loader: async ({ deps, context }): Promise<FeedResponse> => {
    return context.queryClient.ensureQueryData(
      feedQueryOptions(deps.page - 1, PAGE_SIZE),
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
        <OnboardingChecklist />
        <h1 className="text-xl font-bold">New from friends</h1>
      </div>

      <div className="my-8">
        <div className="py-2">
          <h2 className="text-muted-foreground font-semibold">
            Recent activity from people you follow
          </h2>
        </div>
        <Separator />

        {data.content.length > 0 ? (
          <>
            <FeedList items={data.content} />
            <FeedPagination
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
              No activity to show yet
            </p>
            <p className="text-muted-foreground text-sm">
              Follow other players to see their plays, ratings, and reviews
              here.
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
