package com.checkpoint.api.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

/**
 * Mirrors {@link RateLimiterTest} but validates the configuration used by
 * {@code SteamApiClientImpl} (1 request/sec, 30s timeout). Confirms that the Steam-specific
 * limiter throttles requests the same way the IGDB one does.
 */
class SteamApiClientRateLimiterTest {

    private static RateLimiter buildSteamLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(30))
                .build();
        return RateLimiterRegistry.of(config).rateLimiter("steamApiLimiterTest");
    }

    @Test
    @DisplayName("Steam rate limiter throttles to 1 request per second")
    void throttlesToOnePerSecond() {
        RateLimiter limiter = buildSteamLimiter();

        Instant start = Instant.now();
        RateLimiter.waitForPermission(limiter);
        RateLimiter.waitForPermission(limiter);
        RateLimiter.waitForPermission(limiter);
        Instant end = Instant.now();

        // 3 calls = 2 throttled waits; ideal is ~2000ms. Resilience4j's refresh
        // window isn't aligned with wall-clock time, so on a noisy CI runner
        // the two waits can be noticeably shorter than 1s each. 1500ms still
        // proves throttling (3 unthrottled calls would complete in <100ms)
        // without flaking — observed minimum on GHA: ~1720ms.
        assertThat(Duration.between(start, end).toMillis()).isGreaterThanOrEqualTo(1500);
    }
}
