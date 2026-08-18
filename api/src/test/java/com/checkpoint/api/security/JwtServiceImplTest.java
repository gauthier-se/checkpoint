package com.checkpoint.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.checkpoint.api.security.impl.JwtServiceImpl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Unit tests for {@link JwtServiceImpl}.
 */
class JwtServiceImplTest {

    private static final String SECRET_KEY = Base64.getEncoder().encodeToString(
            "this-is-a-very-long-secret-key-for-testing-purposes-only-minimum-256-bits".getBytes());
    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    private JwtServiceImpl jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(SECRET_KEY, EXPIRATION_MS);
        userDetails = User.builder()
                .username("user@test.com")
                .password("password")
                .roles("USER")
                .build();
    }

    @Nested
    @DisplayName("Token generation")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate a non-null token")
        void shouldGenerateNonNullToken() {
            // When
            String token = jwtService.generateToken(userDetails);

            // Then
            assertThat(token).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("Should generate token with correct subject")
        void shouldGenerateTokenWithCorrectSubject() {
            // When
            String token = jwtService.generateToken(userDetails);

            // Then
            String username = jwtService.extractUsername(token);
            assertThat(username).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("Should include extra claims in generated token")
        void shouldIncludeExtraClaims() {
            // Given
            Map<String, Object> claims = Map.of("role", "ADMIN");

            // When
            String token = jwtService.generateToken(claims, userDetails);

            // Then
            assertThat(token).isNotNull();
            String username = jwtService.extractUsername(token);
            assertThat(username).isEqualTo("user@test.com");
        }
    }

    @Nested
    @DisplayName("Token validation")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should validate a valid token")
        void shouldValidateValidToken() {
            // Given
            String token = jwtService.generateToken(userDetails);

            // When
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token for different user")
        void shouldRejectTokenForDifferentUser() {
            // Given
            String token = jwtService.generateToken(userDetails);
            UserDetails otherUser = User.builder()
                    .username("other@test.com")
                    .password("password")
                    .roles("USER")
                    .build();

            // When
            boolean isValid = jwtService.isTokenValid(token, otherUser);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Given: create a service with 0ms expiration
            JwtServiceImpl expiredJwtService = new JwtServiceImpl(SECRET_KEY, 0L);
            String token = expiredJwtService.generateToken(userDetails);

            // When / Then
            assertThatThrownBy(() -> expiredJwtService.isTokenValid(token, userDetails))
                    .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectTokenWithInvalidSignature() {
            // Given: generate token with a different key
            String otherKey = Base64.getEncoder().encodeToString(
                    "another-secret-key-that-is-long-enough-for-hmac-sha-256-bits".getBytes());
            SecretKey key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(otherKey));
            String token = Jwts.builder()
                    .subject("user@test.com")
                    .signWith(key)
                    .compact();

            // When / Then
            assertThatThrownBy(() -> jwtService.extractUsername(token))
                    .isInstanceOf(io.jsonwebtoken.security.SecurityException.class);
        }
    }

    @Nested
    @DisplayName("Username extraction")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should extract username from valid token")
        void shouldExtractUsername() {
            // Given
            String token = jwtService.generateToken(userDetails);

            // When
            String username = jwtService.extractUsername(token);

            // Then
            assertThat(username).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("Should throw for malformed token")
        void shouldThrowForMalformedToken() {
            // When / Then
            assertThatThrownBy(() -> jwtService.extractUsername("not.a.token"))
                    .isInstanceOf(Exception.class);
        }
    }
}
