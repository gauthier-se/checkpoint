import { Suspense } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import type {
  AdminReportTypeFilter,
  AdminReportsSearchParams,
} from '@/types/admin'
import { AdminReportsFilters } from '@/components/admin/moderation/admin-reports-filters'
import { AdminReportsTable } from '@/components/admin/moderation/admin-reports-table'
import { Skeleton } from '@/components/ui/skeleton'
import { parseTrimmedString } from '@/lib/search-params'
import { adminReportsQueryOptions } from '@/queries/admin/moderation'

const VALID_TYPES: ReadonlyArray<AdminReportTypeFilter> = [
  'all',
  'review',
  'comment',
]

function parseType(value: unknown): AdminReportTypeFilter {
  const type = parseTrimmedString(value)
  return type && (VALID_TYPES as ReadonlyArray<string>).includes(type)
    ? (type as AdminReportTypeFilter)
    : 'all'
}

export const Route = createFileRoute(
  '/_app/_protected/admin/moderation/reports/',
)({
  validateSearch: (
    search: Record<string, unknown>,
  ): AdminReportsSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    type: parseType(search.type),
  }),
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    void context.queryClient.prefetchQuery(adminReportsQueryOptions(deps))
  },
  component: AdminReportsPage,
})

function AdminReportsPage() {
  const search = Route.useSearch()

  return (
    <>
      <AdminReportsFilters search={search} />
      <Suspense fallback={<TableSkeleton />}>
        <AdminReportsContent />
      </Suspense>
    </>
  )
}

function AdminReportsContent() {
  const search = Route.useSearch()
  const { data } = useSuspenseQuery(adminReportsQueryOptions(search))

  return (
    <AdminReportsTable
      rows={data.content}
      metadata={data.metadata}
      search={search}
    />
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
