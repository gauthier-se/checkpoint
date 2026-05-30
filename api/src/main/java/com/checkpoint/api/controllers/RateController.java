package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.RateRequestDto;
import com.checkpoint.api.dto.catalog.RateResponseDto;
import com.checkpoint.api.services.RateService;

import jakarta.validation.Valid;

/**
 * REST controller for standalone game rating endpoints.
 *
 * <p>All endpoints require authentication (JWT or session).
 * The authenticated user is resolved from the security context.</p>
 */
@Tag(name = "Library and Collection", description = "Rate games")
@RestController
@RequestMapping("/api/me/games/{videoGameId}/rate")
public class RateController {

    private static final Logger log = LoggerFactory.getLogger(RateController.class);

    private final RateService rateService;

    public RateController(RateService rateService) {
        this.rateService = rateService;
    }

    /**
     * Creates or updates the authenticated user's rating for a specific game.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID
     * @param request the rate request body containing the score
     * @return the created or updated rating
     */
    @PutMapping
    public ResponseEntity<RateResponseDto> rateGame(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId,
            @Valid @RequestBody RateRequestDto request) {

        log.info("PUT /api/me/games/{}/rate - user: {}, score: {}", videoGameId, userDetails.getUsername(), request.score());

        RateResponseDto response = rateService.rateGame(userDetails.getUsername(), videoGameId, request.score());
        return ResponseEntity.ok(response);
    }

    /**
     * Removes the authenticated user's rating for a specific game.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<Void> removeRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId) {

        log.info("DELETE /api/me/games/{}/rate - user: {}", videoGameId, userDetails.getUsername());

        rateService.removeRating(userDetails.getUsername(), videoGameId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the authenticated user's rating for a specific game.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID
     * @return the rating if found, or 404 Not Found if no rating exists
     */
    @GetMapping
    public ResponseEntity<RateResponseDto> getMyRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId) {

        log.info("GET /api/me/games/{}/rate - user: {}", videoGameId, userDetails.getUsername());

        RateResponseDto response = rateService.getUserRating(userDetails.getUsername(), videoGameId);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }
}
