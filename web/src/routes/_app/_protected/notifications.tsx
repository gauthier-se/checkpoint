import { useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, Bell, CheckCheck, X } from 'lucide-react'
import type {
  Notification,
  NotificationFilter,
  NotificationType,
} from '@/types/notification'
import {
  NOTIFICATION_FILTERS,
  NOTIFICATION_FILTER_LABELS,
} from '@/types/notification'
import {
  NotificationItem,
  getNotificationHref,
} from '@/components/notifications/notification-item'
import { Button } from '@/components/ui/button'
import { ButtonGroup } from '@/components/ui/button-group'
import { Separator } from '@/components/ui/separator'
import {
  markAllNotificationsAsRead,
  markBulkNotificationsAsRead,
  markNotificationAsRead,
  notificationsQueryOptions,
  unreadCountQueryOptions,
} from '@/queries/notifications'
import { useAuth } from '@/hooks/use-auth'
import { getPageNumbers } from '@/lib/pagination'
import { cn } from '@/lib/utils'

const PAGE_SIZE = 20

function isNotificationFilter(value: unknown): value is NotificationFilter {
  return (
    typeof value === 'string' &&
    (NOTIFICATION_FILTERS as ReadonlyArray<string>).includes(value)
  )
}

interface NotificationsSearch {
  page: number
  filter: NotificationFilter
}

function filterToQueryParams(filter: NotificationFilter): {
  type?: NotificationType
  isRead?: boolean
} {
  if (filter === 'all') return {}
  if (filter === 'unread') return { isRead: false }
  return { type: filter }
}

export const Route = createFileRoute('/_app/_protected/notifications')({
  validateSearch: (search: Record<string, unknown>): NotificationsSearch => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    filter: isNotificationFilter(search.filter) ? search.filter : 'all',
  }),
  loaderDeps: ({ search }) => search,
  loader: async ({ deps, context }) => {
    await context.queryClient.ensureQueryData(
      notificationsQueryOptions({
        page: deps.page - 1,
        size: PAGE_SIZE,
        ...filterToQueryParams(deps.filter),
      }),
    )
  },
  component: NotificationsPage,
})

