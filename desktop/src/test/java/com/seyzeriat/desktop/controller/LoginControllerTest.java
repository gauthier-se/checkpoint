package com.seyzeriat.desktop.controller;

import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.exception.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginControllerTest {

    private AuthenticationService mockAuthService;
    private LoginController loginController;

    @BeforeEach
    void setUp() {
        mockAuthService = mock(AuthenticationService.class);
        loginController = new LoginController(mockAuthService);
    }

    @Test
    void testConstructorInjectsService() {
        assertNotNull(loginController, "LoginController should be instantiated");
        // We just verify it compiles and instantiates properly with the mock
    }

}
