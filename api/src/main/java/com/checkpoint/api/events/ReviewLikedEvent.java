package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Event published when a user likes a review.
 * The review's author is awarded XP, subject to a per-author daily cap
 * enforced by the gamification listener.
 */
public class ReviewLikedEvent {

    private final UUID likerId;
    private final UUID reviewAuthorId;
    private final UUID reviewId;
    private final UUID likeId;

    public ReviewLikedEvent(UUID likerId, UUID reviewAuthorId, UUID reviewId, UUID likeId) {
        this.likerId = likerId;
        this.reviewAuthorId = reviewAuthorId;
        this.reviewId = reviewId;
        this.likeId = likeId;
    }

    public UUID getLikerId() {
        return likerId;
    }

    public UUID getReviewAuthorId() {
        return reviewAuthorId;
    }

    public UUID getReviewId() {
        return reviewId;
    }

    public UUID getLikeId() {
        return likeId;
    }
}
