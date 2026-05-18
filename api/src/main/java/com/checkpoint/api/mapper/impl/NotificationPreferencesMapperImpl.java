package com.checkpoint.api.mapper.impl;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.notification.NotificationPreferencesDto;
import com.checkpoint.api.entities.NotificationPreferences;
import com.checkpoint.api.mapper.NotificationPreferencesMapper;

/**
 * Implementation of {@link NotificationPreferencesMapper}.
 */
@Component
public class NotificationPreferencesMapperImpl implements NotificationPreferencesMapper {

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationPreferencesDto toDto(NotificationPreferences preferences) {
        if (preferences == null) {
            return null;
        }

        return new NotificationPreferencesDto(
                preferences.getFollowEnabled(),
                preferences.getLikeReviewEnabled(),
                preferences.getLikeListEnabled(),
                preferences.getLikeGameEnabled(),
                preferences.getCommentReplyEnabled(),
                preferences.getLevelUpEnabled(),
                preferences.getBadgeUnlockedEnabled()
        );
    }
}
