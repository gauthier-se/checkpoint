import { Suspense } from 'react'
import { Link, createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { AdminUserDetailCard } from '@/components/admin/users/admin-user-detail-card'
import { AdminUserModerationCard } from '@/components/admin/users/admin-user-moderation-card'
import { ErrorPage } from '@/components/errors/error-page'
import { Skeleton } from '@/components/ui/skeleton'
import { seo } from '@/lib/seo'
import { adminUserQueryOptions } from '@/queries/admin/users'
import { isApiError } from '@/services/api'

export const Route = createFileRoute('/_app/_protected/admin/users/$userId')({
  head: () => ({
    meta: seo({ title: 'User — Admin — Checkpoint' }),
  }),
  loader: ({ context, params }) => {
    void context.queryClient.prefetchQuery(adminUserQueryOptions(params.userId))
  },
  errorComponent: ({ error, reset }) => (
    <ErrorPage
      status={isApiError(error) ? error.status : undefined}
      message={isApiError(error) ? error.message : undefined}
      onRetry={reset}
    />
  ),
  component: AdminUserDetailPage,
})

function AdminUserDetailPage() {
  return (
    <>
      <Link
        to="/admin/users"
        search={{ page: 1, status: 'all' }}
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to users
      </Link>

      <Suspense fallback={<UserDetailSkeleton />}>
        <AdminUserDetailContent />
      </Suspense>
    </>
  )
}

function AdminUserDetailContent() {
  const { userId } = Route.useParams()
  const { data: user } = useSuspenseQuery(adminUserQueryOptions(userId))

  return (
    <div className="flex flex-col gap-6">
      <AdminUserDetailCard user={user} />
      <AdminUserModerationCard user={user} />
    </div>
  )
}

function UserDetailSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <Skeleton className="h-64 w-full rounded-xl" />
      <Skeleton className="h-56 w-full rounded-xl" />
    </div>
  )
}
