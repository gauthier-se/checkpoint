package com.checkpoint.api.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.ReviewViewRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.checkpoint.api.services.BadgeAwardingService;

/**
 * Implementation of {@link BadgeAwardingService}.
 */
@Service
@Transactional
public class BadgeAwardingServiceImpl implements BadgeAwardingService {

    private static final Logger log = LoggerFactory.getLogger(BadgeAwardingServiceImpl.class);

    // Genre names match the labels imported from IGDB (case-insensitive lookup).
    private static final String GENRE_RPG = "Role-playing (RPG)";
    private static final String GENRE_SHOOTER = "Shooter";
    private static final String GENRE_PLATFORM = "Platform";
    private static final String GENRE_INDIE = "Indie";

    // Easter-egg thresholds.
    private static final int TIME_TRAVELER_YEARS = 30;
    private static final int ALL_YOUR_BASE_PLAYS = 1000;
    private static final int CAKE_IS_A_LIE_ONE_STAR_COUNT = 13;
    private static final int INDECISIVE_CHANGE_THRESHOLD = 5;
    private static final long MISSION_FAILED_DELETE_WINDOW_MINUTES = 5;
    private static final int LEEROY_BACKLOG_THRESHOLD = 20;
    private static final int FREEMAN_DAYS_AWAY = 30;
    private static final int SNAKE_BACKLOG_DAYS = 365;
    private static final int STAY_AWHILE_REVIEWS_THRESHOLD = 50;

    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final ReviewRepository reviewRepository;
    private final UserGameRepository userGameRepository;
    private final UserGamePlayRepository userGamePlayRepository;
    private final LikeRepository likeRepository;
    private final RateRepository rateRepository;
    private final BacklogRepository backlogRepository;
    private final ReviewViewRepository reviewViewRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BadgeAwardingServiceImpl(UserRepository userRepository,
                                    BadgeRepository badgeRepository,
                                    ReviewRepository reviewRepository,
                                    UserGameRepository userGameRepository,
                                    UserGamePlayRepository userGamePlayRepository,
                                    LikeRepository likeRepository,
                                    RateRepository rateRepository,
                                    BacklogRepository backlogRepository,
                                    ReviewViewRepository reviewViewRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.badgeRepository = badgeRepository;
        this.reviewRepository = reviewRepository;
        this.userGameRepository = userGameRepository;
        this.userGamePlayRepository = userGamePlayRepository;
        this.likeRepository = likeRepository;
        this.rateRepository = rateRepository;
        this.backlogRepository = backlogRepository;
        this.reviewViewRepository = reviewViewRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void awardIfEligible(UUID userId, BadgeCode code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        Badge badge = badgeRepository.findByCode(code.name()).orElse(null);
        if (badge == null) {
            log.warn("Badge code {} not found in DB — skipping award for user {}. Seed the badge catalog.",
                    code, userId);
            return;
        }

        if (user.getBadges().contains(badge)) {
            return;
        }

        user.addBadge(badge);
        userRepository.save(user);
        log.info("Awarded badge {} to user {}", code, userId);
        eventPublisher.publishEvent(new BadgeUnlockedEvent(userId, code));
    }

    @Override
    public void checkReviewBadges(UUID userId) {
        long count = reviewRepository.countByUserId(userId);
        if (count >= 1) {
            awardIfEligible(userId, BadgeCode.FIRST_REVIEW);
        }
        if (count >= 10) {
            awardIfEligible(userId, BadgeCode.REVIEW_10);
        }
        if (count >= 50) {
            awardIfEligible(userId, BadgeCode.REVIEW_50);
        }
    }

    @Override
    public void checkGameFinishedBadges(UUID userId) {
        long count = userGameRepository.countByUserIdAndStatus(userId, PlayStatus.COMPLETED);
        if (count >= 1) {
            awardIfEligible(userId, BadgeCode.FIRST_GAME_FINISHED);
        }
        if (count >= 10) {
            awardIfEligible(userId, BadgeCode.GAME_FINISHED_10);
        }
        if (count >= 50) {
            awardIfEligible(userId, BadgeCode.GAME_FINISHED_50);
        }
        if (count >= 100) {
            awardIfEligible(userId, BadgeCode.BACKLOG_HUNDRED);
        }

        // TIME_TRAVELER: finishing a game released ≥ 30 years ago.
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).minusYears(TIME_TRAVELER_YEARS);
        if (userGameRepository.existsCompletedGameOlderThan(userId, cutoff)) {
            awardIfEligible(userId, BadgeCode.TIME_TRAVELER);
        }
    }

