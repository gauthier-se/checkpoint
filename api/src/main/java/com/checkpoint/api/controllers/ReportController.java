package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.ReportRequestDto;
import com.checkpoint.api.dto.catalog.ReportResponseDto;
import com.checkpoint.api.services.ReportService;

import jakarta.validation.Valid;

/**
 * REST controller for reporting reviews.
 *
 * <p>Allows authenticated users to report a review for moderation.</p>
 */
@Tag(name = "Reviews and Comments", description = "Report inappropriate reviews")
@RestController
@RequestMapping("/api/reviews/{reviewId}/report")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    /**
     * Constructs a new ReportController.
     *
     * @param reportService the report service
     */
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Reports a review for moderation.
     *
     * @param userDetails the authenticated user principal
     * @param reviewId    the review ID to report
     * @param request     the report request containing the reason
     * @return the created report with 201 status
     */
    @PostMapping
    public ResponseEntity<ReportResponseDto> reportReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReportRequestDto request) {

        log.info("POST /api/reviews/{}/report - user: {}", reviewId, userDetails.getUsername());

        ReportResponseDto response = reportService.reportReview(
                userDetails.getUsername(), reviewId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
