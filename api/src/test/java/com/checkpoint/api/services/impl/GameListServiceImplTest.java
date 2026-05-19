package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.list.AddGameToListRequestDto;
import com.checkpoint.api.dto.list.CreateGameListRequestDto;
import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.dto.list.GameListDetailDto;
import com.checkpoint.api.dto.list.GameListEntryDto;
import com.checkpoint.api.dto.list.ReorderGamesRequestDto;
import com.checkpoint.api.dto.list.UpdateGameListRequestDto;
import com.checkpoint.api.entities.GameList;
import com.checkpoint.api.entities.GameListEntry;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.GameAlreadyInListException;
import com.checkpoint.api.exceptions.GameListNotFoundException;
import com.checkpoint.api.exceptions.GameNotInListException;
import com.checkpoint.api.exceptions.UnauthorizedListAccessException;
import com.checkpoint.api.mapper.GameListMapper;
import com.checkpoint.api.repositories.CommentRepository;
import com.checkpoint.api.repositories.GameListEntryRepository;
import com.checkpoint.api.repositories.GameListRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

@ExtendWith(MockitoExtension.class)
class GameListServiceImplTest {

    @Mock
    private GameListRepository gameListRepository;

    @Mock
    private GameListEntryRepository gameListEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private GameListMapper gameListMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GameListServiceImpl service;

    private User testUser;
    private User otherUser;
    private VideoGame testGame;
    private GameList testList;
    private GameListDetailDto testDetailDto;

    @BeforeEach
    void setUp() {
        service = new GameListServiceImpl(
                gameListRepository, gameListEntryRepository,
                userRepository, videoGameRepository,
                likeRepository, commentRepository, gameListMapper,
                eventPublisher);

        testUser = new User("testuser", "user@example.com", "password");
        testUser.setId(UUID.randomUUID());

        otherUser = new User("otheruser", "other@example.com", "password");
        otherUser.setId(UUID.randomUUID());

        testGame = new VideoGame("The Witcher 3", "Epic RPG", LocalDate.of(2015, 5, 19));
        testGame.setId(UUID.randomUUID());
        testGame.setCoverUrl("cover.jpg");

        testList = new GameList("My Favorites", testUser);
        testList.setId(UUID.randomUUID());
        testList.setIsPrivate(false);
        testList.setCreatedAt(LocalDateTime.now());
        testList.setUpdatedAt(LocalDateTime.now());

        testDetailDto = new GameListDetailDto(
                testList.getId(), testList.getTitle(), null, false,
                0, 0L, 0L, testUser.getPseudo(), null,
                List.of(), true, false,
                testList.getCreatedAt(), testList.getUpdatedAt());
    }

    @Nested
    @DisplayName("createList")
    class CreateList {

        @Test
        @DisplayName("should create a list successfully")
        void createList_shouldCreateListSuccessfully() {
            // Given
            CreateGameListRequestDto request = new CreateGameListRequestDto("My List", "A description", false);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.save(any(GameList.class))).thenReturn(testList);
            when(gameListMapper.toDetailDto(any(GameList.class), eq(List.of()), eq(0L), eq(0L), eq(true), eq(false)))
                    .thenReturn(testDetailDto);

            // When
            GameListDetailDto result = service.createList("user@example.com", request);

            // Then
            assertThat(result).isEqualTo(testDetailDto);
            verify(gameListRepository).save(any(GameList.class));
        }

        @Test
        @DisplayName("should default isPrivate to false when null")
        void createList_shouldDefaultIsPrivateToFalse() {
            // Given
            CreateGameListRequestDto request = new CreateGameListRequestDto("My List", null, null);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.save(any(GameList.class))).thenReturn(testList);
            when(gameListMapper.toDetailDto(any(GameList.class), anyList(), anyLong(), anyLong(), anyBoolean(), anyBoolean()))
                    .thenReturn(testDetailDto);

            // When
            service.createList("user@example.com", request);

