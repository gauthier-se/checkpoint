package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seyzeriat.desktop.dto.LoginResponseDto;
import com.seyzeriat.desktop.exception.AuthenticationException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.TokenManager;

/**
 * API client implementation for {@link AuthenticationService}.
 * Handles HTTP communication with the backend to manage user authentication.
 */
public class AuthApiClient implements AuthenticationService {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new AuthApiClient using the default HTTP client.
     */
    public AuthApiClient() {
        this(HttpClient.newHttpClient());
    }

    /**
     * Constructs a new AuthApiClient with a provided HTTP client.
     *
     * @param httpClient the HTTP client to use for requests
     */
    public AuthApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Authenticates a user with their email and password.
     *
     * @param email the user's email address
     * @param password the user's password
     * @throws AuthenticationException if the authentication fails
     */
    @Override
    public void login(String email, String password) throws AuthenticationException {
        String jsonBody = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/auth/token"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LoginResponseDto dto = objectMapper.readValue(response.body(), LoginResponseDto.class);
                if (dto.getAccessToken() == null || dto.getAccessToken().isBlank()) {
                    throw new AuthenticationException("Le serveur n'a pas retourné de token.");
                }
                TokenManager.getInstance().setToken(dto.getAccessToken());
                TokenManager.getInstance().setRefreshToken(dto.getRefreshToken());
            } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new AuthenticationException("Email ou mot de passe incorrect.");
            } else {
                throw new AuthenticationException(
                        "Erreur serveur (HTTP " + response.statusCode() + "). Veuillez réessayer.");
            }
        } catch (IOException e) {
            throw new AuthenticationException("Impossible de contacter le serveur : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("La requête a été interrompue.");
        }
    }

    /**
     * Refreshes the authentication tokens using the current refresh token.
     *
     * @throws AuthenticationException if the token refresh fails
     */
    @Override
    public void refreshTokens() throws AuthenticationException {
        String refreshToken = TokenManager.getInstance().getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationException("No refresh token available. Please log in again.");
        }

        String jsonBody = String.format("{\"refreshToken\":\"%s\"}", refreshToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/auth/refresh"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Client-Type", "Desktop")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LoginResponseDto dto = objectMapper.readValue(response.body(), LoginResponseDto.class);
                if (dto.getAccessToken() == null || dto.getAccessToken().isBlank()) {
                    throw new AuthenticationException("Le serveur n'a pas retourné de token.");
                }
                TokenManager.getInstance().setToken(dto.getAccessToken());
                TokenManager.getInstance().setRefreshToken(dto.getRefreshToken());
            } else if (response.statusCode() == 401) {
                TokenManager.getInstance().clear();
                throw new AuthenticationException("Session expirée. Veuillez vous reconnecter.");
            } else {
                throw new AuthenticationException(
                        "Erreur serveur (HTTP " + response.statusCode() + "). Veuillez réessayer.");
            }
        } catch (IOException e) {
            throw new AuthenticationException("Impossible de contacter le serveur : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("La requête a été interrompue.");
        }
    }

    /**
     * Logs out the current user by clearing the authentication tokens.
     */
    @Override
    public void logout() {
        TokenManager.getInstance().clear();
    }
}
