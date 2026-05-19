package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.XpGrant;
import com.checkpoint.api.enums.XpEventType;
import com.checkpoint.api.events.UserLeveledUpEvent;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.XpGrantRepository;

/**
 * Unit tests for {@link GamificationServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class GamificationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private XpGrantRepository xpGrantRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GamificationServiceImpl gamificationService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        gamificationService = new GamificationServiceImpl(userRepository, xpGrantRepository, eventPublisher);

        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setXpPoint(0);
        testUser.setLevel(1);
    }

    @Nested
    @DisplayName("addXp()")
    class AddXp {

        @Test
        @DisplayName("Should add XP to user without leveling up")
        void addXp_shouldAddXpWithoutLevelUp() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.addXp(userId, 50);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getXpPoint()).isEqualTo(50);
            assertThat(savedUser.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should level up when XP reaches threshold")
        void addXp_shouldLevelUpWhenThresholdReached() {
            testUser.setXpPoint(950);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.addXp(userId, 50);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getXpPoint()).isEqualTo(1000);
            assertThat(savedUser.getLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle multiple level ups at once")
        void addXp_shouldHandleMultipleLevelUps() {
            testUser.setXpPoint(900);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.addXp(userId, 2100);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getXpPoint()).isEqualTo(3000);
            assertThat(savedUser.getLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should not level up when XP is below threshold")
        void addXp_shouldNotLevelUpBelowThreshold() {
            testUser.setXpPoint(800);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.addXp(userId, 100);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getXpPoint()).isEqualTo(900);
            assertThat(savedUser.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should level up correctly at higher levels")
        void addXp_shouldLevelUpCorrectlyAtHigherLevels() {
            testUser.setXpPoint(1900);
            testUser.setLevel(2);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.addXp(userId, 100);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getXpPoint()).isEqualTo(2000);
            assertThat(savedUser.getLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should throw when user not found")
        void addXp_shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gamificationService.addXp(userId, 50))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("Should publish UserLeveledUpEvent with the new level when leveling up")
        void addXp_shouldPublishLevelUpEvent() {
            testUser.setXpPoint(950);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.addXp(userId, 50);

            ArgumentCaptor<UserLeveledUpEvent> eventCaptor =
                    ArgumentCaptor.forClass(UserLeveledUpEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
            assertThat(eventCaptor.getValue().getNewLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should not publish UserLeveledUpEvent when no level-up occurs")
        void addXp_shouldNotPublishWhenNoLevelUp() {
            testUser.setXpPoint(100);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.addXp(userId, 50);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("awardXp()")
    class AwardXp {

        @Test
        @DisplayName("Should insert audit row and award XP on first grant")
        void awardXp_shouldInsertAndAwardOnFirstGrant() {
            UUID targetId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.awardXp(userId, 10, XpEventType.USER_FOLLOWED, targetId);

            ArgumentCaptor<XpGrant> grantCaptor = ArgumentCaptor.forClass(XpGrant.class);
            verify(xpGrantRepository).saveAndFlush(grantCaptor.capture());
            XpGrant savedGrant = grantCaptor.getValue();
            assertThat(savedGrant.getUser()).isSameAs(testUser);
            assertThat(savedGrant.getEventType()).isEqualTo(XpEventType.USER_FOLLOWED);
            assertThat(savedGrant.getTargetId()).isEqualTo(targetId);
            assertThat(savedGrant.getXpAmount()).isEqualTo(10);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getXpPoint()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should skip XP update when the audit insert violates the unique key")
        void awardXp_shouldSkipOnDuplicate() {
            UUID targetId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(xpGrantRepository.saveAndFlush(any(XpGrant.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_xp_grants_user_event_target"));

            gamificationService.awardXp(userId, 10, XpEventType.USER_FOLLOWED, targetId);

            verify(userRepository, never()).save(any(User.class));
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should publish UserLeveledUpEvent when the awarded XP crosses a threshold")
        void awardXp_shouldPublishLevelUpEvent() {
            testUser.setXpPoint(995);
            UUID targetId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            gamificationService.awardXp(userId, 10, XpEventType.GAME_RATED, targetId);

            ArgumentCaptor<UserLeveledUpEvent> eventCaptor =
                    ArgumentCaptor.forClass(UserLeveledUpEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getNewLevel()).isEqualTo(2);
        }
    }
}
