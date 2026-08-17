import { Suspense } from 'react'
import { Link, createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { AdminReportDetailCard } from '@/components/admin/moderation/admin-report-detail-card'
import { ErrorPage } from '@/components/errors/error-page'
import { Skeleton } from '@/components/ui/skeleton'
import { adminReportQueryOptions } from '@/queries/admin/moderation'
import { isApiError } from '@/services/api'

export const Route = createFileRoute(
  '/_app/_protected/admin/moderation/reports/$reportId',
)({
  loader: ({ context, params }) => {
    void context.queryClient.prefetchQuery(
      adminReportQueryOptions(params.reportId),
    )
  },
  errorComponent: ({ error, reset }) => (
    <ErrorPage
      status={isApiError(error) ? error.status : undefined}
      message={isApiError(error) ? error.message : undefined}
      onRetry={reset}
    />
  ),
  component: AdminReportDetailPage,
})

function AdminReportDetailPage() {
  return (
    <>
      <Link
        to="/admin/moderation/reports"
        search={{ page: 1, type: 'all' }}
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to the queue
      </Link>

      <Suspense fallback={<Skeleton className="h-96 w-full rounded-xl" />}>
        <AdminReportDetailContent />
      </Suspense>
    </>
  )
}

function AdminReportDetailContent() {
  const { reportId } = Route.useParams()
  const { data: report } = useSuspenseQuery(adminReportQueryOptions(reportId))

  return <AdminReportDetailCard report={report} />
}
