import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { toast } from 'sonner'
import type { AdminReportDetail } from '@/types/admin'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { AdminConfirmButton } from '@/components/admin/admin-confirm-button'
import { AdminReportTypeBadge } from '@/components/admin/moderation/admin-report-type-badge'
import { formatAdminDateTime } from '@/lib/admin-format'
import {
  deleteAdminComment,
  deleteAdminReview,
  dismissAdminReport,
  invalidateModeratedContent,
} from '@/queries/admin/moderation'

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <div className="mt-1 text-sm">{value}</div>
    </div>
  )
}

export function AdminReportDetailCard({
  report,
}: {
  report: AdminReportDetail
}) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const targetLabel = report.type === 'review' ? 'review' : 'comment'

  // Both actions close the report, so both return to the queue.
  const backToQueue = async () => {
    await invalidateModeratedContent(queryClient)
    await navigate({
      to: '/admin/moderation/reports',
      search: { page: 1, type: 'all' },
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex flex-wrap items-center gap-2">
          Report on a <AdminReportTypeBadge type={report.type} />
        </CardTitle>
        <CardDescription>
          Filed by {report.reporterUsername ?? 'a deleted account'} on{' '}
          {formatAdminDateTime(report.createdAt)}
        </CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-5">
        <Row
          label="Reason given"
          value={
            report.reason ?? (
              <span className="text-muted-foreground">No reason given.</span>
            )
          }
        />

        <Row
          label={`Reported ${targetLabel}`}
          value={
            <div className="rounded-lg border bg-muted/40 p-3">
              <p className="mb-2 text-xs text-muted-foreground">
                by {report.targetAuthorUsername ?? 'a deleted account'}
              </p>
              <p className="whitespace-pre-wrap">
                {report.targetFullContent ?? (
                  <span className="text-muted-foreground">
                    This content has already been removed.
                  </span>
                )}
              </p>
            </div>
          }
        />

        <div className="flex flex-wrap items-center gap-2 border-t pt-4">
          <AdminConfirmButton
            label="Dismiss report"
            variant="outline"
            title="Dismiss this report?"
            description={`The report is deleted and the ${targetLabel} stays online. Use this when the content does not break the rules.`}
            mutationFn={() => dismissAdminReport(report.id)}
            onSuccess={async () => {
              toast.success('Report dismissed')
              await backToQueue()
            }}
          />

          <AdminConfirmButton
            label={`Remove ${targetLabel}`}
            disabled={report.targetId === null}
            disabledReason="This content has already been removed"
            title={`Remove this ${targetLabel}?`}
            description={`The ${targetLabel} is deleted permanently and disappears from the public pages. This cannot be undone.`}
            mutationFn={() =>
              report.type === 'review'
                ? deleteAdminReview(report.targetId!)
                : deleteAdminComment(report.targetId!)
            }
            onSuccess={async () => {
              toast.success(
                targetLabel === 'review' ? 'Review removed' : 'Comment removed',
              )
              await backToQueue()
            }}
          />
        </div>
      </CardContent>
    </Card>
  )
}