function NotificationsPage() {
  const { page, filter } = Route.useSearch()
  const navigate = useNavigate({ from: '/notifications' })
  const queryClient = useQueryClient()
  const { user } = useAuth()

  const [selectMode, setSelectMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())

  const { data, isLoading, isFetching } = useQuery(
    notificationsQueryOptions({
      page: page - 1,
      size: PAGE_SIZE,
      ...filterToQueryParams(filter),
    }),
  )
  const { data: unreadData } = useQuery(unreadCountQueryOptions())

  const notifications = data?.content ?? []
  const totalPages = data?.metadata.totalPages ?? 0
  const hasNext = data?.metadata.hasNext ?? false
  const hasPrevious = data?.metadata.hasPrevious ?? false

  const markReadMutation = useMutation({
    mutationFn: markNotificationAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const markBulkMutation = useMutation({
    mutationFn: markBulkNotificationsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      setSelectedIds(new Set())
      setSelectMode(false)
    },
  })

  const markAllMutation = useMutation({
    mutationFn: markAllNotificationsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const handleFilterClick = (newFilter: NotificationFilter) => {
    setSelectedIds(new Set())
    navigate({ search: { filter: newFilter, page: 1 } })
  }

  const handleItemClick = (notification: Notification) => {
    if (!notification.isRead) {
      markReadMutation.mutate(notification.id)
    }
    if (
      notification.type === 'BADGE_UNLOCKED' ||
      notification.type === 'LEVEL_UP'
    ) {
      queryClient.removeQueries({
        queryKey: ['users', user?.username, 'profile'],
      })
    }
    navigate({ to: getNotificationHref(notification, user?.username ?? null) })
  }

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const toggleSelectMode = () => {
    setSelectMode((prev) => !prev)
    setSelectedIds(new Set())
  }

  const handleMarkSelected = () => {
    if (selectedIds.size === 0) return
    markBulkMutation.mutate(Array.from(selectedIds))
  }

  const hasUnread = (unreadData?.count ?? 0) > 0

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:py-10">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Notifications</h1>
          <p className="text-sm text-muted-foreground">
            {unreadData?.count ?? 0} unread
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {selectMode ? (
            <>
              <Button
                size="sm"
                onClick={handleMarkSelected}
                disabled={selectedIds.size === 0 || markBulkMutation.isPending}
              >
                <CheckCheck className="mr-1 size-4" />
                Mark selected as read ({selectedIds.size})
              </Button>
              <Button size="sm" variant="ghost" onClick={toggleSelectMode}>
                <X className="mr-1 size-4" />
                Cancel
              </Button>
            </>
          ) : (
            <>
              {hasUnread && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => markAllMutation.mutate()}
                  disabled={markAllMutation.isPending}
                >
                  <CheckCheck className="mr-1 size-4" />
                  Mark all as read
                </Button>
              )}
              <Button
                size="sm"
                variant="outline"
                onClick={toggleSelectMode}
                disabled={notifications.length === 0}
              >
                Select
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="mb-4 flex flex-wrap gap-2">
        {NOTIFICATION_FILTERS.map((f) => (
          <Button
            key={f}
            size="sm"
            variant={f === filter ? 'default' : 'outline'}
            onClick={() => handleFilterClick(f)}
            className="min-h-9"
          >
            {NOTIFICATION_FILTER_LABELS[f]}
          </Button>
        ))}
      </div>

      <Separator />

      <div
        className={cn(
          'mt-2 divide-y divide-border/50',
          isFetching && 'opacity-60',
        )}
      >
        {isLoading ? (
          <p className="px-3 py-8 text-center text-sm text-muted-foreground">
            Loading...
          </p>
        ) : notifications.length === 0 ? (
          <div className="flex flex-col items-center gap-2 px-3 py-12">
            <Bell className="size-10 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">
              No notifications to show
            </p>
          </div>
        ) : (
          notifications.map((notification) => (
            <NotificationItem
              key={notification.id}
              notification={notification}
              selectable={selectMode}
              selected={selectedIds.has(notification.id)}
              onToggleSelect={() => toggleSelect(notification.id)}
              onClick={() => handleItemClick(notification)}
            />
          ))
        )}
      </div>

      {totalPages > 1 && (
        <NotificationsPagination
          page={page}
          totalPages={totalPages}
          hasNext={hasNext}
          hasPrevious={hasPrevious}
          filter={filter}
        />
      )}
    </div>
  )
}

function NotificationsPagination({
  page,
  totalPages,
  hasNext,
  hasPrevious,
  filter,
}: {
  page: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
  filter: NotificationFilter
}) {
  return (
    <div className="mt-6 flex items-center justify-between">
      <Link
        to="/notifications"
        search={{ filter, page: page - 1 }}
        disabled={!hasPrevious}
      >
        <Button variant="outline" size="sm" disabled={!hasPrevious}>
          <ArrowLeft className="size-4" />
          Previous
        </Button>
      </Link>
      <ButtonGroup>
        {getPageNumbers(page, totalPages).map((p, i) =>
          p === '...' ? (
            <Button key={`ellipsis-${i}`} variant="outline" size="sm" disabled>
              ...
            </Button>
          ) : (
            <Link key={p} to="/notifications" search={{ filter, page: p }}>
              <Button variant={p === page ? 'default' : 'outline'} size="sm">
                {p}
              </Button>
            </Link>
          ),
        )}
      </ButtonGroup>
      <Link
        to="/notifications"
        search={{ filter, page: page + 1 }}
        disabled={!hasNext}
      >
        <Button variant="outline" size="sm" disabled={!hasNext}>
          Next
          <ArrowRight className="size-4" />
        </Button>
      </Link>
    </div>
  )
}
