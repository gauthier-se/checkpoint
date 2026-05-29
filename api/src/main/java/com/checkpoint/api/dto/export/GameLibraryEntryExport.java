package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.enums.PlayStatus;

public record GameLibraryEntryExport(
        UUID gameId,
        String gameTitle,
        PlayStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
