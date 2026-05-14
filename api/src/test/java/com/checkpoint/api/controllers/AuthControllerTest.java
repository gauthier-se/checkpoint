package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.auth.ForgotPasswordRequestDto;
import com.checkpoint.api.dto.auth.LoginRequestDto;
import com.checkpoint.api.dto.auth.RegisterRequestDto;
import com.checkpoint.api.dto.auth.ResetPasswordRequestDto;
import com.checkpoint.api.dto.auth.TokenPairDto;
import com.checkpoint.api.dto.auth.UserMeDto;
import com.checkpoint.api.exceptions.InvalidRefreshTokenException;
import com.checkpoint.api.exceptions.RegistrationConflictException;
import com.checkpoint.api.client.SteamOpenIdClient;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.AuthService;
import com.checkpoint.api.services.SteamOpenIdStateService;
import com.checkpoint.api.services.SteamService;
import com.checkpoint.api.services.TwoFactorService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link AuthController}.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TwoFactorService twoFactorService;

    @MockitoBean
    private SteamOpenIdClient steamOpenIdClient;

    @MockitoBean
    private SteamService steamService;

    @MockitoBean
    private SteamOpenIdStateService steamOpenIdStateService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should return token pair for Desktop client (via header)")
        void shouldReturnTokenForDesktopClient() throws Exception {
            // Given
            when(authService.authenticateAndGenerateTokenPair(any(LoginRequestDto.class)))
                    .thenReturn(new TokenPairDto("access.token.here", "refresh-token-uuid"));

            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Client-Type", "Desktop")
                            .content("""
                                    {"email": "user@test.com", "password": "password123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access.token.here"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-uuid"));

            verify(authService).authenticateAndGenerateTokenPair(any(LoginRequestDto.class));
            verify(authService, never()).authenticateAndSetCookie(any(), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("Should set checkpoint_token cookie for Web client (no header)")
        void shouldSetCookieForWebClient() throws Exception {
            // Given
            doNothing().when(authService).authenticateAndSetCookie(
                    any(LoginRequestDto.class), any(HttpServletResponse.class));

            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@test.com", "password": "password123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Login successful"));

            verify(authService).authenticateAndSetCookie(
                    any(LoginRequestDto.class), any(HttpServletResponse.class));
            verify(authService, never()).authenticateAndGenerateTokenPair(any());
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials (Desktop)")
        void shouldReturn401ForInvalidDesktopCredentials() throws Exception {
            // Given
            when(authService.authenticateAndGenerateTokenPair(any(LoginRequestDto.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Client-Type", "Desktop")
                            .content("""
                                    {"email": "user@test.com", "password": "wrong"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials (Web)")
        void shouldReturn401ForInvalidWebCredentials() throws Exception {
            // Given
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authService).authenticateAndSetCookie(
                            any(LoginRequestDto.class), any(HttpServletResponse.class));

            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@test.com", "password": "wrong"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 for missing email")
        void shouldReturn400ForMissingEmail() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"password": "password123"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for invalid email format")
        void shouldReturn400ForInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "not-an-email", "password": "password123"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for missing password")
        void shouldReturn400ForMissingPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@test.com"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for empty body")
        void shouldReturn400ForEmptyBody() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle case-insensitive X-Client-Type header")
        void shouldHandleCaseInsensitiveHeader() throws Exception {
            // Given
            when(authService.authenticateAndGenerateTokenPair(any(LoginRequestDto.class)))
                    .thenReturn(new TokenPairDto("access.token.here", "refresh-token-uuid"));

            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Client-Type", "desktop")
                            .content("""
                                    {"email": "user@test.com", "password": "password123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access.token.here"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/token")
    class TokenTests {

        @Test
        @DisplayName("Should return token pair")
        void shouldReturnToken() throws Exception {
            // Given
            when(authService.authenticateAndGenerateTokenPair(any(LoginRequestDto.class)))
                    .thenReturn(new TokenPairDto("access.token.here", "refresh-token-uuid"));

            // When / Then
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@test.com", "password": "password123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access.token.here"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-uuid"));
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials")
        void shouldReturn401ForInvalidCredentials() throws Exception {
            // Given
            when(authService.authenticateAndGenerateTokenPair(any(LoginRequestDto.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When / Then
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@test.com", "password": "wrong"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 for missing fields")
        void shouldReturn400ForMissingFields() throws Exception {
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should return 201 Created on successful registration")
        void shouldReturn201OnSuccess() throws Exception {
            doNothing().when(authService).register(any(RegisterRequestDto.class));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"pseudo": "newuser", "email": "newuser@test.com", "password": "password123", "confirmPassword": "password123"}
                                    """))
                    .andExpect(status().isCreated());

            verify(authService).register(any(RegisterRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 for missing fields")
        void shouldReturn400ForMissingFields() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@test.com"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for invalid email")
        void shouldReturn400ForInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"pseudo": "user", "email": "invalid", "password": "password123", "confirmPassword": "password123"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for short password")
        void shouldReturn400ForShortPassword() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"pseudo": "user", "email": "user@test.com", "password": "short", "confirmPassword": "short"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for missing confirmPassword")
        void shouldReturn400ForMissingConfirmPassword() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"pseudo": "user", "email": "user@test.com", "password": "password123"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("Password confirmation is required")));
        }

        @Test
        @DisplayName("Should return 400 for password mismatch")
        void shouldReturn400ForPasswordMismatch() throws Exception {
            // Given
            doThrow(new IllegalArgumentException("Passwords do not match"))
                    .when(authService).register(any(RegisterRequestDto.class));

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"pseudo": "user", "email": "user@test.com", "password": "password123", "confirmPassword": "different123"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Passwords do not match"));
        }

        @Test
        @DisplayName("Should return 409 for duplicate email")
        void shouldReturn409ForDuplicateEmail() throws Exception {
            // Given
            doThrow(new RegistrationConflictException("Email is already in use"))
                    .when(authService).register(any(RegisterRequestDto.class));

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"pseudo": "newuser", "email": "existing@test.com", "password": "password123", "confirmPassword": "password123"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email is already in use"));
        }

        @Test
        @DisplayName("Should return 409 for duplicate pseudo")
        void shouldReturn409ForDuplicatePseudo() throws Exception {
            // Given
            doThrow(new RegistrationConflictException("Pseudo is already in use"))
                    .when(authService).register(any(RegisterRequestDto.class));

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"pseudo": "existinguser", "email": "new@test.com", "password": "password123", "confirmPassword": "password123"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Pseudo is already in use"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should clear cookies and return success message on logout")
        void shouldClearCookieAndReturnSuccess() throws Exception {
            // Given
            doNothing().when(authService).clearAuthCookie(any(), any(HttpServletResponse.class));

            // When / Then
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout successful"));

            verify(authService).clearAuthCookie(any(), any(HttpServletResponse.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("Should refresh access token for Web client using cookie")
        void shouldRefreshForWebClient() throws Exception {
            // Given
            doNothing().when(authService).refreshTokenAndSetCookie(any(), any(HttpServletResponse.class));

            // When / Then
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("checkpoint_refresh", "valid-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Token refreshed"));

            verify(authService).refreshTokenAndSetCookie(any(), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("Should return token pair for Desktop client")
        void shouldReturnTokenPairForDesktopClient() throws Exception {
            // Given
            when(authService.refreshTokenForDesktop(any()))
                    .thenReturn(new TokenPairDto("new.access.token", "new-refresh-token-uuid"));

            // When / Then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Client-Type", "Desktop")
                            .content("""
                                    {"refreshToken": "old-refresh-token-uuid"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token-uuid"));

            verify(authService).refreshTokenForDesktop("old-refresh-token-uuid");
        }

        @Test
        @DisplayName("Should return 401 when Web refresh token is invalid or expired")
        void shouldReturn401ForInvalidWebRefreshToken() throws Exception {
            // Given
            doThrow(new InvalidRefreshTokenException("Refresh token has expired"))
                    .when(authService).refreshTokenAndSetCookie(any(), any(HttpServletResponse.class));

            // When / Then
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("checkpoint_refresh", "expired-token")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when Desktop refresh token is revoked")
        void shouldReturn401ForRevokedDesktopToken() throws Exception {
            // Given
            when(authService.refreshTokenForDesktop(any()))
                    .thenThrow(new InvalidRefreshTokenException("Refresh token has been revoked"));

            // When / Then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Client-Type", "Desktop")
                            .content("""
                                    {"refreshToken": "revoked-token"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when Desktop sends missing refresh token")
        void shouldReturn401ForMissingDesktopToken() throws Exception {
            // Given
            when(authService.refreshTokenForDesktop(null))
                    .thenThrow(new InvalidRefreshTokenException("Refresh token is required"));

            // When / Then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Client-Type", "Desktop"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class MeTests {

        @Test
        @DisplayName("Should return current user profile with role")
        @WithMockUser(username = "alice@test.com")
        void shouldReturnCurrentUserProfile() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            UserMeDto userMeDto = new UserMeDto(userId, "alice", "alice@test.com", "ADMIN", "My bio", null, false, false, null, null, null);

            when(authService.getCurrentUser("alice@test.com")).thenReturn(userMeDto);

            // When / Then
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.email").value("alice@test.com"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));

            verify(authService).getCurrentUser("alice@test.com");
        }

        @Test
        @DisplayName("Should return user profile with USER role")
        @WithMockUser(username = "bob@test.com")
        void shouldReturnUserWithDefaultRole() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            UserMeDto userMeDto = new UserMeDto(userId, "bob", "bob@test.com", "USER", null, null, false, false, null, null, null);

            when(authService.getCurrentUser("bob@test.com")).thenReturn(userMeDto);

            // When / Then
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("Should return 401 when user not found in database")
        @WithMockUser(username = "unknown@test.com")
        void shouldReturn401WhenUserNotFound() throws Exception {
            // Given
            when(authService.getCurrentUser("unknown@test.com"))
                    .thenThrow(new UsernameNotFoundException("User not found with email: unknown@test.com"));

            // When / Then
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should return 200 OK on successful request")
        void shouldReturn200OnSuccess() throws Exception {
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("user@test.com");
            doNothing().when(authService).forgotPassword(any(ForgotPasswordRequestDto.class));

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("If the email exists, a password reset link has been logged."));
        }

        @Test
        @DisplayName("Should return 400 Bad Request if email is empty")
        void shouldReturn400IfEmailEmpty() throws Exception {
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("");

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email is required")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request if email is invalid format")
        void shouldReturn400IfEmailInvalid() throws Exception {
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("invalid-email");

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email must be valid")));
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should return 200 OK on successful request")
        void shouldReturn200OnSuccess() throws Exception {
            ResetPasswordRequestDto request = new ResetPasswordRequestDto("valid-token", "new-password123");
            doNothing().when(authService).resetPassword(any(ResetPasswordRequestDto.class));

            mockMvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password has been reset successfully."));
        }

        @Test
        @DisplayName("Should return 400 Bad Request if token is empty")
        void shouldReturn400IfTokenEmpty() throws Exception {
            ResetPasswordRequestDto request = new ResetPasswordRequestDto("", "new-password123");

            mockMvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Token is required")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request if password is too short")
        void shouldReturn400IfPasswordTooShort() throws Exception {
            ResetPasswordRequestDto request = new ResetPasswordRequestDto("valid-token", "short");

            mockMvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Password must be at least 8 characters long")));
        }
    }

    @Nested
    @DisplayName("Steam OpenID endpoints")
    class SteamOpenId {

        private static final String STEAM_ID = "76561198000000000";
        private static final String STATE_TOKEN = "sentinel.state.jwt";

        private void stubValidLoginState() {
            when(steamOpenIdStateService.verify(org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(java.util.Optional.of(
                            new SteamOpenIdStateService.Claims("login", null)));
        }

        private void stubValidLinkState(String email) {
            when(steamOpenIdStateService.verify(org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(java.util.Optional.of(
                            new SteamOpenIdStateService.Claims("link", email)));
        }

        @Test
        @DisplayName("GET /steam/openid/start returns HTML that redirects the browser to Steam")
        void start_redirectsToSteam() throws Exception {
            when(steamOpenIdStateService.issue(org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.nullable(String.class)))
                    .thenReturn(STATE_TOKEN);
            when(steamOpenIdClient.buildAuthenticationUrl(
                    org.mockito.ArgumentMatchers.contains("action=link"),
                    org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn("https://steamcommunity.com/openid/login?openid.mode=checkid_setup");

            mockMvc.perform(get("/api/auth/steam/openid/start").param("action", "link"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "steamcommunity.com/openid/login")));
        }

        @Test
        @DisplayName("GET /steam/openid/start embeds the state JWT in the return URL")
        @WithMockUser(username = "alice@test.com")
        void start_includesStateInReturnUrl() throws Exception {
            when(steamOpenIdStateService.issue(org.mockito.ArgumentMatchers.eq("link"),
                    org.mockito.ArgumentMatchers.eq("alice@test.com")))
                    .thenReturn(STATE_TOKEN);
            org.mockito.ArgumentCaptor<String> returnUrlCaptor =
                    org.mockito.ArgumentCaptor.forClass(String.class);
            when(steamOpenIdClient.buildAuthenticationUrl(returnUrlCaptor.capture(),
                    org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn("https://steamcommunity.com/openid/login");

            mockMvc.perform(get("/api/auth/steam/openid/start").param("action", "link"))
                    .andExpect(status().isOk());

            String returnUrl = returnUrlCaptor.getValue();
            org.junit.jupiter.api.Assertions.assertTrue(returnUrl.contains("state="),
                    "return URL should carry the state token: " + returnUrl);
            org.junit.jupiter.api.Assertions.assertTrue(returnUrl.contains(STATE_TOKEN),
                    "return URL should contain the issued state JWT: " + returnUrl);
        }

        @Test
        @DisplayName("GET /steam/openid/callback action=login navigates to / on success when SteamID is linked")
        void callback_loginSuccess() throws Exception {
            stubValidLoginState();
            com.checkpoint.api.entities.User user = new com.checkpoint.api.entities.User();
            user.setEmail("alice@test.com");

            when(steamOpenIdClient.verifyAndExtractSteamId(any())).thenReturn(STEAM_ID);
            when(steamService.findUserBySteamId(STEAM_ID))
                    .thenReturn(java.util.Optional.of(user));
            doNothing().when(authService)
                    .establishWebSession(org.mockito.ArgumentMatchers.eq("alice@test.com"),
                            any(HttpServletResponse.class));

            mockMvc.perform(get("/api/auth/steam/openid/callback")
                            .param("action", "login")
                            .param("state", STATE_TOKEN)
                            .param("openid.mode", "id_res")
                            .param("openid.claimed_id",
                                    "https://steamcommunity.com/openid/id/" + STEAM_ID))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "localhost:3000/")));

            verify(authService).establishWebSession(org.mockito.ArgumentMatchers.eq("alice@test.com"),
                    any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("GET /steam/openid/callback action=login navigates to /login?error=steam_not_linked when no user")
        void callback_loginNoLinkedUser() throws Exception {
            stubValidLoginState();
            when(steamOpenIdClient.verifyAndExtractSteamId(any())).thenReturn(STEAM_ID);
            when(steamService.findUserBySteamId(STEAM_ID))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/api/auth/steam/openid/callback")
                            .param("action", "login")
                            .param("state", STATE_TOKEN)
                            .param("openid.mode", "id_res"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "error=steam_not_linked")));

            verify(authService, never()).establishWebSession(
                    org.mockito.ArgumentMatchers.anyString(), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("GET /steam/openid/callback action=link links to authenticated user and navigates to settings")
        @WithMockUser(username = "alice@test.com")
        void callback_linkSuccess() throws Exception {
            stubValidLinkState("alice@test.com");
            when(steamOpenIdClient.verifyAndExtractSteamId(any())).thenReturn(STEAM_ID);

            mockMvc.perform(get("/api/auth/steam/openid/callback")
                            .param("action", "link")
                            .param("state", STATE_TOKEN)
                            .param("openid.mode", "id_res")
                            .param("openid.claimed_id",
                                    "https://steamcommunity.com/openid/id/" + STEAM_ID))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "/settings/integrations")));

            verify(steamService).linkVerifiedSteamAccount(
                    org.mockito.ArgumentMatchers.eq("alice@test.com"),
                    org.mockito.ArgumentMatchers.eq(STEAM_ID));
        }

        @Test
        @DisplayName("GET /steam/openid/callback navigates to /login?error=steam_openid_failed on verification failure")
        void callback_verificationFails() throws Exception {
            stubValidLoginState();
            when(steamOpenIdClient.verifyAndExtractSteamId(any()))
                    .thenThrow(new com.checkpoint.api.exceptions.SteamOpenIdException("bad"));

            mockMvc.perform(get("/api/auth/steam/openid/callback")
                            .param("action", "login")
                            .param("state", STATE_TOKEN)
                            .param("openid.mode", "id_res"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "error=steam_openid_failed")));
        }

        @Test
        @DisplayName("GET /steam/openid/callback rejects requests without a state parameter")
        void callback_rejectsMissingState() throws Exception {
            when(steamOpenIdStateService.verify(org.mockito.ArgumentMatchers.nullable(String.class)))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/api/auth/steam/openid/callback")
                            .param("action", "login")
                            .param("openid.mode", "id_res"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "error=steam_openid_failed")));

            verify(steamOpenIdClient, never()).verifyAndExtractSteamId(any());
        }

        @Test
        @DisplayName("GET /steam/openid/callback rejects an invalid/expired/tampered state token")
        void callback_rejectsInvalidState() throws Exception {
            when(steamOpenIdStateService.verify(org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/api/auth/steam/openid/callback")
                            .param("action", "login")
                            .param("state", "tampered.jwt.value")
                            .param("openid.mode", "id_res"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "error=steam_openid_failed")));

            verify(steamOpenIdClient, never()).verifyAndExtractSteamId(any());
        }

        @Test
        @DisplayName("GET /steam/openid/callback rejects when state action does not match URL action")
        void callback_rejectsActionMismatch() throws Exception {
            // State minted for link, but the callback claims action=login.
            stubValidLinkState("alice@test.com");

            mockMvc.perform(get("/api/auth/steam/openid/callback")
                            .param("action", "login")
                            .param("state", STATE_TOKEN)
                            .param("openid.mode", "id_res"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "error=steam_openid_failed")));

            verify(steamOpenIdClient, never()).verifyAndExtractSteamId(any());
        }

        @Test
        @DisplayName("GET /steam/openid/callback action=link rejects when state email does not match authenticated user")
        @WithMockUser(username = "alice@test.com")
        void callback_rejectsUserIdMismatchForLink() throws Exception {
            // State was minted for bob, but alice is the one driving the callback.
            stubValidLinkState("bob@test.com");

            mockMvc.perform(get("/api/auth/steam/openid/callback")
                            .param("action", "link")
                            .param("state", STATE_TOKEN)
                            .param("openid.mode", "id_res")
                            .param("openid.claimed_id",
                                    "https://steamcommunity.com/openid/id/" + STEAM_ID))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString(
                                    "error=steam_openid_failed")));

            verify(steamOpenIdClient, never()).verifyAndExtractSteamId(any());
            verify(steamService, never()).linkVerifiedSteamAccount(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString());
        }
    }
}
