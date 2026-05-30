package com.seyzeriat.desktop.service.impl;

import com.seyzeriat.desktop.dto.GameSummaryResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.GameReferencedException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GameApiClientTest {

    private HttpClient mockHttpClient;
    private AuthenticationService mockAuthService;
    private GameApiClient gameApiClient;

    @BeforeEach
    void setUp() {
        TokenManager.getInstance().setToken("valid-token");
        mockHttpClient = mock(HttpClient.class);
        mockAuthService = mock(AuthenticationService.class);
        gameApiClient = new GameApiClient(mockAuthService, mockHttpClient);
    }

    @Test
    void testGetGamesSuccess() throws Exception {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        String jsonBody = "{\"content\":[{\"id\":\"1\", \"title\":\"Zelda\"}], \"totalPages\":1}";
        when(mockResponse.body()).thenReturn(jsonBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act
        PagedResponse<GameSummaryResult> result = gameApiClient.getGames(0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Zelda", result.getContent().get(0).getTitle());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertEquals("GET", request.method());
        assertTrue(request.uri().toString().contains("/games?page=0&size=10"));
        assertTrue(request.headers().firstValue("Authorization").get().contains("valid-token"));
    }

    @Test
    void testDeleteGameThrowsReferencedException() throws Exception {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(409);
        String jsonBody = "{\"blockingReferences\":{\"reviews\":5, \"reports\":2}}";
        when(mockResponse.body()).thenReturn(jsonBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        GameReferencedException ex = assertThrows(GameReferencedException.class, () ->
                gameApiClient.deleteGame("game-123")
        );
        
        assertNotNull(ex.getBlockingReferences());
        assertEquals(5L, ex.getBlockingReferences().get("reviews"));
        assertEquals(2L, ex.getBlockingReferences().get("reports"));
    }
}
