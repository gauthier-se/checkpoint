package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.ReviewRequestDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.services.ReviewService;

import jakarta.validation.Valid;

/**
 * REST controller for managing reviews tied to play log entries.
 *
 * <p>Each play log can have at most one review. A user can have multiple reviews
 * for the same game (one per playthrough).</p>
 *
 * <p>All endpoints require authentication. The authenticated user is resolved
 * from the security context.</p>
 */
@Tag(name = "Play Logs", description = "Reviews attached to a play log entry")
@RestController
@RequestMapping("/api/me/plays/{playId}/review")
public class PlayLogReviewController {

    private static final Logger log = LoggerFactory.getLogger(PlayLogReviewController.class);

    private final ReviewService reviewService;

    /**
     * Constructs a new PlayLogReviewController.
     *
     * @param reviewService the review service
     */
    public PlayLogReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Creates a review for a specific play log entry.
     *
     * @param userDetails the authenticated user principal
     * @param playId      the play log ID
     * @param request     the review request body containing content and spoiler flag
     * @return the created review with 201 status
     */
    @PostMapping
    public ResponseEntity<ReviewResponseDto> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID playId,
            @Valid @RequestBody ReviewRequestDto request) {

        log.info("POST /api/me/plays/{}/review - user: {}", playId, userDetails.getUsername());

        ReviewResponseDto response = reviewService.createPlayLogReview(
                userDetails.getUsername(), playId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the review of a specific play log entry.
     *
     * @param userDetails the authenticated user principal
     * @param playId      the play log ID
     * @param request     the review request body containing updated content and spoiler flag
     * @return the updated review
     */
    @PutMapping
    public ResponseEntity<ReviewResponseDto> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID playId,
            @Valid @RequestBody ReviewRequestDto request) {

        log.info("PUT /api/me/plays/{}/review - user: {}", playId, userDetails.getUsername());

        ReviewResponseDto response = reviewService.updatePlayLogReview(
                userDetails.getUsername(), playId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes the review of a specific play log entry.
     *
     * @param userDetails the authenticated user principal
     * @param playId      the play log ID
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID playId) {

        log.info("DELETE /api/me/plays/{}/review - user: {}", playId, userDetails.getUsername());

        reviewService.deletePlayLogReview(userDetails.getUsername(), playId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the review of a specific play log entry.
     *
     * @param userDetails the authenticated user principal
     * @param playId      the play log ID
     * @return the review
     */
    @GetMapping
    public ResponseEntity<ReviewResponseDto> getReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID playId) {

        log.info("GET /api/me/plays/{}/review - user: {}", playId, userDetails.getUsername());

        ReviewResponseDto response = reviewService.getPlayLogReview(
                userDetails.getUsername(), playId);

        return ResponseEntity.ok(response);
    }
}
