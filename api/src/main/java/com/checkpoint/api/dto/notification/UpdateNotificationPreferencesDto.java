package com.checkpoint.api.dto.notification;

/**
 * Write DTO for partially updating a user's notification preferences.
 * All fields are nullable: a {@code null} value means the corresponding
 * preference is left unchanged.
 */
public record UpdateNotificationPreferencesDto(
        Boolean followEnabled,
        Boolean likeReviewEnabled,
        Boolean likeListEnabled,
        Boolean likeGameEnabled,
        Boolean commentReplyEnabled,
        Boolean levelUpEnabled,
        Boolean badgeUnlockedEnabled,
        Boolean mentionEnabled
) {}
