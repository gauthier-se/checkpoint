import { Suspense, useMemo } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import type { AdminReviewsSearchParams } from '@/types/admin'
import { AdminReviewsTable } from '@/components/admin/moderation/admin-reviews-table'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import {
  toModeratedReviewRow,
  toReportedReviewRow,
} from '@/lib/admin-moderation'
import {
  adminReportedReviewsQueryOptions,
  adminReviewsQueryOptions,
} from '@/queries/admin/moderation'

export const Route = createFileRoute(
  '/_app/_protected/admin/moderation/reviews',
)({
  validateSearch: (
    search: Record<string, unknown>,
  ): AdminReviewsSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    // Reported-only is the useful default: it is the actual work queue.
    reported: search.reported !== false && search.reported !== 'false',
  }),
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    // Branch on the prefetch rather than on the options: the two listings
    // return different records, so a ternary would union their query types.
    if (deps.reported) {
      void context.queryClient.prefetchQuery(
        adminReportedReviewsQueryOptions({ page: deps.page }),
      )
    } else {
      void context.queryClient.prefetchQuery(
        adminReviewsQueryOptions({ page: deps.page }),
      )
    }
  },
  component: AdminReviewsPage,
})

function AdminReviewsPage() {
  const search = Route.useSearch()
  const navigate = useNavigate()

  return (
    <>
      <div className="mb-4 flex items-center gap-2">
        <Button
          variant={search.reported ? 'default' : 'outline'}
          size="sm"
          onClick={() =>
            void navigate({
              to: '/admin/moderation/reviews',
              search: { page: 1, reported: true },
            })
          }
        >
          Reported only
        </Button>
        <Button
          variant={search.reported ? 'outline' : 'default'}
          size="sm"
          onClick={() =>
            void navigate({
              to: '/admin/moderation/reviews',
              search: { page: 1, reported: false },
            })
          }
        >
          All reviews
        </Button>
      </div>

      <Suspense fallback={<TableSkeleton />}>
        {search.reported ? <ReportedReviews /> : <AllReviews />}
      </Suspense>
    </>
  )
}

function ReportedReviews() {
  const search = Route.useSearch()
  const { data } = useSuspenseQuery(
    adminReportedReviewsQueryOptions({ page: search.page }),
  )
  const rows = useMemo(
    () => data.content.map(toReportedReviewRow),
    [data.content],
  )

  return (
    <AdminReviewsTable rows={rows} metadata={data.metadata} search={search} />
  )
}

function AllReviews() {
  const search = Route.useSearch()
  const { data } = useSuspenseQuery(
    adminReviewsQueryOptions({ page: search.page }),
  )
  const rows = useMemo(
    () => data.content.map(toModeratedReviewRow),
    [data.content],
  )

  return (
    <AdminReviewsTable rows={rows} metadata={data.metadata} search={search} />
  )
}

function TableSkeleton() {
  return (
    <div className="flex flex-col gap-2 rounded-lg border p-4">
      {Array.from({ length: 8 }, (_, index) => (
        <Skeleton key={index} className="h-9 w-full" />
      ))}
    </div>
  )
}
