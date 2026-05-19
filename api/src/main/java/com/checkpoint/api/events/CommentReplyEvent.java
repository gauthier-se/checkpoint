package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Event published when a user posts a reply to a comment.
 * The replier is awarded XP at most once per parent comment
 * (enforced by the unique constraint on {@code xp_grants}).
 */
public class CommentReplyEvent {

    private final UUID replierId;
    private final UUID parentCommentAuthorId;
    private final UUID parentCommentId;

    public CommentReplyEvent(UUID replierId, UUID parentCommentAuthorId, UUID parentCommentId) {
        this.replierId = replierId;
        this.parentCommentAuthorId = parentCommentAuthorId;
        this.parentCommentId = parentCommentId;
    }

    public UUID getReplierId() {
        return replierId;
    }

    public UUID getParentCommentAuthorId() {
        return parentCommentAuthorId;
    }

    public UUID getParentCommentId() {
        return parentCommentId;
    }
}
