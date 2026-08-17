import { Link } from '@tanstack/react-router'
import type { AdminUser, AdminUsersSearchParams } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'
import type { AdminDataTableColumn } from '@/components/admin/admin-data-table'
import { AdminDataTable } from '@/components/admin/admin-data-table'
import { AdminUserStatusBadge } from '@/components/admin/users/admin-user-status-badge'
import { BanUserButton } from '@/components/admin/users/ban-user-button'

interface AdminUsersTableProps {
  rows: Array<AdminUser>
  metadata: PaginationMetadata
  search: AdminUsersSearchParams
  isLoading?: boolean
}

const columns: Array<AdminDataTableColumn<AdminUser>> = [
  {
    id: 'username',
    header: 'Username',
    cell: (user) => (
      <Link
        to="/admin/users/$userId"
        params={{ userId: user.id }}
        className="font-medium underline-offset-4 hover:underline"
      >
        {user.username}
      </Link>
    ),
  },
  {
    id: 'email',
    header: 'Email',
    cell: (user) => <span className="text-muted-foreground">{user.email}</span>,
  },
  {
    id: 'status',
    header: 'Status',
    className: 'w-28',
    cell: (user) => <AdminUserStatusBadge banned={user.banned} />,
  },
  {
    id: 'actions',
    header: <span className="sr-only">Actions</span>,
    className: 'w-24 text-right',
    cell: (user) => (
      <div className="flex justify-end">
        <BanUserButton user={user} />
      </div>
    ),
  },
]

export function AdminUsersTable({
  rows,
  metadata,
  search,
  isLoading,
}: AdminUsersTableProps) {
  return (
    <AdminDataTable
      caption="User accounts"
      columns={columns}
      rows={rows}
      rowKey={(user) => user.id}
      page={metadata.page + 1}
      metadata={metadata}
      linkProps={(page) => ({
        to: '/admin/users',
        search: { ...search, page },
      })}
      isLoading={isLoading}
      emptyMessage={
        search.q || search.status !== 'all'
          ? 'No accounts match these filters.'
          : 'No accounts yet.'
      }
    />
  )
}
