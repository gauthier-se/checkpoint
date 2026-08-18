package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Event published every time a user records a rating: whether the rating is
 * brand new or an update of an existing one. The distinct {@link GameRatedEvent}
 * only fires on first-time ratings (XP must not be granted on re-rates), so the
 * badge system needs this broader signal to evaluate threshold + change-count
 * triggers like {@code THE_CAKE_IS_A_LIE} and {@code INDECISIVE}.
 */
public class RateRecordedEvent {

    private final UUID userId;
    private final UUID videoGameId;

    public RateRecordedEvent(UUID userId, UUID videoGameId) {
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
