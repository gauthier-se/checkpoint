package com.checkpoint.api.services.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.RecommendedGameDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.GameRecommendationService;
import com.checkpoint.api.services.GameTrendingService;

/**
 * v1 SQL-based tag-overlap implementation of {@link GameRecommendationService}.
 *
 * <p>Builds a per-user affinity profile from rates, library statuses, game-likes, and
 * wishes, then scores candidate games by the sum of shared genre / platform / company
 * weights plus a small average-rating tiebreaker and a recency boost. The profile also
 * remembers whether each source game was <em>loved</em> (rated/owned/liked) or only
 * <em>wishlisted</em>, so the per-recommendation reason can be phrased accordingly.</p>
 */
@Service
@Transactional(readOnly = true)
public class GameRecommendationServiceImpl implements GameRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(GameRecommendationServiceImpl.class);

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 30;
    private static final int CANDIDATE_POOL_CAP = 200;

    private static final double RATE_HIGH_WEIGHT = 2.0;
    private static final double RATE_MID_WEIGHT = 1.0;
    private static final double STATUS_COMPLETED_WEIGHT = 1.5;
    private static final double STATUS_PLAYING_WEIGHT = 0.5;
    private static final double LIKE_WEIGHT = 2.0;
    private static final double WISH_WEIGHT = 0.5;

    private static final String COLD_START_REASON = "Trending this week";

    private final UserRepository userRepository;
    private final RateRepository rateRepository;
    private final UserGameRepository userGameRepository;
    private final WishRepository wishRepository;
    private final LikeRepository likeRepository;
    private final VideoGameRepository videoGameRepository;
    private final GameTrendingService gameTrendingService;

    public GameRecommendationServiceImpl(UserRepository userRepository,
                                         RateRepository rateRepository,
                                         UserGameRepository userGameRepository,
                                         WishRepository wishRepository,
                                         LikeRepository likeRepository,
                                         VideoGameRepository videoGameRepository,
                                         GameTrendingService gameTrendingService) {
        this.userRepository = userRepository;
        this.rateRepository = rateRepository;
        this.userGameRepository = userGameRepository;
        this.wishRepository = wishRepository;
        this.likeRepository = likeRepository;
        this.videoGameRepository = videoGameRepository;
        this.gameTrendingService = gameTrendingService;
    }

    @Override
    public List<RecommendedGameDto> getRecommendationsFor(String userEmail, int size) {
        int validatedSize = clampSize(size);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));
        UUID userId = user.getId();

        log.debug("Building recommendations for user {} (size={})", userEmail, validatedSize);

        AffinityProfile profile = buildAffinityProfile(userId);
        Map<UUID, Double> affinityByGameId = profile.weightByGameId();
        if (affinityByGameId.isEmpty()) {
            log.debug("Cold-start path for user {} — no affinity signal, falling back to trending", userEmail);
            return mapTrendingToRecommendations(gameTrendingService.getTrendingGames(validatedSize));
        }

        List<VideoGame> affinityGames = videoGameRepository.findAllByIdInWithRelationships(affinityByGameId.keySet());

        Map<UUID, Double> genreScores = new HashMap<>();
        Map<UUID, Double> platformScores = new HashMap<>();
        Map<UUID, Double> companyScores = new HashMap<>();
        for (VideoGame liked : affinityGames) {
            double weight = affinityByGameId.getOrDefault(liked.getId(), 0.0);
            if (weight <= 0) {
                continue;
            }
            for (Genre g : liked.getGenres()) {
                genreScores.merge(g.getId(), weight, Double::sum);
            }
            for (com.checkpoint.api.entities.Platform p : liked.getPlatforms()) {
                platformScores.merge(p.getId(), weight, Double::sum);
            }
            for (Company c : liked.getCompanies()) {
                companyScores.merge(c.getId(), weight, Double::sum);
            }
        }

        if (genreScores.isEmpty() && companyScores.isEmpty()) {
            log.debug("Profile resolved to no genre/company signal for user {} — falling back to trending", userEmail);
            return mapTrendingToRecommendations(gameTrendingService.getTrendingGames(validatedSize));
        }

        List<UUID> candidateIds = videoGameRepository.findCandidateIdsForRecommendation(
                userId,
                GameTagScorer.ensureNonEmpty(genreScores.keySet()),
                GameTagScorer.ensureNonEmpty(companyScores.keySet()),
                PageRequest.of(0, CANDIDATE_POOL_CAP));

        if (candidateIds.isEmpty()) {
            log.debug("No candidate games for user {} — falling back to trending", userEmail);
            return mapTrendingToRecommendations(gameTrendingService.getTrendingGames(validatedSize));
        }

        List<VideoGame> candidates = videoGameRepository.findAllByIdInWithRelationships(candidateIds);
        LocalDate today = LocalDate.now();

        List<ScoredCandidate> scored = new ArrayList<>(candidates.size());
        for (VideoGame candidate : candidates) {
            GameTagScorer.TagScore tagScore =
                    GameTagScorer.score(candidate, genreScores, platformScores, companyScores, today);
            if (tagScore.total() <= 0) {
                continue;
            }
            scored.add(new ScoredCandidate(candidate, tagScore.total(),
                    tagScore.genreContribution(), tagScore.companyContribution()));
        }

        Comparator<ScoredCandidate> byTitle =
                Comparator.comparing(s -> s.game.getTitle(), Comparator.nullsLast(String::compareTo));
        scored.sort(Comparator
                .comparingDouble((ScoredCandidate s) -> s.score).reversed()
                .thenComparing(byTitle));

        List<RecommendedGameDto> top = new ArrayList<>(validatedSize);
        for (ScoredCandidate s : scored) {
            if (top.size() >= validatedSize) {
                break;
            }
            String reason = buildReason(s, affinityGames,
                    profile.lovedGameIds(), profile.wishlistedGameIds(),
                    genreScores, companyScores);
            top.add(new RecommendedGameDto(
                    s.game.getId(),
                    s.game.getTitle(),
                    s.game.getCoverUrl(),
                    s.game.getReleaseDate(),
                    s.game.getAverageRating(),
                    reason));
        }
        return top;
    }

    /**
     * Builds the user's affinity profile: a per-game weight map (used to score candidate
     * tags) plus two source sets — games the user <em>loved</em> (high/mid rates,
     * COMPLETED/PLAYING library entries, or game-likes) and games they only
     * <em>wishlisted</em>. The source sets let {@link #buildReason} phrase the reason as
     * "Because you liked …" versus "Similar to … on your wishlist". A game may legitimately
     * appear in both sets; {@code buildReason} treats "loved" as taking precedence.
     */
    private AffinityProfile buildAffinityProfile(UUID userId) {
        Map<UUID, Double> weightByGameId = new HashMap<>();
        Set<UUID> lovedGameIds = new HashSet<>();
        Set<UUID> wishlistedGameIds = new HashSet<>();

        for (Rate rate : rateRepository.findAllByUserId(userId)) {
            Integer score = rate.getScore();
            if (score == null) {
                continue;
            }
            double weight = ratingWeight(score);
            if (weight > 0) {
                UUID gameId = rate.getVideoGame().getId();
                weightByGameId.merge(gameId, weight, Double::sum);
                lovedGameIds.add(gameId);
            }
        }

        for (UserGame ug : userGameRepository.findAllByUserId(userId)) {
            double weight = statusWeight(ug.getStatus());
            if (weight > 0) {
                UUID gameId = ug.getVideoGame().getId();
                weightByGameId.merge(gameId, weight, Double::sum);
                lovedGameIds.add(gameId);
            }
        }

        for (UUID likedId : likeRepository.findVideoGameIdsByUser(userId)) {
            weightByGameId.merge(likedId, LIKE_WEIGHT, Double::sum);
            lovedGameIds.add(likedId);
        }

        for (UUID wishedId : wishRepository.findVideoGameIdsByUserId(userId)) {
            weightByGameId.merge(wishedId, WISH_WEIGHT, Double::sum);
            wishlistedGameIds.add(wishedId);
        }

        return new AffinityProfile(weightByGameId, lovedGameIds, wishlistedGameIds);
    }

    private static double ratingWeight(int score) {
        if (score >= 8) {
            return RATE_HIGH_WEIGHT;
        }
        if (score >= 6) {
            return RATE_MID_WEIGHT;
        }
        return 0.0;
    }

    private static double statusWeight(GameStatus status) {
        if (status == GameStatus.COMPLETED) {
            return STATUS_COMPLETED_WEIGHT;
        }
        if (status == GameStatus.PLAYING) {
            return STATUS_PLAYING_WEIGHT;
        }
        return 0.0;
    }

    private List<RecommendedGameDto> mapTrendingToRecommendations(List<GameCardDto> trending) {
        List<RecommendedGameDto> out = new ArrayList<>(trending.size());
        for (GameCardDto t : trending) {
            out.add(new RecommendedGameDto(
                    t.id(), t.title(), t.coverUrl(), t.releaseDate(), t.averageRating(),
                    COLD_START_REASON));
        }
        return out;
    }

    private String buildReason(ScoredCandidate scored,
                               List<VideoGame> affinityGames,
                               Set<UUID> lovedGameIds,
                               Set<UUID> wishlistedGameIds,
                               Map<UUID, Double> genreScores,
                               Map<UUID, Double> companyScores) {
        VideoGame candidate = scored.game;
        Set<UUID> candidateGenreIds = idSet(candidate.getGenres().stream().map(Genre::getId).toList());
        Set<UUID> candidateCompanyIds = idSet(candidate.getCompanies().stream().map(Company::getId).toList());

        VideoGame bestSource = null;
        int bestSharedTags = 0;
        for (VideoGame source : affinityGames) {
            int shared = 0;
            for (Genre g : source.getGenres()) {
                if (candidateGenreIds.contains(g.getId())) {
                    shared++;
                }
            }
            for (Company c : source.getCompanies()) {
                if (candidateCompanyIds.contains(c.getId())) {
                    shared++;
                }
            }
            if (shared > bestSharedTags
                    || (shared == bestSharedTags && shared > 0 && bestSource != null
                        && source.getTitle() != null
                        && source.getTitle().compareTo(bestSource.getTitle()) < 0)) {
                bestSharedTags = shared;
                bestSource = source;
            }
        }

        if (bestSharedTags >= 3 && bestSource != null) {
            // "loved" (rated/owned/liked) takes precedence over a pure wishlist source.
            if (lovedGameIds.contains(bestSource.getId())) {
                return "Because you liked " + bestSource.getTitle();
            }
            if (wishlistedGameIds.contains(bestSource.getId())) {
                return "Similar to " + bestSource.getTitle() + " on your wishlist";
            }
        }

        if (scored.companyContribution > scored.genreContribution && !candidateCompanyIds.isEmpty()) {
            Company topCompany = candidate.getCompanies().stream()
                    .filter(c -> companyScores.containsKey(c.getId()))
                    .max(Comparator
                            .comparingDouble((Company c) -> companyScores.getOrDefault(c.getId(), 0.0))
                            .thenComparing(Comparator.comparing(Company::getName, Comparator.nullsLast(String::compareTo)).reversed()))
                    .orElse(null);
            if (topCompany != null) {
                if (bestSource != null && bestSharedTags > 0) {
                    return "From " + topCompany.getName() + ", like " + bestSource.getTitle();
                }
                return "From " + topCompany.getName();
            }
        }

        Comparator<Genre> byGenreName =
                Comparator.comparing(Genre::getName, Comparator.nullsLast(String::compareTo));
        List<String> topGenreNames = candidate.getGenres().stream()
                .filter(g -> genreScores.containsKey(g.getId()))
                .sorted(Comparator
                        .comparingDouble((Genre g) -> genreScores.getOrDefault(g.getId(), 0.0)).reversed()
                        .thenComparing(byGenreName))
                .limit(2)
                .map(Genre::getName)
                .toList();
        if (!topGenreNames.isEmpty()) {
            return "Matches your favorite genres: " + String.join(", ", topGenreNames);
        }

        return "Recommended for you";
    }

    private static Set<UUID> idSet(List<UUID> ids) {
        return new HashSet<>(ids);
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * Per-user taste signal: the merged tag-scoring weight for each source game, plus the
     * set of games the user loved (rated/owned/liked) and the set they only wishlisted.
     */
    private record AffinityProfile(Map<UUID, Double> weightByGameId,
                                   Set<UUID> lovedGameIds,
                                   Set<UUID> wishlistedGameIds) {
    }

    private static final class ScoredCandidate {
        final VideoGame game;
        final double score;
        final double genreContribution;
        final double companyContribution;

        ScoredCandidate(VideoGame game, double score, double genreContribution, double companyContribution) {
            this.game = game;
            this.score = score;
            this.genreContribution = genreContribution;
            this.companyContribution = companyContribution;
        }
    }
}
