import { formatDistanceToNow } from 'date-fns'
import { Award, Trophy } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { Notification, NotificationType } from '@/types/notification'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Checkbox } from '@/components/ui/checkbox'
import { resolvePictureUrl } from '@/lib/picture'
import { cn } from '@/lib/utils'

function getSystemIcon(type: NotificationType): LucideIcon | null {
  switch (type) {
    case 'LEVEL_UP':
      return Trophy
    case 'BADGE_UNLOCKED':
      return Award
    default:
      return null
  }
}

export function getNotificationHref(
  notification: Notification,
  currentUsername: string | null,
): string {
  switch (notification.type) {
    case 'FOLLOW':
    case 'MENTION':
      return `/profile/${notification.senderPseudo}`
    case 'LIKE_REVIEW':
    case 'COMMENT_REPLY':
    case 'LIKE_GAME':
      return `/games/${notification.referenceId}`
    case 'LIKE_LIST':
      return `/lists/${notification.referenceId}`
    case 'LEVEL_UP':
      return currentUsername ? `/profile/${currentUsername}` : '/profile'
    case 'BADGE_UNLOCKED':
      return currentUsername ? `/profile/${currentUsername}#badges` : '/profile'
    default:
      return '/'
  }
}

interface NotificationItemProps {
  notification: Notification
  onClick: () => void
  selectable?: boolean
  selected?: boolean
  onToggleSelect?: () => void
  className?: string
}

export function NotificationItem({
  notification,
  onClick,
  selectable = false,
  selected = false,
  onToggleSelect,
  className,
}: NotificationItemProps) {
  const timeAgo = formatDistanceToNow(new Date(notification.createdAt), {
    addSuffix: true,
  })

  const SystemIcon = getSystemIcon(notification.type)

  return (
    <div
      className={cn(
        'flex w-full items-start gap-3 rounded-md px-3 py-2.5 text-left transition-colors hover:bg-muted/50',
        !notification.isRead && 'bg-muted/30',
        className,
      )}
    >
      {selectable && (
        <div className="flex h-9 items-center">
          <Checkbox
            checked={selected}
            onCheckedChange={() => onToggleSelect?.()}
            aria-label="Select notification"
          />
        </div>
      )}
      <button
        type="button"
        onClick={onClick}
        className="flex flex-1 items-start gap-3 text-left"
      >
        {SystemIcon ? (
          <div
            data-testid="notification-icon"
            className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary"
          >
            <SystemIcon className="size-4" />
          </div>
        ) : (
          <Avatar className="size-8 shrink-0">
            {notification.senderPicture && (
              <AvatarImage
                src={resolvePictureUrl(notification.senderPicture)}
              />
            )}
            <AvatarFallback className="text-xs">
              {notification.senderPseudo?.charAt(0).toUpperCase() ?? '?'}
            </AvatarFallback>
          </Avatar>
        )}
        <div className="min-w-0 flex-1">
          <p className="text-sm leading-snug">{notification.message}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{timeAgo}</p>
        </div>
        {!notification.isRead && (
          <span className="mt-2 size-2 shrink-0 rounded-full bg-primary" />
        )}
      </button>
    </div>
  )
}
