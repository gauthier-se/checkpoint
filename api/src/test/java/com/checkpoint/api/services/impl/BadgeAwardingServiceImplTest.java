package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.enums.BadgeCode;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.events.BadgeUnlockedEvent;
import com.checkpoint.api.repositories.BadgeRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Unit tests for {@link BadgeAwardingServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class BadgeAwardingServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private BadgeRepository badgeRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private UserGameRepository userGameRepository;
    @Mock private UserGamePlayRepository userGamePlayRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private com.checkpoint.api.repositories.RateRepository rateRepository;
    @Mock private com.checkpoint.api.repositories.BacklogRepository backlogRepository;
    @Mock private com.checkpoint.api.repositories.ReviewViewRepository reviewViewRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private BadgeAwardingServiceImpl service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        service = new BadgeAwardingServiceImpl(
                userRepository, badgeRepository, reviewRepository,
                userGameRepository, userGamePlayRepository, likeRepository,
                rateRepository, backlogRepository, reviewViewRepository, eventPublisher);

        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
    }

    private Badge badge(BadgeCode code) {
        Badge b = new Badge(code.name(), code.getDefaultName(),
                code.getDefaultDescription(), null);
        b.setId(UUID.randomUUID());
        return b;
    }

    private void stubBadge(BadgeCode code) {
        when(badgeRepository.findByCode(code.name()))
                .thenReturn(Optional.of(badge(code)));
    }

    private void stubUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    @Nested
    @DisplayName("awardIfEligible()")
    class AwardIfEligible {

        @Test
        @DisplayName("Should add the badge and publish the event when the user does not own it")
        void shouldAwardWhenEligible() {
            // Given
            Badge badge = badge(BadgeCode.FIRST_REVIEW);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(badgeRepository.findByCode(BadgeCode.FIRST_REVIEW.name()))
                    .thenReturn(Optional.of(badge));

            // When
            service.awardIfEligible(userId, BadgeCode.FIRST_REVIEW);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getBadges()).contains(badge);

            ArgumentCaptor<BadgeUnlockedEvent> eventCaptor =
                    ArgumentCaptor.forClass(BadgeUnlockedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
            assertThat(eventCaptor.getValue().getCode()).isEqualTo(BadgeCode.FIRST_REVIEW);
        }

        @Test
        @DisplayName("Should be a no-op when the user already owns the badge")
        void shouldBeIdempotent() {
            // Given
            Badge badge = badge(BadgeCode.FIRST_REVIEW);
            user.getBadges().add(badge);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(badgeRepository.findByCode(BadgeCode.FIRST_REVIEW.name()))
                    .thenReturn(Optional.of(badge));

            // When
            service.awardIfEligible(userId, BadgeCode.FIRST_REVIEW);

            // Then
            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw when the user does not exist")
        void shouldThrowWhenUserMissing() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.awardIfEligible(userId, BadgeCode.FIRST_REVIEW))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("Should be a no-op when the badge code is missing from the catalog")
        void shouldNoOpWhenBadgeMissing() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(badgeRepository.findByCode(BadgeCode.FIRST_REVIEW.name()))
                    .thenReturn(Optional.empty());

            // When
            service.awardIfEligible(userId, BadgeCode.FIRST_REVIEW);

            // Then
            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("checkReviewBadges()")
    class CheckReviewBadges {

        @Test
        @DisplayName("Should award only FIRST_REVIEW when review count is 1")
        void shouldAwardFirstReviewOnly() {
            // Given
            when(reviewRepository.countByUserId(userId)).thenReturn(1L);
            stubUser();
            stubBadge(BadgeCode.FIRST_REVIEW);

            // When
            service.checkReviewBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.FIRST_REVIEW.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.REVIEW_10.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.REVIEW_50.name());
        }

        @Test
        @DisplayName("Should evaluate FIRST_REVIEW and REVIEW_10 when count is 10")
        void shouldEvaluateFirstReviewAndReview10AtTen() {
            // Given
            when(reviewRepository.countByUserId(userId)).thenReturn(10L);
            stubUser();
            stubBadge(BadgeCode.FIRST_REVIEW);
            stubBadge(BadgeCode.REVIEW_10);

            // When
            service.checkReviewBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.FIRST_REVIEW.name());
            verify(badgeRepository).findByCode(BadgeCode.REVIEW_10.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.REVIEW_50.name());
        }
    }

    @Nested
    @DisplayName("checkGameFinishedBadges()")
    class CheckGameFinishedBadges {

        @Test
        @DisplayName("Should award only FIRST_GAME_FINISHED when completed count is 1")
        void shouldAwardFirstGameFinishedOnly() {
            // Given
            when(userGameRepository.countByUserIdAndStatus(userId, PlayStatus.COMPLETED))
                    .thenReturn(1L);
            stubUser();
            stubBadge(BadgeCode.FIRST_GAME_FINISHED);

            // When
            service.checkGameFinishedBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.FIRST_GAME_FINISHED.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.GAME_FINISHED_10.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.GAME_FINISHED_50.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.BACKLOG_HUNDRED.name());
        }

        @Test
        @DisplayName("Should evaluate all four tiers when count is 100")
        void shouldEvaluateAllTiersAtHundred() {
            // Given
            when(userGameRepository.countByUserIdAndStatus(userId, PlayStatus.COMPLETED))
                    .thenReturn(100L);
            stubUser();
            stubBadge(BadgeCode.FIRST_GAME_FINISHED);
            stubBadge(BadgeCode.GAME_FINISHED_10);
            stubBadge(BadgeCode.GAME_FINISHED_50);
            stubBadge(BadgeCode.BACKLOG_HUNDRED);

            // When
            service.checkGameFinishedBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.FIRST_GAME_FINISHED.name());
            verify(badgeRepository).findByCode(BadgeCode.GAME_FINISHED_10.name());
            verify(badgeRepository).findByCode(BadgeCode.GAME_FINISHED_50.name());
            verify(badgeRepository).findByCode(BadgeCode.BACKLOG_HUNDRED.name());
        }
    }

    @Nested
    @DisplayName("checkLevelBadges()")
    class CheckLevelBadges {

        @Test
        @DisplayName("Should award LEVEL_5 only at level 5")
        void shouldAwardLevel5Only() {
            // Given
            stubUser();
            stubBadge(BadgeCode.LEVEL_5);

            // When
            service.checkLevelBadges(userId, 5);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.LEVEL_5.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.LEVEL_10.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.LEVEL_25.name());
        }

        @Test
        @DisplayName("Should evaluate all three level badges at level 25")
        void shouldEvaluateAllLevelBadgesAt25() {
            // Given
            stubUser();
            stubBadge(BadgeCode.LEVEL_5);
            stubBadge(BadgeCode.LEVEL_10);
            stubBadge(BadgeCode.LEVEL_25);

            // When
            service.checkLevelBadges(userId, 25);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.LEVEL_5.name());
            verify(badgeRepository).findByCode(BadgeCode.LEVEL_10.name());
            verify(badgeRepository).findByCode(BadgeCode.LEVEL_25.name());
        }

        @Test
        @DisplayName("Should award no badges below level 5")
        void shouldAwardNothingBelow5() {
            // When
            service.checkLevelBadges(userId, 4);

            // Then
            verify(badgeRepository, never()).findByCode(any());
            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("checkPlayLogBadges()")
    class CheckPlayLogBadges {

        @Test
        @DisplayName("Should award CENTURION at 100 plays and MULTIPLATFORM_NOMAD at 5 platforms")
        void shouldAwardBothWhenThresholdsMet() {
            // Given
            when(userGamePlayRepository.countByUserId(userId)).thenReturn(100L);
            when(userGamePlayRepository.countDistinctPlatformsByUserId(userId)).thenReturn(5L);
            stubUser();
            stubBadge(BadgeCode.CENTURION);
            stubBadge(BadgeCode.MULTIPLATFORM_NOMAD);

            // When
            service.checkPlayLogBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.CENTURION.name());
            verify(badgeRepository).findByCode(BadgeCode.MULTIPLATFORM_NOMAD.name());
        }

        @Test
        @DisplayName("Should not award when thresholds are not met")
        void shouldNotAwardBelowThresholds() {
            // Given
            when(userGamePlayRepository.countByUserId(userId)).thenReturn(99L);
            when(userGamePlayRepository.countDistinctPlatformsByUserId(userId)).thenReturn(4L);

            // When
            service.checkPlayLogBadges(userId);

            // Then
            verify(badgeRepository, never()).findByCode(BadgeCode.CENTURION.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.MULTIPLATFORM_NOMAD.name());
        }
    }

    @Nested
    @DisplayName("checkLibrarySizeBadges()")
    class CheckLibrarySizeBadges {

        @Test
        @DisplayName("Should award LIBRARY_50 only when library size is 50")
        void shouldAwardLibrary50() {
            // Given
            when(userGameRepository.countByUserId(userId)).thenReturn(50L);
            stubUser();
            stubBadge(BadgeCode.LIBRARY_50);

            // When
            service.checkLibrarySizeBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.LIBRARY_50.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.LIBRARY_200.name());
        }

        @Test
        @DisplayName("Should award both tiers when library size reaches 200")
        void shouldAwardBothTiersAt200() {
            // Given
            when(userGameRepository.countByUserId(userId)).thenReturn(200L);
            stubUser();
            stubBadge(BadgeCode.LIBRARY_50);
            stubBadge(BadgeCode.LIBRARY_200);

            // When
            service.checkLibrarySizeBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.LIBRARY_50.name());
            verify(badgeRepository).findByCode(BadgeCode.LIBRARY_200.name());
        }
    }

    @Nested
    @DisplayName("checkGenreBadges()")
    class CheckGenreBadges {

        @Test
        @DisplayName("Should award RPG_DISCIPLE when 10 RPGs are completed")
        void shouldAwardRpgDiscipleAt10Rpgs() {
            // Given
            when(userGameRepository.countCompletedByUserIdAndGenreName(eq(userId), anyString()))
                    .thenReturn(0L);
            when(userGameRepository.countCompletedByUserIdAndGenreName(userId, "Role-playing (RPG)"))
                    .thenReturn(10L);
            stubUser();
            stubBadge(BadgeCode.RPG_DISCIPLE);

            // When
            service.checkGenreBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.RPG_DISCIPLE.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.SHOOTER_TRIGGER_HAPPY.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.PLATFORMER_HERO.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.INDIE_GEM_HUNTER.name());
        }

        @Test
        @DisplayName("Should award INDIE_GEM_HUNTER only at 20 indies (higher threshold)")
        void shouldAwardIndieAt20() {
            // Given
            when(userGameRepository.countCompletedByUserIdAndGenreName(eq(userId), anyString()))
                    .thenReturn(0L);
            when(userGameRepository.countCompletedByUserIdAndGenreName(userId, "Indie"))
                    .thenReturn(19L);

            // When
            service.checkGenreBadges(userId);

            // Then
            verify(badgeRepository, never()).findByCode(BadgeCode.INDIE_GEM_HUNTER.name());
        }
    }

    @Nested
    @DisplayName("checkReviewQualityBadges()")
    class CheckReviewQualityBadges {

        @Test
        @DisplayName("Should award FIVE_STAR_STREAK when the last 3 reviews are all 5-star")
        void shouldAwardFiveStarStreak() {
            // Given
            List<Review> recent = List.of(
                    reviewWithScore(10), reviewWithScore(10), reviewWithScore(10));
            when(reviewRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(recent);
            when(reviewRepository.countOneStarReviewsByUserId(userId)).thenReturn(0L);
            when(reviewRepository.existsLongReviewByUserId(userId)).thenReturn(false);
            stubUser();
            stubBadge(BadgeCode.FIVE_STAR_STREAK);

            // When
            service.checkReviewQualityBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.FIVE_STAR_STREAK.name());
        }

        @Test
        @DisplayName("Should not award FIVE_STAR_STREAK when a recent review is not 5-star")
        void shouldNotAwardWhenStreakBroken() {
            // Given
            List<Review> recent = List.of(
                    reviewWithScore(10), reviewWithScore(8), reviewWithScore(10));
            when(reviewRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(recent);
            when(reviewRepository.countOneStarReviewsByUserId(userId)).thenReturn(0L);
            when(reviewRepository.existsLongReviewByUserId(userId)).thenReturn(false);

            // When
            service.checkReviewQualityBadges(userId);

            // Then
            verify(badgeRepository, never()).findByCode(BadgeCode.FIVE_STAR_STREAK.name());
        }

        @Test
        @DisplayName("Should award BRUTAL_CRITIC at 10 one-star reviews")
        void shouldAwardBrutalCriticAt10() {
            // Given
            when(reviewRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
            when(reviewRepository.countOneStarReviewsByUserId(userId)).thenReturn(10L);
            when(reviewRepository.existsLongReviewByUserId(userId)).thenReturn(false);
            stubUser();
            stubBadge(BadgeCode.BRUTAL_CRITIC);

            // When
            service.checkReviewQualityBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.BRUTAL_CRITIC.name());
        }

        @Test
        @DisplayName("Should award WORDSMITH when a 1000+ character review exists")
        void shouldAwardWordsmith() {
            // Given
            when(reviewRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
            when(reviewRepository.countOneStarReviewsByUserId(userId)).thenReturn(0L);
            when(reviewRepository.existsLongReviewByUserId(userId)).thenReturn(true);
            stubUser();
            stubBadge(BadgeCode.WORDSMITH);

            // When
            service.checkReviewQualityBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.WORDSMITH.name());
        }

        private Review reviewWithScore(int score) {
            Review r = new Review();
            UserGamePlay play = new UserGamePlay();
            play.setScore(score);
            r.setUserGamePlay(play);
            return r;
        }
    }

    @Nested
    @DisplayName("checkSocialBadges()")
    class CheckSocialBadges {

        @Test
        @DisplayName("Should award all four social badges when every threshold is met")
        void shouldAwardAllSocialBadges() {
            // Given
            when(userRepository.countFollowingByUserId(userId)).thenReturn(10L);
            when(userRepository.countFollowersByUserId(userId)).thenReturn(10L);
            when(likeRepository.countByUserId(userId)).thenReturn(100L);
            when(likeRepository.countLikesReceivedOnReviewsByUserId(userId)).thenReturn(50L);
            stubUser();
            stubBadge(BadgeCode.NETWORKER);
            stubBadge(BadgeCode.CHARISMATIC);
            stubBadge(BadgeCode.PRAISE_THE_SUN);
            stubBadge(BadgeCode.BELOVED_REVIEWER);

            // When
            service.checkSocialBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.NETWORKER.name());
            verify(badgeRepository).findByCode(BadgeCode.CHARISMATIC.name());
            verify(badgeRepository).findByCode(BadgeCode.PRAISE_THE_SUN.name());
            verify(badgeRepository).findByCode(BadgeCode.BELOVED_REVIEWER.name());
        }

        @Test
        @DisplayName("Should award none when no threshold is met")
        void shouldAwardNoneBelowThresholds() {
            // Given
            when(userRepository.countFollowingByUserId(userId)).thenReturn(9L);
            when(userRepository.countFollowersByUserId(userId)).thenReturn(9L);
            when(likeRepository.countByUserId(userId)).thenReturn(99L);
            when(likeRepository.countLikesReceivedOnReviewsByUserId(userId)).thenReturn(49L);

            // When
            service.checkSocialBadges(userId);

            // Then
            verify(badgeRepository, never()).findByCode(any());
        }
    }

    @Nested
    @DisplayName("checkLongevityBadges()")
    class CheckLongevityBadges {

        @Test
        @DisplayName("Should award VETERAN_30 when 30+ days have passed since registration")
        void shouldAwardVeteran30() {
            // Given
            user.setCreatedAt(LocalDateTime.now().minusDays(31));
            stubUser();
            stubBadge(BadgeCode.VETERAN_30);

            // When
            service.checkLongevityBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.VETERAN_30.name());
            verify(badgeRepository, never()).findByCode(BadgeCode.LIFER.name());
        }

        @Test
        @DisplayName("Should award both VETERAN_30 and LIFER when 365+ days have passed")
        void shouldAwardLifer() {
            // Given
            user.setCreatedAt(LocalDateTime.now().minusDays(400));
            stubUser();
            stubBadge(BadgeCode.VETERAN_30);
            stubBadge(BadgeCode.LIFER);

            // When
            service.checkLongevityBadges(userId);

            // Then
            verify(badgeRepository).findByCode(BadgeCode.VETERAN_30.name());
            verify(badgeRepository).findByCode(BadgeCode.LIFER.name());
        }

        @Test
        @DisplayName("Should award nothing when newly registered")
        void shouldAwardNothingForNewUser() {
            // Given
            user.setCreatedAt(LocalDateTime.now().minusDays(5));
            stubUser();

            // When
            service.checkLongevityBadges(userId);

            // Then
            verify(badgeRepository, never()).findByCode(any());
        }
    }
}
