package com.seyzeriat.desktop.service.impl;

import com.seyzeriat.desktop.exception.AuthenticationException;
import com.seyzeriat.desktop.service.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthApiClientTest {

    private HttpClient mockHttpClient;
    private AuthApiClient authApiClient;

    @BeforeEach
    void setUp() {
        TokenManager.getInstance().clear();
        mockHttpClient = mock(HttpClient.class);
        authApiClient = new AuthApiClient(mockHttpClient);
    }

    @Test
    void testLoginSuccess() throws Exception {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"accessToken\":\"test-access-token\", \"refreshToken\":\"test-refresh-token\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act
        authApiClient.login("test@test.com", "password123");

        // Assert
        assertEquals("test-access-token", TokenManager.getInstance().getToken());
        assertEquals("test-refresh-token", TokenManager.getInstance().getRefreshToken());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertEquals("POST", request.method());
        assertTrue(request.uri().toString().endsWith("/api/auth/token"));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(401); // Unauthorized
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authApiClient.login("test@test.com", "wrong")
        );
        assertTrue(ex.getMessage().contains("incorrect"), "Exception message should mention incorrect credentials");
        assertNull(TokenManager.getInstance().getToken(), "Token should be null");
    }

    @Test
    void testRefreshTokensSuccess() throws Exception {
        // Arrange
        TokenManager.getInstance().setRefreshToken("old-refresh-token");
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"accessToken\":\"new-access-token\", \"refreshToken\":\"new-refresh-token\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act
        authApiClient.refreshTokens();

        // Assert
        assertEquals("new-access-token", TokenManager.getInstance().getToken());
        assertEquals("new-refresh-token", TokenManager.getInstance().getRefreshToken());
    }

    @Test
    void testLogoutClearsTokens() {
        // Arrange
        TokenManager.getInstance().setToken("some-token");
        TokenManager.getInstance().setRefreshToken("some-refresh");

        // Act
        authApiClient.logout();

        // Assert
        assertNull(TokenManager.getInstance().getToken());
        assertNull(TokenManager.getInstance().getRefreshToken());
    }
}
