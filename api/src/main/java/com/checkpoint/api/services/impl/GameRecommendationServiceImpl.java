package com.checkpoint.api.services.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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
 * <p>Builds a per-user affinity profile from rates, library statuses, and wishes,
 * then scores candidate games by the sum of shared genre / platform / company
 * weights plus a small average-rating tiebreaker and a recency boost.</p>
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
    private static final double WISH_WEIGHT = 0.5;

    private static final double GENRE_SCORE_WEIGHT = 1.0;
    private static final double COMPANY_SCORE_WEIGHT = 0.6;
    private static final double PLATFORM_SCORE_WEIGHT = 0.4;
    private static final double AVERAGE_RATING_TIEBREAKER_WEIGHT = 0.2;
    private static final double RECENCY_BOOST = 0.5;
    private static final int RECENCY_BOOST_YEARS = 2;

    private static final String COLD_START_REASON = "Trending this week";

    private final UserRepository userRepository;
    private final RateRepository rateRepository;
    private final UserGameRepository userGameRepository;
    private final WishRepository wishRepository;
    private final VideoGameRepository videoGameRepository;
    private final GameTrendingService gameTrendingService;

    public GameRecommendationServiceImpl(UserRepository userRepository,
                                         RateRepository rateRepository,
                                         UserGameRepository userGameRepository,
                                         WishRepository wishRepository,
                                         VideoGameRepository videoGameRepository,
                                         GameTrendingService gameTrendingService) {
        this.userRepository = userRepository;
        this.rateRepository = rateRepository;
        this.userGameRepository = userGameRepository;
        this.wishRepository = wishRepository;
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

        Map<UUID, Double> affinityByGameId = buildAffinityByGameId(userId);
        if (affinityByGameId.isEmpty()) {
            log.debug("Cold-start path for user {} — no liked games, falling back to trending", userEmail);
            return mapTrendingToRecommendations(gameTrendingService.getTrendingGames(validatedSize));
        }

        List<VideoGame> likedGames = videoGameRepository.findAllByIdInWithRelationships(affinityByGameId.keySet());

        Map<UUID, Double> genreScores = new HashMap<>();
        Map<UUID, Double> platformScores = new HashMap<>();
        Map<UUID, Double> companyScores = new HashMap<>();
        for (VideoGame liked : likedGames) {
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
                ensureNonEmpty(genreScores.keySet()),
                ensureNonEmpty(companyScores.keySet()),
                PageRequest.of(0, CANDIDATE_POOL_CAP));

        if (candidateIds.isEmpty()) {
            log.debug("No candidate games for user {} — falling back to trending", userEmail);
            return mapTrendingToRecommendations(gameTrendingService.getTrendingGames(validatedSize));
        }

        List<VideoGame> candidates = videoGameRepository.findAllByIdInWithRelationships(candidateIds);
        LocalDate today = LocalDate.now();

        List<ScoredCandidate> scored = new ArrayList<>(candidates.size());
        for (VideoGame candidate : candidates) {
            double genreContrib = sumScores(candidate.getGenres().stream().map(Genre::getId).toList(), genreScores);
            double platformContrib = sumScores(candidate.getPlatforms().stream().map(com.checkpoint.api.entities.Platform::getId).toList(), platformScores);
            double companyContrib = sumScores(candidate.getCompanies().stream().map(Company::getId).toList(), companyScores);

            double rating = candidate.getAverageRating() != null ? candidate.getAverageRating() : 0.0;
            double recency = isRecent(candidate.getReleaseDate(), today) ? RECENCY_BOOST : 0.0;

            double score = genreContrib * GENRE_SCORE_WEIGHT
                    + platformContrib * PLATFORM_SCORE_WEIGHT
                    + companyContrib * COMPANY_SCORE_WEIGHT
                    + rating * AVERAGE_RATING_TIEBREAKER_WEIGHT
                    + recency;

            if (score <= 0) {
                continue;
            }
            scored.add(new ScoredCandidate(candidate, score,
                    genreContrib * GENRE_SCORE_WEIGHT,
                    companyContrib * COMPANY_SCORE_WEIGHT));
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
            String reason = buildReason(s, likedGames, genreScores, companyScores);
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

    private Map<UUID, Double> buildAffinityByGameId(UUID userId) {
        Map<UUID, Double> affinity = new HashMap<>();

        for (Rate rate : rateRepository.findAllByUserId(userId)) {
            Integer score = rate.getScore();
            if (score == null) {
                continue;
            }
            double weight = ratingWeight(score);
            if (weight > 0) {
                affinity.merge(rate.getVideoGame().getId(), weight, Double::sum);
            }
        }

        for (UserGame ug : userGameRepository.findAllByUserId(userId)) {
            double weight = statusWeight(ug.getStatus());
            if (weight > 0) {
                affinity.merge(ug.getVideoGame().getId(), weight, Double::sum);
            }
        }

        for (UUID wishedId : wishRepository.findVideoGameIdsByUserId(userId)) {
            affinity.merge(wishedId, WISH_WEIGHT, Double::sum);
        }

        return affinity;
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

    private static double sumScores(Collection<UUID> ids, Map<UUID, Double> scores) {
        double sum = 0.0;
        for (UUID id : ids) {
            Double v = scores.get(id);
            if (v != null) {
                sum += v;
            }
        }
        return sum;
    }

    private static boolean isRecent(LocalDate releaseDate, LocalDate today) {
        return releaseDate != null && !releaseDate.isBefore(today.minusYears(RECENCY_BOOST_YEARS));
    }

    /**
     * Empty collections in JPQL {@code IN} clauses are rejected by some Hibernate
     * dialects. Substitute a sentinel UUID that cannot match any real row so the
     * query reduces to "false" rather than throwing.
     */
    private static Collection<UUID> ensureNonEmpty(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of(new UUID(0L, 0L));
        }
        return ids;
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
                               List<VideoGame> likedGames,
                               Map<UUID, Double> genreScores,
                               Map<UUID, Double> companyScores) {
        VideoGame candidate = scored.game;
        Set<UUID> candidateGenreIds = idSet(candidate.getGenres().stream().map(Genre::getId).toList());
        Set<UUID> candidateCompanyIds = idSet(candidate.getCompanies().stream().map(Company::getId).toList());

        VideoGame bestSingleLiked = null;
        int bestSharedTags = 0;
        for (VideoGame liked : likedGames) {
            int shared = 0;
            for (Genre g : liked.getGenres()) {
                if (candidateGenreIds.contains(g.getId())) {
                    shared++;
                }
            }
            for (Company c : liked.getCompanies()) {
                if (candidateCompanyIds.contains(c.getId())) {
                    shared++;
                }
            }
            if (shared > bestSharedTags
                    || (shared == bestSharedTags && shared > 0 && bestSingleLiked != null
                        && liked.getTitle() != null
                        && liked.getTitle().compareTo(bestSingleLiked.getTitle()) < 0)) {
                bestSharedTags = shared;
                bestSingleLiked = liked;
            }
        }

        if (bestSharedTags >= 3 && bestSingleLiked != null) {
            return "Because you liked " + bestSingleLiked.getTitle();
        }

        if (scored.companyContribution > scored.genreContribution && !candidateCompanyIds.isEmpty()) {
            Company topCompany = candidate.getCompanies().stream()
                    .filter(c -> companyScores.containsKey(c.getId()))
                    .max(Comparator
                            .comparingDouble((Company c) -> companyScores.getOrDefault(c.getId(), 0.0))
                            .thenComparing(Comparator.comparing(Company::getName, Comparator.nullsLast(String::compareTo)).reversed()))
                    .orElse(null);
            if (topCompany != null) {
                if (bestSingleLiked != null && bestSharedTags > 0) {
                    return "From " + topCompany.getName() + ", like " + bestSingleLiked.getTitle();
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
