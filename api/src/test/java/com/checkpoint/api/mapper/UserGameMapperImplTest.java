package com.checkpoint.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.mapper.impl.UserGameMapperImpl;

/**
 * Unit tests for {@link UserGameMapperImpl}.
 */
class UserGameMapperImplTest {

    private UserGameMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserGameMapperImpl();
    }

    @Test
    @DisplayName("toResponseDto should map all fields correctly")
    void toResponseDto_shouldMapAllFields() {
        // Given
        User user = new User("testuser", "user@example.com", "password");
        user.setId(UUID.randomUUID());

        VideoGame videoGame = new VideoGame("Elden Ring", "Open-world RPG", LocalDate.of(2022, 2, 25));
        videoGame.setId(UUID.randomUUID());
        videoGame.setCoverUrl("elden-ring-cover.jpg");

        UserGame userGame = new UserGame(user, videoGame, PlayStatus.ARE_PLAYING);
        userGame.setId(UUID.randomUUID());
        userGame.setCreatedAt(LocalDateTime.of(2025, 6, 1, 10, 0));
        userGame.setUpdatedAt(LocalDateTime.of(2025, 6, 15, 14, 30));
        userGame.setNotes("Defeated Margit on third try");

        // When
        UserGameResponseDto result = mapper.toResponseDto(userGame);

        // Then
        assertThat(result.id()).isEqualTo(userGame.getId());
        assertThat(result.videoGameId()).isEqualTo(videoGame.getId());
        assertThat(result.title()).isEqualTo("Elden Ring");
        assertThat(result.coverUrl()).isEqualTo("elden-ring-cover.jpg");
        assertThat(result.releaseDate()).isEqualTo(LocalDate.of(2022, 2, 25));
        assertThat(result.status()).isEqualTo(PlayStatus.ARE_PLAYING);
        assertThat(result.addedAt()).isEqualTo(LocalDateTime.of(2025, 6, 1, 10, 0));
        assertThat(result.updatedAt()).isEqualTo(LocalDateTime.of(2025, 6, 15, 14, 30));
        assertThat(result.notes()).isEqualTo("Defeated Margit on third try");
    }

    @Test
    @DisplayName("toResponseDto should handle null cover URL and release date")
    void toResponseDto_shouldHandleNullFields() {
        // Given
        User user = new User("testuser", "user@example.com", "password");
        user.setId(UUID.randomUUID());

        VideoGame videoGame = new VideoGame("Unknown Game", null, null);
        videoGame.setId(UUID.randomUUID());

        UserGame userGame = new UserGame(user, videoGame, PlayStatus.ARE_PLAYING);
        userGame.setId(UUID.randomUUID());
        userGame.setCreatedAt(LocalDateTime.now());
        userGame.setUpdatedAt(LocalDateTime.now());

        // When
        UserGameResponseDto result = mapper.toResponseDto(userGame);

        // Then
        assertThat(result.title()).isEqualTo("Unknown Game");
        assertThat(result.coverUrl()).isNull();
        assertThat(result.releaseDate()).isNull();
        assertThat(result.status()).isEqualTo(PlayStatus.ARE_PLAYING);
        assertThat(result.notes()).isNull();
    }
}
