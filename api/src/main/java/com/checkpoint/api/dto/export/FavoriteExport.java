package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

public record FavoriteExport(
        UUID gameId,
        String gameTitle,
        Integer displayOrder,
        LocalDateTime createdAt) {
}
