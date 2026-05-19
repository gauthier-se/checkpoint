package com.checkpoint.api.services.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserLoginStreak;
import com.checkpoint.api.enums.XpEventType;
import com.checkpoint.api.repositories.UserLoginStreakRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.GamificationService;
import com.checkpoint.api.services.LoginStreakService;

/**
 * Implementation of {@link LoginStreakService}.
 *
 * <p>Day rollover is computed in UTC so streaks behave identically regardless
 * of the host time zone. The {@link Clock} is injected to make the date
 * boundary deterministic in tests.</p>
 */
@Service
@Transactional
public class LoginStreakServiceImpl implements LoginStreakService {

    private static final Logger log = LoggerFactory.getLogger(LoginStreakServiceImpl.class);

    private static final int DAILY_XP_PER_DAY = 10;
    private static final int DAILY_XP_DAY_CAP = 7;
    private static final int WEEKLY_MILESTONE_XP = 100;

    private final UserLoginStreakRepository streakRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final Clock clock;

    public LoginStreakServiceImpl(UserLoginStreakRepository streakRepository,
                                  UserRepository userRepository,
                                  GamificationService gamificationService,
                                  Clock clock) {
        this.streakRepository = streakRepository;
        this.userRepository = userRepository;
        this.gamificationService = gamificationService;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordActivity(UUID userId) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));

        UserLoginStreak streak = streakRepository.findByUserId(userId)
                .orElseGet(() -> createStreakForUser(userId));

        LocalDate last = streak.getLastActivityDate();
        if (last != null && last.equals(today)) {
            log.debug("Streak already recorded today for user {} (day {})", userId, streak.getCurrentDay());
            return;
        }

        int newDay;
        if (last == null || ChronoUnit.DAYS.between(last, today) > 1) {
            newDay = 1;
        } else {
            // last is exactly today - 1
            newDay = streak.getCurrentDay() + 1;
        }

        streak.setCurrentDay(newDay);
        streak.setLastActivityDate(today);
        streakRepository.save(streak);
        log.info("User {} streak advanced to day {}", userId, newDay);

        int dailyBonus = DAILY_XP_PER_DAY * Math.min(newDay, DAILY_XP_DAY_CAP);
        gamificationService.awardXp(userId, dailyBonus, XpEventType.STREAK_DAILY, null);

        if (newDay > 0 && newDay % 7 == 0) {
            gamificationService.awardXp(userId, WEEKLY_MILESTONE_XP, XpEventType.STREAK_WEEKLY_MILESTONE, null);
        }
    }

    private UserLoginStreak createStreakForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        return new UserLoginStreak(user);
    }
}
