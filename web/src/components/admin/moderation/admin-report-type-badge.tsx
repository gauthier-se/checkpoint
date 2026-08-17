import type { AdminReportType } from '@/types/admin'
import { Badge } from '@/components/ui/badge'

export function AdminReportTypeBadge({ type }: { type: AdminReportType }) {
  return (
    <Badge variant="outline">{type === 'review' ? 'Review' : 'Comment'}</Badge>
  )
}