    @Override
    public void checkLevelBadges(UUID userId, int newLevel) {
        if (newLevel >= 5) {
            awardIfEligible(userId, BadgeCode.LEVEL_5);
        }
        if (newLevel >= 10) {
            awardIfEligible(userId, BadgeCode.LEVEL_10);
        }
        if (newLevel >= 25) {
            awardIfEligible(userId, BadgeCode.LEVEL_25);
        }
    }

    @Override
    public void checkPlayLogBadges(UUID userId) {
        long playCount = userGamePlayRepository.countByUserId(userId);
        if (playCount >= 100) {
            awardIfEligible(userId, BadgeCode.CENTURION);
        }
        if (playCount >= ALL_YOUR_BASE_PLAYS) {
            awardIfEligible(userId, BadgeCode.ALL_YOUR_BASE);
        }

        long platformCount = userGamePlayRepository.countDistinctPlatformsByUserId(userId);
        if (platformCount >= 5) {
            awardIfEligible(userId, BadgeCode.MULTIPLATFORM_NOMAD);
        }

        // NIGHT_OWL: any play logged between 02:00 and 04:59 server time.
        if (userGamePlayRepository.existsNightOwlPlayByUserId(userId)) {
            awardIfEligible(userId, BadgeCode.NIGHT_OWL);
        }
    }

    @Override
    public void checkLibrarySizeBadges(UUID userId) {
        long count = userGameRepository.countByUserId(userId);
        if (count >= 50) {
            awardIfEligible(userId, BadgeCode.LIBRARY_50);
        }
        if (count >= 200) {
            awardIfEligible(userId, BadgeCode.LIBRARY_200);
        }
    }

    @Override
    public void checkGenreBadges(UUID userId) {
        if (userGameRepository.countCompletedByUserIdAndGenreName(userId, GENRE_RPG) >= 10) {
            awardIfEligible(userId, BadgeCode.RPG_DISCIPLE);
        }
        if (userGameRepository.countCompletedByUserIdAndGenreName(userId, GENRE_SHOOTER) >= 10) {
            awardIfEligible(userId, BadgeCode.SHOOTER_TRIGGER_HAPPY);
        }
        if (userGameRepository.countCompletedByUserIdAndGenreName(userId, GENRE_PLATFORM) >= 10) {
            awardIfEligible(userId, BadgeCode.PLATFORMER_HERO);
        }
        if (userGameRepository.countCompletedByUserIdAndGenreName(userId, GENRE_INDIE) >= 20) {
            awardIfEligible(userId, BadgeCode.INDIE_GEM_HUNTER);
        }
    }

    @Override
    public void checkReviewQualityBadges(UUID userId) {
        // FIVE_STAR_STREAK: three most recent reviews all linked to a play log with score == 10.
        List<Review> recent = reviewRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId);
        if (recent.size() == 3 && recent.stream().allMatch(BadgeAwardingServiceImpl::isFiveStar)) {
            awardIfEligible(userId, BadgeCode.FIVE_STAR_STREAK);
        }

        // BRUTAL_CRITIC: ten reviews with linked play log score <= 2.
        if (reviewRepository.countOneStarReviewsByUserId(userId) >= 10) {
            awardIfEligible(userId, BadgeCode.BRUTAL_CRITIC);
        }

