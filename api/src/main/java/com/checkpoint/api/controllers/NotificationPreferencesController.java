package com.checkpoint.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.notification.NotificationPreferencesDto;
import com.checkpoint.api.dto.notification.UpdateNotificationPreferencesDto;
import com.checkpoint.api.services.NotificationPreferencesService;

/**
 * REST controller for managing the authenticated user's notification preferences.
 *
 * <p>Both endpoints require authentication and operate on the current user only.
 * The first call lazily creates a preferences row with every type enabled.</p>
 */
@Tag(name = "Notifications and Feed", description = "Notification preferences")
@RestController
@RequestMapping("/api/me/notification-preferences")
public class NotificationPreferencesController {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferencesController.class);

    private final NotificationPreferencesService preferencesService;

    public NotificationPreferencesController(NotificationPreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    /**
     * Returns the authenticated user's notification preferences, creating
     * defaults (all enabled) if none exist yet.
     *
     * @param userDetails the authenticated user principal
     * @return the preferences DTO
     */
    @GetMapping
    public ResponseEntity<NotificationPreferencesDto> getPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/me/notification-preferences - user: {}", userDetails.getUsername());

        NotificationPreferencesDto response = preferencesService.getOrCreate(userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates the authenticated user's notification preferences.
     * Fields set to {@code null} in the body are left unchanged.
     *
     * @param userDetails the authenticated user principal
     * @param body        the partial update payload
     * @return the updated preferences DTO
     */
    @PutMapping
    public ResponseEntity<NotificationPreferencesDto> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateNotificationPreferencesDto body) {

        log.info("PUT /api/me/notification-preferences - user: {}", userDetails.getUsername());

        NotificationPreferencesDto response = preferencesService.update(userDetails.getUsername(), body);

        return ResponseEntity.ok(response);
    }
}
