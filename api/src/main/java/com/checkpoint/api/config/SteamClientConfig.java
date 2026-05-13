package com.checkpoint.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Configuration for the Steam Web API client.
 *
 * <p>Unlike IGDB, Steam authenticates each call with an API key passed as a query parameter,
 * so no bearer-token refresh logic is required. The key is read from {@code steam.api.key}
 * and injected by {@link com.checkpoint.api.client.impl.SteamApiClientImpl} when building requests.</p>
 */
@Configuration
public class SteamClientConfig {

    @Value("${steam.api.base-url}")
    private String baseUrl;

    /**
     * RestClient targeting the Steam Web API ({@code https://api.steampowered.com}).
     *
     * @return configured RestClient instance
     */
    @Bean
    @Qualifier("steamClient")
    public RestClient steamClient() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Steam base URL must not be null or empty");
        }
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
