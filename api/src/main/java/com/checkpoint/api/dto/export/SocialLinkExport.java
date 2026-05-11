package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;

public record SocialLinkExport(
        String url,
        LocalDateTime createdAt) {
}
