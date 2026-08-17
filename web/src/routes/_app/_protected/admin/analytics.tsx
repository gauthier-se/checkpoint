import { Suspense } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { AdminPageHeader } from '@/components/admin/admin-page-header'
import { AdminStatTile } from '@/components/admin/analytics/admin-stat-tile'
import { AdminTopBarChart } from '@/components/admin/analytics/admin-top-bar-chart'
import { Skeleton } from '@/components/ui/skeleton'
import { seo } from '@/lib/seo'
import { withoutEmptyRanks } from '@/lib/admin-analytics'
import { adminAnalyticsQueryOptions } from '@/queries/admin/analytics'

export const Route = createFileRoute('/_app/_protected/admin/analytics')({
  head: () => ({
    meta: seo({ title: 'Analytics — Admin — Checkpoint' }),
  }),
  loader: ({ context }) => {
    void context.queryClient.prefetchQuery(adminAnalyticsQueryOptions())
  },
  component: AdminAnalyticsPage,
})

function AdminAnalyticsPage() {
  return (
    <>
      <AdminPageHeader
        title="Analytics"
        description="Platform activity at a glance."
      />
      <Suspense fallback={<AnalyticsSkeleton />}>
        <AdminAnalyticsContent />
      </Suspense>
    </>
  )
}

function AdminAnalyticsContent() {
  const { data } = useSuspenseQuery(adminAnalyticsQueryOptions())

  const bannedUsers = Math.max(0, data.totalUsers - data.activeUsers)

  return (
    <div className="flex flex-col gap-6">
      {/* Five running totals: headline numbers, so tiles rather than a chart. */}
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <AdminStatTile label="Users" value={data.totalUsers} />
        <AdminStatTile
          label="Active users"
          value={data.activeUsers}
          ratio={{
            value: data.activeUsers,
            of: data.totalUsers,
            label:
              bannedUsers === 0
                ? 'No banned accounts'
                : `${bannedUsers} banned account${bannedUsers === 1 ? '' : 's'}`,
          }}
        />
        <AdminStatTile label="Games" value={data.totalGames} />
        <AdminStatTile label="Reviews" value={data.totalReviews} />
        <AdminStatTile
          label="Reports on file"
          value={data.openReports}
          detail="Every report, dismissed or not"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <AdminTopBarChart
          title="Most reviewed games"
          description="Top five by number of reviews."
          valueNoun="review"
          emptyMessage="No reviews yet."
          data={withoutEmptyRanks(
            data.topReviewedGames.map((game) => ({
              id: game.id,
              label: game.title,
              value: game.reviewCount,
            })),
          )}
        />

        <AdminTopBarChart
          title="Most active reviewers"
          description="Top five by number of reviews written."
          valueNoun="review"
          emptyMessage="No reviewers yet."
          data={withoutEmptyRanks(
            data.topReviewers.map((reviewer) => ({
              id: reviewer.id,
              label: reviewer.username,
              value: reviewer.reviewCount,
            })),
          )}
        />
      </div>
    </div>
  )
}

function AnalyticsSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {Array.from({ length: 5 }, (_, index) => (
          <Skeleton key={index} className="h-28 w-full rounded-xl" />
        ))}
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        <Skeleton className="h-72 w-full rounded-xl" />
        <Skeleton className="h-72 w-full rounded-xl" />
      </div>
    </div>
  )
}
