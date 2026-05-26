import type { PaginationMetadata } from './game'

export type NotificationType =
  | 'FOLLOW'
  | 'LIKE_REVIEW'
  | 'LIKE_LIST'
  | 'LIKE_GAME'
  | 'COMMENT_REPLY'
  | 'LEVEL_UP'
  | 'BADGE_UNLOCKED'
  | 'MENTION'

export type NotificationFilter = 'all' | 'unread' | NotificationType

export const NOTIFICATION_TYPES: ReadonlyArray<NotificationType> = [
  'FOLLOW',
  'LIKE_REVIEW',
  'LIKE_LIST',
  'LIKE_GAME',
  'COMMENT_REPLY',
  'LEVEL_UP',
  'BADGE_UNLOCKED',
  'MENTION',
]

export const NOTIFICATION_FILTERS: ReadonlyArray<NotificationFilter> = [
  'all',
  'unread',
  ...NOTIFICATION_TYPES,
]

export const NOTIFICATION_FILTER_LABELS: Record<NotificationFilter, string> = {
  all: 'All',
  unread: 'Unread',
  FOLLOW: 'Follows',
  LIKE_REVIEW: 'Review likes',
  LIKE_LIST: 'List likes',
  LIKE_GAME: 'Game likes',
  COMMENT_REPLY: 'Replies',
  LEVEL_UP: 'Level ups',
  BADGE_UNLOCKED: 'Badges',
  MENTION: 'Mentions',
}

export interface Notification {
  id: string
  senderPseudo: string | null
  senderPicture: string | null
  type: NotificationType
  referenceId: string | null
  message: string
  isRead: boolean
  createdAt: string
}

export interface NotificationsResponse {
  content: Array<Notification>
  metadata: PaginationMetadata
}

export interface UnreadCountResponse {
  count: number
}
