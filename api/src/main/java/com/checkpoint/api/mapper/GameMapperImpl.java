package com.checkpoint.api.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.entities.VideoGame;

/**
 * Implementation of {@link GameMapper} for mapping IGDB DTOs to VideoGame entities.
 */
@Component
public class GameMapperImpl implements GameMapper {

    @Override
    public VideoGame toEntity(IgdbGameDto dto) {
        if (dto == null) {
            return null;
        }

        VideoGame entity = new VideoGame();
        mapBasicFields(dto, entity);
        return entity;
    }

    @Override
    public void updateEntity(IgdbGameDto dto, VideoGame entity) {
        if (dto == null || entity == null) {
            return;
        }

        mapBasicFields(dto, entity);
    }

    /**
     * Maps basic fields from DTO to entity.
     *
     * @param dto the source DTO
     * @param entity the target entity
     */
    private void mapBasicFields(IgdbGameDto dto, VideoGame entity) {
        entity.setIgdbId(dto.id());
        entity.setTitle(dto.name());

        // Use summary as description, fallback to storyline if summary is null
        String description = dto.summary();
        if (description == null || description.isBlank()) {
            description = dto.storyline();
        }
        entity.setDescription(description);

        // Convert Unix timestamp to LocalDate
        entity.setReleaseDate(convertUnixTimestampToLocalDate(dto.firstReleaseDate()));

        // Set cover URL using the cover_big size
        if (dto.cover() != null) {
            entity.setCoverUrl(dto.cover().getCoverBigUrl());
        }
    }

    /**
     * Converts a Unix timestamp (seconds since epoch) to LocalDate.
     *
     * @param timestamp the Unix timestamp
     * @return LocalDate or null if timestamp is null
     */
    private LocalDate convertUnixTimestampToLocalDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
