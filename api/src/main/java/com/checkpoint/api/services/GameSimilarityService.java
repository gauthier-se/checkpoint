package com.checkpoint.api.services;

import java.util.List;
import java.util.UUID;

import com.checkpoint.api.dto.catalog.GameCardDto;

/**
 * Service interface for item-to-item "similar games" discovery.
 *
 * <p>Scores the catalog against the seed game's genres / platforms / companies
 * (shared tag overlap, via {@code GameTagScorer}) and returns the top matches.</p>
 */
public interface GameSimilarityService {

    /**
     * Returns up to {@code size} games similar to the given game, best match first.
     *
     * @param gameId the seed game's ID
     * @param size   requested number of results (clamped to 1..30)
     * @return ordered list of similar game cards; empty when the seed game has no
     *         genre/company tags or no neighbours exist
     * @throws com.checkpoint.api.exceptions.GameNotFoundException if the seed game does not exist
     */
    List<GameCardDto> getSimilarGames(UUID gameId, int size);
}
