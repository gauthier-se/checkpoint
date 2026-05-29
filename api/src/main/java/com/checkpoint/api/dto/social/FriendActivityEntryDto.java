package com.checkpoint.api.dto.social;

import java.util.UUID;

import com.checkpoint.api.enums.PlayStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single friend's engagement with a game on the friend-activity panel.
 *
 * @param userId            the friend's user ID
 * @param pseudo            the friend's display name
 * @param picture           the friend's profile picture URL (may be null)
 * @param primaryPlayStatus the status of the friend's most recent play, or null if they haven't played
 * @param rating            the friend's rating displayed as a 1–5 (half-step) value, or null if unrated
 * @param hasReview         whether any of the friend's plays for the game has an attached review
 * @param latestPlayId      the ID of the friend's most recent play for the game, or null if none
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FriendActivityEntryDto(
        UUID userId,
        String pseudo,
        String picture,
        PlayStatus primaryPlayStatus,
        Double rating,
        boolean hasReview,
        UUID latestPlayId
) {}
