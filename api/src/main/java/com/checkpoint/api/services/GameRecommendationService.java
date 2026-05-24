package com.checkpoint.api.services;

import java.util.List;

import com.checkpoint.api.dto.catalog.RecommendedGameDto;

/**
 * Service interface for personalised game recommendations.
 *
 * <p>v1 uses a SQL-based tag-overlap algorithm over the user's library
 * (rates, statuses, wishes) crossed with genres / platforms / companies.
 * The future ALS implementation (TE-268/269/270) is expected to drop in
 * behind this same interface without changing the wire contract.</p>
 */
public interface GameRecommendationService {

    /**
     * Returns up to {@code size} game recommendations for the given user.
     * Falls back to trending games (with reason {@code "Trending this week"})
     * when the user has no liked games yet.
     *
     * @param userEmail the authenticated user's email (resolved from the JWT)
     * @param size      requested number of recommendations (clamped to 1..30)
     * @return ordered list of recommendations, never null and — for an authenticated
     *         user — never empty as long as the catalog has at least one trending game
     */
    List<RecommendedGameDto> getRecommendationsFor(String userEmail, int size);
}
