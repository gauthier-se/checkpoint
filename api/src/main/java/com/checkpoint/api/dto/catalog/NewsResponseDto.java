package com.checkpoint.api.dto.catalog;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.entities.NewsSource;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for a news article.
 *
 * @param id          the news ID
 * @param title       the news title
 * @param description the news content
 * @param picture     the cover image URL
 * @param publishedAt the publication timestamp (null if draft)
 * @param createdAt   the creation timestamp
 * @param updatedAt   the last update timestamp
 * @param author      the news author (null for imported news)
 * @param source      the news origin (MANUAL / STEAM / RSS)
 * @param externalUrl link to the original article for imported news
 * @param feedName    human-readable feed name for imported news
 * @param videoGameId linked game ID for STEAM per-game news
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NewsResponseDto(
        UUID id,
        String title,
        String description,
        String picture,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        NewsAuthorDto author,
        NewsSource source,
        String externalUrl,
        String feedName,
        UUID videoGameId
) {}
