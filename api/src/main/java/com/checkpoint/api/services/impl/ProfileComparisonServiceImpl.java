package com.checkpoint.api.services.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.profile.CommonGameEntryDto;
import com.checkpoint.api.dto.profile.ProfileComparisonDto;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.exceptions.ProfilePrivateException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.ProfileComparisonService;

/**
 * Implementation of {@link ProfileComparisonService}.
 *
 * <p>The affinity score (0–100) blends two components:</p>
 * <ol>
 *   <li><b>Library overlap</b> (Jaccard, 60% weight): {@code commonCount / unionCount * 100},
 *       guarded against an empty union.</li>
 *   <li><b>Rating similarity</b> (40% weight): the average absolute difference between both
 *       users' 5-star ratings for the games they both rated, capped at {@link #MAX_RATING_DIFF}
 *       and inverted to a 0–100 score.</li>
 * </ol>
 *
 * <p>When the two users share no rated games, the rating component carries no weight and the
 * final score equals the (rounded) library overlap score.</p>
 */
@Service
@Transactional(readOnly = true)
public class ProfileComparisonServiceImpl implements ProfileComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ProfileComparisonServiceImpl.class);

    private static final double LIBRARY_WEIGHT = 0.6;
    private static final double RATING_WEIGHT = 0.4;

    /** Maximum average rating difference (5-star scale) that maps to a rating score of 0. */
    private static final double MAX_RATING_DIFF = 4.0;

    private final UserRepository userRepository;
    private final UserGameRepository userGameRepository;
    private final RateRepository rateRepository;

    /**
     * Constructs a new ProfileComparisonServiceImpl.
     */
    public ProfileComparisonServiceImpl(UserRepository userRepository,
                                        UserGameRepository userGameRepository,
                                        RateRepository rateRepository) {
        this.userRepository = userRepository;
        this.userGameRepository = userGameRepository;
        this.rateRepository = rateRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProfileComparisonDto compare(String viewerEmail, String targetUsername, Pageable pageable) {
        log.info("Comparing profile of viewer {} with target {}", viewerEmail, targetUsername);

        User viewer = userRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new UserNotFoundException(viewerEmail));
        User target = userRepository.findByPseudo(targetUsername)
                .orElseThrow(() -> new UserNotFoundException(targetUsername));

        if (viewer.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Cannot compare a profile with itself");
        }

        enforceComparisonPrivacy(viewer, target);

        UUID viewerId = viewer.getId();
        UUID targetId = target.getId();

        List<UUID> commonIds = userGameRepository.findCommonVideoGameIds(viewerId, targetId);
        int commonGamesCount = commonIds.size();

        long unionCount = userGameRepository.countDistinctGamesByUserIds(viewerId, targetId);
        int viewerLibrarySize = (int) userGameRepository.countByUserId(viewerId);
        int targetLibrarySize = (int) userGameRepository.countByUserId(targetId);

        List<CommonGameEntryDto> entries = buildCommonEntries(viewerId, targetId, commonIds);

        int affinityScore = computeAffinityScore(commonGamesCount, unionCount, entries);

        List<CommonGameEntryDto> sorted = entries.stream()
                .sorted(Comparator.comparing(CommonGameEntryDto::ratingDiff,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Page<CommonGameEntryDto> page = paginate(sorted, pageable);

        return new ProfileComparisonDto(
                affinityScore,
                commonGamesCount,
                viewerLibrarySize,
                targetLibrarySize,
                PagedResponseDto.from(page));
    }

    /**
     * Builds a {@link CommonGameEntryDto} for each game the two users have in common,
     * resolving both users' statuses and ratings in batch (two library queries plus one
     * rating query). Returns an empty list when there are no common games.
     */
    private List<CommonGameEntryDto> buildCommonEntries(UUID viewerId, UUID targetId, List<UUID> commonIds) {
        if (commonIds.isEmpty()) {
            return List.of();
        }

        List<UserGame> viewerGames = userGameRepository.findByUserIdAndVideoGameIdIn(viewerId, commonIds);
        Map<UUID, PlayStatus> targetStatusByGame = userGameRepository
                .findByUserIdAndVideoGameIdIn(targetId, commonIds).stream()
                .collect(Collectors.toMap(ug -> ug.getVideoGame().getId(), UserGame::getStatus));

        Map<UUID, Integer> viewerScoreByGame = new HashMap<>();
        Map<UUID, Integer> targetScoreByGame = new HashMap<>();
        for (Rate rate : rateRepository.findByUserIdInAndVideoGameIdIn(List.of(viewerId, targetId), commonIds)) {
            UUID gameId = rate.getVideoGame().getId();
            if (rate.getUser().getId().equals(viewerId)) {
                viewerScoreByGame.put(gameId, rate.getScore());
            } else if (rate.getUser().getId().equals(targetId)) {
                targetScoreByGame.put(gameId, rate.getScore());
            }
        }

        List<CommonGameEntryDto> entries = new ArrayList<>(viewerGames.size());
        for (UserGame viewerGame : viewerGames) {
            VideoGame game = viewerGame.getVideoGame();
            UUID gameId = game.getId();

            Double viewerRating = toStars(viewerScoreByGame.get(gameId));
            Double targetRating = toStars(targetScoreByGame.get(gameId));
            Double ratingDiff = (viewerRating != null && targetRating != null)
                    ? Math.abs(viewerRating - targetRating)
                    : null;

            entries.add(new CommonGameEntryDto(
                    gameId,
                    game.getTitle(),
                    game.getCoverUrl(),
                    game.getReleaseDate(),
                    viewerGame.getStatus(),
                    targetStatusByGame.get(gameId),
                    viewerRating,
                    targetRating,
                    ratingDiff));
        }
        return entries;
    }

    /**
     * Computes the affinity score (0–100). The library component is the Jaccard overlap of the
     * two libraries; the rating component inverts the average absolute rating difference for the
     * games both users rated. When there is no common rating, the rating component is omitted and
     * the final score is the rounded library score.
     */
    private int computeAffinityScore(int commonGamesCount, long unionCount, List<CommonGameEntryDto> entries) {
        double libraryScore = unionCount > 0
                ? (double) commonGamesCount / unionCount * 100.0
                : 0.0;

        List<Double> ratingDiffs = entries.stream()
                .map(CommonGameEntryDto::ratingDiff)
                .filter(Objects::nonNull)
                .toList();

        if (ratingDiffs.isEmpty()) {
            return (int) Math.round(libraryScore);
        }

        double avgDiff = ratingDiffs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double cappedDiff = Math.min(avgDiff, MAX_RATING_DIFF);
        double ratingScore = (1.0 - cappedDiff / MAX_RATING_DIFF) * 100.0;

        return (int) Math.round(LIBRARY_WEIGHT * libraryScore + RATING_WEIGHT * ratingScore);
    }

    /**
     * Enforces the comparison privacy rule: a private target may only be compared by a follower.
     * The self-compare case is rejected earlier, so the viewer is never the owner here.
     */
    private void enforceComparisonPrivacy(User viewer, User target) {
        if (!Boolean.TRUE.equals(target.getIsPrivate())) {
            return;
        }
        if (userRepository.isFollowing(viewer.getId(), target.getId())) {
            return;
        }
        throw new ProfilePrivateException(target.getPseudo());
    }

    /**
     * Converts a raw 1–10 score to the 5-star display scale ({@code score / 2}), or null.
     */
    private static Double toStars(Integer score) {
        return score == null ? null : score / 2.0;
    }

    /**
     * Builds a {@link Page} from an already-sorted list using in-memory pagination.
     * The common games list is bounded by the smaller library, so this stays cheap.
     */
    private Page<CommonGameEntryDto> paginate(List<CommonGameEntryDto> sorted, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= sorted.size()) {
            return new PageImpl<>(List.of(), pageable, sorted.size());
        }
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }
}
