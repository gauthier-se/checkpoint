package com.seyzeriat.desktop.service.impl;

import com.seyzeriat.desktop.exception.ApiException;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BaseApiClientTest {

    private HttpClient mockHttpClient;
    private AuthenticationService mockAuthService;
    private DummyApiClient dummyApiClient;

    static class DummyApiClient extends BaseApiClient {
        public DummyApiClient(AuthenticationService authService, HttpClient httpClient) {
            super(authService, httpClient);
        }

        public String fetchSomething() throws Exception {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/dummy"));
            HttpResponse<String> response = sendWithAuth(requestBuilder);
            return response.body();
        }
    }

    @BeforeEach
    void setUp() {
        TokenManager.getInstance().clear();
        mockHttpClient = mock(HttpClient.class);
        mockAuthService = mock(AuthenticationService.class);
        dummyApiClient = new DummyApiClient(mockAuthService, mockHttpClient);
    }

    @Test
    void testSendRequestAddsAuthorizationHeader() throws Exception {
        // Arrange
        TokenManager.getInstance().setToken("my-valid-token");
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("Success");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act
        String result = dummyApiClient.fetchSomething();

        // Assert
        assertEquals("Success", result);
        
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        
        HttpRequest capturedRequest = requestCaptor.getValue();
        assertTrue(capturedRequest.headers().firstValue("Authorization").isPresent());
        assertEquals("Bearer my-valid-token", capturedRequest.headers().firstValue("Authorization").get());
    }

    @Test
    void testSendRequestThrowsUnauthorizedExceptionOn401() throws Exception {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
            dummyApiClient.fetchSomething();
        });
        assertTrue(ex.getMessage().contains("Session expirée ou accès refusé"));
    }

    @Test
    void testSendRequestReturnsResponseOn500() throws Exception {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("Internal Server Error");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act
        String result = dummyApiClient.fetchSomething();

        // Assert
        assertEquals("Internal Server Error", result);
    }
}
