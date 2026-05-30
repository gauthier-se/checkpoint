package com.checkpoint.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.admin.AdminAnalyticsDto;
import com.checkpoint.api.services.AdminAnalyticsService;

/**
 * REST controller for the admin analytics dashboard.
 * All endpoints require the {@code ROLE_ADMIN} authority.
 */
@Tag(name = "Admin", description = "Admin: analytics dashboard")
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsController.class);

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    /**
     * Returns the platform-wide analytics snapshot for the admin dashboard.
     *
     * @return KPIs and top-5 lists
     */
    @GetMapping
    public ResponseEntity<AdminAnalyticsDto> getAnalytics() {
        log.info("Admin request: fetching analytics dashboard");

        AdminAnalyticsDto analytics = adminAnalyticsService.getAnalytics();

        return ResponseEntity.ok(analytics);
    }
}
