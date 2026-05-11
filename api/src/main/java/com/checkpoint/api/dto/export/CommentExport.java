package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A comment authored by the user. The polymorphic target is exposed via the
 * {@code reviewId} and {@code gameListId} fields — exactly one is non-null.
 */
public record CommentExport(
        UUID id,
        String content,
        UUID reviewId,
        UUID gameListId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
