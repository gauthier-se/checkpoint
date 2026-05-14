package com.checkpoint.api.client.impl;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.checkpoint.api.client.SteamApiClient;
import com.checkpoint.api.dto.steam.SteamOwnedGameDto;
import com.checkpoint.api.dto.steam.SteamOwnedGamesResponseDto;
import com.checkpoint.api.dto.steam.SteamPlayerSummariesResponseDto;
import com.checkpoint.api.dto.steam.SteamPlayerSummaryDto;
import com.checkpoint.api.dto.steam.SteamResolveVanityResponseDto;
import com.checkpoint.api.exceptions.SteamApiException;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

/**
 * Implementation of {@link SteamApiClient} backed by the public Steam Web API.
 *
 * <p>Mirrors the structure of {@link IgdbApiClientImpl}: a Resilience4j rate limiter
 * conservatively throttles calls to 1 request per second. Steam allows ~100k calls/day per key,
 * but bursts are punished, so we stay well under the threshold.</p>
 */
@Component
public class SteamApiClientImpl implements SteamApiClient {

    private static final Logger log = LoggerFactory.getLogger(SteamApiClientImpl.class);

    private final RestClient steamClient;
    private final String apiKey;
    private final RateLimiter rateLimiter;

    public SteamApiClientImpl(@Qualifier("steamClient") RestClient steamClient,
                              @Value("${steam.api.key:}") String apiKey) {
        this.steamClient = steamClient;
        this.apiKey = apiKey;
        this.rateLimiter = createRateLimiter();
    }

    private RateLimiter createRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(30))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        return registry.rateLimiter("steamApiLimiter");
    }

    @Override
    public Optional<SteamPlayerSummaryDto> fetchPlayerSummary(String steamId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new SteamApiException("Steam API key is not configured");
        }

        RateLimiter.waitForPermission(rateLimiter);

        log.debug("Fetching Steam player summary for {}", steamId);

        try {
            SteamPlayerSummariesResponseDto response = steamClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ISteamUser/GetPlayerSummaries/v0002/")
                            .queryParam("key", apiKey)
                            .queryParam("steamids", steamId)
                            .build())
                    .retrieve()
                    .body(SteamPlayerSummariesResponseDto.class);

            if (response == null || response.response() == null) {
                return Optional.empty();
            }
            List<SteamPlayerSummaryDto> players = response.response().players();
            if (players == null || players.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(players.get(0));
        } catch (Exception e) {
            log.error("Error calling Steam GetPlayerSummaries for {}: {}", steamId, e.getMessage());
            throw new SteamApiException("Failed to fetch player summary from Steam", e);
        }
    }

    @Override
    public Optional<String> resolveVanityUrl(String vanity) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new SteamApiException("Steam API key is not configured");
        }

        RateLimiter.waitForPermission(rateLimiter);

        log.debug("Resolving Steam vanity URL for {}", vanity);

        try {
            SteamResolveVanityResponseDto response = steamClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ISteamUser/ResolveVanityURL/v0001/")
                            .queryParam("key", apiKey)
                            .queryParam("vanityurl", vanity)
                            .build())
                    .retrieve()
                    .body(SteamResolveVanityResponseDto.class);

            if (response == null || response.response() == null) {
                return Optional.empty();
            }
            SteamResolveVanityResponseDto.SteamResolveVanityResponse inner = response.response();
            if (inner.success() == null || inner.success() != 1) {
                return Optional.empty();
            }
            if (inner.steamId() == null || inner.steamId().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(inner.steamId());
        } catch (Exception e) {
            log.error("Error calling Steam ResolveVanityURL for {}: {}", vanity, e.getMessage());
            throw new SteamApiException("Failed to resolve Steam vanity URL", e);
        }
    }

    @Override
    public List<SteamOwnedGameDto> getOwnedGames(String steamId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new SteamApiException("Steam API key is not configured");
        }

        RateLimiter.waitForPermission(rateLimiter);

        log.debug("Fetching Steam owned games for {}", steamId);

        try {
            SteamOwnedGamesResponseDto response = steamClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/IPlayerService/GetOwnedGames/v0001/")
                            .queryParam("key", apiKey)
                            .queryParam("steamid", steamId)
                            .queryParam("include_appinfo", "true")
                            .queryParam("include_played_free_games", "true")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(SteamOwnedGamesResponseDto.class);

            if (response == null || response.response() == null) {
                return List.of();
            }
            List<SteamOwnedGameDto> games = response.response().games();
            return games != null ? games : List.of();
        } catch (Exception e) {
            log.error("Error calling Steam GetOwnedGames for {}: {}", steamId, e.getMessage());
            throw new SteamApiException("Failed to fetch owned games from Steam", e);
        }
    }
}
