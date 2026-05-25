package com.checkpoint.api.services;

import java.util.List;
import java.util.UUID;

import com.checkpoint.api.dto.catalog.GameCardDto;

/**
 * Service interface for item-to-item "similar games" discovery.
 *
 * <p>Unlike {@link GameRecommendationService}, which is personalised around a user's
 * whole library, this answers "what is similar to <em>this</em> game" by scoring the
 * catalog against the seed game's genres / platforms / companies (shared tag overlap,
 * reusing {@code GameTagScorer}). When a viewer is provided, games already in their
 * library / wishlist / favorites / likes are filtered out; anonymous callers get the
 * unfiltered ranking.</p>
 */
public interface GameSimilarityService {

    /**
     * Returns up to {@code size} games similar to the given game, best match first.
     *
     * @param gameId      the seed game's ID
     * @param viewerEmail the authenticated viewer's email, or {@code null} when anonymous
     * @param size        requested number of results (clamped to 1..30)
     * @return ordered list of similar game cards; empty when the seed game has no
     *         genre/company tags or no neighbour survives the filters
     * @throws com.checkpoint.api.exceptions.GameNotFoundException if the seed game does not exist
     */
    List<GameCardDto> getSimilarGames(UUID gameId, String viewerEmail, int size);
}
