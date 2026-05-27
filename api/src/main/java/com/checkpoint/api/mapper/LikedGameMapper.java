package com.checkpoint.api.mapper;

import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.entities.Like;

/**
 * Mapper for converting top-level game {@link Like} entities to DTOs.
 */
public interface LikedGameMapper {

    /**
     * Converts a game Like entity to a response DTO.
     *
     * @param like the entity to convert (must reference a video game)
     * @return the response DTO
     */
    LikedGameResponseDto toResponseDto(Like like);
}
