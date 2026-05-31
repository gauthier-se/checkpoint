package com.checkpoint.api.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.GameDetailDto;

/**
 * Service interface for game catalog operations.
 * Provides methods for retrieving games for public display.
 */
public interface GameCatalogService {

    /**
     * Retrieves a paginated list of games for the catalog with optional filters.
     *
     * @param pageable   pagination and sorting parameters
     * @param genres     optional genre name filters (case-insensitive; matches any)
     * @param platforms  optional platform name filters (case-insensitive; matches any)
     * @param yearMin    optional minimum release year (inclusive)
     * @param yearMax    optional maximum release year (inclusive)
     * @param ratingMin  optional minimum average rating (inclusive)
     * @param ratingMax  optional maximum average rating (inclusive)
     * @return page of game cards matching the filters
     */
    Page<GameCardDto> getGameCatalog(Pageable pageable,
                                      List<String> genres,
                                      List<String> platforms,
                                      Integer yearMin,
                                      Integer yearMax,
                                      Double ratingMin,
                                      Double ratingMax);

    /**
     * Retrieves detailed information about a specific game.
     *
     * @param id the game ID
     * @return game details
     * @throws GameNotFoundException if the game is not found
     */
    GameDetailDto getGameDetails(UUID id);

    /**
     * Returns the games appearing in the most users' backlogs, ranked by descending count.
     *
     * @param size the maximum number of games to return
     * @return the most-backlogged games as game cards
     */
    List<GameCardDto> getMostBackloggedGames(int size);

    /**
     * Returns the games appearing in the most users' wishlists, ranked by descending count.
     *
     * @param size the maximum number of games to return
     * @return the most-wishlisted games as game cards
     */
    List<GameCardDto> getMostWishlistedGames(int size);
}
