import { Link, createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Users } from 'lucide-react'
import type { FeedItemType, FeedTab } from '@/types/feed'
import { FEED_ITEM_TYPES } from '@/types/feed'
import { FeedList, FeedListSkeleton } from '@/components/feed/feed-list'
import { FeedPagination } from '@/components/feed/feed-pagination'
import { FEED_TAB_OPTIONS, FeedTabs } from '@/components/feed/feed-tabs'
import { OnboardingChecklist } from '@/components/onboarding/onboarding-checklist'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { feedQueryOptions } from '@/queries/feed'

import { seo } from '@/lib/seo'

type FeedSearchParams = {
  page: number
  type?: FeedItemType
}

const PAGE_SIZE = 20

export const Route = createFileRoute('/_app/_protected/feed/')({
  head: () => ({
    meta: seo({ title: 'Feed — Checkpoint' }),
  }),
  validateSearch: (search: Record<string, unknown>): FeedSearchParams => {
    const rawType = search.type
    const type = FEED_ITEM_TYPES.includes(rawType as FeedItemType)
      ? (rawType as FeedItemType)
      : undefined
    return {
      page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
      type,
    }
  },
  loaderDeps: ({ search }) => search,
  loader: async ({ deps, context }) => {
    // Best-effort SSR prefetch only. The feed is a protected endpoint; on a hard
    // refresh the server-side request may not be authenticated, so swallow the
    // error rather than 500 — the component's useQuery refetches on the client
    // (where the session cookie is always present).
    try {
      await context.queryClient.ensureQueryData(
        feedQueryOptions(deps.page - 1, PAGE_SIZE, deps.type),
      )
    } catch {
      // Handled client-side by useQuery.
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { page, type } = Route.useSearch()
  const navigate = Route.useNavigate()
  const feedQuery = useQuery(feedQueryOptions(page - 1, PAGE_SIZE, type))
  const data = feedQuery.data

  function handleTabChange(value: FeedTab) {
    // Reset to the first page whenever the filter changes.
    navigate({
      search: { page: 1, type: value === 'all' ? undefined : value },
    })
  }

  const activeLabel = FEED_TAB_OPTIONS.find(
    (opt) => opt.value === (type ?? 'all'),
  )?.label

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
        <FeedTabs value={type ?? 'all'} onValueChange={handleTabChange} />

        {!data ? (
          <FeedListSkeleton />
        ) : data.content.length > 0 ? (
          <>
            <FeedList items={data.content} />
            <FeedPagination
              page={page}
              totalPages={data.metadata.totalPages}
              hasNext={data.metadata.hasNext}
              hasPrevious={data.metadata.hasPrevious}
            />
          </>
        ) : type ? (
          <div className="flex flex-col items-center gap-4 py-16 text-center">
            <Users className="text-muted-foreground size-12" />
            <p className="text-muted-foreground text-lg">
              No {activeLabel?.toLowerCase()} activity in the last 30 days
            </p>
            <Button variant="outline" onClick={() => handleTabChange('all')}>
              Show all activity
            </Button>
          </div>
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
