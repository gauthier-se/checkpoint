package com.checkpoint.api.services;

import java.util.UUID;

import com.checkpoint.api.dto.social.FriendGameActivityDto;
import com.checkpoint.api.dto.social.FriendWantToPlayDto;

/**
 * Service exposing aggregated social signals (friend activity and friend
 * wishlist/backlog) for a specific game, scoped to the viewer's followings.
 */
public interface GameSocialService {

    /**
     * Returns the friend-activity payload for a game: per-status counts and an
     * ordered list of friend entries (most recent play first). When no viewer is
     * authenticated or they follow no one engaged with the game, returns an
     * empty payload.
     *
     * @param videoGameId the video game ID
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @return the friend-activity DTO (never null)
     */
    FriendGameActivityDto getFriendsActivity(UUID videoGameId, String viewerEmail);

    /**
     * Returns the friend "want to play" payload for a game: wishlist/backlog
     * counts and an ordered list of friend entries. When no viewer is
     * authenticated or they follow no one with the game in either collection,
     * returns an empty payload.
     *
     * @param videoGameId the video game ID
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @return the want-to-play DTO (never null)
     */
    FriendWantToPlayDto getFriendsWantToPlay(UUID videoGameId, String viewerEmail);
}
