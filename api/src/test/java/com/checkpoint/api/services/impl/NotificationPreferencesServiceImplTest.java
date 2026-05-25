package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.dto.notification.NotificationPreferencesDto;
import com.checkpoint.api.dto.notification.UpdateNotificationPreferencesDto;
import com.checkpoint.api.entities.NotificationPreferences;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.mapper.NotificationPreferencesMapper;
import com.checkpoint.api.mapper.impl.NotificationPreferencesMapperImpl;
import com.checkpoint.api.repositories.NotificationPreferencesRepository;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Unit tests for {@link NotificationPreferencesServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferencesServiceImplTest {

    @Mock
    private NotificationPreferencesRepository preferencesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.checkpoint.api.services.OnboardingService onboardingService;

    private NotificationPreferencesMapper preferencesMapper;
    private NotificationPreferencesServiceImpl preferencesService;

    private User user;

    @BeforeEach
    void setUp() {
        preferencesMapper = new NotificationPreferencesMapperImpl();
        preferencesService = new NotificationPreferencesServiceImpl(
                preferencesRepository, userRepository, preferencesMapper, onboardingService);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPseudo("user");
    }

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        @Test
        @DisplayName("should return the existing preferences row when one exists")
        void getOrCreate_shouldReturnExisting() {
            // Given
            NotificationPreferences existing = new NotificationPreferences(user);
            existing.setFollowEnabled(false);
            existing.setLikeReviewEnabled(true);
            when(preferencesRepository.findByUserEmail("user@example.com"))
                    .thenReturn(Optional.of(existing));

            // When
            NotificationPreferencesDto result = preferencesService.getOrCreate("user@example.com");

            // Then
            assertThat(result.followEnabled()).isFalse();
            assertThat(result.likeReviewEnabled()).isTrue();
            verify(preferencesRepository, never()).save(any(NotificationPreferences.class));
        }

        @Test
        @DisplayName("should lazy-create defaults (all enabled) when no row exists")
        void getOrCreate_shouldCreateDefaultsWhenMissing() {
            // Given
            when(preferencesRepository.findByUserEmail("user@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(preferencesRepository.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            NotificationPreferencesDto result = preferencesService.getOrCreate("user@example.com");

            // Then
            assertThat(result.followEnabled()).isTrue();
            assertThat(result.likeReviewEnabled()).isTrue();
            assertThat(result.likeListEnabled()).isTrue();
            assertThat(result.likeGameEnabled()).isTrue();
            assertThat(result.commentReplyEnabled()).isTrue();
            assertThat(result.levelUpEnabled()).isTrue();
            assertThat(result.badgeUnlockedEnabled()).isTrue();

            ArgumentCaptor<NotificationPreferences> captor = ArgumentCaptor.forClass(NotificationPreferences.class);
            verify(preferencesRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(user);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should apply only non-null fields to the existing row")
        void update_shouldApplyOnlyNonNullFields() {
            // Given
            NotificationPreferences existing = new NotificationPreferences(user);
            existing.setFollowEnabled(true);
            existing.setLikeReviewEnabled(true);
            existing.setLikeListEnabled(true);
            existing.setLikeGameEnabled(true);
            existing.setCommentReplyEnabled(true);

            when(preferencesRepository.findByUserEmail("user@example.com"))
                    .thenReturn(Optional.of(existing));
            when(preferencesRepository.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UpdateNotificationPreferencesDto dto = new UpdateNotificationPreferencesDto(
                    false, null, null, null, false, null, null);

            // When
            NotificationPreferencesDto result = preferencesService.update("user@example.com", dto);

            // Then
            assertThat(result.followEnabled()).isFalse();
            assertThat(result.likeReviewEnabled()).isTrue();
            assertThat(result.likeListEnabled()).isTrue();
            assertThat(result.likeGameEnabled()).isTrue();
            assertThat(result.commentReplyEnabled()).isFalse();
            assertThat(result.levelUpEnabled()).isTrue();
            assertThat(result.badgeUnlockedEnabled()).isTrue();
        }

        @Test
        @DisplayName("should toggle the level-up and badge-unlocked flags independently")
        void update_shouldToggleProgressionFlags() {
            // Given
            NotificationPreferences existing = new NotificationPreferences(user);
            when(preferencesRepository.findByUserEmail("user@example.com"))
                    .thenReturn(Optional.of(existing));
            when(preferencesRepository.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UpdateNotificationPreferencesDto dto = new UpdateNotificationPreferencesDto(
                    null, null, null, null, null, false, false);

            // When
            NotificationPreferencesDto result = preferencesService.update("user@example.com", dto);

            // Then
            assertThat(result.followEnabled()).isTrue();
            assertThat(result.levelUpEnabled()).isFalse();
            assertThat(result.badgeUnlockedEnabled()).isFalse();
        }

        @Test
        @DisplayName("should lazy-create the row when missing before applying the update")
        void update_shouldLazyCreateThenApply() {
            // Given
            when(preferencesRepository.findByUserEmail("user@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(preferencesRepository.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UpdateNotificationPreferencesDto dto = new UpdateNotificationPreferencesDto(
                    false, null, null, null, null, null, null);

            // When
            NotificationPreferencesDto result = preferencesService.update("user@example.com", dto);

            // Then
            assertThat(result.followEnabled()).isFalse();
            assertThat(result.likeReviewEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabled {

        @Test
        @DisplayName("should return true when no preferences row exists for the recipient")
        void isEnabled_shouldReturnTrueWhenNoRow() {
            // Given
            UUID recipientId = UUID.randomUUID();
            when(preferencesRepository.findByUserId(recipientId)).thenReturn(Optional.empty());

            // When
            boolean enabled = preferencesService.isEnabled(recipientId, NotificationType.FOLLOW);

            // Then
            assertThat(enabled).isTrue();
        }

        @Test
        @DisplayName("should return the stored value when a preferences row exists")
        void isEnabled_shouldRespectStoredValue() {
            // Given
            UUID recipientId = UUID.randomUUID();
            NotificationPreferences prefs = new NotificationPreferences(user);
            prefs.setLikeReviewEnabled(false);
            when(preferencesRepository.findByUserId(recipientId)).thenReturn(Optional.of(prefs));

            // When
            boolean enabled = preferencesService.isEnabled(recipientId, NotificationType.LIKE_REVIEW);

            // Then
            assertThat(enabled).isFalse();
        }

        @Test
        @DisplayName("should gate LEVEL_UP and BADGE_UNLOCKED on their own flags")
        void isEnabled_shouldGateProgressionTypes() {
            // Given
            UUID recipientId = UUID.randomUUID();
            NotificationPreferences prefs = new NotificationPreferences(user);
            prefs.setLevelUpEnabled(false);
            prefs.setBadgeUnlockedEnabled(true);
            when(preferencesRepository.findByUserId(recipientId)).thenReturn(Optional.of(prefs));

            // Then
            assertThat(preferencesService.isEnabled(recipientId, NotificationType.LEVEL_UP)).isFalse();
            assertThat(preferencesService.isEnabled(recipientId, NotificationType.BADGE_UNLOCKED)).isTrue();
        }
    }
}
