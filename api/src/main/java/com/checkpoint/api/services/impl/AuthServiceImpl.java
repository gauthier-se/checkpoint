package com.checkpoint.api.services.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.auth.ForgotPasswordRequestDto;
import com.checkpoint.api.dto.auth.LoginRequestDto;
import com.checkpoint.api.dto.auth.RegisterRequestDto;
import com.checkpoint.api.dto.auth.RegisterWithSteamRequestDto;
import com.checkpoint.api.dto.auth.ResetPasswordRequestDto;
import com.checkpoint.api.dto.auth.TokenPairDto;
import com.checkpoint.api.dto.auth.TwoFactorLoginRequestDto;
import com.checkpoint.api.dto.auth.TwoFactorRequiredResponseDto;
import com.checkpoint.api.dto.auth.UserMeDto;
import com.checkpoint.api.dto.onboarding.OnboardingSteps;
import com.checkpoint.api.entities.PasswordResetToken;
import com.checkpoint.api.entities.RefreshToken;
import com.checkpoint.api.entities.Role;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.InvalidTokenException;
import com.checkpoint.api.exceptions.InvalidTotpCodeException;
import com.checkpoint.api.exceptions.RegistrationConflictException;
import com.checkpoint.api.exceptions.TwoFactorRequiredException;
import com.checkpoint.api.services.SteamSignupTokenService;
import com.checkpoint.api.services.TwoFactorService;
import com.checkpoint.api.repositories.PasswordResetTokenRepository;
import com.checkpoint.api.repositories.RoleRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.security.JwtService;
import com.checkpoint.api.services.AuthService;
import com.checkpoint.api.services.EmailService;
import com.checkpoint.api.services.RefreshTokenService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Implementation of {@link AuthService}.
 * Delegates credential validation to Spring Security's {@link AuthenticationManager},
 * then either generates a JWT token (Desktop) or writes JWT HttpOnly cookies (Web).
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String COOKIE_NAME = "checkpoint_token";
    private static final String REFRESH_COOKIE_NAME = "checkpoint_refresh";
    private static final String TWO_FA_COOKIE_NAME = "checkpoint_2fa";
    private static final long TWO_FA_COOKIE_MAX_AGE_SECONDS = 300L;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final TwoFactorService twoFactorService;
    private final SteamSignupTokenService steamSignupTokenService;
    private final boolean cookieSecure;
    private final long jwtExpirationMs;
    private final long refreshExpirationMs;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UserDetailsService userDetailsService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           RoleRepository roleRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           EmailService emailService,
                           RefreshTokenService refreshTokenService,
                           TwoFactorService twoFactorService,
                           SteamSignupTokenService steamSignupTokenService,
                           @Value("${app.cookie.secure:true}") boolean cookieSecure,
                           @Value("${jwt.expiration-ms:86400000}") long jwtExpirationMs,
                           @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
        this.twoFactorService = twoFactorService;
        this.steamSignupTokenService = steamSignupTokenService;
        this.cookieSecure = cookieSecure;
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    @Transactional
    public TokenPairDto authenticateAndGenerateTokenPair(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.email()));

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new TwoFactorRequiredException(twoFactorService.generateIntermediateToken(request.email()));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenPairDto(accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public void authenticateAndSetCookie(LoginRequestDto request, HttpServletResponse servletResponse) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.email()));

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            String intermediateToken = twoFactorService.generateIntermediateToken(request.email());
            ResponseCookie twoFaCookie = buildCookie(TWO_FA_COOKIE_NAME, intermediateToken, TWO_FA_COOKIE_MAX_AGE_SECONDS, "/api/auth/2fa/login");
            servletResponse.addHeader(HttpHeaders.SET_COOKIE, twoFaCookie.toString());
            throw new TwoFactorRequiredException(null);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String accessToken = jwtService.generateToken(userDetails);

        ResponseCookie accessCookie = buildCookie(COOKIE_NAME, accessToken, jwtExpirationMs / 1000, "/api");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie refreshCookie = buildCookie(REFRESH_COOKIE_NAME, refreshToken.getToken(), refreshExpirationMs / 1000, "/api/auth/refresh");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    @Override
    @Transactional
    public void establishWebSession(String email, HttpServletResponse servletResponse) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String accessToken = jwtService.generateToken(userDetails);

        ResponseCookie accessCookie = buildCookie(COOKIE_NAME, accessToken, jwtExpirationMs / 1000, "/api");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        ResponseCookie refreshCookie = buildCookie(REFRESH_COOKIE_NAME, refreshToken.getToken(), refreshExpirationMs / 1000, "/api/auth/refresh");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean requireTwoFactorChallenge(String email, HttpServletResponse servletResponse) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            return false;
        }

        String intermediateToken = twoFactorService.generateIntermediateToken(email);
        ResponseCookie twoFaCookie = buildCookie(TWO_FA_COOKIE_NAME, intermediateToken,
                TWO_FA_COOKIE_MAX_AGE_SECONDS, "/api/auth/2fa/login");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, twoFaCookie.toString());
        return true;
    }

    @Override
    @Transactional
    public void refreshTokenAndSetCookie(String refreshToken, HttpServletResponse servletResponse) {
        RefreshToken existing = refreshTokenService.validateToken(refreshToken);
        User user = existing.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        refreshTokenService.revokeToken(refreshToken);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie accessCookie = buildCookie(COOKIE_NAME, newAccessToken, jwtExpirationMs / 1000, "/api");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = buildCookie(REFRESH_COOKIE_NAME, newRefreshToken.getToken(), refreshExpirationMs / 1000, "/api/auth/refresh");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    @Override
    @Transactional
    public TokenPairDto refreshTokenForDesktop(String refreshToken) {
        RefreshToken existing = refreshTokenService.validateToken(refreshToken);
        User user = existing.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        refreshTokenService.revokeToken(refreshToken);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenPairDto(newAccessToken, newRefreshToken.getToken());
    }

    @Override
    public void clearAuthCookie(String refreshToken, HttpServletResponse servletResponse) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                refreshTokenService.revokeToken(refreshToken);
            } catch (Exception ex) {
                log.warn("Failed to revoke refresh token on logout: {}", ex.getMessage());
            }
        }
        ResponseCookie accessCookie = buildCookie(COOKIE_NAME, "", 0, "/api");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = buildCookie(REFRESH_COOKIE_NAME, "", 0, "/api/auth/refresh");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        SecurityContextHolder.clearContext();
    }

    @Override
    public UserMeDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        String roleName = user.getRole() != null ? user.getRole().getName() : "USER";

        return new UserMeDto(
                user.getId(),
                user.getPseudo(),
                user.getEmail(),
                roleName,
                user.getBio(),
                user.getPicture(),
                user.getIsPrivate(),
                Boolean.TRUE.equals(user.getTwoFactorEnabled()),
                user.getSteamId(),
                user.getSteamDisplayName(),
                user.getSteamAvatarUrl(),
                user.getOnboardingCompletedAt(),
                user.getOnboardingSteps()
        );
    }

    @Override
    public void register(RegisterRequestDto request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new RegistrationConflictException("Email is already in use");
        }
        if (userRepository.existsByPseudo(request.pseudo())) {
            throw new RegistrationConflictException("Pseudo is already in use");
        }

        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role newRole = new Role("USER");
            return roleRepository.save(newRole);
        });

        User user = new User(
                request.pseudo(),
                request.email(),
                passwordEncoder.encode(request.password())
        );
        user.setRole(userRole);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void registerWithSteam(RegisterWithSteamRequestDto request, HttpServletResponse servletResponse) {
        SteamSignupTokenService.Claims claims = steamSignupTokenService.verify(request.token())
                .orElseThrow(() -> new InvalidTokenException("Steam signup token is invalid or expired"));

        if (userRepository.existsByEmail(request.email())) {
            throw new RegistrationConflictException("Email is already in use");
        }
        if (userRepository.existsByPseudo(request.pseudo())) {
            throw new RegistrationConflictException("Pseudo is already in use");
        }
        if (userRepository.findBySteamId(claims.steamId()).isPresent()) {
            throw new RegistrationConflictException("This Steam account is already linked to a CheckPoint user");
        }

        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role newRole = new Role("USER");
            return roleRepository.save(newRole);
        });

        String hashedPassword = (request.password() == null || request.password().isBlank())
                ? null
                : passwordEncoder.encode(request.password());

        User user = new User(request.pseudo(), request.email(), hashedPassword);
        user.setRole(userRole);
        user.setSteamId(claims.steamId());
        user.setSteamDisplayName(claims.steamDisplayName());
        user.setSteamAvatarUrl(claims.steamAvatarUrl());
        user.setSteamProfileUrl(claims.steamProfileUrl());
        user.setSteamSyncedAt(LocalDateTime.now());

        // Account is created with Steam already linked — count that step as done, and the
        // picture step too when Steam handed us an avatar URL.
        user.getOnboardingSteps().put(OnboardingSteps.STEAM, true);
        if (claims.steamAvatarUrl() != null && !claims.steamAvatarUrl().isBlank()) {
            user.getOnboardingSteps().put(OnboardingSteps.PICTURE, true);
        }

        userRepository.save(user);

        establishWebSession(user.getEmail(), servletResponse);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(
                    token,
                    user,
                    LocalDateTime.now().plusMinutes(15)
            );
            passwordResetTokenRepository.save(resetToken);

            String resetLink = "http://localhost:3000/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new InvalidTokenException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    @Override
    public String generateWsToken(UserDetails userDetails) {
        return jwtService.generateToken(userDetails);
    }

    @Override
    @Transactional
    public void completeTwoFactorLoginForWeb(TwoFactorLoginRequestDto request, String twoFaCookie, HttpServletResponse servletResponse) {
        String email = twoFactorService.resolveEmailFromIntermediateToken(twoFaCookie);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!twoFactorService.verifyCode(user.getTotpSecret(), request.code())) {
            throw new InvalidTotpCodeException("Invalid TOTP code. Please try again.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie expiredTwoFaCookie = buildCookie(TWO_FA_COOKIE_NAME, "", 0, "/api/auth/2fa/login");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, expiredTwoFaCookie.toString());

        ResponseCookie accessCookie = buildCookie(COOKIE_NAME, accessToken, jwtExpirationMs / 1000, "/api");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = buildCookie(REFRESH_COOKIE_NAME, refreshToken.getToken(), refreshExpirationMs / 1000, "/api/auth/refresh");
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    @Override
    @Transactional
    public TokenPairDto completeTwoFactorLoginForDesktop(TwoFactorLoginRequestDto request) {
        String email = twoFactorService.resolveEmailFromIntermediateToken(request.intermediateToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!twoFactorService.verifyCode(user.getTotpSecret(), request.code())) {
            throw new InvalidTotpCodeException("Invalid TOTP code. Please try again.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenPairDto(accessToken, refreshToken.getToken());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
