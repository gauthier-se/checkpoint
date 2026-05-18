import { useEffect, useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, CheckCheck } from 'lucide-react'
import type { Notification } from '@/types/notification'
import { Button } from '@/components/ui/button'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from '@/components/ui/drawer'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { KbdHint } from '@/components/ui/kbd'
import { Separator } from '@/components/ui/separator'
import {
  NotificationItem,
  getNotificationHref,
} from '@/components/notifications/notification-item'
import { useAuth } from '@/hooks/use-auth'
import {
  markAllNotificationsAsRead,
  markNotificationAsRead,
  notificationsQueryOptions,
  unreadCountQueryOptions,
} from '@/queries/notifications'

function useIsMobile(breakpoint = 640) {
  const [isMobile, setIsMobile] = useState(false)

  useEffect(() => {
    if (typeof window === 'undefined') return

    const mql = window.matchMedia(`(max-width: ${breakpoint - 1}px)`)
    setIsMobile(mql.matches)
    const handler = (e: MediaQueryListEvent) => setIsMobile(e.matches)
    mql.addEventListener('change', handler)
    return () => mql.removeEventListener('change', handler)
  }, [breakpoint])

  return isMobile
}

function NotificationPanel({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const [allNotifications, setAllNotifications] = useState<Array<Notification>>(
    [],
  )

  const { data, isLoading } = useQuery(
    notificationsQueryOptions({ page, size: 20 }),
  )
  const { data: unreadData } = useQuery(unreadCountQueryOptions())

  useEffect(() => {
    if (data) {
      setAllNotifications((prev) => {
        if (page === 0) return data.content
        const existingIds = new Set(prev.map((n) => n.id))
        const newItems = data.content.filter((n) => !existingIds.has(n.id))
        return [...prev, ...newItems]
      })
    }
  }, [data, page])

  const markReadMutation = useMutation({
    mutationFn: markNotificationAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const markAllReadMutation = useMutation({
    mutationFn: markAllNotificationsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      setAllNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
    },
  })

  const handleItemClick = (notification: Notification) => {
    if (!notification.isRead) {
      markReadMutation.mutate(notification.id)
    }
    onClose()
    navigate({ to: getNotificationHref(notification, user?.username ?? null) })
  }

  const hasUnread = (unreadData?.count ?? 0) > 0

  return (
    <div className="flex max-h-[70vh] flex-col">
      <div className="flex items-center justify-between px-4 py-3">
        <h3 className="font-semibold">Notifications</h3>
        {hasUnread && (
          <Button
            variant="ghost"
            size="sm"
            className="h-auto px-2 py-1 text-xs"
            onClick={() => markAllReadMutation.mutate()}
            disabled={markAllReadMutation.isPending}
          >
            <CheckCheck className="mr-1 size-3.5" />
            Mark all as read
          </Button>
        )}
      </div>
      <Separator />
      <div className="flex-1 overflow-y-auto px-1 py-1">
        {isLoading && page === 0 ? (
          <p className="px-3 py-6 text-center text-sm text-muted-foreground">
            Loading...
          </p>
        ) : allNotifications.length === 0 ? (
          <div className="flex flex-col items-center gap-2 px-3 py-8">
            <Bell className="size-8 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">
              No notifications yet
            </p>
          </div>
        ) : (
          <>
            {allNotifications.map((notification) => (
              <NotificationItem
                key={notification.id}
                notification={notification}
                onClick={() => handleItemClick(notification)}
              />
            ))}
            {data?.metadata.hasNext && (
              <div className="px-3 py-2 text-center">
                <Button
                  variant="ghost"
                  size="sm"
                  className="w-full text-xs"
                  onClick={() => setPage((prev) => prev + 1)}
                  disabled={isLoading}
                >
                  Load more
                </Button>
              </div>
            )}
          </>
        )}
      </div>
      <Separator />
      <div className="px-2 py-1.5">
        <Button
          asChild
          variant="ghost"
          size="sm"
          className="w-full text-xs"
          onClick={onClose}
        >
          <Link to="/notifications" search={{ page: 1, filter: 'all' }}>
            View all notifications
          </Link>
        </Button>
      </div>
    </div>
  )
}

export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const { data: unreadData } = useQuery(unreadCountQueryOptions())
  const isMobile = useIsMobile()

  const unreadCount = unreadData?.count ?? 0

  const bellButton = (
    <Button variant="ghost" size="icon" className="relative size-9">
      <Bell className="size-5" />
      {unreadCount > 0 && (
        <span className="absolute -right-0.5 -top-0.5 flex size-4 items-center justify-center rounded-full bg-destructive text-[10px] font-bold text-destructive-foreground">
          {unreadCount > 99 ? '99+' : unreadCount}
        </span>
      )}
    </Button>
  )

  const handleClose = () => setOpen(false)

  if (isMobile) {
    return (
      <Drawer open={open} onOpenChange={setOpen}>
        <DrawerTrigger asChild>{bellButton}</DrawerTrigger>
        <DrawerContent>
          <DrawerHeader className="sr-only">
            <DrawerTitle>Notifications</DrawerTitle>
          </DrawerHeader>
          <NotificationPanel onClose={handleClose} />
        </DrawerContent>
      </Drawer>
    )
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <Tooltip>
        <TooltipTrigger asChild>
          <PopoverTrigger asChild>{bellButton}</PopoverTrigger>
        </TooltipTrigger>
        <TooltipContent className="flex items-center gap-2">
          <span>Notifications</span>
          <KbdHint keys={['G', 'N']} />
        </TooltipContent>
      </Tooltip>
      <PopoverContent className="w-96 p-0" align="end" sideOffset={8}>
        <NotificationPanel onClose={handleClose} />
      </PopoverContent>
    </Popover>
  )
}
