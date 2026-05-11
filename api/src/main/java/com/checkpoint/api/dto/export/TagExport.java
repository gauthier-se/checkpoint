package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

public record TagExport(
        UUID id,
        String name,
        LocalDateTime createdAt) {
}
