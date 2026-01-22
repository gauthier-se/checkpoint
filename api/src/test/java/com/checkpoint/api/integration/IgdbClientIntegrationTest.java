package com.checkpoint.api.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for IGDB API client.
 * These tests require valid IGDB_CLIENT_ID and IGDB_CLIENT_SECRET environment variables to be set.
 */
@SpringBootTest
@Testcontainers
@EnabledIfEnvironmentVariable(named = "IGDB_CLIENT_ID", matches = ".+")
class IgdbClientIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    @Qualifier("igdbClient")
    private RestClient igdbClient;

    /**
     * Tests that the IGDB client can successfully call the /platforms endpoint
     * and receive a 200 OK response.
     * Note: IGDB uses POST requests with body for queries.
     */
    @Test
    void getPlatforms_shouldReturnOk() {
        var response = igdbClient.post()
                .uri("/platforms")
                .body("fields name; limit 10;")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("name"));
    }
}
