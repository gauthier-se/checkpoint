import { useEffect } from 'react'
import { Outlet, createFileRoute, useNavigate } from '@tanstack/react-router'
import { useAuth } from '@/hooks/use-auth'
import { isAdmin, requireAdmin } from '@/lib/admin'
import { seo } from '@/lib/seo'
import { isApiError } from '@/services/api'
import { AdminNav } from '@/components/admin/admin-nav'
import { ErrorPage } from '@/components/errors/error-page'

export const Route = createFileRoute('/_app/_protected/admin')({
  head: () => ({
    meta: seo({ title: 'Admin — Checkpoint' }),
  }),
  beforeLoad: ({ context }) => requireAdmin(context.queryClient),
  errorComponent: ({ error, reset }) => (
    <ErrorPage
      status={isApiError(error) ? error.status : undefined}
      message={isApiError(error) ? error.message : undefined}
      onRetry={reset}
    />
  ),
  component: AdminLayout,
})

export function AdminLayout() {
  const { user, isLoading } = useAuth()
  const navigate = useNavigate()

  // Covers the SSR fallback in `requireAdmin`, and a role revoked mid-session:
  // the auth query refetches, the user is no longer an admin, and the panel
  // closes itself instead of firing requests the API will reject.
  useEffect(() => {
    if (!isLoading && user && !isAdmin(user)) {
      void navigate({ to: '/' })
    }
  }, [isLoading, user, navigate])

  if (isLoading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <p className="text-muted-foreground">Loading...</p>
      </div>
    )
  }

  // `_protected` holds the render until a user is known, so reaching this with
  // a non-admin means the role changed under us.
  if (!isAdmin(user)) {
    return <ErrorPage status={403} />
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:py-10">
      <div className="grid gap-8 md:grid-cols-[200px_1fr] lg:grid-cols-[250px_1fr]">
        <div className="flex flex-col gap-6">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">Admin</h1>
            <p className="text-sm text-muted-foreground">
              Moderate content, curate the catalog and monitor the platform
            </p>
          </div>
          <AdminNav />
        </div>

        <div className="flex min-w-0 flex-col">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
