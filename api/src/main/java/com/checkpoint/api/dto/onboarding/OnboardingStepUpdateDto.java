package com.checkpoint.api.dto.onboarding;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PATCH /api/me/onboarding}.
 *
 * @param step the step key (see {@link OnboardingSteps})
 * @param done {@code true} when the step has been completed,
 *             {@code false} when the user explicitly skipped it
 */
public record OnboardingStepUpdateDto(
        @NotBlank String step,
        boolean done
) {}
