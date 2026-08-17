import type { AdminReviewsSearchParams } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'
import type { ModeratedReviewRow } from '@/lib/admin-moderation'
import type { AdminDataTableColumn } from '@/components/admin/admin-data-table'
import { Badge } from '@/components/ui/badge'
import { AdminDataTable } from '@/components/admin/admin-data-table'
import { DeleteReviewButton } from '@/components/admin/moderation/delete-review-button'
import { ReviewReportsDialog } from '@/components/admin/moderation/review-reports-dialog'
import { formatAdminDateTime } from '@/lib/admin-format'

const baseColumns: Array<AdminDataTableColumn<ModeratedReviewRow>> = [
  {
    id: 'content',
    header: 'Review',
    cell: (review) => (
      <div className="max-w-md">
        <p className="line-clamp-3">{review.content}</p>
        {review.haveSpoilers && (
          <Badge variant="outline" className="mt-1">
            Spoilers
          </Badge>
        )}
      </div>
    ),
  },
  {
    id: 'author',
    header: 'Author',
    className: 'w-36',
    cell: (review) => review.authorUsername ?? '—',
  },
  {
    id: 'game',
    header: 'Game',
    className: 'w-44',
    cell: (review) => (
      <span className="text-muted-foreground">{review.gameTitle ?? '—'}</span>
    ),
  },
  {
    id: 'createdAt',
    header: 'Posted',
    className: 'w-44',
    cell: (review) => (
      <span className="text-muted-foreground">
        {formatAdminDateTime(review.createdAt)}
      </span>
    ),
  },
]

const reportsColumn: AdminDataTableColumn<ModeratedReviewRow> = {
  id: 'reports',
  header: 'Reports',
  className: 'w-28',
  cell: (review) => <ReviewReportsDialog review={review} />,
}

const actionsColumn: AdminDataTableColumn<ModeratedReviewRow> = {
  id: 'actions',
  header: <span className="sr-only">Actions</span>,
  className: 'w-24 text-right',
  cell: (review) => (
    <div className="flex justify-end">
      <DeleteReviewButton review={review} />
    </div>
  ),
}

interface AdminReviewsTableProps {
  rows: Array<ModeratedReviewRow>
  metadata: PaginationMetadata
  search: AdminReviewsSearchParams
  isLoading?: boolean
}

export function AdminReviewsTable({
  rows,
  metadata,
  search,
  isLoading,
}: AdminReviewsTableProps) {
  // The reports drill-down is only meaningful where a count exists, which is
  // exactly the reported-only listing.
  const columns = search.reported
    ? [...baseColumns, reportsColumn, actionsColumn]
    : [...baseColumns, actionsColumn]

  return (
    <AdminDataTable
      caption={search.reported ? 'Reported reviews' : 'All reviews'}
      columns={columns}
      rows={rows}
      rowKey={(review) => review.id}
      page={search.page}
      metadata={metadata}
      linkProps={(page) => ({
        to: '/admin/moderation/reviews',
        search: { ...search, page },
      })}
      isLoading={isLoading}
      emptyMessage={
        search.reported
          ? 'No reported reviews. Nothing to look at.'
          : 'No reviews yet.'
      }
    />
  )
}
