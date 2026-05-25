package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.checkpoint.api.dto.auth.ForgotPasswordRequestDto;
import com.checkpoint.api.dto.auth.LoginRequestDto;
import com.checkpoint.api.dto.auth.RegisterRequestDto;
import com.checkpoint.api.dto.auth.RegisterWithSteamRequestDto;
import com.checkpoint.api.dto.auth.ResetPasswordRequestDto;
import com.checkpoint.api.dto.auth.TokenPairDto;
import com.checkpoint.api.dto.auth.UserMeDto;
import com.checkpoint.api.entities.PasswordResetToken;
import com.checkpoint.api.entities.RefreshToken;
import com.checkpoint.api.entities.Role;
import com.checkpoint.api.exceptions.InvalidTokenException;
import com.checkpoint.api.exceptions.RegistrationConflictException;
import com.checkpoint.api.repositories.PasswordResetTokenRepository;
import com.checkpoint.api.repositories.RoleRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.security.JwtService;
import com.checkpoint.api.services.impl.AuthServiceImpl;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TwoFactorService twoFactorService;

    @Mock
    private SteamSignupTokenService steamSignupTokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                authenticationManager,
                jwtService,
                userDetailsService,
                userRepository,
                passwordEncoder,
                roleRepository,
                passwordResetTokenRepository,
                emailService,
                refreshTokenService,
                twoFactorService,
                steamSignupTokenService,
                false,       // cookieSecure = false in tests
                86400000L,   // jwtExpirationMs = 24h
                604800000L   // refreshExpirationMs = 7d
        );
    }

    @Nested
    @DisplayName("authenticateAndGenerateTokenPair")
    class AuthenticateAndGenerateTokenPairTests {

        @Test
        @DisplayName("Should authenticate and return access + refresh token pair")
        void shouldAuthenticateAndReturnTokenPair() {
            // Given
            LoginRequestDto request = new LoginRequestDto("user@test.com", "password123");
            UserDetails userDetails = User.builder()
                    .username("user@test.com")
                    .password("encodedPassword")
                    .roles("USER")
                    .build();
            com.checkpoint.api.entities.User userEntity = new com.checkpoint.api.entities.User("alice", "user@test.com", "enc");
            RefreshToken refreshToken = new RefreshToken("refresh-uuid", userEntity, LocalDateTime.now().plusDays(7));

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mock(Authentication.class));
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("access.token.here");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(userEntity));
            when(refreshTokenService.createRefreshToken(userEntity)).thenReturn(refreshToken);

            // When
            TokenPairDto result = authService.authenticateAndGenerateTokenPair(request);

            // Then
            assertThat(result.accessToken()).isEqualTo("access.token.here");
            assertThat(result.refreshToken()).isEqualTo("refresh-uuid");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userDetailsService).loadUserByUsername("user@test.com");
            verify(jwtService).generateToken(userDetails);
            verify(refreshTokenService).createRefreshToken(userEntity);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for invalid credentials")
        void shouldThrowForInvalidCredentials() {
            // Given
            LoginRequestDto request = new LoginRequestDto("user@test.com", "wrongPassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When / Then
            assertThatThrownBy(() -> authService.authenticateAndGenerateTokenPair(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Bad credentials");

            verify(userDetailsService, never()).loadUserByUsername(any());
            verify(jwtService, never()).generateToken(any(UserDetails.class));
        }
    }

    @Nested
    @DisplayName("authenticateAndSetCookie")
    class AuthenticateAndSetCookieTests {

        @Test
        @DisplayName("Should authenticate and write checkpoint_token and checkpoint_refresh cookies")
        void shouldAuthenticateAndSetCookies() {
            // Given
            LoginRequestDto request = new LoginRequestDto("user@test.com", "password123");
            UserDetails userDetails = User.builder()
                    .username("user@test.com")
                    .password("encodedPassword")
                    .roles("USER")
                    .build();
            com.checkpoint.api.entities.User userEntity = new com.checkpoint.api.entities.User("alice", "user@test.com", "enc");
            RefreshToken refreshToken = new RefreshToken("refresh-uuid", userEntity, LocalDateTime.now().plusDays(7));
            HttpServletResponse servletResponse = mock(HttpServletResponse.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mock(Authentication.class));
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("jwt.token.here");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(userEntity));
            when(refreshTokenService.createRefreshToken(userEntity)).thenReturn(refreshToken);

            // When
            authService.authenticateAndSetCookie(request, servletResponse);

            // Then
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService).generateToken(userDetails);

            ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
            verify(servletResponse, org.mockito.Mockito.times(2))
                    .addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

            java.util.List<String> cookieHeaders = headerValueCaptor.getAllValues();
            assertThat(cookieHeaders).anyMatch(h -> h.contains("checkpoint_token=jwt.token.here")
                    && h.contains("HttpOnly") && h.contains("Path=/api"));
            assertThat(cookieHeaders).anyMatch(h -> h.contains("checkpoint_refresh=refresh-uuid")
                    && h.contains("HttpOnly") && h.contains("Path=/api/auth/refresh"));
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for invalid credentials")
        void shouldThrowForInvalidCredentials() {
            // Given
            LoginRequestDto request = new LoginRequestDto("user@test.com", "wrongPassword");
            HttpServletResponse servletResponse = mock(HttpServletResponse.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When / Then
            assertThatThrownBy(() -> authService.authenticateAndSetCookie(request, servletResponse))
                    .isInstanceOf(BadCredentialsException.class);

            verify(servletResponse, never()).addHeader(any(), any());
        }
    }

    @Nested
    @DisplayName("requireTwoFactorChallenge")
    class RequireTwoFactorChallengeTests {

        @Test
        @DisplayName("Should set checkpoint_2fa cookie and return true when 2FA is enabled")
        void shouldChallengeWhenTwoFactorEnabled() {
            // Given
            com.checkpoint.api.entities.User user =
                    new com.checkpoint.api.entities.User("alice", "user@test.com", "enc");
            user.setTwoFactorEnabled(true);
            HttpServletResponse servletResponse = mock(HttpServletResponse.class);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(twoFactorService.generateIntermediateToken("user@test.com"))
                    .thenReturn("intermediate.jwt");

            // When
            boolean challenged = authService.requireTwoFactorChallenge("user@test.com", servletResponse);

            // Then
            assertThat(challenged).isTrue();

            ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
            verify(servletResponse).addHeader(org.mockito.ArgumentMatchers.eq("Set-Cookie"),
                    headerValueCaptor.capture());
            String cookie = headerValueCaptor.getValue();
            assertThat(cookie).contains("checkpoint_2fa=intermediate.jwt")
                    .contains("HttpOnly")
                    .contains("Path=/api/auth/2fa/login");
        }

        @Test
        @DisplayName("Should write no cookie and return false when 2FA is disabled")
        void shouldNotChallengeWhenTwoFactorDisabled() {
            // Given
            com.checkpoint.api.entities.User user =
                    new com.checkpoint.api.entities.User("alice", "user@test.com", "enc");
            HttpServletResponse servletResponse = mock(HttpServletResponse.class);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

            // When
            boolean challenged = authService.requireTwoFactorChallenge("user@test.com", servletResponse);

            // Then
            assertThat(challenged).isFalse();
            verify(servletResponse, never()).addHeader(any(), any());
            verify(twoFactorService, never()).generateIntermediateToken(any());
        }
    }

    @Nested
    @DisplayName("clearAuthCookie")
    class ClearAuthCookieTests {

        @Test
        @DisplayName("Should expire both cookies and revoke the refresh token")
        void shouldWriteExpiredCookies() {
            // Given
            HttpServletResponse servletResponse = mock(HttpServletResponse.class);

            // When
            authService.clearAuthCookie("some-refresh-token", servletResponse);

            // Then
            verify(refreshTokenService).revokeToken("some-refresh-token");

            ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
            verify(servletResponse, org.mockito.Mockito.times(2))
                    .addHeader(any(), headerValueCaptor.capture());

            java.util.List<String> cookieHeaders = headerValueCaptor.getAllValues();
            assertThat(cookieHeaders).anyMatch(h -> h.contains("checkpoint_token=") && h.contains("Max-Age=0"));
            assertThat(cookieHeaders).anyMatch(h -> h.contains("checkpoint_refresh=") && h.contains("Max-Age=0"));
        }

        @Test
        @DisplayName("Should still expire cookies when no refresh token is provided")
        void shouldExpireCookiesWithoutRefreshToken() {
            // Given
            HttpServletResponse servletResponse = mock(HttpServletResponse.class);

            // When
            authService.clearAuthCookie(null, servletResponse);

            // Then
            verify(refreshTokenService, never()).revokeToken(any());
            verify(servletResponse, org.mockito.Mockito.times(2)).addHeader(any(), any());
        }
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register a new user")
        void shouldRegisterNewUser() {
            // Given
            RegisterRequestDto request = new RegisterRequestDto("newuser", "test@test.com", "password123", "password123");
            Role role = new Role("USER");

            when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
            when(userRepository.existsByPseudo("newuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

            // When
            authService.register(request);

            // Then
            verify(userRepository).save(any(com.checkpoint.api.entities.User.class));
        }

        @Test
        @DisplayName("Should create USER role if it does not exist")
        void shouldCreateRoleIfNotFound() {
            RegisterRequestDto request = new RegisterRequestDto("newuser", "test@test.com", "password123", "password123");
            Role newRole = new Role("USER");

            when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
            when(userRepository.existsByPseudo("newuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenReturn(newRole);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

            authService.register(request);

            verify(roleRepository).save(any(Role.class));
            verify(userRepository).save(any(com.checkpoint.api.entities.User.class));
        }

        @Test
        @DisplayName("Should throw RegistrationConflictException if email exists")
        void shouldThrowIfEmailExists() {
            RegisterRequestDto request = new RegisterRequestDto("newuser", "test@test.com", "password123", "password123");
            when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RegistrationConflictException.class)
                    .hasMessageContaining("Email is already in use");
        }

        @Test
        @DisplayName("Should throw RegistrationConflictException if pseudo exists")
        void shouldThrowIfPseudoExists() {
            RegisterRequestDto request = new RegisterRequestDto("newuser", "test@test.com", "password123", "password123");
            when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
            when(userRepository.existsByPseudo("newuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RegistrationConflictException.class)
                    .hasMessageContaining("Pseudo is already in use");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if passwords do not match")
        void shouldThrowIfPasswordsMismatch() {
            RegisterRequestDto request = new RegisterRequestDto("newuser", "test@test.com", "password123", "different123");

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Passwords do not match");

            verify(userRepository, never()).existsByEmail(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("registerWithSteam")
    class RegisterWithSteamTests {

        private static final String STEAM_ID = "76561198000000000";

        private SteamSignupTokenService.Claims claims() {
            return new SteamSignupTokenService.Claims(
                    STEAM_ID,
                    "SteamPersona",
                    "https://cdn/avatar.jpg",
                    "https://steamcommunity.com/id/persona");
        }

        @Test
        @DisplayName("Should create the Steam-linked user, hash the optional password, then establish a web session")
        void shouldCreateUserWithPasswordAndEstablishSession() {
            HttpServletResponse response = mock(HttpServletResponse.class);
            RegisterWithSteamRequestDto request = new RegisterWithSteamRequestDto(
                    "valid-token", "alice@test.com", "alice", true, "password123");
            Role role = new Role("USER");

            when(steamSignupTokenService.verify("valid-token")).thenReturn(Optional.of(claims()));
            when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
            when(userRepository.existsByPseudo("alice")).thenReturn(false);
            when(userRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.empty());
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

            com.checkpoint.api.entities.User loadedAfterSave = new com.checkpoint.api.entities.User(
                    "alice", "alice@test.com", "encodedPassword");
            loadedAfterSave.setSteamId(STEAM_ID);
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(loadedAfterSave));
            UserDetails userDetails = User.builder()
                    .username("alice@test.com")
                    .password("encodedPassword")
                    .roles("USER")
                    .build();
            when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("access.token");
            RefreshToken refresh = mock(RefreshToken.class);
            when(refresh.getToken()).thenReturn("refresh-token");
            when(refreshTokenService.createRefreshToken(any())).thenReturn(refresh);

            authService.registerWithSteam(request, response);

            ArgumentCaptor<com.checkpoint.api.entities.User> savedCaptor =
                    ArgumentCaptor.forClass(com.checkpoint.api.entities.User.class);
            verify(userRepository).save(savedCaptor.capture());
            com.checkpoint.api.entities.User saved = savedCaptor.getValue();
            assertThat(saved.getPseudo()).isEqualTo("alice");
            assertThat(saved.getEmail()).isEqualTo("alice@test.com");
            assertThat(saved.getPassword()).isEqualTo("encodedPassword");
            assertThat(saved.getSteamId()).isEqualTo(STEAM_ID);
            assertThat(saved.getSteamDisplayName()).isEqualTo("SteamPersona");
            assertThat(saved.getSteamAvatarUrl()).isEqualTo("https://cdn/avatar.jpg");
            assertThat(saved.getSteamProfileUrl()).isEqualTo("https://steamcommunity.com/id/persona");
            assertThat(saved.getSteamSyncedAt()).isNotNull();

            verify(response, org.mockito.Mockito.atLeastOnce())
                    .addHeader(org.mockito.ArgumentMatchers.eq("Set-Cookie"), org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("Should create the user with a null password when none is provided")
        void shouldCreateUserWithoutPassword() {
            HttpServletResponse response = mock(HttpServletResponse.class);
            RegisterWithSteamRequestDto request = new RegisterWithSteamRequestDto(
                    "valid-token", "alice@test.com", "alice", true, null);
            Role role = new Role("USER");

            when(steamSignupTokenService.verify("valid-token")).thenReturn(Optional.of(claims()));
            when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
            when(userRepository.existsByPseudo("alice")).thenReturn(false);
            when(userRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.empty());
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));

            com.checkpoint.api.entities.User loadedAfterSave = new com.checkpoint.api.entities.User(
                    "alice", "alice@test.com", null);
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(loadedAfterSave));
            UserDetails userDetails = User.builder()
                    .username("alice@test.com")
                    .password("noop")
                    .roles("USER")
                    .build();
            when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("access.token");
            RefreshToken refresh = mock(RefreshToken.class);
            when(refresh.getToken()).thenReturn("refresh-token");
            when(refreshTokenService.createRefreshToken(any())).thenReturn(refresh);

            authService.registerWithSteam(request, response);

            ArgumentCaptor<com.checkpoint.api.entities.User> savedCaptor =
                    ArgumentCaptor.forClass(com.checkpoint.api.entities.User.class);
            verify(userRepository).save(savedCaptor.capture());
            assertThat(savedCaptor.getValue().getPassword()).isNull();
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("Should reject an invalid signup token")
        void shouldRejectInvalidToken() {
            HttpServletResponse response = mock(HttpServletResponse.class);
            RegisterWithSteamRequestDto request = new RegisterWithSteamRequestDto(
                    "bad-token", "alice@test.com", "alice", true, null);

            when(steamSignupTokenService.verify("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerWithSteam(request, response))
                    .isInstanceOf(InvalidTokenException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject when the email is already in use")
        void shouldRejectExistingEmail() {
            HttpServletResponse response = mock(HttpServletResponse.class);
            RegisterWithSteamRequestDto request = new RegisterWithSteamRequestDto(
                    "valid-token", "alice@test.com", "alice", true, null);

            when(steamSignupTokenService.verify("valid-token")).thenReturn(Optional.of(claims()));
            when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerWithSteam(request, response))
                    .isInstanceOf(RegistrationConflictException.class)
                    .hasMessageContaining("Email");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject when the pseudo is already in use")
        void shouldRejectExistingPseudo() {
            HttpServletResponse response = mock(HttpServletResponse.class);
            RegisterWithSteamRequestDto request = new RegisterWithSteamRequestDto(
                    "valid-token", "alice@test.com", "alice", true, null);

            when(steamSignupTokenService.verify("valid-token")).thenReturn(Optional.of(claims()));
            when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
            when(userRepository.existsByPseudo("alice")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerWithSteam(request, response))
                    .isInstanceOf(RegistrationConflictException.class)
                    .hasMessageContaining("Pseudo");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject when the SteamID is already linked to another user")
        void shouldRejectAlreadyLinkedSteamId() {
            HttpServletResponse response = mock(HttpServletResponse.class);
            RegisterWithSteamRequestDto request = new RegisterWithSteamRequestDto(
                    "valid-token", "alice@test.com", "alice", true, null);

            when(steamSignupTokenService.verify("valid-token")).thenReturn(Optional.of(claims()));
            when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
            when(userRepository.existsByPseudo("alice")).thenReturn(false);
            com.checkpoint.api.entities.User other = new com.checkpoint.api.entities.User();
            other.setSteamId(STEAM_ID);
            when(userRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> authService.registerWithSteam(request, response))
                    .isInstanceOf(RegistrationConflictException.class)
                    .hasMessageContaining("Steam");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return UserMeDto with role and cached Steam fields read straight from the entity")
        void shouldReturnUserMeDto() {
            // Given
            UUID userId = UUID.randomUUID();
            Role role = new Role("ADMIN");
            com.checkpoint.api.entities.User user = new com.checkpoint.api.entities.User();
            user.setId(userId);
            user.setPseudo("alice");
            user.setEmail("alice@test.com");
            user.setRole(role);
            user.setSteamId("76561198000000000");
            user.setSteamDisplayName("AliceOnSteam");
            user.setSteamAvatarUrl("https://avatar.url/m.jpg");

            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            // When
            UserMeDto result = authService.getCurrentUser("alice@test.com");

            // Then
            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.username()).isEqualTo("alice");
            assertThat(result.email()).isEqualTo("alice@test.com");
            assertThat(result.role()).isEqualTo("ADMIN");
            assertThat(result.steamId()).isEqualTo("76561198000000000");
            assertThat(result.steamDisplayName()).isEqualTo("AliceOnSteam");
            assertThat(result.steamAvatarUrl()).isEqualTo("https://avatar.url/m.jpg");
        }

        @Test
        @DisplayName("Should default role to USER when user has no role")
        void shouldDefaultRoleToUser() {
            // Given
            UUID userId = UUID.randomUUID();
            com.checkpoint.api.entities.User user = new com.checkpoint.api.entities.User();
            user.setId(userId);
            user.setPseudo("bob");
            user.setEmail("bob@test.com");
            user.setRole(null);

            when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));

            // When
            UserMeDto result = authService.getCurrentUser("bob@test.com");

            // Then
            assertThat(result.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authService.getCurrentUser("unknown@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("unknown@test.com");
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should generate token and send reset email if email exists")
        void shouldGenerateToken() {
            // Given
            com.checkpoint.api.entities.User user = new com.checkpoint.api.entities.User();
            UUID userId = UUID.randomUUID();
            user.setId(userId);
            user.setEmail("user@test.com");

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("user@test.com");

            // When
            authService.forgotPassword(request);

            // Then
            verify(passwordResetTokenRepository).deleteByUserId(userId);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(any(String.class), any(String.class));
        }

        @Test
        @DisplayName("Should silently ignore if email does not exist")
        void shouldIgnoreIfEmailNotFound() {
            // Given
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("unknown@test.com");

            // When
            authService.forgotPassword(request);

            // Then
            verify(passwordResetTokenRepository, never()).deleteByUserId(any());
            verify(passwordResetTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password and delete token on success")
        void shouldResetPassword() {
            // Given
            com.checkpoint.api.entities.User user = new com.checkpoint.api.entities.User();
            PasswordResetToken token = new PasswordResetToken("valid-token", user, LocalDateTime.now().plusMinutes(15));

            when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("new-password123")).thenReturn("encoded-new-password");

            ResetPasswordRequestDto request = new ResetPasswordRequestDto("valid-token", "new-password123");

            // When
            authService.resetPassword(request);

            // Then
            verify(passwordEncoder).encode("new-password123");
            verify(userRepository).save(user);
            verify(passwordResetTokenRepository).delete(token);
        }

        @Test
        @DisplayName("Should throw if token not found")
        void shouldThrowIfTokenNotFound() {
            // Given
            when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            ResetPasswordRequestDto request = new ResetPasswordRequestDto("invalid-token", "new-password123");

            // When / Then
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid reset token");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw if token expired")
        void shouldThrowIfTokenExpired() {
            // Given
            com.checkpoint.api.entities.User user = new com.checkpoint.api.entities.User();
            PasswordResetToken token = new PasswordResetToken("expired-token", user, LocalDateTime.now().minusMinutes(1));

            when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

            ResetPasswordRequestDto request = new ResetPasswordRequestDto("expired-token", "new-password123");

            // When / Then
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Token has expired");

            verify(passwordResetTokenRepository).delete(token);
            verify(userRepository, never()).save(any());
        }
    }
}
