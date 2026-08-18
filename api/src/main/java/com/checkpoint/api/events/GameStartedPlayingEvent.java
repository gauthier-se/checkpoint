package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Event published when a user marks a game as PLAYING: either by adding a new
 * library entry with that status, or by transitioning an existing entry to
 * PLAYING. Powers the {@code LEEROY} easter-egg badge (jumping into a new game
 * while the backlog is huge).
 */
public class GameStartedPlayingEvent {

    private final UUID userId;
    private final UUID videoGameId;

    public GameStartedPlayingEvent(UUID userId, UUID videoGameId) {
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
