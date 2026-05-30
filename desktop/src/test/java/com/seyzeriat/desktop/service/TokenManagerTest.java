package com.seyzeriat.desktop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenManagerTest {

    @BeforeEach
    void setUp() {
        // We ensure a clean state before each test
        TokenManager.getInstance().clear();
    }

    @Test
    void testSingletonInstance() {
        TokenManager instance1 = TokenManager.getInstance();
        TokenManager instance2 = TokenManager.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2, "TokenManager should be a Singleton");
    }

    @Test
    void testSetAndGetToken() {
        TokenManager manager = TokenManager.getInstance();
        assertNull(manager.getToken(), "Token should initially be null");

        manager.setToken("my-jwt-token");
        assertEquals("my-jwt-token", manager.getToken(), "Token should match what was set");
    }

    @Test
    void testSetAndGetRefreshToken() {
        TokenManager manager = TokenManager.getInstance();
        assertNull(manager.getRefreshToken(), "Refresh token should initially be null");

        manager.setRefreshToken("my-refresh-token");
        assertEquals("my-refresh-token", manager.getRefreshToken(), "Refresh token should match what was set");
    }

    @Test
    void testClearTokens() {
        TokenManager manager = TokenManager.getInstance();
        manager.setToken("token");
        manager.setRefreshToken("refresh-token");

        manager.clear();

        assertNull(manager.getToken(), "Token should be null after clear");
        assertNull(manager.getRefreshToken(), "Refresh token should be null after clear");
    }

    @Test
    void testIsAuthenticated() {
        TokenManager manager = TokenManager.getInstance();
        
        assertFalse(manager.isAuthenticated(), "Should return false when no token is set");
        
        manager.setToken("valid-token");
        assertTrue(manager.isAuthenticated(), "Should return true when a token is set");
        
        manager.setToken("");
        assertFalse(manager.isAuthenticated(), "Should return false for empty token");
        
        manager.setToken(null);
        assertFalse(manager.isAuthenticated(), "Should return false when token is null");
    }
}
