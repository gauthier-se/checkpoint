package com.checkpoint.api.services;

import com.checkpoint.api.dto.onboarding.OnboardingDto;

/**
 * Service for managing a user's onboarding state — which of the first-login wizard steps they
 * have completed, skipped, or are still pending, and whether they have finished the flow.
 *
 * <p>Other services call {@link #markStepDone(String, String)} after a step's underlying action
 * succeeds (e.g. a profile picture upload), so the wizard / checklist re-render with the step
 * already ticked off without the client having to issue a separate PATCH.</p>
 */
public interface OnboardingService {

    /**
     * Returns the current user's onboarding snapshot.
     *
     * @param userEmail the authenticated user's email
     * @return the onboarding DTO ({@code completedAt} is non-null once the flow has been
     *         completed or dismissed)
     */
    OnboardingDto getOnboarding(String userEmail);

    /**
     * Marks the given step as done. Idempotent — no-op if the step is already {@code true}.
     * Silently does nothing for unknown step keys so this can be called liberally from
     * existing services without coupling them to validation logic.
     *
     * @param userEmail the user's email
     * @param step      one of {@link com.checkpoint.api.dto.onboarding.OnboardingSteps}
     */
    void markStepDone(String userEmail, String step);

    /**
     * Explicitly records a step's state. Used by {@code PATCH /api/me/onboarding} so the
     * client can record a "Skip" (done=false) as distinct from "not yet seen" (no entry).
     *
     * @param userEmail the user's email
     * @param step      a valid step key
     * @param done      {@code true} when completed, {@code false} when explicitly skipped
     * @return the updated snapshot
     */
    OnboardingDto updateStep(String userEmail, String step, boolean done);

    /**
     * Sets {@code onboardingCompletedAt} on the user, hiding the wizard and checklist for good.
     *
     * @param userEmail the user's email
     * @return the updated snapshot
     */
    OnboardingDto complete(String userEmail);
}
