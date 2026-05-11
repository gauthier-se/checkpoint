package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.enums.GameStatus;

public record GameLibraryEntryExport(
        UUID gameId,
        String gameTitle,
        GameStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
