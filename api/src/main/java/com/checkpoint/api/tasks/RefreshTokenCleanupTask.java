package com.checkpoint.api.tasks;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.repositories.RefreshTokenRepository;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Scheduled task that removes stale refresh tokens from the database.
 * Runs daily at 03:00 and deletes all tokens whose expiry date is older than 30 days.
 */
@Component
public class RefreshTokenCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupTask.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupTask(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Deletes refresh tokens that expired more than 30 days ago.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "refreshTokenCleanup", lockAtLeastFor = "5m", lockAtMostFor = "15m")
    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        refreshTokenRepository.deleteExpiredBefore(cutoff);
        log.info("Cleaned up refresh tokens expired before {}", cutoff);
    }
}
