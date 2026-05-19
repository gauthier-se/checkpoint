package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserLoginStreak;
import com.checkpoint.api.enums.XpEventType;
import com.checkpoint.api.repositories.UserLoginStreakRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.GamificationService;

/**
 * Unit tests for {@link LoginStreakServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class LoginStreakServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 19);

    @Mock
    private UserLoginStreakRepository streakRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GamificationService gamificationService;

    private LoginStreakServiceImpl streakService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                TODAY.atStartOfDay(ZoneOffset.UTC).plusHours(12).toInstant(),
                ZoneOffset.UTC);
        streakService = new LoginStreakServiceImpl(
                streakRepository, userRepository, gamificationService, fixedClock);

        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
    }

    @Nested
    @DisplayName("recordActivity()")
    class RecordActivity {

        @Test
        @DisplayName("Should start a new streak at day 1 and award 10 XP when no row exists")
        void recordActivity_shouldStartNewStreak() {
            when(streakRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            streakService.recordActivity(userId);

            ArgumentCaptor<UserLoginStreak> streakCaptor = ArgumentCaptor.forClass(UserLoginStreak.class);
            verify(streakRepository).save(streakCaptor.capture());
            UserLoginStreak saved = streakCaptor.getValue();
            assertThat(saved.getCurrentDay()).isEqualTo(1);
            assertThat(saved.getLastActivityDate()).isEqualTo(TODAY);

            verify(gamificationService).awardXp(userId, 10, XpEventType.STREAK_DAILY, null);
            verify(gamificationService, never()).awardXp(eq(userId), eq(100),
                    eq(XpEventType.STREAK_WEEKLY_MILESTONE), any());
        }

        @Test
        @DisplayName("Should continue an existing streak when last activity was yesterday")
        void recordActivity_shouldContinueStreak() {
            UserLoginStreak existing = streakWith(3, TODAY.minusDays(1));
            when(streakRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

            streakService.recordActivity(userId);

            assertThat(existing.getCurrentDay()).isEqualTo(4);
            assertThat(existing.getLastActivityDate()).isEqualTo(TODAY);
            // Day 4 -> 10 * min(4, 7) = 40
            verify(gamificationService).awardXp(userId, 40, XpEventType.STREAK_DAILY, null);
        }

        @Test
        @DisplayName("Should cap the daily bonus at 70 XP from day 7 onward")
        void recordActivity_shouldCapDailyBonusAtSevenDays() {
            UserLoginStreak existing = streakWith(9, TODAY.minusDays(1));
            when(streakRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

            streakService.recordActivity(userId);

            assertThat(existing.getCurrentDay()).isEqualTo(10);
            verify(gamificationService).awardXp(userId, 70, XpEventType.STREAK_DAILY, null);
        }

        @Test
        @DisplayName("Should reset to day 1 when the user skipped at least one full day")
        void recordActivity_shouldResetOnGap() {
            UserLoginStreak existing = streakWith(5, TODAY.minusDays(3));
            when(streakRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

            streakService.recordActivity(userId);

            assertThat(existing.getCurrentDay()).isEqualTo(1);
            verify(gamificationService).awardXp(userId, 10, XpEventType.STREAK_DAILY, null);
        }

        @Test
        @DisplayName("Should no-op when activity was already recorded today")
        void recordActivity_shouldNoOpSameDay() {
            UserLoginStreak existing = streakWith(4, TODAY);
            when(streakRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

            streakService.recordActivity(userId);

            verify(streakRepository, never()).save(any());
            verify(gamificationService, never()).awardXp(any(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("Should grant the weekly milestone bonus on every 7th consecutive day")
        void recordActivity_shouldGrantWeeklyMilestone() {
            UserLoginStreak existing = streakWith(6, TODAY.minusDays(1));
            when(streakRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

            streakService.recordActivity(userId);

            assertThat(existing.getCurrentDay()).isEqualTo(7);
            // Day 7 -> daily bonus 70 XP + weekly milestone 100 XP
            verify(gamificationService).awardXp(userId, 70, XpEventType.STREAK_DAILY, null);
            verify(gamificationService).awardXp(userId, 100, XpEventType.STREAK_WEEKLY_MILESTONE, null);
        }

        @Test
        @DisplayName("Should grant the weekly milestone again on day 14")
        void recordActivity_shouldGrantWeeklyMilestoneOnDay14() {
            UserLoginStreak existing = streakWith(13, TODAY.minusDays(1));
            when(streakRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

            streakService.recordActivity(userId);

            assertThat(existing.getCurrentDay()).isEqualTo(14);
            verify(gamificationService).awardXp(userId, 70, XpEventType.STREAK_DAILY, null);
            verify(gamificationService).awardXp(userId, 100, XpEventType.STREAK_WEEKLY_MILESTONE, null);
        }
    }

    private UserLoginStreak streakWith(int currentDay, LocalDate lastActivity) {
        UserLoginStreak streak = new UserLoginStreak(testUser);
        streak.setCurrentDay(currentDay);
        streak.setLastActivityDate(lastActivity);
        return streak;
    }
}
