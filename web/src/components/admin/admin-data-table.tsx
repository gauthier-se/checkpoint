import type { ReactNode } from 'react'
import type { LinkProps } from '@tanstack/react-router'
import type { PaginationMetadata } from '@/types/game'
import { cn } from '@/lib/utils'
import { PaginationNav } from '@/components/shared/pagination-nav'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

export interface AdminDataTableColumn<T> {
  /** Stable identifier, also used as the React key for the column. */
  id: string
  header: ReactNode
  cell: (row: T) => ReactNode
  /** Applied to both the header cell and the body cells of the column. */
  className?: string
}

interface AdminDataTableProps<T> {
  columns: Array<AdminDataTableColumn<T>>
  rows: Array<T>
  rowKey: (row: T) => string
  /** Current page, **1-based**: matches the route search params. */
  page?: number
  /** Pagination envelope returned by the endpoint; omit for unpaginated lists. */
  metadata?: PaginationMetadata
  /** Builds the router link props for a target page. Required to paginate. */
  linkProps?: (page: number) => LinkProps
  emptyMessage?: string
  isLoading?: boolean
  /** Placeholder row count while loading. */
  skeletonRows?: number
  caption?: string
}

/**
 * Table shared by the admin listings. The admin endpoints expose a uniform
 * `page` / `size` / `sort` contract, so pagination is wired here once rather
 * than in each section.
 */
export function AdminDataTable<T>({
  columns,
  rows,
  rowKey,
  page,
  metadata,
  linkProps,
  emptyMessage = 'Nothing to show.',
  isLoading = false,
  skeletonRows = 8,
  caption,
}: AdminDataTableProps<T>) {
  const showPagination =
    metadata !== undefined && linkProps !== undefined && page !== undefined

  return (
    <div>
      <div className="rounded-lg border">
        <Table>
          {caption && <caption className="sr-only">{caption}</caption>}
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead key={column.id} className={column.className}>
                  {column.header}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: skeletonRows }, (_, index) => (
                <TableRow key={`skeleton-${index}`}>
                  {columns.map((column) => (
                    <TableCell key={column.id} className={column.className}>
                      <Skeleton className="h-5 w-full" />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : rows.length === 0 ? (
              <TableRow className="hover:bg-transparent">
                <TableCell
                  colSpan={columns.length}
                  className="py-10 text-center text-muted-foreground"
                >
                  {emptyMessage}
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow key={rowKey(row)}>
                  {columns.map((column) => (
                    <TableCell
                      key={column.id}
                      className={cn('whitespace-normal', column.className)}
                    >
                      {column.cell(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {showPagination && (
        <PaginationNav
          page={page}
          totalPages={metadata.totalPages}
          hasNext={metadata.hasNext}
          hasPrevious={metadata.hasPrevious}
          linkProps={linkProps}
          hideWhenSinglePage
          className="mt-4"
        />
      )}
    </div>
  )
}
