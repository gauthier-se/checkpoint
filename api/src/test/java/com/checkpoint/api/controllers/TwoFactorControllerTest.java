package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.auth.LoginRequestDto;
import com.checkpoint.api.dto.auth.TokenPairDto;
import com.checkpoint.api.dto.auth.TwoFactorDisableRequestDto;
import com.checkpoint.api.dto.auth.TwoFactorLoginRequestDto;
import com.checkpoint.api.dto.auth.TwoFactorSetupResponseDto;
import com.checkpoint.api.dto.auth.TwoFactorVerifyRequestDto;
import com.checkpoint.api.exceptions.InvalidTotpCodeException;
import com.checkpoint.api.exceptions.TwoFactorRequiredException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.AuthService;
import com.checkpoint.api.services.TwoFactorService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for 2FA endpoints in {@link AuthController}.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class TwoFactorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TwoFactorService twoFactorService;

    @MockitoBean
    private com.checkpoint.api.client.SteamOpenIdClient steamOpenIdClient;

    @MockitoBean
    private com.checkpoint.api.services.SteamService steamService;

    @MockitoBean
    private com.checkpoint.api.services.SteamOpenIdStateService steamOpenIdStateService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("POST /api/auth/login — 2FA flow")
    class LoginTwoFactorTests {

        @Test
        @DisplayName("Should return twoFactorRequired=true for Web client when 2FA is enabled")
        void login_shouldReturnTwoFactorRequiredForWebClient() throws Exception {
            // Given
            doThrow(new TwoFactorRequiredException(null))
                    .when(authService).authenticateAndSetCookie(any(LoginRequestDto.class), any(HttpServletResponse.class));

            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequestDto("user@example.com", "password"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.twoFactorRequired").value(true))
                    .andExpect(jsonPath("$.intermediateToken").doesNotExist());
        }

        @Test
        @DisplayName("Should return twoFactorRequired=true with intermediateToken for Desktop client when 2FA is enabled")
        void login_shouldReturnTwoFactorRequiredWithTokenForDesktopClient() throws Exception {
            // Given
            when(authService.authenticateAndGenerateTokenPair(any(LoginRequestDto.class)))
                    .thenThrow(new TwoFactorRequiredException("intermediate.jwt.token"));

            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Client-Type", "Desktop")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequestDto("user@example.com", "password"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.twoFactorRequired").value(true))
                    .andExpect(jsonPath("$.intermediateToken").value("intermediate.jwt.token"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/2fa/setup")
    class SetupTests {

        @Test
        @DisplayName("Should return provisioning URI and QR code data URL")
        @WithMockUser(username = "user@example.com")
        void setup_shouldReturnSetupResponse() throws Exception {
            // Given
            when(twoFactorService.setup("user@example.com"))
                    .thenReturn(new TwoFactorSetupResponseDto("otpauth://totp/Checkpoint:user@example.com?secret=SECRET", "data:image/png;base64,ABC"));

            // When / Then
            mockMvc.perform(post("/api/auth/2fa/setup"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provisioningUri").value("otpauth://totp/Checkpoint:user@example.com?secret=SECRET"))
                    .andExpect(jsonPath("$.qrCodeDataUrl").value("data:image/png;base64,ABC"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/2fa/verify")
    class VerifyTests {

        @Test
        @DisplayName("Should return success message when code is valid")
        @WithMockUser(username = "user@example.com")
        void verify_shouldReturnSuccessMessage() throws Exception {
            // Given
            doNothing().when(twoFactorService).verifyAndEnable(anyString(), anyString());

            // When / Then
            mockMvc.perform(post("/api/auth/2fa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TwoFactorVerifyRequestDto("123456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Two-factor authentication has been enabled."));
        }

        @Test
        @DisplayName("Should return 401 when TOTP code is invalid")
        @WithMockUser(username = "user@example.com")
        void verify_shouldReturn401OnInvalidCode() throws Exception {
            // Given
            doThrow(new InvalidTotpCodeException("Invalid TOTP code. Please try again."))
                    .when(twoFactorService).verifyAndEnable(anyString(), anyString());

            // When / Then
            mockMvc.perform(post("/api/auth/2fa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TwoFactorVerifyRequestDto("000000"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid TOTP code. Please try again."));
        }

        @Test
        @DisplayName("Should return 400 when code is blank")
        @WithMockUser(username = "user@example.com")
        void verify_shouldReturn400WhenCodeIsBlank() throws Exception {
            mockMvc.perform(post("/api/auth/2fa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TwoFactorVerifyRequestDto(""))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/2fa/disable")
    class DisableTests {

        @Test
        @DisplayName("Should return success message when password and code are valid")
        @WithMockUser(username = "user@example.com")
        void disable_shouldReturnSuccessMessage() throws Exception {
            // Given
            doNothing().when(twoFactorService).disable(anyString(), anyString(), anyString());

            // When / Then
            mockMvc.perform(post("/api/auth/2fa/disable")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TwoFactorDisableRequestDto("my-password", "123456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Two-factor authentication has been disabled."));
        }

        @Test
        @DisplayName("Should return 401 when password or code is wrong")
        @WithMockUser(username = "user@example.com")
        void disable_shouldReturn401OnInvalidCredentials() throws Exception {
            // Given
            doThrow(new InvalidTotpCodeException("Invalid password."))
                    .when(twoFactorService).disable(anyString(), anyString(), anyString());

            // When / Then
            mockMvc.perform(post("/api/auth/2fa/disable")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TwoFactorDisableRequestDto("wrong-password", "000000"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/2fa/login")
    class TwoFactorLoginTests {

        @Test
        @DisplayName("Should return token pair for Desktop client")
        void twoFactorLogin_shouldReturnTokenPairForDesktop() throws Exception {
            // Given
            when(authService.completeTwoFactorLoginForDesktop(any(TwoFactorLoginRequestDto.class)))
                    .thenReturn(new TokenPairDto("access.token", "refresh.token"));

            // When / Then
            mockMvc.perform(post("/api/auth/2fa/login")
                            .header("X-Client-Type", "Desktop")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TwoFactorLoginRequestDto("intermediate.jwt", "123456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access.token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh.token"));
        }

        @Test
        @DisplayName("Should return success message for Web client")
        void twoFactorLogin_shouldReturnSuccessForWeb() throws Exception {
            // Given
            doNothing().when(authService).completeTwoFactorLoginForWeb(
                    any(TwoFactorLoginRequestDto.class), anyString(), any(HttpServletResponse.class));

            // When / Then
            mockMvc.perform(post("/api/auth/2fa/login")
                            .cookie(new jakarta.servlet.http.Cookie("checkpoint_2fa", "intermediate.jwt"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TwoFactorLoginRequestDto(null, "123456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Login successful"));
        }

        @Test
        @DisplayName("Should return 401 when TOTP code is invalid")
        void twoFactorLogin_shouldReturn401OnInvalidCode() throws Exception {
            // Given
            doThrow(new InvalidTotpCodeException("Invalid TOTP code. Please try again."))
                    .when(authService).completeTwoFactorLoginForWeb(
                            any(TwoFactorLoginRequestDto.class), any(), any(HttpServletResponse.class));

            // When / Then
            mockMvc.perform(post("/api/auth/2fa/login")
                            .cookie(new jakarta.servlet.http.Cookie("checkpoint_2fa", "intermediate.jwt"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TwoFactorLoginRequestDto(null, "000000"))))
                    .andExpect(status().isUnauthorized());
        }
    }
}
