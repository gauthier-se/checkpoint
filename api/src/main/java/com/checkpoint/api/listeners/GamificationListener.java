package com.checkpoint.api.listeners;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.checkpoint.api.enums.XpEventType;
import com.checkpoint.api.events.CommentReplyEvent;
import com.checkpoint.api.events.GameFinishedEvent;
import com.checkpoint.api.events.GameRatedEvent;
import com.checkpoint.api.events.ListCreatedEvent;
import com.checkpoint.api.events.ReviewCreatedEvent;
import com.checkpoint.api.events.ReviewLikedEvent;
import com.checkpoint.api.events.UserFollowedEvent;
import com.checkpoint.api.events.UserGainedFollowerEvent;
import com.checkpoint.api.repositories.XpGrantRepository;
import com.checkpoint.api.services.GamificationService;

/**
 * Listens for gamification-related events and delegates XP awards
 * to the {@link GamificationService}.
 *
 * <p>All handlers are asynchronous to avoid blocking the publishing thread.
 * Dedup-keyed events go through {@link GamificationService#awardXp} so that
 * retried or reversed actions cannot farm XP.</p>
 */
@Component
public class GamificationListener {

    private static final Logger log = LoggerFactory.getLogger(GamificationListener.class);

    private static final int REVIEW_XP = 50;
    private static final int GAME_FINISHED_XP = 100;
    private static final int FOLLOW_XP = 10;
    private static final int GAIN_FOLLOWER_XP = 5;
    private static final int LIST_CREATED_XP = 30;
    private static final int REVIEW_LIKED_XP = 5;
    private static final long REVIEW_LIKED_DAILY_CAP = 10L;
    private static final int GAME_RATED_XP = 20;
    private static final int COMMENT_REPLY_XP = 5;

    private final GamificationService gamificationService;
    private final XpGrantRepository xpGrantRepository;

    /**
     * Constructs a new GamificationListener.
     *
     * @param gamificationService the gamification service
     * @param xpGrantRepository   the XP grant audit repository (used for cap queries)
     */
    public GamificationListener(GamificationService gamificationService,
                                XpGrantRepository xpGrantRepository) {
        this.gamificationService = gamificationService;
        this.xpGrantRepository = xpGrantRepository;
    }

    /**
     * Handles a {@link ReviewCreatedEvent} by awarding XP to the user.
     */
    @Async
    @EventListener
    public void onReviewCreated(ReviewCreatedEvent event) {
        log.info("Handling ReviewCreatedEvent for user {}", event.getUserId());
        gamificationService.addXp(event.getUserId(), REVIEW_XP);
    }

    /**
     * Handles a {@link GameFinishedEvent} by awarding XP to the user.
     */
    @Async
    @EventListener
    public void onGameFinished(GameFinishedEvent event) {
        log.info("Handling GameFinishedEvent for user {}", event.getUserId());
        gamificationService.addXp(event.getUserId(), GAME_FINISHED_XP);
    }

    /**
     * Handles a {@link UserFollowedEvent} by awarding XP to the follower.
     * Dedup key: {@code (followerId, USER_FOLLOWED, followedUserId)} — unfollow
     * + refollow does not grant a second time.
     */
    @Async
    @EventListener
    public void onUserFollowed(UserFollowedEvent event) {
        log.info("Handling UserFollowedEvent: follower={}, followed={}",
                event.getFollowerId(), event.getFollowedUserId());
        gamificationService.awardXp(event.getFollowerId(), FOLLOW_XP,
                XpEventType.USER_FOLLOWED, event.getFollowedUserId());
    }

    /**
     * Handles a {@link UserGainedFollowerEvent} by awarding XP to the followed user.
     * Dedup key: {@code (followedUserId, USER_GAINED_FOLLOWER, followerId)}.
     */
    @Async
    @EventListener
    public void onUserGainedFollower(UserGainedFollowerEvent event) {
        log.info("Handling UserGainedFollowerEvent: followed={}, follower={}",
                event.getFollowedUserId(), event.getFollowerId());
        gamificationService.awardXp(event.getFollowedUserId(), GAIN_FOLLOWER_XP,
                XpEventType.USER_GAINED_FOLLOWER, event.getFollowerId());
    }

    /**
     * Handles a {@link ListCreatedEvent} by awarding the first-public-list XP.
     * The publisher already filters: this event only fires for the user's first
     * list AND only when that list is public. We still dedup by listId.
     */
    @Async
    @EventListener
    public void onListCreated(ListCreatedEvent event) {
        log.info("Handling ListCreatedEvent: user={}, list={}",
                event.getUserId(), event.getListId());
        gamificationService.awardXp(event.getUserId(), LIST_CREATED_XP,
                XpEventType.FIRST_LIST_CREATED, event.getListId());
    }

    /**
     * Handles a {@link ReviewLikedEvent} by awarding XP to the review's author.
     * Enforces a 24h rolling cap: an author can earn at most
     * {@value #REVIEW_LIKED_DAILY_CAP} grants of this type in any 24h window.
     */
    @Async
    @EventListener
    public void onReviewLiked(ReviewLikedEvent event) {
        log.info("Handling ReviewLikedEvent: author={}, liker={}, review={}",
                event.getReviewAuthorId(), event.getLikerId(), event.getReviewId());

        long grantsLast24h = xpGrantRepository.countByUserIdAndEventTypeAfter(
                event.getReviewAuthorId(),
                XpEventType.REVIEW_LIKED,
                LocalDateTime.now().minusHours(24));

        if (grantsLast24h >= REVIEW_LIKED_DAILY_CAP) {
            log.debug("Review-like XP cap reached for author {} ({}), skipping",
                    event.getReviewAuthorId(), grantsLast24h);
            return;
        }

        gamificationService.awardXp(event.getReviewAuthorId(), REVIEW_LIKED_XP,
                XpEventType.REVIEW_LIKED, event.getLikeId());
    }

    /**
     * Handles a {@link GameRatedEvent} by awarding XP to the rater.
     * Dedup key: {@code (userId, GAME_RATED, videoGameId)} — re-rating the same
     * game never grants twice, even though the publisher already filters re-rates.
     */
    @Async
    @EventListener
    public void onGameRated(GameRatedEvent event) {
        log.info("Handling GameRatedEvent: user={}, game={}",
                event.getUserId(), event.getVideoGameId());
        gamificationService.awardXp(event.getUserId(), GAME_RATED_XP,
                XpEventType.GAME_RATED, event.getVideoGameId());
    }

    /**
     * Handles a {@link CommentReplyEvent} by awarding XP to the replier.
     * Dedup key: {@code (replierId, COMMENT_REPLY, parentCommentId)} — only the
     * first reply to a given parent earns XP.
     */
    @Async
    @EventListener
    public void onCommentReply(CommentReplyEvent event) {
        log.info("Handling CommentReplyEvent: replier={}, parent={}",
                event.getReplierId(), event.getParentCommentId());
        gamificationService.awardXp(event.getReplierId(), COMMENT_REPLY_XP,
                XpEventType.COMMENT_REPLY, event.getParentCommentId());
    }
}
