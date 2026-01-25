package com.checkpoint.api.client;

import java.util.List;

import com.checkpoint.api.dto.igdb.IgdbGameDto;

/**
 * Interface for IGDB API client operations.
 * Abstracts the communication with the IGDB API.
 */
public interface IgdbApiClient {

    /**
     * Fetches recently released games from IGDB.
     *
     * @param limit maximum number of games to fetch
     * @return list of games
     */
    List<IgdbGameDto> fetchRecentlyReleasedGames(int limit);

    /**
     * Fetches games by their IDs from IGDB.
     *
     * @param gameIds list of IGDB game IDs
     * @return list of games
     */
    List<IgdbGameDto> fetchGamesByIds(List<Long> gameIds);

    /**
     * Searches games by name on IGDB.
     *
     * @param searchQuery the search term
     * @param limit maximum number of results
     * @return list of matching games
     */
    List<IgdbGameDto> searchGames(String searchQuery, int limit);

    /**
     * Fetches top rated games from IGDB.
     *
     * @param limit maximum number of games to fetch
     * @param minRatingCount minimum number of ratings required
     * @return list of top rated games
     */
    List<IgdbGameDto> fetchTopRatedGames(int limit, int minRatingCount);
}
