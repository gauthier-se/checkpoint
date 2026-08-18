package com.checkpoint.api.dto.social;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.enums.FeedItemType;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Polymorphic activity feed item. Common fields are always present;
 * type-specific fields are nullable and excluded from JSON when null.
 *
 * @param id            the entity ID (play, rate, review, or list ID)
 * @param type          the activity type discriminator
 * @param createdAt     when the activity occurred
 * @param user          who performed the activity
 * @param game          which game is involved (null for LIST type)
 * @param playStatus    play session status (PLAY type only)
 * @param score         rating score 1-10: half-star steps, display = score / 2 (RATING type only)
 * @param reviewContent truncated review text (REVIEW type only)
 * @param haveSpoilers  whether the review contains spoilers (REVIEW type only)
 * @param listTitle     the list title (LIST type only)
 * @param listGameCount number of games in the list (LIST type only)
 * @param logId         the associated play-log ID, used to link to the log detail page (REVIEW type only, may be null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedItemDto(
        UUID id,
        FeedItemType type,
        LocalDateTime createdAt,
        FeedUserDto user,
        FeedGameDto game,
        String playStatus,
        Integer score,
        String reviewContent,
        Boolean haveSpoilers,
        String listTitle,
        Integer listGameCount,
        UUID logId
) {
}
