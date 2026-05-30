package com.checkpoint.api.services;

import java.util.List;

import com.checkpoint.api.dto.igdb.IgdbGameDto;
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
     * Imports a pre-fetched list of games in bulk, skipping any game whose
     * {@code igdbId} already exists in the database. Failures on individual
     * games are caught and reported in the returned stats — the operation
     * does not abort.
     *
     * @param games the IGDB game DTOs to import
     * @return statistics describing the outcome
     */
    BulkImportStats bulkImport(List<IgdbGameDto> games);

    /**
     * Same as {@link #bulkImport(List)} but reports live progress through the
     * supplied listener after each game, so an asynchronous job can expose
     * how far along it is. Each game is persisted in its own transaction.
     *
     * @param games    the IGDB game DTOs to import
     * @param progress the listener notified after every processed game
     * @return statistics describing the outcome
     */
    BulkImportStats bulkImport(List<IgdbGameDto> games, ImportProgressListener progress);

    /**
     * Result object containing import statistics.
     */
    record ImportResult(int created, int updated, int failed, List<VideoGame> games) {}

    /**
     * Aggregated counts produced by {@link #bulkImport(List)}.
     *
     * @param totalFetched number of games supplied for import
     * @param imported     number of newly persisted games
     * @param skipped      number of games skipped because they already exist
     * @param failed       number of games whose import threw an exception
     * @param errors       titles (or IGDB IDs) of games that failed to import
     */
    record BulkImportStats(int totalFetched, int imported, int skipped, int failed, List<String> errors) {}
}
