package com.checkpoint.api.dto.catalog;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.enums.PlayStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing a review bundled with minimal game information.
 * Used by cross-game review listings (popular / recent) where the consumer
 * needs to render a card linking back to the reviewed game.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewCardDto(
        UUID id,
        String content,
        Boolean haveSpoilers,
        LocalDateTime createdAt,
        ReviewUserDto user,
        UUID playLogId,
        String platformName,
        PlayStatus playStatus,
        Boolean isReplay,
        long likesCount,
        boolean hasLiked,
        long commentsCount,
        UUID gameId,
        String gameTitle,
        String gameCoverUrl
) {
}
