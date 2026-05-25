package com.checkpoint.api.dto.onboarding;

import java.util.List;

/**
 * String constants for the keys stored in the {@code onboarding_steps} JSONB column on
 * {@code users}. Centralised here so the controller, service and frontend stay in sync.
 */
public final class OnboardingSteps {

    public static final String WELCOME = "welcome";
    public static final String PICTURE = "picture";
    public static final String BIO = "bio";
    public static final String STEAM = "steam";
    public static final String TWOFA = "twofa";
    public static final String NOTIFICATIONS = "notifications";
    public static final String FAVORITES = "favorites";
    public static final String FOLLOW = "follow";

    /** All valid step keys, in display order. */
    public static final List<String> ALL = List.of(
            WELCOME, PICTURE, BIO, STEAM, TWOFA, NOTIFICATIONS, FAVORITES, FOLLOW);

    private OnboardingSteps() {}

    public static boolean isValid(String step) {
        return step != null && ALL.contains(step);
    }
}
