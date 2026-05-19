package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Event published when a user is followed by another user.
 * The followed user is awarded XP via the gamification system.
 */
public class UserGainedFollowerEvent {

    private final UUID followedUserId;
    private final UUID followerId;

    public UserGainedFollowerEvent(UUID followedUserId, UUID followerId) {
        this.followedUserId = followedUserId;
        this.followerId = followerId;
    }

    public UUID getFollowedUserId() {
        return followedUserId;
    }

    public UUID getFollowerId() {
        return followerId;
    }
}
