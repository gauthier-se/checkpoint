package com.checkpoint.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.onboarding.OnboardingDto;
import com.checkpoint.api.dto.onboarding.OnboardingStepUpdateDto;
import com.checkpoint.api.services.OnboardingService;

import jakarta.validation.Valid;

/**
 * REST endpoints driving the first-login onboarding wizard and persistent checklist.
 *
 * <p>The state is also embedded in {@code GET /api/auth/me} so the frontend doesn't usually
 * need to hit {@code GET /api/me/onboarding} explicitly. The PATCH and complete endpoints
 * cover explicit user actions (Skip, Dismiss, Finish).</p>
 */
@Tag(name = "Account and Profile", description = "New user onboarding flow")
@RestController
@RequestMapping("/api/me/onboarding")
public class OnboardingController {

    private static final Logger log = LoggerFactory.getLogger(OnboardingController.class);

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public ResponseEntity<OnboardingDto> getOnboarding(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/me/onboarding - user: {}", userDetails.getUsername());
        return ResponseEntity.ok(onboardingService.getOnboarding(userDetails.getUsername()));
    }

    @PatchMapping
    public ResponseEntity<OnboardingDto> updateStep(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OnboardingStepUpdateDto request) {
        log.info("PATCH /api/me/onboarding - user: {}, step: {}, done: {}",
                userDetails.getUsername(), request.step(), request.done());
        return ResponseEntity.ok(
                onboardingService.updateStep(userDetails.getUsername(), request.step(), request.done()));
    }

    @PostMapping("/complete")
    public ResponseEntity<OnboardingDto> complete(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /api/me/onboarding/complete - user: {}", userDetails.getUsername());
        return ResponseEntity.ok(onboardingService.complete(userDetails.getUsername()));
    }
}
