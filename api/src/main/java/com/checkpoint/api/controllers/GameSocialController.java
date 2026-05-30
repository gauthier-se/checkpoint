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

import com.checkpoint.api.dto.social.FriendGameActivityDto;
import com.checkpoint.api.dto.social.FriendWantToPlayDto;
import com.checkpoint.api.services.GameSocialService;

/**
 * REST controller exposing per-game social signals scoped to the viewer's
 * follow graph: friend activity and friend wishlist/backlog.
 *
 * <p>Both endpoints respond {@code 200} with an empty payload for anonymous or
 * follow-less viewers; the frontend hides the corresponding section.</p>
 */
@Tag(name = "Games", description = "Reviews and comments on a game")
@RestController
@RequestMapping("/api/games/{gameId}")
public class GameSocialController {

    private static final Logger log = LoggerFactory.getLogger(GameSocialController.class);

    private final GameSocialService gameSocialService;

    public GameSocialController(GameSocialService gameSocialService) {
        this.gameSocialService = gameSocialService;
    }

    /**
     * Returns the friend-activity payload for a game.
     *
     * @param gameId      the video game ID
     * @param userDetails the authenticated viewer, or null if anonymous
     * @return the aggregated friend-activity DTO
     */
    @GetMapping("/friends-activity")
    public ResponseEntity<FriendGameActivityDto> getFriendsActivity(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        log.info("GET /api/games/{}/friends-activity - viewer: {}",
                gameId, viewerEmail != null ? viewerEmail : "anonymous");

        return ResponseEntity.ok(gameSocialService.getFriendsActivity(gameId, viewerEmail));
    }

    /**
     * Returns the friend "want to play" payload (wishlist + backlog) for a game.
     *
     * @param gameId      the video game ID
     * @param userDetails the authenticated viewer, or null if anonymous
     * @return the aggregated want-to-play DTO
     */
    @GetMapping("/friends-want-to-play")
    public ResponseEntity<FriendWantToPlayDto> getFriendsWantToPlay(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        log.info("GET /api/games/{}/friends-want-to-play - viewer: {}",
                gameId, viewerEmail != null ? viewerEmail : "anonymous");

        return ResponseEntity.ok(gameSocialService.getFriendsWantToPlay(gameId, viewerEmail));
    }
}
