package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Event published when a user follows another user.
 * The follower is awarded XP via the gamification system.
 */
public class UserFollowedEvent {

    private final UUID followerId;
    private final UUID followedUserId;

    public UserFollowedEvent(UUID followerId, UUID followedUserId) {
        this.followerId = followerId;
        this.followedUserId = followedUserId;
    }

    public UUID getFollowerId() {
        return followerId;
    }

    public UUID getFollowedUserId() {
        return followedUserId;
    }
}
