package com.checkpoint.api.listeners;

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
import org.springframework.context.ApplicationEventPublisher;

import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.enums.BadgeCode;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.events.BadgeUnlockedEvent;
import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.events.UserLeveledUpEvent;
import com.checkpoint.api.repositories.BadgeRepository;

/**
 * Unit tests for {@link ProgressionNotificationListener}.
 */
@ExtendWith(MockitoExtension.class)
class ProgressionNotificationListenerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BadgeRepository badgeRepository;

    private ProgressionNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new ProgressionNotificationListener(eventPublisher, badgeRepository);
    }

    @Nested
    @DisplayName("onUserLeveledUp()")
    class OnUserLeveledUp {

        @Test
        @DisplayName("Should publish a LEVEL_UP NotificationEvent with the new level in the message")
        void shouldPublishLevelUpNotification() {
            // Given
            UUID userId = UUID.randomUUID();
            int newLevel = 7;
            UserLeveledUpEvent event = new UserLeveledUpEvent(userId, newLevel);

            // When
            listener.onUserLeveledUp(event);

            // Then
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            NotificationEvent published = captor.getValue();
            assertThat(published.getRecipientId()).isEqualTo(userId);
            assertThat(published.getSenderId()).isNull();
            assertThat(published.getType()).isEqualTo(NotificationType.LEVEL_UP);
            assertThat(published.getReferenceId()).isNull();
            assertThat(published.getMessage()).isEqualTo("You reached level 7!");
        }
    }

    @Nested
    @DisplayName("onBadgeUnlocked()")
    class OnBadgeUnlocked {

        @Test
        @DisplayName("Should publish a BADGE_UNLOCKED NotificationEvent with the badge id and name")
        void shouldPublishBadgeUnlockedNotification() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID badgeId = UUID.randomUUID();
            Badge badge = new Badge(BadgeCode.LEVEL_5.name(), "Rising Star");
            badge.setId(badgeId);
            when(badgeRepository.findByCode(BadgeCode.LEVEL_5.name())).thenReturn(Optional.of(badge));

            BadgeUnlockedEvent event = new BadgeUnlockedEvent(userId, BadgeCode.LEVEL_5);

            // When
            listener.onBadgeUnlocked(event);

            // Then
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            NotificationEvent published = captor.getValue();
            assertThat(published.getRecipientId()).isEqualTo(userId);
            assertThat(published.getSenderId()).isNull();
            assertThat(published.getType()).isEqualTo(NotificationType.BADGE_UNLOCKED);
            assertThat(published.getReferenceId()).isEqualTo(badgeId);
            assertThat(published.getMessage()).isEqualTo("You unlocked the badge: Rising Star");
        }

        @Test
        @DisplayName("Should not publish anything when the badge is missing from the catalog")
        void shouldSkipWhenBadgeMissing() {
            // Given
            UUID userId = UUID.randomUUID();
            when(badgeRepository.findByCode(BadgeCode.FIRST_REVIEW.name())).thenReturn(Optional.empty());

            BadgeUnlockedEvent event = new BadgeUnlockedEvent(userId, BadgeCode.FIRST_REVIEW);

            // When
            listener.onBadgeUnlocked(event);

            // Then
            verify(eventPublisher, never()).publishEvent(any(Object.class));
        }
    }
}
