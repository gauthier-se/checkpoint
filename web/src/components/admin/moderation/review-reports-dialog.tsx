import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import type { ModeratedReviewRow } from '@/lib/admin-moderation'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { formatAdminDateTime } from '@/lib/admin-format'
import { adminReviewReportsQueryOptions } from '@/queries/admin/moderation'

/**
 * Shows why a review was reported. There is no admin endpoint to fetch a single
 * review, so the review itself comes from the row that opened the dialog and
 * only its reports are fetched.
 */
export function ReviewReportsDialog({
  review,
}: {
  review: ModeratedReviewRow
}) {
  const [open, setOpen] = useState(false)
  const [page, setPage] = useState(1)

  const { data, isPending } = useQuery({
    ...adminReviewReportsQueryOptions(review.id, page),
    enabled: open,
  })

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next)
        if (!next) setPage(1)
      }}
    >
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          {review.reportCount === null
            ? 'Reports'
            : `${review.reportCount} report${review.reportCount === 1 ? '' : 's'}`}
        </Button>
      </DialogTrigger>

      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Reports on this review</DialogTitle>
          <DialogDescription>
            By {review.authorUsername ?? 'a deleted account'}
            {review.gameTitle ? ` on ${review.gameTitle}` : ''}
          </DialogDescription>
        </DialogHeader>

        <p className="max-h-32 overflow-y-auto rounded-lg border bg-muted/40 p-3 text-sm whitespace-pre-wrap">
          {review.content}
        </p>

        {isPending ? (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        ) : data && data.content.length > 0 ? (
          <>
            <ul className="flex max-h-64 flex-col gap-3 overflow-y-auto">
              {data.content.map((report) => (
                <li key={report.id} className="rounded-lg border p-3 text-sm">
                  <p className="text-xs text-muted-foreground">
                    {report.reporterUsername ?? 'A deleted account'} —{' '}
                    {formatAdminDateTime(report.createdAt)}
                  </p>
                  <p className="mt-1 whitespace-pre-wrap">
                    {report.reason ?? 'No reason given.'}
                  </p>
                </li>
              ))}
            </ul>

            {data.metadata.totalPages > 1 && (
              <div className="flex items-center justify-between text-sm">
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={!data.metadata.hasPrevious}
                  onClick={() => setPage((current) => current - 1)}
                >
                  Previous
                </Button>
                <span className="text-muted-foreground">
                  Page {data.metadata.page + 1} of {data.metadata.totalPages}
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={!data.metadata.hasNext}
                  onClick={() => setPage((current) => current + 1)}
                >
                  Next
                </Button>
              </div>
            )}
          </>
        ) : (
          <p className="py-6 text-center text-sm text-muted-foreground">
            No reports on this review.
          </p>
        )}
      </DialogContent>
    </Dialog>
  )
}
