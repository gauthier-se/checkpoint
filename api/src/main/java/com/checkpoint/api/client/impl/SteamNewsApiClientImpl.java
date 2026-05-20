package com.checkpoint.api.client.impl;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.checkpoint.api.client.SteamNewsApiClient;
import com.checkpoint.api.dto.steam.SteamNewsResponseDto;
import com.checkpoint.api.exceptions.SteamApiException;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

/**
 * Implementation of {@link SteamNewsApiClient}.
 *
 * <p>Mirrors {@link SteamApiClientImpl}: same {@code steamClient} {@code RestClient}
 * bean, separate Resilience4j {@link RateLimiter} (1 req/s) so the news pass cannot
 * starve the profile-refresh pass.</p>
 */
@Component
public class SteamNewsApiClientImpl implements SteamNewsApiClient {

    private static final Logger log = LoggerFactory.getLogger(SteamNewsApiClientImpl.class);

    private final RestClient steamClient;
    private final RateLimiter rateLimiter;

    public SteamNewsApiClientImpl(@Qualifier("steamClient") RestClient steamClient) {
        this.steamClient = steamClient;
        this.rateLimiter = createRateLimiter();
    }

    private RateLimiter createRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(30))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        return registry.rateLimiter("steamNewsApiLimiter");
    }

    @Override
    public List<SteamNewsResponseDto.NewsItem> fetchNewsForApp(long steamAppId, int count) {
        RateLimiter.waitForPermission(rateLimiter);

        log.debug("Fetching Steam news for appId {} (count={})", steamAppId, count);

        try {
            SteamNewsResponseDto response = steamClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ISteamNews/GetNewsForApp/v0002/")
                            .queryParam("appid", steamAppId)
                            .queryParam("count", count)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(SteamNewsResponseDto.class);

            if (response == null || response.appnews() == null
                    || response.appnews().newsitems() == null) {
                return List.of();
            }
            return response.appnews().newsitems();
        } catch (Exception e) {
            log.error("Error calling Steam GetNewsForApp for appId {}: {}", steamAppId, e.getMessage());
            throw new SteamApiException("Failed to fetch news from Steam", e);
        }
    }
}
