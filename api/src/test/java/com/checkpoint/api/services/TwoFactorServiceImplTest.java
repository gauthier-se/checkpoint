package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.checkpoint.api.dto.auth.TwoFactorSetupResponseDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.InvalidTokenException;
import com.checkpoint.api.exceptions.InvalidTotpCodeException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.security.JwtService;
import com.checkpoint.api.services.impl.TwoFactorServiceImpl;
import com.checkpoint.api.services.OnboardingService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TwoFactorServiceImpl")
class TwoFactorServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OnboardingService onboardingService;

    private TwoFactorService twoFactorService;

    @BeforeEach
    void setUp() {
        twoFactorService = new TwoFactorServiceImpl(userRepository, jwtService, passwordEncoder, onboardingService);
    }

    @Nested
    @DisplayName("setup()")
    class SetupTests {

        @Test
        @DisplayName("Should generate secret, save it on user, and return setup response")
        void setup_shouldGenerateSecretAndReturnResponse() {
            // Given
            User user = new User("testuser", "test@example.com", "password");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            TwoFactorSetupResponseDto response = twoFactorService.setup("test@example.com");

            // Then
            assertThat(response.provisioningUri()).isNotBlank();
            assertThat(response.qrCodeDataUrl()).startsWith("data:image/");
            assertThat(user.getTotpSecret()).isNotBlank();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void setup_shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> twoFactorService.setup("unknown@example.com"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("verifyAndEnable()")
    class VerifyAndEnableTests {

        @Test
        @DisplayName("Should throw InvalidTotpCodeException when totpSecret is null")
        void verifyAndEnable_shouldThrowWhenSecretNotSet() {
            // Given
            User user = new User("testuser", "test@example.com", "password");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            // When / Then
            assertThatThrownBy(() -> twoFactorService.verifyAndEnable("test@example.com", "123456"))
                    .isInstanceOf(InvalidTotpCodeException.class)
                    .hasMessageContaining("setup has not been initiated");
        }

        @Test
        @DisplayName("Should throw InvalidTotpCodeException when code is wrong")
        void verifyAndEnable_shouldThrowOnInvalidCode() {
            // Given
            User user = new User("testuser", "test@example.com", "password");
            user.setTotpSecret("JBSWY3DPEHPK3PXP");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            // When / Then
            assertThatThrownBy(() -> twoFactorService.verifyAndEnable("test@example.com", "000000"))
                    .isInstanceOf(InvalidTotpCodeException.class);
        }
    }

    @Nested
    @DisplayName("disable()")
    class DisableTests {

        @Test
        @DisplayName("Should throw InvalidTotpCodeException when password does not match")
        void disable_shouldThrowOnWrongPassword() {
            // Given
            User user = new User("testuser", "test@example.com", "$2a$10$hash");
            user.setTotpSecret("JBSWY3DPEHPK3PXP");
            user.setTwoFactorEnabled(true);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password", "$2a$10$hash")).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> twoFactorService.disable("test@example.com", "wrong-password", "123456"))
                    .isInstanceOf(InvalidTotpCodeException.class)
                    .hasMessageContaining("Invalid password");
        }

        @Test
        @DisplayName("Should throw InvalidTotpCodeException when TOTP code is wrong")
        void disable_shouldThrowOnInvalidTotpCode() {
            // Given
            User user = new User("testuser", "test@example.com", "$2a$10$hash");
            user.setTotpSecret("JBSWY3DPEHPK3PXP");
            user.setTwoFactorEnabled(true);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("correct-password", "$2a$10$hash")).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> twoFactorService.disable("test@example.com", "correct-password", "000000"))
                    .isInstanceOf(InvalidTotpCodeException.class);
        }
    }

    @Nested
    @DisplayName("generateIntermediateToken()")
    class GenerateIntermediateTokenTests {

        @Test
        @DisplayName("Should delegate to JwtService with 5-minute expiration")
        void generateIntermediateToken_shouldDelegateToJwtService() {
            // Given
            when(jwtService.generateIntermediateToken(eq("test@example.com"), eq(300_000L)))
                    .thenReturn("intermediate.jwt.token");

            // When
            String token = twoFactorService.generateIntermediateToken("test@example.com");

            // Then
            assertThat(token).isEqualTo("intermediate.jwt.token");
        }
    }

    @Nested
    @DisplayName("resolveEmailFromIntermediateToken()")
    class ResolveEmailTests {

        @Test
        @DisplayName("Should throw InvalidTokenException when token is null")
        void resolveEmail_shouldThrowWhenTokenIsNull() {
            assertThatThrownBy(() -> twoFactorService.resolveEmailFromIntermediateToken(null))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token is not intermediate type")
        void resolveEmail_shouldThrowWhenNotIntermediateToken() {
            // Given
            when(jwtService.isIntermediateToken("regular.jwt")).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> twoFactorService.resolveEmailFromIntermediateToken("regular.jwt"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should return email when token is valid and intermediate")
        void resolveEmail_shouldReturnEmailForValidToken() {
            // Given
            when(jwtService.isIntermediateToken("intermediate.jwt")).thenReturn(true);
            when(jwtService.extractUsername("intermediate.jwt")).thenReturn("test@example.com");

            // When
            String email = twoFactorService.resolveEmailFromIntermediateToken("intermediate.jwt");

            // Then
            assertThat(email).isEqualTo("test@example.com");
        }
    }
}
