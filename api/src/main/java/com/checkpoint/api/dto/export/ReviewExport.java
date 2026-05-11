package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewExport(
        UUID id,
        UUID gameId,
        String gameTitle,
        String content,
        Boolean haveSpoilers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
