package com.checkpoint.api.config;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration class for IGDB API client.
 * Creates a RestClient bean configured with OAuth2 authentication via Twitch.
 * The access token is automatically managed and refreshed when needed.
 */
@Configuration
public class IgdbClientConfig {

    private static final Logger log = LoggerFactory.getLogger(IgdbClientConfig.class);

    @Value("${igdb.api.client-id}")
    private String clientId;

    @Value("${igdb.api.client-secret}")
    private String clientSecret;

    @Value("${igdb.api.base-url}")
    private String baseUrl;

    @Value("${igdb.api.auth-url}")
    private String authUrl;

    private String accessToken;
    private Instant tokenExpiry;

    /**
     * DTO for Twitch OAuth2 token response.
     */
    record TwitchTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") int expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {}

    /**
     * Creates a RestClient bean for IGDB API communication.
     * The Client-ID and Bearer token are automatically added to every request.
     *
     * @return configured RestClient instance
     */
    @Bean
    public RestClient igdbClient() {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("IGDB base URL must not be null or empty");
        }
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    // Ensure we have a valid access token
                    String token = getOrRefreshAccessToken();

                    // Add required IGDB headers
                    request.getHeaders().set("Client-ID", clientId);
                    request.getHeaders().set("Authorization", "Bearer " + token);

                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Gets the current access token, refreshing it if expired or not yet obtained.
     *
     * @return valid access token
     */
    private synchronized String getOrRefreshAccessToken() {
        if (accessToken == null || tokenExpiry == null || Instant.now().isAfter(tokenExpiry.minusSeconds(60))) {
            refreshAccessToken();
        }
        return accessToken;
    }

    /**
     * Requests a new access token from Twitch OAuth2 endpoint.
     */
    private void refreshAccessToken() {
        log.info("Refreshing IGDB access token...");

        RestClient authClient = RestClient.create();

        String tokenUrl = String.format("%s?client_id=%s&client_secret=%s&grant_type=client_credentials",
                authUrl, clientId, clientSecret);

        TwitchTokenResponse response = authClient.post()
                .uri(tokenUrl)
                .retrieve()
                .body(TwitchTokenResponse.class);

        if (response != null) {
            this.accessToken = response.accessToken();
            this.tokenExpiry = Instant.now().plusSeconds(response.expiresIn());
            log.info("IGDB access token refreshed successfully. Expires in {} seconds.", response.expiresIn());
        } else {
            throw new RuntimeException("Failed to obtain IGDB access token");
        }
    }
}
