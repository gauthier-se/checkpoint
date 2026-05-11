package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.AccountService;
import com.checkpoint.api.services.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link AccountController}.
 */
@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @DisplayName("DELETE /api/me - deletes the account and clears auth cookies")
    @WithMockUser(username = "alice@test.com")
    void deleteAccount_returns204AndClearsCookies() throws Exception {
        mockMvc.perform(delete("/api/me")
                        .cookie(new Cookie("checkpoint_refresh", "refresh-token-value")))
                .andExpect(status().isNoContent());

        verify(accountService).deleteCurrentUser("alice@test.com");
        verify(authService).clearAuthCookie(eq("refresh-token-value"), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("DELETE /api/me - still clears cookies when no refresh cookie is sent")
    @WithMockUser(username = "alice@test.com")
    void deleteAccount_handlesMissingRefreshCookie() throws Exception {
        mockMvc.perform(delete("/api/me"))
                .andExpect(status().isNoContent());

        verify(accountService).deleteCurrentUser("alice@test.com");
        verify(authService).clearAuthCookie(eq(null), any(HttpServletResponse.class));
    }
}
