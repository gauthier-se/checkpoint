package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Event published when a user rates a game for the first time.
 * The publisher must filter re-rates: this event only fires on the
 * initial rating creation for a (user, game) pair.
 */
public class GameRatedEvent {

    private final UUID userId;
    private final UUID videoGameId;

    public GameRatedEvent(UUID userId, UUID videoGameId) {
        this.userId = userId;
        this.videoGameId = videoGameId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getVideoGameId() {
        return videoGameId;
    }
}
