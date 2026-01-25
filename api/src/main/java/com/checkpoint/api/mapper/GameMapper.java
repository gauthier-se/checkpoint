package com.checkpoint.api.mapper;

import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.entities.VideoGame;

/**
 * Interface for mapping between IGDB DTOs and local entities.
 */
public interface GameMapper {

    /**
     * Converts an IGDB game DTO to a new VideoGame entity.
     * Does not persist the entity or resolve relationships.
     *
     * @param dto the IGDB game DTO
     * @return a new VideoGame entity with basic fields populated
     */
    VideoGame toEntity(IgdbGameDto dto);

    /**
     * Updates an existing VideoGame entity with data from an IGDB game DTO.
     * Does not modify relationships (genres, platforms, companies).
     *
     * @param dto the IGDB game DTO
     * @param entity the existing VideoGame entity to update
     */
    void updateEntity(IgdbGameDto dto, VideoGame entity);
}
