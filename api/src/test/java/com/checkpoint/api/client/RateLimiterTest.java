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
 * Unit tests for rate limiting behavior.
 * Tests that the rate limiter correctly throttles requests.
 */
class RateLimiterTest {

    @Test
    @DisplayName("RateLimiter should throttle requests to 1 per second")
    void rateLimiter_shouldThrottleRequests() {
        // Given - A rate limiter configured for 1 request per second
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("testLimiter");

        // When - Making 3 requests
        Instant start = Instant.now();

        // First request should be immediate
        RateLimiter.waitForPermission(rateLimiter);
        Instant afterFirst = Instant.now();

        // Second request should wait
        RateLimiter.waitForPermission(rateLimiter);
        Instant afterSecond = Instant.now();

        // Third request should wait
        RateLimiter.waitForPermission(rateLimiter);
        Instant afterThird = Instant.now();

        // Then
        // First request should be nearly instant
        Duration firstDuration = Duration.between(start, afterFirst);
        assertThat(firstDuration.toMillis()).isLessThan(100);

        // Total time for 3 requests should be at least 2 seconds (2 waits)
        Duration totalDuration = Duration.between(start, afterThird);
        assertThat(totalDuration.toMillis()).isGreaterThanOrEqualTo(1900);
    }

    @Test
    @DisplayName("RateLimiter should allow burst after refresh period")
    void rateLimiter_shouldAllowBurstAfterRefresh() throws InterruptedException {
        // Given
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(500))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("testLimiter2");

        // When - First request
        RateLimiter.waitForPermission(rateLimiter);

        // Wait for refresh
        Thread.sleep(600);

        // Second request after refresh
        Instant beforeSecond = Instant.now();
        RateLimiter.waitForPermission(rateLimiter);
        Instant afterSecond = Instant.now();

        // Then - Second request should be nearly instant after refresh
        Duration secondDuration = Duration.between(beforeSecond, afterSecond);
        assertThat(secondDuration.toMillis()).isLessThan(100);
    }
}
