import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { Notification } from '@/types/notification'
import {
  NotificationItem,
  getNotificationHref,
} from '@/components/notifications/notification-item'

function makeNotification(overrides: Partial<Notification> = {}): Notification {
  return {
    id: 'n-1',
    senderPseudo: null,
    senderPicture: null,
    type: 'LEVEL_UP',
    referenceId: null,
    message: 'You reached level 5!',
    isRead: false,
    createdAt: new Date().toISOString(),
    ...overrides,
  }
}

describe('NotificationItem', () => {
  it('renders the system icon for LEVEL_UP instead of an avatar', () => {
    render(
      <NotificationItem
        notification={makeNotification({ type: 'LEVEL_UP' })}
        onClick={vi.fn()}
      />,
    )

    expect(screen.getByTestId('notification-icon')).toBeInTheDocument()
    expect(screen.getByText('You reached level 5!')).toBeInTheDocument()
  })

  it('renders the system icon for BADGE_UNLOCKED', () => {
    render(
      <NotificationItem
        notification={makeNotification({
          type: 'BADGE_UNLOCKED',
          message: 'You unlocked the badge: Rising Star',
          referenceId: 'badge-id',
        })}
        onClick={vi.fn()}
      />,
    )

    expect(screen.getByTestId('notification-icon')).toBeInTheDocument()
    expect(
      screen.getByText('You unlocked the badge: Rising Star'),
    ).toBeInTheDocument()
  })

  it('renders the sender avatar (not a system icon) for FOLLOW', () => {
    render(
      <NotificationItem
        notification={makeNotification({
          type: 'FOLLOW',
          senderPseudo: 'alice',
          message: 'alice started following you',
        })}
        onClick={vi.fn()}
      />,
    )

    expect(screen.queryByTestId('notification-icon')).toBeNull()
    expect(screen.getByText('alice started following you')).toBeInTheDocument()
  })
})

describe('getNotificationHref', () => {
  it('returns /profile/{username} for LEVEL_UP when a username is known', () => {
    const href = getNotificationHref(
      makeNotification({ type: 'LEVEL_UP' }),
      'bob',
    )
    expect(href).toBe('/profile/bob')
  })

  it('returns /profile/{username}/badges for BADGE_UNLOCKED', () => {
    const href = getNotificationHref(
      makeNotification({ type: 'BADGE_UNLOCKED' }),
      'bob',
    )
    expect(href).toBe('/profile/bob/badges')
  })

  it('falls back to /profile when the username is null', () => {
    expect(
      getNotificationHref(makeNotification({ type: 'LEVEL_UP' }), null),
    ).toBe('/profile')
    expect(
      getNotificationHref(makeNotification({ type: 'BADGE_UNLOCKED' }), null),
    ).toBe('/profile')
  })

  it('preserves existing routes for non-progression types', () => {
    expect(
      getNotificationHref(
        makeNotification({ type: 'FOLLOW', senderPseudo: 'alice' }),
        'bob',
      ),
    ).toBe('/profile/alice')
    expect(
      getNotificationHref(
        makeNotification({ type: 'LIKE_REVIEW', referenceId: 'game-1' }),
        'bob',
      ),
    ).toBe('/games/game-1')
  })
})