        // WORDSMITH: any review with content length >= 1000.
        if (reviewRepository.existsLongReviewByUserId(userId)) {
            awardIfEligible(userId, BadgeCode.WORDSMITH);
        }
    }

    private static boolean isFiveStar(Review review) {
        UserGamePlay play = review.getUserGamePlay();
        return play != null && play.getScore() != null && play.getScore() == 10;
    }

    @Override
    public void checkSocialBadges(UUID userId) {
        if (userRepository.countFollowingByUserId(userId) >= 10) {
            awardIfEligible(userId, BadgeCode.NETWORKER);
        }
        if (userRepository.countFollowersByUserId(userId) >= 10) {
            awardIfEligible(userId, BadgeCode.CHARISMATIC);
        }
        if (likeRepository.countByUserId(userId) >= 100) {
            awardIfEligible(userId, BadgeCode.PRAISE_THE_SUN);
        }
        if (likeRepository.countLikesReceivedOnReviewsByUserId(userId) >= 50) {
            awardIfEligible(userId, BadgeCode.BELOVED_REVIEWER);
        }
    }

    @Override
    public void checkRatingBadges(UUID userId) {
        if (rateRepository.countOneStarByUserId(userId) == CAKE_IS_A_LIE_ONE_STAR_COUNT) {
            awardIfEligible(userId, BadgeCode.THE_CAKE_IS_A_LIE);
        }
        if (rateRepository.existsRateChangedAtLeastByUserId(userId, INDECISIVE_CHANGE_THRESHOLD)) {
            awardIfEligible(userId, BadgeCode.INDECISIVE);
        }
    }

    @Override
    public void checkGameRemovedBadges(UUID userId) {
        awardIfEligible(userId, BadgeCode.YOU_DIED);
    }

    @Override
    public void checkReviewDeletedBadges(UUID userId, LocalDateTime reviewCreatedAt) {
        if (reviewCreatedAt == null) {
            return;
        }
        long minutesLived = ChronoUnit.MINUTES.between(reviewCreatedAt, LocalDateTime.now());
        if (minutesLived >= 0 && minutesLived <= MISSION_FAILED_DELETE_WINDOW_MINUTES) {
            awardIfEligible(userId, BadgeCode.MISSION_FAILED);
        }
    }

    @Override
    public void checkGameStartedBadges(UUID userId) {
        if (backlogRepository.countByUserId(userId) >= LEEROY_BACKLOG_THRESHOLD) {
            awardIfEligible(userId, BadgeCode.LEEROY);
        }
    }

    @Override
    public void checkReturningUserBadges(UUID userId, long daysAway) {
        if (daysAway >= FREEMAN_DAYS_AWAY) {
            awardIfEligible(userId, BadgeCode.WAKE_UP_MR_FREEMAN);
        }
    }

    @Override
    public void awardLongBacklogUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(SNAKE_BACKLOG_DAYS);
        List<UUID> userIds = backlogRepository.findUserIdsWithEntryOlderThan(cutoff);
        log.info("SNAKE_BACKLOG sweep: {} candidate user(s) with backlog entries older than {} days",
                userIds.size(), SNAKE_BACKLOG_DAYS);
        for (UUID userId : userIds) {
            awardIfEligible(userId, BadgeCode.SNAKE_BACKLOG);
        }
    }

    @Override
    public void checkReaderBadges(UUID userId) {
        if (reviewViewRepository.countByUserId(userId) >= STAY_AWHILE_REVIEWS_THRESHOLD) {
            awardIfEligible(userId, BadgeCode.STAY_AWHILE_REVIEWS);
        }
    }

    @Override
    public void checkLongevityBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        long days = ChronoUnit.DAYS.between(
                user.getCreatedAt().toLocalDate(),
                LocalDate.now(ZoneOffset.UTC));
        if (days >= 30) {
            awardIfEligible(userId, BadgeCode.VETERAN_30);
        }
        if (days >= 365) {
            awardIfEligible(userId, BadgeCode.LIFER);
        }
    }
}
