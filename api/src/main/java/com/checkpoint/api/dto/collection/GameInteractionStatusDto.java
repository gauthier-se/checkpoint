package com.checkpoint.api.dto.collection;

import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.enums.Priority;

/**
 * DTO representing a user's aggregate interaction state with a specific game.
 *
 * @param inWishlist        whether the game is in the user's wishlist
 * @param wishlistPriority  the priority assigned in the wishlist, or null
 * @param inBacklog         whether the game is in the user's backlog
 * @param backlogPriority   the priority assigned in the backlog, or null
 * @param inLibrary         whether the game is in the user's library
 * @param libraryStatus     the game's status in the library
 * @param libraryNotes      the private notes attached to the library entry, or null
 * @param playCount         the number of play sessions
 * @param userRating        the user's global rating from the Rate entity
 * @param hasReview         whether the user has reviewed the game
 * @param lastPlayRating    the score from the most recent scored play log, or null
 * @param liked             whether the user likes (loves) the game — distinct from the wishlist
 */
public record GameInteractionStatusDto(
        boolean inWishlist,
        Priority wishlistPriority,
        boolean inBacklog,
        Priority backlogPriority,
        boolean inLibrary,
        GameStatus libraryStatus,
        String libraryNotes,
        int playCount,
        Integer userRating,
        boolean hasReview,
        Integer lastPlayRating,
        boolean liked
) {
}
