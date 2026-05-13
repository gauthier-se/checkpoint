package com.checkpoint.api.services;

import org.springframework.security.core.userdetails.UserDetails;

import com.checkpoint.api.dto.auth.LoginRequestDto;
import com.checkpoint.api.dto.auth.TokenPairDto;
import com.checkpoint.api.dto.auth.TwoFactorLoginRequestDto;
import com.checkpoint.api.dto.auth.TwoFactorRequiredResponseDto;
import com.checkpoint.api.dto.auth.UserMeDto;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Service interface for authentication operations.
 * Supports hybrid authentication: JWT in HttpOnly cookie for Web, JWT in Authorization header for Desktop.
 */
public interface AuthService {

    /**
     * Authenticates a user and returns a token pair (for Desktop clients).
     * Issues a new access token (JWT) and a long-lived refresh token.
     *
     * @param request the login credentials
     * @return a {@link TokenPairDto} containing the access token and refresh token
     */
    TokenPairDto authenticateAndGenerateTokenPair(LoginRequestDto request);

    /**
     * Authenticates a user and writes both a {@code checkpoint_token} (access) and a
     * {@code checkpoint_refresh} (refresh) HttpOnly cookie (for Web clients).
     *
     * @param request         the login credentials
     * @param servletResponse the HTTP servlet response to write the cookies on
     */
    void authenticateAndSetCookie(LoginRequestDto request, HttpServletResponse servletResponse);

    /**
     * Validates the given refresh token, issues a new access token, and rotates
     * the refresh token cookie (for Web clients).
     *
     * @param refreshToken    the value of the {@code checkpoint_refresh} cookie
     * @param servletResponse the HTTP servlet response to write the new cookies on
     */
    void refreshTokenAndSetCookie(String refreshToken, HttpServletResponse servletResponse);

    /**
     * Validates the given refresh token, issues a new access token, and rotates
     * the refresh token (for Desktop clients).
     *
     * @param refreshToken the opaque refresh token string
     * @return a new {@link TokenPairDto}
     */
    TokenPairDto refreshTokenForDesktop(String refreshToken);

    /**
     * Issues a JWT access cookie and refresh cookie for the user identified by {@code email},
     * skipping password authentication. Used by trusted authentication flows (Steam OpenID, etc.)
     * after the caller has already verified the user's identity.
     *
     * @param email           the user's email
     * @param servletResponse the HTTP servlet response to write the cookies on
     */
    void establishWebSession(String email, HttpServletResponse servletResponse);

    /**
     * Clears auth cookies and revokes the refresh token (for Web logout).
     * The refresh token parameter is optional; if {@code null} only the cookies are expired.
     *
     * @param refreshToken    the value of the {@code checkpoint_refresh} cookie, or {@code null}
     * @param servletResponse the HTTP servlet response to write the expired cookies on
     */
    void clearAuthCookie(String refreshToken, HttpServletResponse servletResponse);

    /**
     * Retrieves profile information for the currently authenticated user.
     *
     * @param email the authenticated user's email
     * @return user profile information
     */
    UserMeDto getCurrentUser(String email);

    /**
     * Registers a new user account.
     *
     * @param request the registration details
     */
    void register(com.checkpoint.api.dto.auth.RegisterRequestDto request);

    /**
     * Handles forgot password requests by generating a token.
     *
     * @param request the forgot password request containing the email
     */
    void forgotPassword(com.checkpoint.api.dto.auth.ForgotPasswordRequestDto request);

    /**
     * Resets the user's password using the provided token.
     *
     * @param request the reset password request containing the token and new password
     */
    void resetPassword(com.checkpoint.api.dto.auth.ResetPasswordRequestDto request);

    /**
     * Generates a short-lived JWT for WebSocket authentication.
     *
     * @param userDetails the authenticated user principal
     * @return a JWT token string
     */
    String generateWsToken(UserDetails userDetails);

    /**
     * Completes the 2FA login step for Web clients.
     * Reads the intermediate token from the {@code checkpoint_2fa} cookie, verifies the TOTP code,
     * and on success sets the {@code checkpoint_token} and {@code checkpoint_refresh} cookies.
     *
     * @param request         the 2FA login request containing the TOTP code
     * @param twoFaCookie     the value of the {@code checkpoint_2fa} HttpOnly cookie
     * @param servletResponse the HTTP servlet response to write the final auth cookies on
     */
    void completeTwoFactorLoginForWeb(TwoFactorLoginRequestDto request, String twoFaCookie, HttpServletResponse servletResponse);

    /**
     * Completes the 2FA login step for Desktop clients.
     * The intermediate token is expected in {@link TwoFactorLoginRequestDto#intermediateToken()}.
     * On success, returns a full {@link TokenPairDto}.
     *
     * @param request the 2FA login request containing the intermediate token and TOTP code
     * @return token pair for the Desktop client
     */
    TokenPairDto completeTwoFactorLoginForDesktop(TwoFactorLoginRequestDto request);
}
