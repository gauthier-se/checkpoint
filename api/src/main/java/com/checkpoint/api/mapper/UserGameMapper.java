package com.checkpoint.api.mapper;

import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.entities.UserGame;

/**
 * Mapper for converting {@link UserGame} entities to DTOs.
 */
public interface UserGameMapper {

    /**
     * Converts a UserGame entity to a response DTO without a rating.
     *
     * @param userGame the entity to convert
     * @return the response DTO with a null userRating
     */
    UserGameResponseDto toResponseDto(UserGame userGame);

    /**
     * Converts a UserGame entity to a response DTO, populating the user's rating
     * from the raw Rate.score (1–10 integer scale). The score is converted to
     * the half-star display scale (0.5–5.0). A null rawRateScore yields a null
     * userRating in the response.
     *
     * @param userGame      the entity to convert
     * @param rawRateScore  the raw Rate.score (1–10) or null if the user hasn't rated the game
     * @return the response DTO with userRating populated
     */
    UserGameResponseDto toResponseDto(UserGame userGame, Integer rawRateScore);
}
