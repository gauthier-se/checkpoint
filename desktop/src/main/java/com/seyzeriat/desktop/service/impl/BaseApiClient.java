package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seyzeriat.desktop.exception.AuthenticationException;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.TokenManager;

/**
 * Abstract base class for API clients.
 * Provides common functionality for handling HTTP requests, including authentication
 * and token management.
 */
public abstract class BaseApiClient {

    protected static final String BASE_URL = "http://localhost:8080/api/v1";
    protected final HttpClient httpClient;
    protected final ObjectMapper objectMapper;
    private final AuthenticationService authService;

    /**
     * Constructs a new BaseApiClient using the default HTTP client.
     *
     * @param authService the authentication service to manage tokens
     */
    public BaseApiClient(AuthenticationService authService) {
        this(authService, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    /**
     * Constructs a new BaseApiClient with a specified HTTP client.
     *
     * @param authService the authentication service to manage tokens
     * @param httpClient  the HTTP client to use for requests
     */
    public BaseApiClient(AuthenticationService authService, HttpClient httpClient) {
        this.authService = authService;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected HttpResponse<String> sendWithAuth(HttpRequest.Builder builder)
            throws IOException, InterruptedException, UnauthorizedException {
        addAuthHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            try {
                authService.refreshTokens();
                addAuthHeader(builder);
                response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (AuthenticationException e) {
                TokenManager.getInstance().clear();
                throw new UnauthorizedException("Session expirée. Veuillez vous reconnecter.");
            }
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            TokenManager.getInstance().clear();
            throw new UnauthorizedException(
                    "Session expirée ou accès refusé (HTTP " + response.statusCode() + "). Veuillez vous reconnecter.");
        }

        return response;
    }

    private void addAuthHeader(HttpRequest.Builder builder) {
        String token = TokenManager.getInstance().getToken();
        if (token != null && !token.isBlank()) {
            builder.setHeader("Authorization", "Bearer " + token);
        }
    }
}
