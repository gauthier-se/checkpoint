package com.checkpoint.api.dto.notification;

/**
 * Read DTO exposing a user's per-type notification preferences.
 * Each flag indicates whether notifications of the corresponding type
 * should be created and pushed to the user.
 */
public record NotificationPreferencesDto(
        Boolean followEnabled,
        Boolean likeReviewEnabled,
        Boolean likeListEnabled,
        Boolean likeGameEnabled,
        Boolean commentReplyEnabled,
        Boolean levelUpEnabled,
        Boolean badgeUnlockedEnabled
) {}
