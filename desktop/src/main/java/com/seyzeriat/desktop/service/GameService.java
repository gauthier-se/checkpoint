package com.seyzeriat.desktop.service;

import java.io.IOException;
import java.util.List;
import com.seyzeriat.desktop.dto.*;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.exception.GameReferencedException;

/**
 * Service interface for managing games.
 * Provides operations to search, import, manage, and retrieve game details and catalogs.
 */
public interface GameService {

    /**
     * Searches for games in an external system.
     *
     * @param query the search query
     * @param limit the maximum number of results to return
     * @return a list of external game results
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    List<ExternalGameResult> searchExternalGames(String query, int limit) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Imports a game from an external system.
     *
     * @param externalId the external identifier of the game to import
     * @return the result of the imported game
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    ImportedGameResult importGame(Long externalId) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Starts a job to import top-rated games.
     *
     * @param limit the maximum number of games to import
     * @param minRatingCount the minimum rating count for the games to be imported
     * @return the status of the import job
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    ImportJobStatus startTopRatedImport(int limit, int minRatingCount) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Starts a job to import recent games.
     *
     * @param limit the maximum number of recent games to import
     * @return the status of the import job
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    ImportJobStatus startRecentImport(int limit) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Retrieves the status of a specific import job.
     *
     * @param jobId the unique identifier of the import job
     * @return the current status of the job
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    ImportJobStatus getImportJob(String jobId) throws IOException, InterruptedException, UnauthorizedException;
    
    /**
     * Retrieves a paginated list of games.
     *
     * @param page the page number to retrieve (zero-based)
     * @param size the maximum number of items per page
     * @return a paged response containing game summaries
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    PagedResponse<GameSummaryResult> getGames(int page, int size) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Retrieves detailed information about a specific game.
     *
     * @param id the unique identifier of the game
     * @return the detailed result for the specified game
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    GameDetailResult getGameDetail(String id) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Creates a new game.
     *
     * @param payload the payload containing the details for the new game
     * @return the detailed result for the newly created game
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    GameDetailResult createGame(GameFormPayload payload) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Updates an existing game.
     *
     * @param id the unique identifier of the game to update
     * @param payload the payload containing the updated details
     * @return the detailed result for the updated game
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    GameDetailResult updateGame(String id, GameFormPayload payload) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Deletes a specific game.
     *
     * @param id the unique identifier of the game to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     * @throws GameReferencedException if the game cannot be deleted because it is referenced elsewhere
     */
    void deleteGame(String id) throws IOException, InterruptedException, UnauthorizedException, GameReferencedException;
    
    /**
     * Retrieves a list of available genres.
     *
     * @return a list of genre options
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    List<CatalogOption> getGenres() throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Retrieves a list of available platforms.
     *
     * @return a list of platform options
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    List<CatalogOption> getPlatforms() throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Retrieves a list of available companies.
     *
     * @return a list of company options
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    List<CatalogOption> getCompanies() throws IOException, InterruptedException, UnauthorizedException;
}
