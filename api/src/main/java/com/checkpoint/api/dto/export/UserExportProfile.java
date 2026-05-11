package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserExportProfile(
        UUID id,
        String username,
        String email,
        String bio,
        String picture,
        Boolean isPrivate,
        Integer level,
        Integer xpPoint,
        LocalDateTime createdAt) {
}
