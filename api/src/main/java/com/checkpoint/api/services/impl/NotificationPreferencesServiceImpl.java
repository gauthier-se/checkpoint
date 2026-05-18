package com.checkpoint.api.services.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.notification.NotificationPreferencesDto;
import com.checkpoint.api.dto.notification.UpdateNotificationPreferencesDto;
import com.checkpoint.api.entities.NotificationPreferences;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.mapper.NotificationPreferencesMapper;
import com.checkpoint.api.repositories.NotificationPreferencesRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.NotificationPreferencesService;

/**
 * Implementation of {@link NotificationPreferencesService}.
 */
@Service
@Transactional
public class NotificationPreferencesServiceImpl implements NotificationPreferencesService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferencesServiceImpl.class);

    private final NotificationPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;
    private final NotificationPreferencesMapper preferencesMapper;

    public NotificationPreferencesServiceImpl(NotificationPreferencesRepository preferencesRepository,
                                              UserRepository userRepository,
                                              NotificationPreferencesMapper preferencesMapper) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
        this.preferencesMapper = preferencesMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationPreferencesDto getOrCreate(String userEmail) {
        return preferencesMapper.toDto(loadOrCreate(userEmail));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationPreferencesDto update(String userEmail, UpdateNotificationPreferencesDto dto) {
        NotificationPreferences preferences = loadOrCreate(userEmail);

        if (dto.followEnabled() != null) {
            preferences.setFollowEnabled(dto.followEnabled());
        }
        if (dto.likeReviewEnabled() != null) {
            preferences.setLikeReviewEnabled(dto.likeReviewEnabled());
        }
        if (dto.likeListEnabled() != null) {
            preferences.setLikeListEnabled(dto.likeListEnabled());
        }
        if (dto.likeGameEnabled() != null) {
            preferences.setLikeGameEnabled(dto.likeGameEnabled());
        }
        if (dto.commentReplyEnabled() != null) {
            preferences.setCommentReplyEnabled(dto.commentReplyEnabled());
        }
        if (dto.levelUpEnabled() != null) {
            preferences.setLevelUpEnabled(dto.levelUpEnabled());
        }
        if (dto.badgeUnlockedEnabled() != null) {
            preferences.setBadgeUnlockedEnabled(dto.badgeUnlockedEnabled());
        }

        NotificationPreferences saved = preferencesRepository.save(preferences);
        log.info("Notification preferences updated for user {}", userEmail);

        return preferencesMapper.toDto(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(UUID recipientId, NotificationType type) {
        return preferencesRepository.findByUserId(recipientId)
                .map(prefs -> prefs.isEnabled(type))
                .orElse(true);
    }

    private NotificationPreferences loadOrCreate(String userEmail) {
        return preferencesRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
                    log.info("Creating default notification preferences for user {}", userEmail);
                    return preferencesRepository.save(new NotificationPreferences(user));
                });
    }
}
