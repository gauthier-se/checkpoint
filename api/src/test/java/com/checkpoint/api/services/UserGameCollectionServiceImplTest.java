package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.collection.UserGameRequestDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.exceptions.GameAlreadyInLibraryException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameNotInLibraryException;
import com.checkpoint.api.mapper.UserGameMapper;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.impl.UserGameCollectionServiceImpl;

/**
 * Unit tests for {@link UserGameCollectionServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class UserGameCollectionServiceImplTest {

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private UserGameMapper userGameMapper;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private UserGameCollectionServiceImpl service;

    private User testUser;
    private VideoGame testGame;
    private UserGame testUserGame;
    private UserGameResponseDto testResponseDto;

    @BeforeEach
    void setUp() {
        service = new UserGameCollectionServiceImpl(
                userGameRepository, userRepository, videoGameRepository, userGameMapper, eventPublisher);

        testUser = new User("testuser", "user@example.com", "password");
        testUser.setId(UUID.randomUUID());

        testGame = new VideoGame("The Witcher 3", "Epic RPG", LocalDate.of(2015, 5, 19));
        testGame.setId(UUID.randomUUID());
        testGame.setCoverUrl("cover.jpg");

        testUserGame = new UserGame(testUser, testGame, PlayStatus.ARE_PLAYING);
        testUserGame.setId(UUID.randomUUID());
        testUserGame.setCreatedAt(LocalDateTime.now());
        testUserGame.setUpdatedAt(LocalDateTime.now());

        testResponseDto = new UserGameResponseDto(
                testUserGame.getId(), testGame.getId(), testGame.getTitle(),
                testGame.getCoverUrl(), testGame.getReleaseDate(), PlayStatus.ARE_PLAYING,
                testUserGame.getCreatedAt(), testUserGame.getUpdatedAt(), null, null);
    }

    @Nested
    @DisplayName("addGameToLibrary")
    class AddGameToLibrary {

        @Test
        @DisplayName("should add game to library successfully")
        void shouldAddGameSuccessfully() {
            // Given
            UserGameRequestDto request = new UserGameRequestDto(testGame.getId(), PlayStatus.ARE_PLAYING, null);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(userGameRepository.existsByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(false);
            when(userGameRepository.save(any(UserGame.class))).thenReturn(testUserGame);
            when(userGameMapper.toResponseDto(testUserGame)).thenReturn(testResponseDto);

            // When
            UserGameResponseDto result = service.addGameToLibrary("user@example.com", request);

            // Then
            assertThat(result.videoGameId()).isEqualTo(testGame.getId());
            assertThat(result.title()).isEqualTo("The Witcher 3");
            assertThat(result.status()).isEqualTo(PlayStatus.ARE_PLAYING);

            ArgumentCaptor<UserGame> captor = ArgumentCaptor.forClass(UserGame.class);
            verify(userGameRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(testUser);
            assertThat(captor.getValue().getVideoGame()).isEqualTo(testGame);
            assertThat(captor.getValue().getStatus()).isEqualTo(PlayStatus.ARE_PLAYING);
            assertThat(captor.getValue().getNotes()).isNull();
        }

        @Test
        @DisplayName("should persist notes when provided on add")
        void shouldPersistNotesOnAdd() {
            // Given
            UserGameRequestDto request = new UserGameRequestDto(testGame.getId(), PlayStatus.ARE_PLAYING, "Great combat system");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(userGameRepository.existsByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(false);
            when(userGameRepository.save(any(UserGame.class))).thenReturn(testUserGame);
            when(userGameMapper.toResponseDto(testUserGame)).thenReturn(testResponseDto);

            // When
            service.addGameToLibrary("user@example.com", request);

            // Then
            ArgumentCaptor<UserGame> captor = ArgumentCaptor.forClass(UserGame.class);
            verify(userGameRepository).save(captor.capture());
            assertThat(captor.getValue().getNotes()).isEqualTo("Great combat system");
        }

        @Test
        @DisplayName("should throw GameAlreadyInLibraryException when game already exists")
        void shouldThrowWhenGameAlreadyInLibrary() {
            // Given
            UserGameRequestDto request = new UserGameRequestDto(testGame.getId(), PlayStatus.ARE_PLAYING, null);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(userGameRepository.existsByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.addGameToLibrary("user@example.com", request))
                    .isInstanceOf(GameAlreadyInLibraryException.class)
                    .hasMessageContaining(testGame.getId().toString());

            verify(userGameRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw GameNotFoundException when video game does not exist")
        void shouldThrowWhenVideoGameNotFound() {
            // Given
            UUID unknownGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(unknownGameId, PlayStatus.ARE_PLAYING, null);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(unknownGameId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.addGameToLibrary("user@example.com", request))
                    .isInstanceOf(GameNotFoundException.class)
                    .hasMessageContaining(unknownGameId.toString());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            UserGameRequestDto request = new UserGameRequestDto(testGame.getId(), PlayStatus.ARE_PLAYING, null);
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.addGameToLibrary("unknown@example.com", request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown@example.com");
        }
    }

    @Nested
    @DisplayName("updateGameStatus")
    class UpdateGameStatus {

        @Test
        @DisplayName("should update game status successfully")
        void shouldUpdateStatusSuccessfully() {
            // Given
            UserGameRequestDto request = new UserGameRequestDto(testGame.getId(), PlayStatus.COMPLETED, null);
            UserGame updatedUserGame = new UserGame(testUser, testGame, PlayStatus.COMPLETED);
            updatedUserGame.setId(testUserGame.getId());
            updatedUserGame.setCreatedAt(testUserGame.getCreatedAt());
            updatedUserGame.setUpdatedAt(LocalDateTime.now());

            UserGameResponseDto updatedResponse = new UserGameResponseDto(
                    testUserGame.getId(), testGame.getId(), testGame.getTitle(),
                    testGame.getCoverUrl(), testGame.getReleaseDate(), PlayStatus.COMPLETED,
                    testUserGame.getCreatedAt(), updatedUserGame.getUpdatedAt(), null, null);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.of(testUserGame));
            when(userGameRepository.save(testUserGame)).thenReturn(updatedUserGame);
            when(userGameMapper.toResponseDto(updatedUserGame)).thenReturn(updatedResponse);

            // When
            UserGameResponseDto result = service.updateGameStatus("user@example.com", testGame.getId(), request);

            // Then
            assertThat(result.status()).isEqualTo(PlayStatus.COMPLETED);
            verify(userGameRepository).save(testUserGame);
        }

        @Test
        @DisplayName("should update notes on update")
        void shouldUpdateNotes() {
            // Given
            UserGameRequestDto request = new UserGameRequestDto(testGame.getId(), PlayStatus.ARE_PLAYING, "New note");
            testUserGame.setNotes("Old note");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.of(testUserGame));
            when(userGameRepository.save(testUserGame)).thenReturn(testUserGame);
            when(userGameMapper.toResponseDto(testUserGame)).thenReturn(testResponseDto);

            // When
            service.updateGameStatus("user@example.com", testGame.getId(), request);

            // Then
            assertThat(testUserGame.getNotes()).isEqualTo("New note");
        }

        @Test
        @DisplayName("should clear notes when request notes is null")
        void shouldClearNotesWhenNull() {
            // Given
            UserGameRequestDto request = new UserGameRequestDto(testGame.getId(), PlayStatus.COMPLETED, null);
            testUserGame.setNotes("Existing note to be cleared");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.of(testUserGame));
            when(userGameRepository.save(testUserGame)).thenReturn(testUserGame);
            when(userGameMapper.toResponseDto(testUserGame)).thenReturn(testResponseDto);

            // When
            service.updateGameStatus("user@example.com", testGame.getId(), request);

            // Then
            assertThat(testUserGame.getNotes()).isNull();
        }

        @Test
        @DisplayName("should throw GameNotInLibraryException when game not in library")
        void shouldThrowWhenGameNotInLibrary() {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, PlayStatus.ABANDONED, null);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.findByUserIdAndVideoGameId(testUser.getId(), videoGameId))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.updateGameStatus("user@example.com", videoGameId, request))
                    .isInstanceOf(GameNotInLibraryException.class)
                    .hasMessageContaining(videoGameId.toString());
        }
    }

    @Nested
    @DisplayName("getUserLibrary")
    class GetUserLibrary {

        @Test
        @DisplayName("should return paginated user library with rating populated")
        void shouldReturnPaginatedLibrary() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            UserGameResponseDto dtoWithRating = new UserGameResponseDto(
                    testUserGame.getId(), testGame.getId(), testGame.getTitle(),
                    testGame.getCoverUrl(), testGame.getReleaseDate(), PlayStatus.ARE_PLAYING,
                    testUserGame.getCreatedAt(), testUserGame.getUpdatedAt(), null, 4.0);
            Page<Object[]> projection = new PageImpl<>(
                    List.<Object[]>of(new Object[] { testUserGame, 8 }), pageable, 1);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.findLibraryProjection(testUser.getId(), null, pageable))
                    .thenReturn(projection);
            when(userGameMapper.toResponseDto(testUserGame, 8)).thenReturn(dtoWithRating);

            // When
            Page<UserGameResponseDto> result = service.getUserLibrary("user@example.com", null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("The Witcher 3");
            assertThat(result.getContent().get(0).userRating()).isEqualTo(4.0);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should pass status filter to the repository")
        void shouldFilterByStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Object[]> projection = new PageImpl<>(
                    List.<Object[]>of(new Object[] { testUserGame, null }), pageable, 1);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.findLibraryProjection(testUser.getId(), PlayStatus.COMPLETED, pageable))
                    .thenReturn(projection);
            when(userGameMapper.toResponseDto(testUserGame, null)).thenReturn(testResponseDto);

            // When
            Page<UserGameResponseDto> result = service.getUserLibrary("user@example.com", PlayStatus.COMPLETED, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(userGameRepository).findLibraryProjection(testUser.getId(), PlayStatus.COMPLETED, pageable);
        }

        @Test
        @DisplayName("should route to rating-sorted query when sort is rating")
        void shouldRouteToRatingSortedQuery() {
            // Given
            Pageable pageable = PageRequest.of(0, 20,
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "rating"));
            Page<Object[]> projection = new PageImpl<>(
                    List.<Object[]>of(new Object[] { testUserGame, 9 }), PageRequest.of(0, 20), 1);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.findLibraryProjectionSortedByRating(any(), any(), any()))
                    .thenReturn(projection);
            when(userGameMapper.toResponseDto(testUserGame, 9)).thenReturn(testResponseDto);

            // When
            service.getUserLibrary("user@example.com", null, pageable);

            // Then
            verify(userGameRepository).findLibraryProjectionSortedByRating(any(), any(), any());
            verify(userGameRepository, never()).findLibraryProjection(any(), any(), any());
        }

        @Test
        @DisplayName("should return empty page when library is empty")
        void shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Object[]> emptyPage = new PageImpl<>(List.<Object[]>of(), pageable, 0);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.findLibraryProjection(testUser.getId(), null, pageable))
                    .thenReturn(emptyPage);

            // When
            Page<UserGameResponseDto> result = service.getUserLibrary("user@example.com", null, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("removeGameFromLibrary")
    class RemoveGameFromLibrary {

        @Test
        @DisplayName("should remove game from library successfully")
        void shouldRemoveGameSuccessfully() {
            // Given
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.existsByUserIdAndVideoGameId(testUser.getId(), testGame.getId()))
                    .thenReturn(true);

            // When
            service.removeGameFromLibrary("user@example.com", testGame.getId());

            // Then
            verify(userGameRepository).deleteByUserIdAndVideoGameId(testUser.getId(), testGame.getId());
        }

        @Test
        @DisplayName("should throw GameNotInLibraryException when game not in library")
        void shouldThrowWhenGameNotInLibrary() {
            // Given
            UUID videoGameId = UUID.randomUUID();
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(userGameRepository.existsByUserIdAndVideoGameId(testUser.getId(), videoGameId))
                    .thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> service.removeGameFromLibrary("user@example.com", videoGameId))
                    .isInstanceOf(GameNotInLibraryException.class)
                    .hasMessageContaining(videoGameId.toString());

            verify(userGameRepository, never()).deleteByUserIdAndVideoGameId(any(), any());
        }
    }
}
