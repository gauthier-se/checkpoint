package com.checkpoint.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.checkpoint.api.dto.igdb.IgdbCoverDto;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbGenreDto;
import com.checkpoint.api.entities.VideoGame;

/**
 * Unit tests for {@link GameMapperImpl}.
 */
class GameMapperImplTest {

    private GameMapperImpl gameMapper;

    @BeforeEach
    void setUp() {
        gameMapper = new GameMapperImpl();
    }

    @Test
    @DisplayName("toEntity should map basic fields correctly")
    void toEntity_shouldMapBasicFields() {
        // Given
        IgdbCoverDto cover = new IgdbCoverDto(
                1L, "co1wyy", "//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg",
                264, 374, false, false
        );

        IgdbGameDto dto = new IgdbGameDto(
                1942L,
                "The Witcher 3: Wild Hunt",
                "the-witcher-3-wild-hunt",
                "An RPG game",
                "Geralt's story",
                1431993600L, // May 19, 2015
                92.5, 1250, 93.8, 85, 93.15, 1335,
                cover,
                List.of(new IgdbGenreDto(12L, "RPG", "rpg", "url")),
                null, null, null, null, null, null, null,
                null, null, null, null, "https://igdb.com/game"
        );

        // When
        VideoGame entity = gameMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getIgdbId()).isEqualTo(1942L);
        assertThat(entity.getTitle()).isEqualTo("The Witcher 3: Wild Hunt");
        assertThat(entity.getDescription()).isEqualTo("An RPG game");
        assertThat(entity.getReleaseDate()).isEqualTo(LocalDate.of(2015, 5, 19));
        assertThat(entity.getCoverUrl()).isEqualTo("https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.jpg");
    }

    @Test
    @DisplayName("toEntity should use storyline when summary is null")
    void toEntity_shouldUseStorylineWhenSummaryIsNull() {
        // Given
        IgdbGameDto dto = new IgdbGameDto(
                1L, "Test Game", "test-game",
                null, // summary is null
                "This is the storyline",
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null
        );

        // When
        VideoGame entity = gameMapper.toEntity(dto);

        // Then
        assertThat(entity.getDescription()).isEqualTo("This is the storyline");
    }

    @Test
    @DisplayName("toEntity should handle null release date")
    void toEntity_shouldHandleNullReleaseDate() {
        // Given
        IgdbGameDto dto = new IgdbGameDto(
                1L, "Test Game", "test-game",
                "Summary", null,
                null, // no release date
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null
        );

        // When
        VideoGame entity = gameMapper.toEntity(dto);

        // Then
        assertThat(entity.getReleaseDate()).isNull();
    }

    @Test
    @DisplayName("toEntity should return null for null input")
    void toEntity_shouldReturnNullForNullInput() {
        // When
        VideoGame entity = gameMapper.toEntity(null);

        // Then
        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("updateEntity should update existing entity")
    void updateEntity_shouldUpdateExistingEntity() {
        // Given
        VideoGame existingEntity = new VideoGame();
        existingEntity.setTitle("Old Title");
        existingEntity.setDescription("Old Description");

        IgdbGameDto dto = new IgdbGameDto(
                123L, "New Title", "new-title",
                "New Description", null,
                1609459200L, // Jan 1, 2021
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null
        );

        // When
        gameMapper.updateEntity(dto, existingEntity);

        // Then
        assertThat(existingEntity.getIgdbId()).isEqualTo(123L);
        assertThat(existingEntity.getTitle()).isEqualTo("New Title");
        assertThat(existingEntity.getDescription()).isEqualTo("New Description");
        assertThat(existingEntity.getReleaseDate()).isEqualTo(LocalDate.of(2021, 1, 1));
    }

    @Test
    @DisplayName("updateEntity should handle null dto gracefully")
    void updateEntity_shouldHandleNullDtoGracefully() {
        // Given
        VideoGame existingEntity = new VideoGame();
        existingEntity.setTitle("Original Title");

        // When
        gameMapper.updateEntity(null, existingEntity);

        // Then - entity unchanged
        assertThat(existingEntity.getTitle()).isEqualTo("Original Title");
    }

    @Test
    @DisplayName("updateEntity should handle null entity gracefully")
    void updateEntity_shouldHandleNullEntityGracefully() {
        // Given
        IgdbGameDto dto = new IgdbGameDto(
                1L, "Test", "test", null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null
        );

        // When / Then - should not throw
        gameMapper.updateEntity(dto, null);
    }
}
