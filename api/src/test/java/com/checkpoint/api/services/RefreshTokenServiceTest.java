package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.entities.RefreshToken;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.InvalidRefreshTokenException;
import com.checkpoint.api.repositories.RefreshTokenRepository;
import com.checkpoint.api.services.impl.RefreshTokenServiceImpl;

/**
 * Unit tests for {@link RefreshTokenServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, 604800000L);
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Should persist a new refresh token and return it")
        void createRefreshToken_shouldPersistAndReturnToken() {
            // Given
            User user = new User("alice", "alice@test.com", "encodedPwd");
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            RefreshToken result = refreshTokenService.createRefreshToken(user);

            // Then
            assertThat(result.getToken()).isNotBlank();
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getExpiryDate()).isAfter(LocalDateTime.now());
            assertThat(result.isRevoked()).isFalse();
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should throw when token is null")
        void validateToken_shouldThrowIfNull() {
            // When / Then
            assertThatThrownBy(() -> refreshTokenService.validateToken(null))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Refresh token is required");
        }

        @Test
        @DisplayName("Should throw when token is blank")
        void validateToken_shouldThrowIfBlank() {
            // When / Then
            assertThatThrownBy(() -> refreshTokenService.validateToken("  "))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Refresh token is required");
        }

        @Test
        @DisplayName("Should throw when token is not found in repository")
        void validateToken_shouldThrowIfNotFound() {
            // Given
            when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> refreshTokenService.validateToken("unknown"))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Refresh token not found");
        }

        @Test
        @DisplayName("Should throw when token is revoked")
        void validateToken_shouldThrowIfRevoked() {
            // Given
            User user = new User("alice", "alice@test.com", "encodedPwd");
            RefreshToken revokedToken = new RefreshToken("tok", user, LocalDateTime.now().plusDays(7));
            revokedToken.setRevoked(true);
            when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(revokedToken));

            // When / Then
            assertThatThrownBy(() -> refreshTokenService.validateToken("tok"))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        @DisplayName("Should throw when token is expired")
        void validateToken_shouldThrowIfExpired() {
            // Given
            User user = new User("alice", "alice@test.com", "encodedPwd");
            RefreshToken expiredToken = new RefreshToken("tok", user, LocalDateTime.now().minusDays(1));
            when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(expiredToken));

            // When / Then
            assertThatThrownBy(() -> refreshTokenService.validateToken("tok"))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Should return the token entity when valid")
        void validateToken_shouldReturnEntityWhenValid() {
            // Given
            User user = new User("alice", "alice@test.com", "encodedPwd");
            RefreshToken validToken = new RefreshToken("tok", user, LocalDateTime.now().plusDays(7));
            when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(validToken));

            // When
            RefreshToken result = refreshTokenService.validateToken("tok");

            // Then
            assertThat(result).isEqualTo(validToken);
        }
    }

    @Nested
    @DisplayName("revokeToken")
    class RevokeTokenTests {

        @Test
        @DisplayName("Should set revoked flag on the token")
        void revokeToken_shouldSetRevokedFlag() {
            // Given
            User user = new User("alice", "alice@test.com", "encodedPwd");
            RefreshToken token = new RefreshToken("tok", user, LocalDateTime.now().plusDays(7));
            when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            refreshTokenService.revokeToken("tok");

            // Then
            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("Should silently ignore unknown tokens")
        void revokeToken_shouldIgnoreUnknownToken() {
            // Given
            when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

            // When / Then: no exception
            refreshTokenService.revokeToken("unknown");
        }

        @Test
        @DisplayName("Should silently ignore null token")
        void revokeToken_shouldIgnoreNull() {
            // When / Then: no exception, no repo call
            refreshTokenService.revokeToken(null);
        }
    }

    @Nested
    @DisplayName("revokeAllForUser")
    class RevokeAllForUserTests {

        @Test
        @DisplayName("Should call repository to revoke all tokens for user")
        void revokeAllForUser_shouldCallRepository() {
            // Given
            UUID userId = UUID.randomUUID();

            // When
            refreshTokenService.revokeAllForUser(userId);

            // Then
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }
    }
}
