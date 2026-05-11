package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

public record RatingExport(
        UUID gameId,
        String gameTitle,
        Integer score,
        LocalDateTime createdAt) {
}
