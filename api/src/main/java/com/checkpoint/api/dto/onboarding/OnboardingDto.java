package com.checkpoint.api.dto.onboarding;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Snapshot of the current user's onboarding state.
 *
 * @param completedAt {@code null} while onboarding is still in progress, otherwise the timestamp
 *                    at which the user either finished the wizard or dismissed the checklist
 * @param steps       map of step key (see {@link OnboardingSteps}) to {@code true} when done,
 *                    {@code false} when explicitly skipped, or absent when not yet seen
 */
public record OnboardingDto(
        LocalDateTime completedAt,
        Map<String, Boolean> steps
) {}
