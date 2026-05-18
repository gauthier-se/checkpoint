package com.checkpoint.api.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.events.BadgeUnlockedEvent;
import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.events.UserLeveledUpEvent;
import com.checkpoint.api.repositories.BadgeRepository;

/**
 * Listens for progression-related domain events ({@link UserLeveledUpEvent},
 * {@link BadgeUnlockedEvent}) and bridges them to the notification pipeline by
 * publishing {@link NotificationEvent}s. Keeps {@code GamificationServiceImpl}
 * and {@code BadgeAwardingServiceImpl} decoupled from {@code NotificationService}.
 *
 * <p>Handlers fire {@link TransactionPhase#AFTER_COMMIT} so that the upstream
 * persistence (level update / badge award) is already visible when we query for it.</p>
 */
@Component
public class ProgressionNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ProgressionNotificationListener.class);

    private final ApplicationEventPublisher eventPublisher;
    private final BadgeRepository badgeRepository;

    public ProgressionNotificationListener(ApplicationEventPublisher eventPublisher,
                                           BadgeRepository badgeRepository) {
        this.eventPublisher = eventPublisher;
        this.badgeRepository = badgeRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserLeveledUp(UserLeveledUpEvent event) {
        log.info("Creating LEVEL_UP notification for user {} (new level {})",
                event.getUserId(), event.getNewLevel());

        eventPublisher.publishEvent(new NotificationEvent(
                event.getUserId(),
                null,
                NotificationType.LEVEL_UP,
                null,
                "You reached level " + event.getNewLevel() + "!"
        ));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBadgeUnlocked(BadgeUnlockedEvent event) {
        Badge badge = badgeRepository.findByCode(event.getCode().name()).orElse(null);
        if (badge == null) {
            log.warn("Badge {} not found when creating BADGE_UNLOCKED notification for user {}",
                    event.getCode(), event.getUserId());
            return;
        }

        log.info("Creating BADGE_UNLOCKED notification for user {} (badge {})",
                event.getUserId(), badge.getCode());

        eventPublisher.publishEvent(new NotificationEvent(
                event.getUserId(),
                null,
                NotificationType.BADGE_UNLOCKED,
                badge.getId(),
                "You unlocked the badge: " + badge.getName()
        ));
    }
}
