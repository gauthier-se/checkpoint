package com.checkpoint.api.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.checkpoint.api.events.GameFinishedEvent;
import com.checkpoint.api.events.GameRemovedFromLibraryEvent;
import com.checkpoint.api.events.GameStartedPlayingEvent;
import com.checkpoint.api.events.PlayLogCreatedEvent;
import com.checkpoint.api.events.RateRecordedEvent;
import com.checkpoint.api.events.ReviewCreatedEvent;
import com.checkpoint.api.events.ReviewDeletedEvent;
import com.checkpoint.api.events.ReviewLikedEvent;
import com.checkpoint.api.events.UserActivityEvent;
import com.checkpoint.api.events.UserFollowedEvent;
import com.checkpoint.api.events.UserGainedFollowerEvent;
import com.checkpoint.api.events.UserLeveledUpEvent;
import com.checkpoint.api.services.BadgeAwardingService;

/**
 * Listens for domain events and delegates badge evaluation to {@link BadgeAwardingService}.
 *
 * <p>Uses {@link TransactionalEventListener} with {@link TransactionPhase#AFTER_COMMIT}
 * so that listeners only fire once the publishing transaction has been committed.
 * This guarantees the count queries in the service (e.g. {@code countByUserId})
 * include the freshly persisted row that triggered the event.</p>
 *
 * <p>Listeners are also {@link Async} to avoid blocking the publishing thread.</p>
 */
@Component
public class BadgeListener {

    private static final Logger log = LoggerFactory.getLogger(BadgeListener.class);

    private final BadgeAwardingService badgeAwardingService;

    public BadgeListener(BadgeAwardingService badgeAwardingService) {
        this.badgeAwardingService = badgeAwardingService;
    }

    /**
     * Handles a {@link ReviewCreatedEvent} by evaluating review-count and
     * review-quality badges.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCreated(ReviewCreatedEvent event) {
        log.info("Handling ReviewCreatedEvent for badge evaluation, user {}", event.getUserId());
        badgeAwardingService.checkReviewBadges(event.getUserId());
        badgeAwardingService.checkReviewQualityBadges(event.getUserId());
    }

    /**
     * Handles a {@link GameFinishedEvent} by evaluating completion-count and
     * genre-completion badges.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGameFinished(GameFinishedEvent event) {
        log.info("Handling GameFinishedEvent for badge evaluation, user {}", event.getUserId());
        badgeAwardingService.checkGameFinishedBadges(event.getUserId());
        badgeAwardingService.checkGenreBadges(event.getUserId());
    }

    /**
     * Handles a {@link UserLeveledUpEvent} by evaluating level-based badges.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserLeveledUp(UserLeveledUpEvent event) {
        log.info("Handling UserLeveledUpEvent for badge evaluation, user {} now at level {}",
                event.getUserId(), event.getNewLevel());
        badgeAwardingService.checkLevelBadges(event.getUserId(), event.getNewLevel());
    }

    /**
     * Handles a {@link PlayLogCreatedEvent} by evaluating play-count, library-size
     * and platform-diversity badges.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlayLogCreated(PlayLogCreatedEvent event) {
        log.info("Handling PlayLogCreatedEvent for badge evaluation, user {}", event.getUserId());
        badgeAwardingService.checkPlayLogBadges(event.getUserId());
        badgeAwardingService.checkLibrarySizeBadges(event.getUserId());
    }

    /**
     * Handles a {@link ReviewLikedEvent} by evaluating social badges for both the
     * liker (e.g. {@code PRAISE_THE_SUN}) and the review author (e.g.
     * {@code BELOVED_REVIEWER}).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewLiked(ReviewLikedEvent event) {
        log.info("Handling ReviewLikedEvent for badge evaluation, liker {}, author {}",
                event.getLikerId(), event.getReviewAuthorId());
        badgeAwardingService.checkSocialBadges(event.getLikerId());
        badgeAwardingService.checkSocialBadges(event.getReviewAuthorId());
    }

    /**
     * Handles a {@link UserFollowedEvent} by evaluating social badges for the
     * follower (e.g. {@code NETWORKER}).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserFollowed(UserFollowedEvent event) {
        log.info("Handling UserFollowedEvent for badge evaluation, follower {}", event.getFollowerId());
        badgeAwardingService.checkSocialBadges(event.getFollowerId());
    }

    /**
     * Handles a {@link UserGainedFollowerEvent} by evaluating social badges for
     * the followed user (e.g. {@code CHARISMATIC}).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserGainedFollower(UserGainedFollowerEvent event) {
        log.info("Handling UserGainedFollowerEvent for badge evaluation, user {}", event.getFollowedUserId());
        badgeAwardingService.checkSocialBadges(event.getFollowedUserId());
    }

    /**
     * Handles a {@link UserActivityEvent} by evaluating longevity badges
     * ({@code VETERAN_30}, {@code LIFER}): checked on every meaningful activity
     * so an active user crosses the threshold organically.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserActivity(UserActivityEvent event) {
        log.debug("Handling UserActivityEvent for longevity badge evaluation, user {}", event.getUserId());
        badgeAwardingService.checkLongevityBadges(event.getUserId());
    }

    /**
     * Handles a {@link RateRecordedEvent} by evaluating rating-driven easter-egg
     * badges ({@code THE_CAKE_IS_A_LIE}, {@code INDECISIVE}). Fires on every rate
     * create or update, unlike {@code GameRatedEvent} which is first-time only.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRateRecorded(RateRecordedEvent event) {
        log.debug("Handling RateRecordedEvent for badge evaluation, user {}", event.getUserId());
        badgeAwardingService.checkRatingBadges(event.getUserId());
    }

    /**
     * Handles a {@link GameRemovedFromLibraryEvent} by awarding the
     * {@code YOU_DIED} easter-egg badge.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGameRemoved(GameRemovedFromLibraryEvent event) {
        log.debug("Handling GameRemovedFromLibraryEvent for badge evaluation, user {}", event.getUserId());
        badgeAwardingService.checkGameRemovedBadges(event.getUserId());
    }

    /**
     * Handles a {@link ReviewDeletedEvent} by checking whether the review lived
     * less than five minutes (MISSION_FAILED easter-egg badge).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewDeleted(ReviewDeletedEvent event) {
        log.debug("Handling ReviewDeletedEvent for badge evaluation, user {}", event.getUserId());
        badgeAwardingService.checkReviewDeletedBadges(event.getUserId(), event.getReviewCreatedAt());
    }

    /**
     * Handles a {@link GameStartedPlayingEvent} by checking the backlog size
     * for the LEEROY easter-egg badge.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGameStartedPlaying(GameStartedPlayingEvent event) {
        log.debug("Handling GameStartedPlayingEvent for badge evaluation, user {}", event.getUserId());
        badgeAwardingService.checkGameStartedBadges(event.getUserId());
    }
}
