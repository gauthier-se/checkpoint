package com.checkpoint.api.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.checkpoint.api.services.BadgeAwardingService;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Nightly sweep that awards the SNAKE_BACKLOG easter-egg badge to every user
 * who has at least one backlog entry that has been sitting untouched for a
 * full year. The check has to be scheduled because no domain event fires "365
 * days later": only time passing makes the user eligible.
 *
 * <p>Idempotent thanks to {@code awardIfEligible}: re-running this task does
 * not double-award.</p>
 */
@Component
public class SnakeBacklogTask {

    private static final Logger log = LoggerFactory.getLogger(SnakeBacklogTask.class);

    private final BadgeAwardingService badgeAwardingService;

    public SnakeBacklogTask(BadgeAwardingService badgeAwardingService) {
        this.badgeAwardingService = badgeAwardingService;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    @SchedulerLock(name = "snakeBacklogBadge", lockAtLeastFor = "1m", lockAtMostFor = "10m")
    public void awardLongBacklogUsers() {
        log.info("SNAKE_BACKLOG nightly sweep starting");
        badgeAwardingService.awardLongBacklogUsers();
    }
}
