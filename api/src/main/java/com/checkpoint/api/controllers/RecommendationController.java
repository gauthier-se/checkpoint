package com.checkpoint.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.RecommendedGameDto;
import com.checkpoint.api.services.GameRecommendationService;

/**
 * REST controller for personalised game recommendations.
 *
 * <p>Authentication is enforced upstream by the JWT filter — endpoints under
 * {@code /api/me/**} are unreachable for anonymous callers.</p>
 */
@Tag(name = "Gamification", description = "Personalized game recommendations")
@RestController
@RequestMapping("/api/me/games")
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);

    private static final int DEFAULT_SIZE = 10;

    private final GameRecommendationService recommendationService;

    public RecommendationController(GameRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Returns up to {@code size} recommendations for the authenticated user.
     * Falls back to trending games when the user has no liked games yet.
     */
    @GetMapping("/recommended")
    public ResponseEntity<List<RecommendedGameDto>> getRecommended(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {

        log.info("GET /api/me/games/recommended - user: {}, size: {}", userDetails.getUsername(), size);

        List<RecommendedGameDto> recommendations =
                recommendationService.getRecommendationsFor(userDetails.getUsername(), size);
        return ResponseEntity.ok(recommendations);
    }
}
