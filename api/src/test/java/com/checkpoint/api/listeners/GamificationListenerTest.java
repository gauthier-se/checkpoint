package com.checkpoint.api.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Unit tests for {@link GamificationListener}.
 */
@ExtendWith(MockitoExtension.class)
class GamificationListenerTest {

    @Mock
    private GamificationService gamificationService;

    @Mock
    private XpGrantRepository xpGrantRepository;

    private GamificationListener gamificationListener;

    @BeforeEach
    void setUp() {
        gamificationListener = new GamificationListener(gamificationService, xpGrantRepository);
    }

    @Nested
    @DisplayName("onReviewCreated()")
    class OnReviewCreated {

        @Test
        @DisplayName("Should award 50 XP when a review is created")
        void onReviewCreated_shouldAward50Xp() {
            UUID userId = UUID.randomUUID();

            gamificationListener.onReviewCreated(new ReviewCreatedEvent(userId));

            verify(gamificationService).addXp(userId, 50);
        }
    }

    @Nested
    @DisplayName("onGameFinished()")
    class OnGameFinished {

        @Test
        @DisplayName("Should award 100 XP when a game is finished")
        void onGameFinished_shouldAward100Xp() {
            UUID userId = UUID.randomUUID();

            gamificationListener.onGameFinished(new GameFinishedEvent(userId));

            verify(gamificationService).addXp(userId, 100);
        }
    }

    @Nested
    @DisplayName("onUserFollowed()")
    class OnUserFollowed {

        @Test
        @DisplayName("Should award 10 XP to the follower, dedup-keyed on the followed user")
        void onUserFollowed_shouldAward10Xp() {
            UUID follower = UUID.randomUUID();
            UUID followed = UUID.randomUUID();

            gamificationListener.onUserFollowed(new UserFollowedEvent(follower, followed));

            verify(gamificationService).awardXp(follower, 10, XpEventType.USER_FOLLOWED, followed);
        }
    }

    @Nested
    @DisplayName("onUserGainedFollower()")
    class OnUserGainedFollower {

        @Test
        @DisplayName("Should award 5 XP to the followed user, dedup-keyed on the follower")
        void onUserGainedFollower_shouldAward5Xp() {
            UUID follower = UUID.randomUUID();
            UUID followed = UUID.randomUUID();

            gamificationListener.onUserGainedFollower(new UserGainedFollowerEvent(followed, follower));

            verify(gamificationService).awardXp(followed, 5, XpEventType.USER_GAINED_FOLLOWER, follower);
        }
    }

    @Nested
    @DisplayName("onListCreated()")
    class OnListCreated {

        @Test
        @DisplayName("Should award 30 XP for a first public list, dedup-keyed on the list id")
        void onListCreated_shouldAward30Xp() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();

            gamificationListener.onListCreated(new ListCreatedEvent(userId, listId));

            verify(gamificationService).awardXp(userId, 30, XpEventType.FIRST_LIST_CREATED, listId);
        }
    }

    @Nested
    @DisplayName("onReviewLiked()")
    class OnReviewLiked {

        @Test
        @DisplayName("Should award 5 XP to the review author when the daily cap has not been hit")
        void onReviewLiked_shouldAwardWhenBelowCap() {
            UUID liker = UUID.randomUUID();
            UUID author = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID likeId = UUID.randomUUID();
            when(xpGrantRepository.countByUserIdAndEventTypeAfter(eq(author),
                    eq(XpEventType.REVIEW_LIKED), any())).thenReturn(5L);

            gamificationListener.onReviewLiked(new ReviewLikedEvent(liker, author, reviewId, likeId));

            verify(gamificationService).awardXp(author, 5, XpEventType.REVIEW_LIKED, likeId);
        }

        @Test
        @DisplayName("Should skip the grant when the author has hit the 10/day cap")
        void onReviewLiked_shouldSkipAtCap() {
            UUID liker = UUID.randomUUID();
            UUID author = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID likeId = UUID.randomUUID();
            when(xpGrantRepository.countByUserIdAndEventTypeAfter(eq(author),
                    eq(XpEventType.REVIEW_LIKED), any())).thenReturn(10L);

            gamificationListener.onReviewLiked(new ReviewLikedEvent(liker, author, reviewId, likeId));

            verify(gamificationService, never()).awardXp(any(), anyInt(), any(), any());
        }
    }

    @Nested
    @DisplayName("onGameRated()")
    class OnGameRated {

        @Test
        @DisplayName("Should award 20 XP for a first-time rating, dedup-keyed on the game id")
        void onGameRated_shouldAward20Xp() {
            UUID userId = UUID.randomUUID();
            UUID gameId = UUID.randomUUID();

            gamificationListener.onGameRated(new GameRatedEvent(userId, gameId));

            verify(gamificationService).awardXp(userId, 20, XpEventType.GAME_RATED, gameId);
        }
    }

    @Nested
    @DisplayName("onCommentReply()")
    class OnCommentReply {

        @Test
        @DisplayName("Should award 5 XP for a reply, dedup-keyed on the parent comment id")
        void onCommentReply_shouldAward5Xp() {
            UUID replier = UUID.randomUUID();
            UUID parentAuthor = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();

            gamificationListener.onCommentReply(new CommentReplyEvent(replier, parentAuthor, parentId));

            verify(gamificationService).awardXp(replier, 5, XpEventType.COMMENT_REPLY, parentId);
        }
    }
}
