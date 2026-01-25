package com.checkpoint.api.client;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.checkpoint.api.dto.igdb.IgdbGameDto;

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
