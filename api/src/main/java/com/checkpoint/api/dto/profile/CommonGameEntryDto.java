package com.checkpoint.api.dto.profile;

import java.time.LocalDate;
import java.util.UUID;

import com.checkpoint.api.enums.GameStatus;

/**
 * DTO representing a single game that two users have in common, with each user's
 * library status and rating for the profile comparison view.
 *
 * <p>Ratings are exposed on the 5-star display scale ({@code score / 2}, range 0.5–5.0),
 * not the raw 1–10 storage scale. {@code ratingDiff} is the absolute difference between
 * the two display-scale ratings and is {@code null} when either user has not rated the game.</p>
 *
 * @param videoGameId  the video game ID
 * @param title        the game title
 * @param coverUrl     the game cover image URL (may be null)
 * @param releaseDate  the game release date (may be null)
 * @param viewerStatus the viewer's library status for this game
 * @param targetStatus the compared user's library status for this game
 * @param viewerRating the viewer's rating on the 5-star scale, or null if unrated
 * @param targetRating the compared user's rating on the 5-star scale, or null if unrated
 * @param ratingDiff   the absolute difference between both ratings, or null if either is unrated
 */
public record CommonGameEntryDto(
        UUID videoGameId,
        String title,
        String coverUrl,
        LocalDate releaseDate,
        GameStatus viewerStatus,
        GameStatus targetStatus,
        Double viewerRating,
        Double targetRating,
        Double ratingDiff
) {}
