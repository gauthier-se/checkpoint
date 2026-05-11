package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.enums.Priority;

public record WishlistEntryExport(
        UUID gameId,
        String gameTitle,
        Priority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
