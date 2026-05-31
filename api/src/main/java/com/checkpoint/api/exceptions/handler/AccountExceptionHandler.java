package com.checkpoint.api.exceptions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.checkpoint.api.dto.error.ErrorResponse;
import com.checkpoint.api.exceptions.InvalidRefreshTokenException;
import com.checkpoint.api.exceptions.InvalidTokenException;
import com.checkpoint.api.exceptions.InvalidTotpCodeException;
import com.checkpoint.api.exceptions.ProfilePrivateException;
import com.checkpoint.api.exceptions.PseudoAlreadyExistsException;
import com.checkpoint.api.exceptions.RegistrationConflictException;
import com.checkpoint.api.exceptions.UserBannedException;
import com.checkpoint.api.exceptions.UserNotFoundException;

/**
 * Handles exceptions raised by account, profile, authentication, token, 2FA,
 * and registration endpoints.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AccountExceptionHandler extends AbstractExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountExceptionHandler.class);

    /** Handles {@link UserNotFoundException}. */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link UserBannedException} when a banned user attempts to authenticate. */
    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<ErrorResponse> handleUserBanned(UserBannedException ex) {
        log.warn("Banned user attempted access: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Handles {@link ProfilePrivateException} when a non-owner accesses private profile data. */
    @ExceptionHandler(ProfilePrivateException.class)
    public ResponseEntity<ErrorResponse> handleProfilePrivate(ProfilePrivateException ex) {
        log.warn("Private profile access: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Handles {@link PseudoAlreadyExistsException} when a pseudo update collides with an existing one. */
    @ExceptionHandler(PseudoAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePseudoAlreadyExists(PseudoAlreadyExistsException ex) {
        log.warn("Pseudo already exists: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link RegistrationConflictException} when a registration uses a duplicate email or pseudo. */
    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationConflict(RegistrationConflictException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link InvalidRefreshTokenException} when a refresh token is missing, revoked, or expired. */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        log.warn("Invalid refresh token: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Handles {@link InvalidTokenException} when a password reset token is invalid or expired. */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Invalid token: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Handles {@link InvalidTotpCodeException} when a submitted TOTP code is invalid. */
    @ExceptionHandler(InvalidTotpCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTotpCode(InvalidTotpCodeException ex) {
        log.warn("Invalid TOTP code: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
