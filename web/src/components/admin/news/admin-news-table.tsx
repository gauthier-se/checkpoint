import type { AdminNews, AdminNewsSearchParams } from '@/types/admin'
import type { PaginationMetadata } from '@/types/game'
import type { AdminDataTableColumn } from '@/components/admin/admin-data-table'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { AdminDataTable } from '@/components/admin/admin-data-table'
import { DeleteNewsButton } from '@/components/admin/news/delete-news-button'
import { NewsEditorDialog } from '@/components/admin/news/news-editor-dialog'
import { NewsStatusBadge } from '@/components/admin/news/news-status-badge'
import { PublishNewsButton } from '@/components/admin/news/publish-news-button'
import { formatAdminDateTime } from '@/lib/admin-format'
import { isImported } from '@/lib/admin-news'

const columns: Array<AdminDataTableColumn<AdminNews>> = [
  {
    id: 'title',
    header: 'Title',
    cell: (article) => (
      <div className="max-w-md">
        <p className="font-medium">{article.title}</p>
        {article.description && (
          <p className="line-clamp-2 text-sm text-muted-foreground">
            {article.description}
          </p>
        )}
      </div>
    ),
  },
  {
    id: 'status',
    header: 'Status',
    className: 'w-28',
    cell: (article) => <NewsStatusBadge article={article} />,
  },
  {
    id: 'source',
    header: 'Source',
    className: 'w-36',
    cell: (article) => (
      <div className="flex flex-col gap-1">
        <Badge variant="outline">{article.source}</Badge>
        {article.feedName && (
          <span className="text-xs text-muted-foreground">
            {article.feedName}
          </span>
        )}
      </div>
    ),
  },
  {
    id: 'createdAt',
    header: 'Created',
    className: 'w-44',
    cell: (article) => (
      <span className="text-muted-foreground">
        {formatAdminDateTime(article.createdAt)}
      </span>
    ),
  },
  {
    id: 'actions',
    header: <span className="sr-only">Actions</span>,
    className: 'w-64 text-right',
    cell: (article) => (
      <div className="flex items-center justify-end gap-2">
        <PublishNewsButton article={article} />
        {/* Imported articles belong to their feed: re-importing would undo any
            edit, so only manual ones are editable. */}
        {!isImported(article) && (
          <NewsEditorDialog
            article={article}
            trigger={
              <Button variant="outline" size="sm">
                Edit
              </Button>
            }
          />
        )}
        <DeleteNewsButton article={article} />
      </div>
    ),
  },
]

interface AdminNewsTableProps {
  rows: Array<AdminNews>
  metadata: PaginationMetadata
  search: AdminNewsSearchParams
  isLoading?: boolean
}

export function AdminNewsTable({
  rows,
  metadata,
  search,
  isLoading,
}: AdminNewsTableProps) {
  return (
    <AdminDataTable
      caption="News articles"
      columns={columns}
      rows={rows}
      rowKey={(article) => article.id}
      page={search.page}
      metadata={metadata}
      linkProps={(page) => ({ to: '/admin/news', search: { page } })}
      isLoading={isLoading}
      emptyMessage="No articles yet. Write one or import a feed."
    />
  )
}
