import { useNavigate } from '@tanstack/react-router'
import type {
  AdminReportTypeFilter,
  AdminReportsSearchParams,
} from '@/types/admin'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const TYPE_OPTIONS: ReadonlyArray<{
  value: AdminReportTypeFilter
  label: string
}> = [
  { value: 'all', label: 'All reports' },
  { value: 'review', label: 'Reviews only' },
  { value: 'comment', label: 'Comments only' },
]

export function AdminReportsFilters({
  search,
}: {
  search: AdminReportsSearchParams
}) {
  const navigate = useNavigate()

  return (
    <div className="mb-4 flex flex-wrap items-center gap-2">
      <Select
        value={search.type}
        onValueChange={(value) =>
          void navigate({
            to: '/admin/moderation/reports',
            // A different filter invalidates the current page number.
            search: { type: value as AdminReportTypeFilter, page: 1 },
            replace: true,
          })
        }
      >
        <SelectTrigger className="w-48" aria-label="Filter by reported content">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {TYPE_OPTIONS.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
