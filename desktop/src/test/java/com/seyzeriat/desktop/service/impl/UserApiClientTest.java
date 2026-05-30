package com.seyzeriat.desktop.service.impl;

import com.seyzeriat.desktop.dto.UserResult;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserApiClientTest {

    private HttpClient mockHttpClient;
    private AuthenticationService mockAuthService;
    private UserApiClient userApiClient;

    @BeforeEach
    void setUp() {
        TokenManager.getInstance().setToken("valid-token");
        mockHttpClient = mock(HttpClient.class);
        mockAuthService = mock(AuthenticationService.class);
        userApiClient = new UserApiClient(mockAuthService, mockHttpClient);
    }

    @Test
    void testGetUsersSuccess() throws Exception {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        String jsonBody = "[{\"id\":\"user1\", \"username\":\"PlayerOne\", \"role\":\"USER\", \"banned\":false}]";
        when(mockResponse.body()).thenReturn(jsonBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act
        List<UserResult> users = userApiClient.getUsers();

        // Assert
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("PlayerOne", users.get(0).getUsername());
        assertFalse(users.get(0).isBanned());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertEquals("GET", request.method());
        assertTrue(request.uri().toString().endsWith("/admin/users"));
    }

    @Test
    void testBanUserSuccess() throws Exception {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200); // Or 204 depending on the API
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Act
        userApiClient.banUser("user123");

        // Assert
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertEquals("POST", request.method());
        assertTrue(request.uri().toString().endsWith("/admin/users/user123/ban"));
    }
}
