package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.EasterEggService;

/**
 * REST controller for the easter-egg badge triggers that have to come from the
 * client (Konami code, barrel-roll URL, Rickroll keyword detection, bell-click
 * spamming, and review-view tracking).
 *
 * <p>The server hard-codes each endpoint → badge mapping. Request bodies are
 * never trusted: hitting one of these endpoints awards a specific badge and
 * only that one. There is no "award me badge X" endpoint by design.</p>
 */
@Tag(name = "Gamification", description = "Easter egg discovery")
@RestController
@RequestMapping("/api/me/easter-eggs")
public class EasterEggController {

    private static final Logger log = LoggerFactory.getLogger(EasterEggController.class);

    private final EasterEggService easterEggService;
    private final UserRepository userRepository;

    public EasterEggController(EasterEggService easterEggService,
                               UserRepository userRepository) {
        this.easterEggService = easterEggService;
        this.userRepository = userRepository;
    }

    @PostMapping("/konami")
    public ResponseEntity<Void> konami(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        log.info("POST /api/me/easter-eggs/konami - user: {}", userDetails.getUsername());
        easterEggService.recordKonami(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/barrel-roll")
    public ResponseEntity<Void> barrelRoll(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        log.info("POST /api/me/easter-eggs/barrel-roll - user: {}", userDetails.getUsername());
        easterEggService.recordBarrelRoll(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rickroll")
    public ResponseEntity<Void> rickroll(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        log.info("POST /api/me/easter-eggs/rickroll - user: {}", userDetails.getUsername());
        easterEggService.recordRickroll(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/navi-clicks")
    public ResponseEntity<Void> naviClicks(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        easterEggService.recordBellClick(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/review-view/{reviewId}")
    public ResponseEntity<Void> reviewView(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable UUID reviewId) {
        UUID userId = resolveUserId(userDetails);
        easterEggService.recordReviewView(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getUsername()))
                .getId();
    }
}
