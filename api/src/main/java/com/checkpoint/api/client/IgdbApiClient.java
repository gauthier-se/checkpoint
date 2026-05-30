package com.checkpoint.api.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.checkpoint.api.dto.igdb.IgdbExternalGameDto;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbTimeToBeatDto;

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

    /**
     * Fetches the time-to-beat statistics for a single game from IGDB.
     * Best-effort: returns {@code null} if the endpoint fails or returns no data.
     *
     * @param igdbGameId the IGDB game ID
     * @return the time-to-beat DTO, or {@code null} when unavailable
     */
    IgdbTimeToBeatDto fetchTimeToBeat(long igdbGameId);

    /**
     * Fetches the time-to-beat statistics for many games in a single batched
     * operation. The {@code /game_time_to_beat} endpoint caps each response at
     * 500 rows, so the input is chunked internally. Best-effort: a chunk that
     * fails is logged and skipped, so the import is never blocked.
     *
     * <p>This is the bulk-import counterpart of {@link #fetchTimeToBeat(long)}:
     * it replaces one request per game with one request per 500 games.</p>
     *
     * @param igdbGameIds the IGDB game IDs to look up
     * @return an IGDB game ID → time-to-beat map; games without data are absent
     */
    Map<Long, IgdbTimeToBeatDto> fetchTimeToBeatForGames(Collection<Long> igdbGameIds);

    /**
     * Resolves a list of Steam application IDs to IGDB game IDs via IGDB's
     * {@code /external_games} endpoint (category 1 = Steam).
     *
     * <p>The endpoint caps each response at 500 rows, so the input is chunked internally.
     * Rows whose {@code game} field is null (orphan external entries) are skipped.</p>
     *
     * @param steamAppIds the Steam application IDs to look up
     * @return one DTO per matched row; appIds without an IGDB match are simply absent
     */
    List<IgdbExternalGameDto> findIgdbIdsForSteamAppIds(List<Long> steamAppIds);

    /**
     * Resolves a collection of IGDB game IDs to their Steam application IDs via the
     * same {@code /external_games} endpoint. The IGDB-to-Steam direction is used by
     * the news import task to populate {@code VideoGame.steamAppId} lazily.
     *
     * <p>Games without a Steam release are simply absent from the returned map.</p>
     *
     * @param igdbIds the IGDB game IDs to look up
     * @return an IGDB game ID → Steam appId map
     */
    Map<Long, Long> findSteamAppIdsForIgdbIds(Collection<Long> igdbIds);
}
