import { Link } from '@tanstack/react-router'
import type { AdminReport, AdminReportsSearchParams } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'
import type { AdminDataTableColumn } from '@/components/admin/admin-data-table'
import { AdminDataTable } from '@/components/admin/admin-data-table'
import { AdminReportTypeBadge } from '@/components/admin/moderation/admin-report-type-badge'
import { formatAdminDateTime } from '@/lib/admin-format'

const columns: Array<AdminDataTableColumn<AdminReport>> = [
  {
    id: 'type',
    header: 'Type',
    className: 'w-24',
    cell: (report) => <AdminReportTypeBadge type={report.type} />,
  },
  {
    id: 'content',
    header: 'Reported content',
    cell: (report) => (
      <p className="line-clamp-2 max-w-md">
        {report.contentPreview ?? (
          <span className="text-muted-foreground">Content unavailable</span>
        )}
      </p>
    ),
  },
  {
    id: 'reason',
    header: 'Reason',
    cell: (report) => (
      <p className="line-clamp-2 max-w-xs text-muted-foreground">
        {report.reason ?? '-'}
      </p>
    ),
  },
  {
    id: 'reporter',
    header: 'Reported by',
    className: 'w-36',
    cell: (report) => report.reporterUsername ?? '-',
  },
  {
    id: 'createdAt',
    header: 'Filed',
    className: 'w-44',
    cell: (report) => (
      <span className="text-muted-foreground">
        {formatAdminDateTime(report.createdAt)}
      </span>
    ),
  },
  {
    id: 'actions',
    header: <span className="sr-only">Actions</span>,
    className: 'w-24 text-right',
    cell: (report) => (
      <Link
        to="/admin/moderation/reports/$reportId"
        params={{ reportId: report.id }}
        className="text-sm underline-offset-4 hover:underline"
      >
        Open
      </Link>
    ),
  },
]

interface AdminReportsTableProps {
  rows: Array<AdminReport>
  metadata: PaginationMetadata
  search: AdminReportsSearchParams
  isLoading?: boolean
}

export function AdminReportsTable({
  rows,
  metadata,
  search,
  isLoading,
}: AdminReportsTableProps) {
  return (
    <AdminDataTable
      caption="Reports queue"
      columns={columns}
      rows={rows}
      rowKey={(report) => report.id}
      page={search.page}
      metadata={metadata}
      linkProps={(page) => ({
        to: '/admin/moderation/reports',
        search: { ...search, page },
      })}
      isLoading={isLoading}
      emptyMessage={
        search.type === 'all'
          ? 'The queue is empty. Nothing to moderate.'
          : 'No reports of this kind.'
      }
    />
  )
}
