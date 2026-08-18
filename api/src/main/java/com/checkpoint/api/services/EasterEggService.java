package com.checkpoint.api.services;

import java.util.UUID;

/**
 * Service for the small handful of easter-egg badges whose triggers cannot be
 * detected on the backend (Konami code typed on the homepage, a special URL
 * sort param, clicks on the notification bell, etc.). The web client signals
 * the trigger through a dedicated POST endpoint and the server is the one
 * deciding whether to award, so the mapping {endpoint → badge} stays
 * un-forgeable. The payload (if any) is never trusted.
 */
public interface EasterEggService {

    /** Konami code typed on the home page. */
    void recordKonami(UUID userId);

    /** {@code /games?sort=barrel-roll} hit. */
    void recordBarrelRoll(UUID userId);

    /** A Rickroll keyword or URL was entered anywhere in the app. */
    void recordRickroll(UUID userId);

    /**
     * Records a click on the notification bell. The server keeps an in-memory
     * per-user counter for the current process lifetime; the badge fires once
     * 50 clicks accumulate without a server restart in between.
     */
    void recordBellClick(UUID userId);

    /**
     * Records that the user has just opened a review. Reviews authored by the
     * viewer themselves are ignored.
     */
    void recordReviewView(UUID viewerId, UUID reviewId);
}
