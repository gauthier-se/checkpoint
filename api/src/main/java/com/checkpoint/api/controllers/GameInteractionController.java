package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.collection.GameInteractionStatusDto;
import com.checkpoint.api.services.GameInteractionService;

/**
 * REST controller for retrieving aggregated game interactions.
 *
 * <p>All endpoints require authentication (JWT or session).
 * The authenticated user is resolved from the security context.</p>
 */
@Tag(name = "Library and Collection", description = "Add, update and remove games in the current user collection")
@RestController
@RequestMapping("/api/me/games")
public class GameInteractionController {

    private static final Logger log = LoggerFactory.getLogger(GameInteractionController.class);

    private final GameInteractionService gameInteractionService;

    public GameInteractionController(GameInteractionService gameInteractionService) {
        this.gameInteractionService = gameInteractionService;
    }

    /**
     * Retrieves the aggregate interaction status for a specific user and game.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the ID of the video game
     * @return the interaction status DTO
     */
    @GetMapping("/{videoGameId}/status")
    public ResponseEntity<GameInteractionStatusDto> getGameInteractionStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId) {

        log.info("GET /api/me/games/{}/status - user: {}", videoGameId, userDetails.getUsername());

        GameInteractionStatusDto response = gameInteractionService.getGameInteractionStatus(
                userDetails.getUsername(), videoGameId);

        return ResponseEntity.ok(response);
    }
}
