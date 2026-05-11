package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GameListExport(
        UUID id,
        String title,
        String description,
        Boolean isPrivate,
        List<GameListEntryExport> entries,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
