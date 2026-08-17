import { useEffect, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Search, X } from 'lucide-react'
import type { AdminUserStatus, AdminUsersSearchParams } from '@/types/admin'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const STATUS_OPTIONS: ReadonlyArray<{
  value: AdminUserStatus
  label: string
}> = [
  { value: 'all', label: 'All accounts' },
  { value: 'active', label: 'Active only' },
  { value: 'banned', label: 'Banned only' },
]

interface AdminUsersFiltersProps {
  search: AdminUsersSearchParams
}

export function AdminUsersFilters({ search }: AdminUsersFiltersProps) {
  const navigate = useNavigate()
  const [query, setQuery] = useState(search.q ?? '')

  // Keep the input in sync with back/forward navigation and filter resets.
  useEffect(() => {
    setQuery(search.q ?? '')
  }, [search.q])

  function updateFilter(updates: Partial<AdminUsersSearchParams>) {
    void navigate({
      to: '/admin/users',
      // Any filter change invalidates the current page number.
      search: { ...search, ...updates, page: 1 },
      replace: true,
    })
  }

  function applyQuery() {
    const trimmed = query.trim()
    const next = trimmed === '' ? undefined : trimmed
    if (next !== search.q) {
      updateFilter({ q: next })
    }
  }

  const hasFilters = search.q !== undefined || search.status !== 'all'

  return (
    <div className="mb-4 flex flex-wrap items-center gap-2">
      <div className="relative min-w-56 flex-1">
        <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onBlur={applyQuery}
          onKeyDown={(event) => {
            if (event.key === 'Enter') applyQuery()
          }}
          placeholder="Search by username or email"
          aria-label="Search users"
          className="pl-8"
        />
      </div>

      <Select
        value={search.status}
        onValueChange={(value) =>
          updateFilter({ status: value as AdminUserStatus })
        }
      >
        <SelectTrigger className="w-44" aria-label="Filter by status">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {STATUS_OPTIONS.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {hasFilters && (
        <Button
          variant="ghost"
          onClick={() => {
            setQuery('')
            updateFilter({ q: undefined, status: 'all' })
          }}
        >
          <X className="size-4" />
          Clear
        </Button>
      )}
    </div>
  )
}
