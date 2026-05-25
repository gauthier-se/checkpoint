package com.checkpoint.api.services.impl;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;

/**
 * Shared tag-overlap scoring used by both the personalised recommendation service
 * ({@link GameRecommendationServiceImpl}) and the item-to-item similarity service
 * ({@link GameSimilarityServiceImpl}).
 *
 * <p>Given per-tag weight maps (genre / platform / company id &rarr; weight) it scores a
 * candidate game by the weighted sum of its shared tags, plus a small average-rating
 * tiebreaker and a recency boost. The recommendation service seeds the maps from the
 * user's taste profile; the similarity service seeds them from a single source game.</p>
 */
public final class GameTagScorer {

    public static final double GENRE_SCORE_WEIGHT = 1.0;
    public static final double COMPANY_SCORE_WEIGHT = 0.6;
    public static final double PLATFORM_SCORE_WEIGHT = 0.4;
    public static final double AVERAGE_RATING_TIEBREAKER_WEIGHT = 0.2;
    public static final double RECENCY_BOOST = 0.5;
    public static final int RECENCY_BOOST_YEARS = 2;

    private GameTagScorer() {
    }

    /**
     * Scores a candidate game against the given per-tag weight maps.
     *
     * @param candidate      the game being scored (genres / platforms / companies must be loaded)
     * @param genreScores    weight per genre id in the reference profile
     * @param platformScores weight per platform id in the reference profile
     * @param companyScores  weight per company id in the reference profile
     * @param today          reference date for the recency boost
     * @return the total score plus the weighted genre and company contributions (the latter
     *         two let the recommendation service phrase a reason)
     */
    public static TagScore score(VideoGame candidate,
                                 Map<UUID, Double> genreScores,
                                 Map<UUID, Double> platformScores,
                                 Map<UUID, Double> companyScores,
                                 LocalDate today) {
        double genreContribution =
                sumScores(candidate.getGenres().stream().map(Genre::getId).toList(), genreScores)
                        * GENRE_SCORE_WEIGHT;
        double platformContribution =
                sumScores(candidate.getPlatforms().stream().map(Platform::getId).toList(), platformScores)
                        * PLATFORM_SCORE_WEIGHT;
        double companyContribution =
                sumScores(candidate.getCompanies().stream().map(Company::getId).toList(), companyScores)
                        * COMPANY_SCORE_WEIGHT;

        double rating = candidate.getAverageRating() != null ? candidate.getAverageRating() : 0.0;
        double recency = isRecent(candidate.getReleaseDate(), today) ? RECENCY_BOOST : 0.0;

        double total = genreContribution
                + platformContribution
                + companyContribution
                + rating * AVERAGE_RATING_TIEBREAKER_WEIGHT
                + recency;

        return new TagScore(total, genreContribution, companyContribution);
    }

    /** Sums the weights of every tag id that appears in {@code scores}. */
    public static double sumScores(Collection<UUID> ids, Map<UUID, Double> scores) {
        double sum = 0.0;
        for (UUID id : ids) {
            Double v = scores.get(id);
            if (v != null) {
                sum += v;
            }
        }
        return sum;
    }

    /** A game counts as recent when released within the last {@link #RECENCY_BOOST_YEARS} years. */
    public static boolean isRecent(LocalDate releaseDate, LocalDate today) {
        return releaseDate != null && !releaseDate.isBefore(today.minusYears(RECENCY_BOOST_YEARS));
    }

    /**
     * Empty collections in JPQL {@code IN} clauses are rejected by some Hibernate
     * dialects. Substitute a sentinel UUID that cannot match any real row so the
     * query reduces to "false" rather than throwing.
     */
    public static Collection<UUID> ensureNonEmpty(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of(new UUID(0L, 0L));
        }
        return ids;
    }

    /**
     * A candidate's total score plus the weighted genre and company contributions.
     */
    public record TagScore(double total, double genreContribution, double companyContribution) {
    }
}
