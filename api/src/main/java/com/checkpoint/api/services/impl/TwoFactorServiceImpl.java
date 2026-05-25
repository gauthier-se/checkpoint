package com.checkpoint.api.services.impl;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.auth.TwoFactorSetupResponseDto;
import com.checkpoint.api.dto.onboarding.OnboardingSteps;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.InvalidTokenException;
import com.checkpoint.api.exceptions.InvalidTotpCodeException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.security.JwtService;
import com.checkpoint.api.services.OnboardingService;
import com.checkpoint.api.services.TwoFactorService;

/**
 * Implementation of {@link TwoFactorService} using the dev.samstevens.totp library.
 * Generates TOTP secrets, QR codes, and verifies time-based one-time passwords.
 */
@Service
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorServiceImpl.class);
    private static final long INTERMEDIATE_TOKEN_EXPIRATION_MS = 5 * 60 * 1000L;
    private static final String APP_ISSUER = "Checkpoint";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OnboardingService onboardingService;

    public TwoFactorServiceImpl(UserRepository userRepository,
                                 JwtService jwtService,
                                 PasswordEncoder passwordEncoder,
                                 OnboardingService onboardingService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.onboardingService = onboardingService;
    }

    @Override
    @Transactional
    public TwoFactorSetupResponseDto setup(String email) {
        User user = loadUser(email);

        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        user.setTotpSecret(secret);
        userRepository.save(user);

        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(APP_ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String provisioningUri = qrData.getUri();
        String qrCodeDataUrl = generateQrCodeDataUrl(qrData);

        log.info("2FA setup initiated for user: {}", email);
        return new TwoFactorSetupResponseDto(provisioningUri, qrCodeDataUrl);
    }

    @Override
    @Transactional
    public void verifyAndEnable(String email, String code) {
        User user = loadUser(email);

        if (user.getTotpSecret() == null) {
            throw new InvalidTotpCodeException("2FA setup has not been initiated. Call /2fa/setup first.");
        }

        if (!verifyCode(user.getTotpSecret(), code)) {
            throw new InvalidTotpCodeException("Invalid TOTP code. Please try again.");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        onboardingService.markStepDone(email, OnboardingSteps.TWOFA);
        log.info("2FA enabled for user: {}", email);
    }

    @Override
    @Transactional
    public void disable(String email, String password, String code) {
        User user = loadUser(email);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidTotpCodeException("Invalid password.");
        }

        if (user.getTotpSecret() == null || !verifyCode(user.getTotpSecret(), code)) {
            throw new InvalidTotpCodeException("Invalid TOTP code. Please try again.");
        }

        user.setTwoFactorEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        log.info("2FA disabled for user: {}", email);
    }

    @Override
    public String generateIntermediateToken(String email) {
        return jwtService.generateIntermediateToken(email, INTERMEDIATE_TOKEN_EXPIRATION_MS);
    }

    @Override
    public String resolveEmailFromIntermediateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Intermediate token is required.");
        }
        if (!jwtService.isIntermediateToken(token)) {
            throw new InvalidTokenException("Invalid or expired 2FA session token.");
        }
        try {
            return jwtService.extractUsername(token);
        } catch (Exception ex) {
            throw new InvalidTokenException("Invalid or expired 2FA session token.");
        }
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
    }

    private String generateQrCodeDataUrl(QrData qrData) {
        try {
            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(qrData);
            String mimeType = generator.getImageMimeType();
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (Exception ex) {
            log.error("Failed to generate QR code: {}", ex.getMessage());
            throw new IllegalStateException("Failed to generate QR code", ex);
        }
    }
}
