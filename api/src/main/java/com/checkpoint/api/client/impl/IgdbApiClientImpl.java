package com.checkpoint.api.client.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.dto.igdb.IgdbExternalGameDto;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbTimeToBeatDto;
import com.checkpoint.api.exceptions.IgdbApiException;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

/**
 * Implementation of {@link IgdbApiClient} with rate limiting.
 * Uses Resilience4j RateLimiter to ensure we don't exceed IGDB API limits.
 *
 * IGDB API allows approximately 4 requests per second, but we use 1 per second
 * to be safe and avoid any risk of being rate limited.
 */
@Component
public class IgdbApiClientImpl implements IgdbApiClient {

    private static final Logger log = LoggerFactory.getLogger(IgdbApiClientImpl.class);

    /**
     * Common fields to request from IGDB for game data.
     * Using expanded fields (*) for nested objects.
     */
    private static final String GAME_FIELDS = """
            fields id, name, slug, summary, storyline, first_release_date,
            rating, rating_count, aggregated_rating, aggregated_rating_count,
            total_rating, total_rating_count, url,
            cover.*, genres.*, platforms.*, platforms.platform_logo.*,
            involved_companies.*, involved_companies.company.*,
            involved_companies.company.logo.*, screenshots.*,
            artworks.image_id, artworks.url,
            videos.name, videos.video_id,
            game_modes.*, themes.*, player_perspectives.*;
            """;

    private final RestClient igdbClient;
    private final RateLimiter rateLimiter;

    public IgdbApiClientImpl(RestClient igdbClient) {
        this.igdbClient = igdbClient;
        this.rateLimiter = createRateLimiter();
    }

    /**
     * Creates a rate limiter configured for 1 request per second.
     * This is conservative but ensures we never hit IGDB rate limits.
     */
    private RateLimiter createRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(30))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        return registry.rateLimiter("igdbApiLimiter");
    }

    @Override
    public List<IgdbGameDto> fetchRecentlyReleasedGames(int limit) {
        log.info("Fetching {} recently released games from IGDB", limit);

        long now = Instant.now().getEpochSecond();
        long thirtyDaysAgo = now - (30L * 24 * 60 * 60);

        String query = GAME_FIELDS + String.format("""
                where first_release_date >= %d & first_release_date <= %d;
                sort first_release_date desc;
                limit %d;
                """, thirtyDaysAgo, now, limit);

        return executeQuery("/games", query);
    }

    @Override
    public List<IgdbGameDto> fetchGamesByIds(List<Long> gameIds) {
        if (gameIds == null || gameIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Fetching {} games by IDs from IGDB", gameIds.size());

        String idsString = String.join(",", gameIds.stream().map(String::valueOf).toList());
        String query = GAME_FIELDS + String.format("""
                where id = (%s);
                limit %d;
                """, idsString, gameIds.size());

        return executeQuery("/games", query);
    }

    @Override
    public List<IgdbGameDto> searchGames(String searchQuery, int limit) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return Collections.emptyList();
        }

        log.info("Searching for games matching '{}' on IGDB", searchQuery);

        String query = GAME_FIELDS + String.format("""
                search "%s";
                limit %d;
                """, searchQuery.replace("\"", "\\\""), limit);

        return executeQuery("/games", query);
    }

    @Override
    public List<IgdbGameDto> fetchTopRatedGames(int limit, int minRatingCount) {
        log.info("Fetching top {} rated games from IGDB (min {} ratings)", limit, minRatingCount);

        String query = GAME_FIELDS + String.format("""
                where total_rating_count >= %d & total_rating != null;
                sort total_rating desc;
                limit %d;
                """, minRatingCount, limit);

        return executeQuery("/games", query);
    }

    /**
     * Maximum number of rows IGDB will return in a single {@code /external_games} response.
     * The hard cap is 500; we chunk the input to stay at or below it.
     */
    private static final int EXTERNAL_GAMES_BATCH_SIZE = 500;

    /**
     * IGDB {@code external_game_source} identifier for Steam. The legacy {@code category}
     * field on {@code /external_games} was replaced by {@code external_game_source}
     * (a foreign key to the {@code external_game_sources} table); the integer value
     * for Steam is unchanged at {@code 1}.
     */
    private static final int STEAM_EXTERNAL_GAME_SOURCE = 1;

    @Override
    public List<IgdbExternalGameDto> findIgdbIdsForSteamAppIds(List<Long> steamAppIds) {
        if (steamAppIds == null || steamAppIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Resolving {} Steam appIds via IGDB external_games", steamAppIds.size());

        List<IgdbExternalGameDto> aggregated = new ArrayList<>();
        for (int i = 0; i < steamAppIds.size(); i += EXTERNAL_GAMES_BATCH_SIZE) {
            List<Long> batch = steamAppIds.subList(
                    i, Math.min(i + EXTERNAL_GAMES_BATCH_SIZE, steamAppIds.size()));
            aggregated.addAll(fetchExternalGamesBatch(batch));
        }

        return aggregated.stream()
                .filter(row -> row.game() != null)
                .toList();
    }

    private List<IgdbExternalGameDto> fetchExternalGamesBatch(List<Long> batch) {
        RateLimiter.waitForPermission(rateLimiter);

        String uids = batch.stream()
                .map(appId -> "\"" + appId + "\"")
                .collect(Collectors.joining(","));

        String query = String.format("""
                fields uid,game;
                where external_game_source = %d & uid = (%s);
                limit %d;
                """, STEAM_EXTERNAL_GAME_SOURCE, uids, EXTERNAL_GAMES_BATCH_SIZE);

        log.debug("Executing IGDB external_games query for {} appIds", batch.size());

        try {
            List<IgdbExternalGameDto> result = igdbClient.post()
                    .uri("/external_games")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<IgdbExternalGameDto>>() {});

            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error executing IGDB external_games query: {}", e.getMessage(), e);
            throw new IgdbApiException("Failed to fetch external games from IGDB", e);
        }
    }

    @Override
    public IgdbTimeToBeatDto fetchTimeToBeat(long igdbGameId) {
        RateLimiter.waitForPermission(rateLimiter);

        String query = String.format("""
                fields game_id, normally, hastily, completely;
                where game_id = %d;
                limit 1;
                """, igdbGameId);

        try {
            List<IgdbTimeToBeatDto> result = igdbClient.post()
                    .uri("/game_time_to_beat")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<IgdbTimeToBeatDto>>() {});

            if (result == null || result.isEmpty()) {
                return null;
            }
            return result.get(0);
        } catch (Exception e) {
            log.warn("Failed to fetch time-to-beat for IGDB game {}: {}", igdbGameId, e.getMessage());
            return null;
        }
    }

    /**
     * Executes a query against the IGDB API with rate limiting.
     *
     * @param endpoint the API endpoint (e.g., "/games")
     * @param query the IGDB API query
     * @return list of games from the response
     */
    private List<IgdbGameDto> executeQuery(String endpoint, String query) {
        // Wait for rate limiter permission
        RateLimiter.waitForPermission(rateLimiter);

        log.debug("Executing IGDB query on {}: {}", endpoint, query.replace("\n", " "));

        try {
            List<IgdbGameDto> result = igdbClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<IgdbGameDto>>() {});

            log.debug("IGDB returned {} games", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();

        } catch (Exception e) {
            log.error("Error executing IGDB query: {}", e.getMessage(), e);
            throw new IgdbApiException("Failed to fetch data from IGDB", e);
        }
    }
}
