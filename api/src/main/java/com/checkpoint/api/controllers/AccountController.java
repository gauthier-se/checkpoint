package com.checkpoint.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.services.AccountService;
import com.checkpoint.api.services.AuthService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * REST controller for authenticated account-management operations
 * (the user acting on their own account).
 */
@RestController
@RequestMapping("/api/me")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;
    private final AuthService authService;

    public AccountController(AccountService accountService, AuthService authService) {
        this.accountService = accountService;
        this.authService = authService;
    }

    /**
     * Permanently deletes the authenticated user's account and every piece of
     * personal data associated with it (GDPR Article 17 — right to erasure).
     *
     * <p>The session is invalidated: the refresh token row is destroyed by the
     * account-erasure transaction and both the {@code checkpoint_token} and
     * {@code checkpoint_refresh} cookies are expired on the response so the
     * next request from this browser will be unauthenticated.</p>
     *
     * @param userDetails   the authenticated user principal
     * @param refreshCookie the {@code checkpoint_refresh} cookie value, used
     *                      only to expire the cookie on the response
     * @param response      the HTTP response to write the expired cookies on
     * @return 204 No Content
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @CookieValue(value = "checkpoint_refresh", required = false) String refreshCookie,
            HttpServletResponse response) {

        log.info("DELETE /api/me - user: {}", userDetails.getUsername());

        accountService.deleteCurrentUser(userDetails.getUsername());
        authService.clearAuthCookie(refreshCookie, response);

        return ResponseEntity.noContent().build();
    }
}