            // Then
            verify(gameListRepository).save(any(GameList.class));
        }
    }

    @Nested
    @DisplayName("updateList")
    class UpdateList {

        @Test
        @DisplayName("should update list title successfully")
        void updateList_shouldUpdateTitleSuccessfully() {
            // Given
            UpdateGameListRequestDto request = new UpdateGameListRequestDto("New Title", null, null);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(gameListRepository.save(any(GameList.class))).thenReturn(testList);
            when(gameListEntryRepository.findByGameListIdOrderByPositionAsc(testList.getId())).thenReturn(List.of());
            when(likeRepository.countByGameListId(testList.getId())).thenReturn(0L);
            when(gameListMapper.toDetailDto(any(GameList.class), anyList(), anyLong(), anyLong(), anyBoolean(), anyBoolean()))
                    .thenReturn(testDetailDto);

            // When
            GameListDetailDto result = service.updateList("user@example.com", testList.getId(), request);

            // Then
            assertThat(result).isEqualTo(testDetailDto);
        }

        @Test
        @DisplayName("should throw UnauthorizedListAccessException when not owner")
        void updateList_shouldThrowWhenNotOwner() {
            // Given
            UpdateGameListRequestDto request = new UpdateGameListRequestDto("New Title", null, null);
            when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));

            // When & Then
            assertThatThrownBy(() -> service.updateList("other@example.com", testList.getId(), request))
                    .isInstanceOf(UnauthorizedListAccessException.class);
        }

        @Test
        @DisplayName("should throw GameListNotFoundException when list not found")
        void updateList_shouldThrowWhenListNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            UpdateGameListRequestDto request = new UpdateGameListRequestDto("New Title", null, null);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(unknownId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.updateList("user@example.com", unknownId, request))
                    .isInstanceOf(GameListNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteList")
    class DeleteList {

        @Test
        @DisplayName("should delete list successfully")
        void deleteList_shouldDeleteSuccessfully() {
            // Given
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));

            // When
            service.deleteList("user@example.com", testList.getId());

            // Then
            verify(gameListRepository).delete(testList);
        }

        @Test
        @DisplayName("should throw UnauthorizedListAccessException when not owner")
        void deleteList_shouldThrowWhenNotOwner() {
            // Given
            when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));

            // When & Then
            assertThatThrownBy(() -> service.deleteList("other@example.com", testList.getId()))
                    .isInstanceOf(UnauthorizedListAccessException.class);

            verify(gameListRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("addGameToList")
    class AddGameToList {

        @Test
        @DisplayName("should add game to list successfully")
        void addGameToList_shouldAddGameSuccessfully() {
            // Given
            AddGameToListRequestDto request = new AddGameToListRequestDto(testGame.getId());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(gameListEntryRepository.existsByGameListIdAndVideoGameId(testList.getId(), testGame.getId()))
                    .thenReturn(false);
            when(gameListEntryRepository.findMaxPositionByGameListId(testList.getId())).thenReturn(Optional.empty());
            when(gameListEntryRepository.save(any(GameListEntry.class))).thenAnswer(inv -> inv.getArgument(0));
            when(gameListEntryRepository.findByGameListIdOrderByPositionAsc(testList.getId())).thenReturn(List.of());
            when(likeRepository.countByGameListId(testList.getId())).thenReturn(0L);
            when(gameListMapper.toDetailDto(any(GameList.class), anyList(), anyLong(), anyLong(), anyBoolean(), anyBoolean()))
                    .thenReturn(testDetailDto);

            // When
            GameListDetailDto result = service.addGameToList("user@example.com", testList.getId(), request);

            // Then
            assertThat(result).isEqualTo(testDetailDto);
            verify(gameListEntryRepository).save(any(GameListEntry.class));
        }

        @Test
        @DisplayName("should throw GameAlreadyInListException when game already in list")
        void addGameToList_shouldThrowWhenGameAlreadyInList() {
            // Given
            AddGameToListRequestDto request = new AddGameToListRequestDto(testGame.getId());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(gameListEntryRepository.existsByGameListIdAndVideoGameId(testList.getId(), testGame.getId()))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.addGameToList("user@example.com", testList.getId(), request))
                    .isInstanceOf(GameAlreadyInListException.class);

            verify(gameListEntryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeGameFromList")
    class RemoveGameFromList {

        @Test
        @DisplayName("should remove game from list successfully")
        void removeGameFromList_shouldRemoveSuccessfully() {
            // Given
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(gameListEntryRepository.existsByGameListIdAndVideoGameId(testList.getId(), testGame.getId()))
                    .thenReturn(true);
            when(gameListEntryRepository.findByGameListIdOrderByPositionAsc(testList.getId())).thenReturn(List.of());

            // When
            service.removeGameFromList("user@example.com", testList.getId(), testGame.getId());

            // Then
            verify(gameListEntryRepository).deleteByGameListIdAndVideoGameId(testList.getId(), testGame.getId());
        }

        @Test
        @DisplayName("should throw GameNotInListException when game not in list")
        void removeGameFromList_shouldThrowWhenGameNotInList() {
            // Given
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(gameListEntryRepository.existsByGameListIdAndVideoGameId(testList.getId(), testGame.getId()))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.removeGameFromList("user@example.com", testList.getId(), testGame.getId()))
                    .isInstanceOf(GameNotInListException.class);
        }
    }

    @Nested
    @DisplayName("reorderGames")
    class ReorderGames {

        @Test
        @DisplayName("should reorder games successfully")
        void reorderGames_shouldReorderSuccessfully() {
            // Given
            UUID game1Id = UUID.randomUUID();
            UUID game2Id = UUID.randomUUID();
            ReorderGamesRequestDto request = new ReorderGamesRequestDto(List.of(game2Id, game1Id));

            GameListEntry entry1 = new GameListEntry(testList, testGame, 0);
            entry1.setId(UUID.randomUUID());
            GameListEntry entry2 = new GameListEntry(testList, testGame, 1);
            entry2.setId(UUID.randomUUID());

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(gameListEntryRepository.findByGameListIdAndVideoGameId(testList.getId(), game2Id))
                    .thenReturn(Optional.of(entry2));
            when(gameListEntryRepository.findByGameListIdAndVideoGameId(testList.getId(), game1Id))
                    .thenReturn(Optional.of(entry1));
            when(gameListEntryRepository.findByGameListIdOrderByPositionAsc(testList.getId())).thenReturn(List.of());
            when(likeRepository.countByGameListId(testList.getId())).thenReturn(0L);
            when(gameListMapper.toDetailDto(any(GameList.class), anyList(), anyLong(), anyLong(), anyBoolean(), anyBoolean()))
                    .thenReturn(testDetailDto);

            // When
            GameListDetailDto result = service.reorderGames("user@example.com", testList.getId(), request);

            // Then
            assertThat(result).isEqualTo(testDetailDto);
        }

        @Test
        @DisplayName("should throw GameNotInListException when game not found in list")
        void reorderGames_shouldThrowWhenGameNotInList() {
            // Given
            UUID unknownGameId = UUID.randomUUID();
            ReorderGamesRequestDto request = new ReorderGamesRequestDto(List.of(unknownGameId));

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(gameListEntryRepository.findByGameListIdAndVideoGameId(testList.getId(), unknownGameId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.reorderGames("user@example.com", testList.getId(), request))
                    .isInstanceOf(GameNotInListException.class);
        }
    }

    @Nested
    @DisplayName("getListDetail")
    class GetListDetail {

        @Test
        @DisplayName("should return detail for public list as anonymous")
        void getListDetail_shouldReturnDetailForPublicList() {
            // Given
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(gameListEntryRepository.findByGameListIdOrderByPositionAsc(testList.getId())).thenReturn(List.of());
            when(likeRepository.countByGameListId(testList.getId())).thenReturn(5L);
            when(gameListMapper.toDetailDto(testList, List.of(), 5L, 0L, false, false)).thenReturn(testDetailDto);

            // When
            GameListDetailDto result = service.getListDetail(testList.getId(), null);

            // Then
            assertThat(result).isEqualTo(testDetailDto);
        }

        @Test
        @DisplayName("should throw UnauthorizedListAccessException for private list as anonymous")
        void getListDetail_shouldThrowForPrivateListAnonymous() {
            // Given
            testList.setIsPrivate(true);
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));

            // When & Then
            assertThatThrownBy(() -> service.getListDetail(testList.getId(), null))
                    .isInstanceOf(UnauthorizedListAccessException.class);
        }

        @Test
        @DisplayName("should return detail for private list as owner")
        void getListDetail_shouldReturnDetailForOwner() {
            // Given
            testList.setIsPrivate(true);
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(likeRepository.existsByUserIdAndGameListId(testUser.getId(), testList.getId())).thenReturn(false);
            when(gameListEntryRepository.findByGameListIdOrderByPositionAsc(testList.getId())).thenReturn(List.of());
            when(likeRepository.countByGameListId(testList.getId())).thenReturn(0L);
            when(gameListMapper.toDetailDto(testList, List.of(), 0L, 0L, true, false)).thenReturn(testDetailDto);

            // When
            GameListDetailDto result = service.getListDetail(testList.getId(), "user@example.com");

            // Then
            assertThat(result).isEqualTo(testDetailDto);
        }

        @Test
        @DisplayName("should throw UnauthorizedListAccessException for private list as non-owner")
        void getListDetail_shouldThrowForPrivateListNonOwner() {
            // Given
            testList.setIsPrivate(true);
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
            when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
            when(likeRepository.existsByUserIdAndGameListId(otherUser.getId(), testList.getId())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.getListDetail(testList.getId(), "other@example.com"))
                    .isInstanceOf(UnauthorizedListAccessException.class);
        }
    }

    @Nested
    @DisplayName("getRecentPublicLists")
    class GetRecentPublicLists {

        @Test
        @DisplayName("should return paginated public lists")
        void getRecentPublicLists_shouldReturnPaginatedLists() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<GameList> page = new PageImpl<>(List.of(testList), pageable, 1);
            GameListCardDto cardDto = new GameListCardDto(
                    testList.getId(), testList.getTitle(), null, false,
                    0, 0L, 0L, testUser.getPseudo(), null, List.of(), testList.getCreatedAt());

            when(gameListRepository.findAllPublic(pageable)).thenReturn(page);
            when(likeRepository.countByGameListId(testList.getId())).thenReturn(0L);
            when(gameListEntryRepository.findByGameListIdOrderByPositionAsc(testList.getId())).thenReturn(List.of());
            when(gameListMapper.toCardDto(testList, 0L, 0L, List.of())).thenReturn(cardDto);

            // When
            Page<GameListCardDto> result = service.getRecentPublicLists(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(cardDto);
        }
    }
}
