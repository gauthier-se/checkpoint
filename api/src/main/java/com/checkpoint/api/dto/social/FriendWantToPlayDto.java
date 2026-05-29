package com.checkpoint.api.dto.social;

import java.util.List;

/**
 * Aggregated wishlist/backlog signals for the game-detail "Want to Play" panel.
 *
 * @param totalCount    total number of distinct friends represented
 * @param wishlistCount number of friends with the game in their wishlist
 * @param backlogCount  number of friends with the game in their backlog
 * @param friends       ordered list of friend entries
 */
public record FriendWantToPlayDto(
        int totalCount,
        long wishlistCount,
        long backlogCount,
        List<FriendWishlistEntryDto> friends
) {}
