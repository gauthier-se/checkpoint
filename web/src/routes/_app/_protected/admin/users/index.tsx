import { Suspense, useMemo } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import type { AdminUserStatus, AdminUsersSearchParams } from '@/types/admin'
import { AdminPageHeader } from '@/components/admin/admin-page-header'
import { AdminUsersFilters } from '@/components/admin/users/admin-users-filters'
import { AdminUsersTable } from '@/components/admin/users/admin-users-table'
import { Skeleton } from '@/components/ui/skeleton'
import { filterAdminUsers, paginateAdminUsers } from '@/lib/admin-users'
import { parseTrimmedString } from '@/lib/search-params'
import { seo } from '@/lib/seo'
import { adminUsersQueryOptions } from '@/queries/admin/users'

const PAGE_SIZE = 20

const VALID_STATUSES: ReadonlyArray<AdminUserStatus> = [
  'all',
  'active',
  'banned',
]

function parseStatus(value: unknown): AdminUserStatus {
  const status = parseTrimmedString(value)
  return status && (VALID_STATUSES as ReadonlyArray<string>).includes(status)
    ? (status as AdminUserStatus)
    : 'all'
}

export const Route = createFileRoute('/_app/_protected/admin/users/')({
  head: () => ({
    meta: seo({ title: 'Users — Admin — Checkpoint' }),
  }),
  validateSearch: (
    search: Record<string, unknown>,
  ): AdminUsersSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    q: parseTrimmedString(search.q),
    status: parseStatus(search.status),
  }),
  loader: ({ context }) => {
    void context.queryClient.prefetchQuery(adminUsersQueryOptions())
  },
  component: AdminUsersPage,
})

function AdminUsersPage() {
  const search = Route.useSearch()

  return (
    <>
      <AdminPageHeader
        title="Users"
        description="Search accounts, inspect their activity, ban and unban."
      />
      <AdminUsersFilters search={search} />
      <Suspense fallback={<UsersTableSkeleton />}>
        <AdminUsersContent />
      </Suspense>
    </>
  )
}

function AdminUsersContent() {
  const search = Route.useSearch()
  const { data: users } = useSuspenseQuery(adminUsersQueryOptions())

  // The endpoint returns every account in one payload, so the filtering and
  // paging the other admin sections delegate to the API happen here.
  const page = useMemo(() => {
    const filtered = filterAdminUsers(users, search)
    return paginateAdminUsers(filtered, search.page, PAGE_SIZE)
  }, [users, search])

  return (
    <AdminUsersTable
      rows={page.rows}
      metadata={page.metadata}
      search={search}
    />
  )
}

function UsersTableSkeleton() {
  return (
    <div className="flex flex-col gap-2 rounded-lg border p-4">
      {Array.from({ length: 8 }, (_, index) => (
        <Skeleton key={index} className="h-9 w-full" />
      ))}
    </div>
  )
}
