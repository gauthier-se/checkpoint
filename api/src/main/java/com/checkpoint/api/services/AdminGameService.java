package com.checkpoint.api.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.checkpoint.api.dto.admin.CreateGameRequestDto;
import com.checkpoint.api.dto.admin.ExternalGameDto;
import com.checkpoint.api.dto.admin.ImportJobStatusDto;
import com.checkpoint.api.dto.admin.UpdateGameRequestDto;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.ExternalApiUnavailableException;
import com.checkpoint.api.exceptions.ExternalGameNotFoundException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameReferencedException;
import com.checkpoint.api.exceptions.ImportAlreadyRunningException;

/**
 * Service interface for admin game management operations.
 * Provides search and import functionality for external game APIs.
 */
public interface AdminGameService {

    /**
     * Searches for games on the external API (IGDB).
     * Returns lightweight DTOs for display before import.
     *
     * @param query the search term
     * @param limit maximum number of results (default 20)
     * @return list of matching external games
     */
    List<ExternalGameDto> searchExternalGames(String query, int limit);

    /**
     * Imports a single game by its external ID.
     * Fetches full details from the external API and saves to the database.
     *
     * @param externalId the IGDB game ID
     * @return the saved VideoGame entity
     * @throws ExternalGameNotFoundException if the game is not found on IGDB
     * @throws ExternalApiUnavailableException if IGDB API is unreachable
     */
    VideoGame importGameByExternalId(Long externalId);

    /**
     * Starts an asynchronous bulk import of the most popular games from IGDB
     * (ordered by rating count). Already-imported games are skipped
     * (deduplication by igdbId). Returns immediately with the job status;
     * progress is tracked via {@link #findImportJob(UUID)}.
     *
     * @param limit          maximum number of games to fetch from IGDB
     * @param minRatingCount minimum number of IGDB ratings to qualify as "popular"
     * @return the initial job status (state {@code PENDING}/{@code RUNNING})
     * @throws ImportAlreadyRunningException if another import is already in progress
     */
    ImportJobStatusDto startTopRatedImport(int limit, int minRatingCount);

    /**
     * Starts an asynchronous bulk import of recently released games from IGDB.
     * Already-imported games are skipped (deduplication by igdbId). Returns
     * immediately with the job status.
     *
     * @param limit maximum number of games to fetch from IGDB
     * @return the initial job status (state {@code PENDING}/{@code RUNNING})
     * @throws ImportAlreadyRunningException if another import is already in progress
     */
    ImportJobStatusDto startRecentImport(int limit);

    /**
     * Returns the current status of a bulk-import job, or empty if unknown
     * (never started, or evicted after completion).
     *
     * @param jobId the job identifier
     * @return the job status snapshot if found
     */
    Optional<ImportJobStatusDto> findImportJob(UUID jobId);

    /**
     * Manually creates a new video game from the supplied admin payload.
     * Title collisions (case-insensitive) and unknown genre/platform/company
     * IDs are rejected with {@link IllegalArgumentException} (mapped to 400).
     *
     * @param request the create payload (validated upstream)
     * @return the persisted video game with relationships loaded
     */
    VideoGame createGame(CreateGameRequestDto request);

    /**
     * Fully updates an existing video game. {@code igdbId} and {@code steamAppId}
     * are intentionally not modifiable here.
     *
     * @param gameId  the video game ID
     * @param request the update payload (validated upstream)
     * @return the updated video game with relationships loaded
     * @throws GameNotFoundException if no game matches {@code gameId}
     */
    VideoGame updateGame(UUID gameId, UpdateGameRequestDto request);

    /**
     * Deletes a video game if and only if no user-owned data references it.
     * Runs the integrity check before issuing the JPA delete so the
     * cascading associations on {@code VideoGame} cannot silently destroy
     * user rows.
     *
     * @param gameId the video game ID
     * @throws GameNotFoundException     if no game matches {@code gameId}
     * @throws GameReferencedException   if the game is still referenced
     */
    void deleteGame(UUID gameId);
}
