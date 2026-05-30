package com.checkpoint.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.profile.FavoriteDto;
import com.checkpoint.api.dto.profile.UpdateFavoritesDto;
import com.checkpoint.api.services.FavoriteService;

import jakarta.validation.Valid;

/**
 * REST controller for the authenticated user's favorite games (top 5, ordered).
 */
@Tag(name = "Library and Collection", description = "Current user favorite games")
@RestController
@RequestMapping("/api/me/favorites")
public class FavoriteController {

    private static final Logger log = LoggerFactory.getLogger(FavoriteController.class);

    private final FavoriteService favoriteService;

    /**
     * Constructs a new FavoriteController.
     *
     * @param favoriteService the favorite service
     */
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * Replaces the authenticated user's favorites with the provided ordered list of game IDs.
     * The list index becomes the resulting {@code displayOrder} (0-based).
     *
     * @param userDetails the authenticated user
     * @param request     the new ordered list of game IDs (max 5, unique)
     * @return the resulting favorites in display order
     */
    @PutMapping
    public ResponseEntity<List<FavoriteDto>> replaceFavorites(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateFavoritesDto request) {

        log.info("PUT /api/me/favorites - user: {} (count: {})",
                userDetails.getUsername(), request.gameIds().size());

        List<FavoriteDto> favorites = favoriteService.replaceFavorites(
                userDetails.getUsername(), request.gameIds());
        return ResponseEntity.ok(favorites);
    }
}
