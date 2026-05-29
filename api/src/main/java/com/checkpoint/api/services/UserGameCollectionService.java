package com.checkpoint.api.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.collection.UserGameRequestDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.enums.PlayStatus;

/**
 * Service for managing a user's game collection (library).
 */
public interface UserGameCollectionService {

    /**
     * Adds a game to the authenticated user's library with the given status.
     *
     * @param userEmail the authenticated user's email
     * @param request   the request containing video game ID and status
     * @return the created user-game entry
     */
    UserGameResponseDto addGameToLibrary(String userEmail, UserGameRequestDto request);

    /**
     * Updates the status of a game already in the user's library.
     *
     * @param userEmail   the authenticated user's email
     * @param videoGameId the video game ID
     * @param request     the request containing the new status
     * @return the updated user-game entry
     */
    UserGameResponseDto updateGameStatus(String userEmail, UUID videoGameId, UserGameRequestDto request);

    /**
     * Returns the authenticated user's game collection (paginated).
     * The response includes each game's {@code userRating} (the authenticated user's own rating).
     * When {@code status} is non-null, only library entries with that status are returned.
     * When the supplied {@link Pageable}'s sort is {@code rating}, results are ordered by the
     * user's rating descending (unrated last).
     *
     * @param userEmail the authenticated user's email
     * @param status    optional status filter (null = all statuses)
     * @param pageable  pagination parameters
     * @return paginated list of games in the user's library
     */
    Page<UserGameResponseDto> getUserLibrary(String userEmail, PlayStatus status, Pageable pageable);

    /**
     * Removes a game from the user's library.
     *
     * @param userEmail   the authenticated user's email
     * @param videoGameId the video game ID to remove
     */
    void removeGameFromLibrary(String userEmail, UUID videoGameId);
}
