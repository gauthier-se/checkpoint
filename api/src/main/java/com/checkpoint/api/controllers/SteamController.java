package com.checkpoint.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.steam.LinkSteamRequestDto;
import com.checkpoint.api.dto.steam.SteamAccountDto;
import com.checkpoint.api.services.SteamService;

import jakarta.validation.Valid;

/**
 * REST controller for the authenticated user's Steam account link.
 *
 * <p>This endpoint covers the <em>manual</em> link path where the user types their SteamID64.
 * The OpenID 2.0 flow lives on {@code /api/auth/steam/openid/**} in {@link AuthController}.</p>
 */
@RestController
@RequestMapping("/api/me/steam")
public class SteamController {

    private static final Logger log = LoggerFactory.getLogger(SteamController.class);

    private final SteamService steamService;

    public SteamController(SteamService steamService) {
        this.steamService = steamService;
    }

    /**
     * Links the given SteamID64 to the authenticated user after validating it through the Steam
     * Web API.
     *
     * @param userDetails the authenticated user
     * @param request     the link request containing the SteamID
     * @return the linked Steam account info
     */
    @PostMapping("/link")
    public ResponseEntity<SteamAccountDto> link(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LinkSteamRequestDto request) {

        log.info("POST /api/me/steam/link - user: {}", userDetails.getUsername());
        SteamAccountDto dto = steamService.linkSteamAccount(userDetails.getUsername(), request.steamId());
        return ResponseEntity.ok(dto);
    }

    /**
     * Clears the Steam account link for the authenticated user.
     *
     * @param userDetails the authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/unlink")
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("DELETE /api/me/steam/unlink - user: {}", userDetails.getUsername());
        steamService.unlinkSteamAccount(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
