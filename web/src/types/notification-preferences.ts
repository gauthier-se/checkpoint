export interface NotificationPreferences {
  followEnabled: boolean
  likeReviewEnabled: boolean
  likeListEnabled: boolean
  likeGameEnabled: boolean
  commentReplyEnabled: boolean
  levelUpEnabled: boolean
  badgeUnlockedEnabled: boolean
}

export type UpdateNotificationPreferences = Partial<NotificationPreferences>
