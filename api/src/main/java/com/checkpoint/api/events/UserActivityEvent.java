package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Generic event signalling that a user performed a "meaningful activity"
 * (review create, play log, rating, list edit, ...). Consumed by the
 * login-streak system to update the user's consecutive-day counter.
 */
public class UserActivityEvent {

    private final UUID userId;

    public UserActivityEvent(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
