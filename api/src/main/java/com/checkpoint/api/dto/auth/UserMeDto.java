package com.checkpoint.api.dto.auth;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for the /api/auth/me endpoint response.
 * Returns basic profile information of the currently authenticated user.
 *
 * @param id                     the user's UUID
 * @param username               the user's pseudo/username
 * @param email                  the user's email address
 * @param role                   the user's role name (e.g., "ADMIN", "USER")
 * @param bio                    the user's biography
 * @param picture                the user's profile picture URL
 * @param isPrivate              whether the user's profile is private
 * @param twoFactorEnabled       whether the user has 2FA enabled
 * @param steamId                the linked SteamID64, or {@code null} if no Steam account is linked
 * @param steamDisplayName       the cached Steam display name (persona name), or {@code null} if Steam
 *                               was unreachable when the link was made and the scheduled refresh has
 *                               not yet succeeded
 * @param steamAvatarUrl         the cached Steam medium avatar URL, or {@code null} under the same
 *                               conditions as {@code steamDisplayName}
 * @param onboardingCompletedAt  {@code null} while onboarding is still in progress, otherwise the
 *                               timestamp at which the user completed or dismissed it
 * @param onboardingSteps        map of step key to done/skipped, see
 *                               {@link com.checkpoint.api.dto.onboarding.OnboardingSteps}
 */
public record UserMeDto(
        UUID id,
        String username,
        String email,
        String role,
        String bio,
        String picture,
        Boolean isPrivate,
        Boolean twoFactorEnabled,
        String steamId,
        String steamDisplayName,
        String steamAvatarUrl,
        LocalDateTime onboardingCompletedAt,
        Map<String, Boolean> onboardingSteps
) {}
