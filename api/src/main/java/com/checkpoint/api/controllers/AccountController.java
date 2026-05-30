package com.checkpoint.api.controllers;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.export.UserDataExportDto;
import com.checkpoint.api.services.AccountService;
import com.checkpoint.api.services.AuthService;
import com.checkpoint.api.services.DataExportService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * REST controller for authenticated account-management operations
 * (the user acting on their own account).
 */
@Tag(name = "Account and Profile", description = "Current user account settings and security")
@RestController
@RequestMapping("/api/me")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;
    private final AuthService authService;
    private final DataExportService dataExportService;

    public AccountController(AccountService accountService,
                             AuthService authService,
                             DataExportService dataExportService) {
        this.accountService = accountService;
        this.authService = authService;
        this.dataExportService = dataExportService;
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

    /**
     * Returns the authenticated user's personal data in a single
     * machine-readable JSON file (GDPR Article 20 — right to data portability).
     *
     * <p>The response body is the export payload itself. The
     * {@code Content-Disposition} header instructs the browser to save it as
     * {@code checkpoint-export-YYYY-MM-DD.json} rather than render it inline.</p>
     *
     * @param userDetails the authenticated user principal
     * @return 200 OK with the export payload, served as a JSON download
     */
    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDataExportDto> exportData(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/me/export - user: {}", userDetails.getUsername());

        UserDataExportDto export = dataExportService.exportForUser(userDetails.getUsername());
        String filename = "checkpoint-export-" + LocalDate.now() + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(export);
    }
}
