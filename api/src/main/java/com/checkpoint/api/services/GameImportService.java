package com.checkpoint.api.services;

import java.util.List;

import com.checkpoint.api.entities.VideoGame;

/**
 * Service interface for importing games from external APIs.
 */
public interface GameImportService {

    /**
     * Imports recently released games from IGDB.
     *
     * @param limit maximum number of games to import
     * @return list of imported/updated video games
     */
    List<VideoGame> importRecentlyReleasedGames(int limit);

    /**
     * Imports games by their IGDB IDs.
     *
     * @param igdbIds list of IGDB game IDs to import
     * @return list of imported/updated video games
     */
    List<VideoGame> importGamesByIds(List<Long> igdbIds);

    /**
     * Searches and imports games matching a query.
     *
     * @param query the search query
     * @param limit maximum number of games to import
     * @return list of imported/updated video games
     */
    List<VideoGame> searchAndImportGames(String query, int limit);

    /**
     * Imports top rated games from IGDB.
     *
     * @param limit maximum number of games to import
     * @param minRatingCount minimum number of ratings required
     * @return list of imported/updated video games
     */
    List<VideoGame> importTopRatedGames(int limit, int minRatingCount);

    /**
     * Result object containing import statistics.
     */
    record ImportResult(int created, int updated, int failed, List<VideoGame> games) {}
}
