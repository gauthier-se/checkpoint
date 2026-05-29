package com.checkpoint.api.dto.social;

import java.util.UUID;

import com.checkpoint.api.enums.FriendCollectionType;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single friend who has the game in their wishlist or backlog.
 *
 * @param userId         the friend's user ID
 * @param pseudo         the friend's display name
 * @param picture        the friend's profile picture URL (may be null)
 * @param collectionType which collection the game appears in
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FriendWishlistEntryDto(
        UUID userId,
        String pseudo,
        String picture,
        FriendCollectionType collectionType
) {}
