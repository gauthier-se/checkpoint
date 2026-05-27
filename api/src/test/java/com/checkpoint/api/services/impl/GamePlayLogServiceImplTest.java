package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.checkpoint.api.dto.playlog.GamePlayLogRequestDto;
import com.checkpoint.api.dto.playlog.GamePlayLogResponseDto;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.PlayLogNotFoundException;
import com.checkpoint.api.events.GameFinishedEvent;
import com.checkpoint.api.mapper.GamePlayLogMapper;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.TagRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.RateService;

@ExtendWith(MockitoExtension.class)
class GamePlayLogServiceImplTest {

    @Mock
    private UserGamePlayRepository userGamePlayRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private PlatformRepository platformRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private BacklogRepository backlogRepository;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private GamePlayLogMapper gamePlayLogMapper;

    @Mock
    private RateService rateService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GamePlayLogServiceImpl gamePlayLogService;

    private User testUser;
    private VideoGame testGame;
    private Platform testPlatform;
    private UserGamePlay testPlayLog;
    private GamePlayLogResponseDto testResponseDto;
    private GamePlayLogRequestDto testRequestDto;

    @BeforeEach
    void setUp() {
        gamePlayLogService = new GamePlayLogServiceImpl(
                userGamePlayRepository,
                userRepository,
                videoGameRepository,
                platformRepository,
                tagRepository,
                wishRepository,
                backlogRepository,
                userGameRepository,
                gamePlayLogMapper,
                rateService,
                eventPublisher
        );

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@example.com");

        testGame = new VideoGame();
        testGame.setId(UUID.randomUUID());
        testGame.setTitle("The Witcher 3");

        testPlatform = new Platform();
        testPlatform.setId(UUID.randomUUID());
        testPlatform.setName("PC");

        testPlayLog = new UserGamePlay(testUser, testGame, testPlatform, PlayStatus.COMPLETED);
        testPlayLog.setId(UUID.randomUUID());
        testPlayLog.setCreatedAt(LocalDateTime.now());

        testResponseDto = new GamePlayLogResponseDto(
                testPlayLog.getId(), testGame.getId(), testGame.getTitle(), null, null,
                testPlatform.getId(), testPlatform.getName(), PlayStatus.COMPLETED,
                false, 2000, LocalDate.now(), LocalDate.now(), "owned",
                LocalDateTime.now(), LocalDateTime.now(), null, null, null, List.of()
        );

        testRequestDto = new GamePlayLogRequestDto(
                testGame.getId(), testPlatform.getId(), PlayStatus.COMPLETED,
                LocalDate.now(), LocalDate.now(), 2000, "owned", false, null, null
        );
    }

    @Nested
    @DisplayName("logPlay()")
    class LogPlay {

        @Test
        @DisplayName("should create new play log")
        void shouldCreateNewPlayLog() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(platformRepository.findById(testPlatform.getId())).thenReturn(Optional.of(testPlatform));
            when(gamePlayLogMapper.toEntity(testRequestDto)).thenReturn(testPlayLog);
            when(userGamePlayRepository.save(any(UserGamePlay.class))).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);

            // When
            GamePlayLogResponseDto result = gamePlayLogService.logPlay(testUser.getEmail(), testRequestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testPlayLog.getId());
            verify(userGamePlayRepository).save(any(UserGamePlay.class));
            verify(eventPublisher).publishEvent(any(GameFinishedEvent.class));
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> gamePlayLogService.logPlay(testUser.getEmail(), testRequestDto))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when game not found")
        void shouldThrowWhenGameNotFound() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> gamePlayLogService.logPlay(testUser.getEmail(), testRequestDto))
                    .isInstanceOf(GameNotFoundException.class);
        }

        @Test
        @DisplayName("should sync global rating when logging with score")
        void shouldSyncGlobalRatingWhenLoggingWithScore() {
            // Given
            GamePlayLogRequestDto requestWithScore = new GamePlayLogRequestDto(
                    testGame.getId(), testPlatform.getId(), PlayStatus.COMPLETED,
                    LocalDate.now(), LocalDate.now(), 2000, "owned", false, 4, null
            );

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(platformRepository.findById(testPlatform.getId())).thenReturn(Optional.of(testPlatform));
            when(gamePlayLogMapper.toEntity(requestWithScore)).thenReturn(testPlayLog);
            when(userGamePlayRepository.save(any(UserGamePlay.class))).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), requestWithScore);

            // Then
            verify(rateService).rateGame(testUser.getEmail(), testGame.getId(), 4);
        }

        @Test
        @DisplayName("should not sync global rating when logging without score")
        void shouldNotSyncRatingWhenLoggingWithoutScore() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(platformRepository.findById(testPlatform.getId())).thenReturn(Optional.of(testPlatform));
            when(gamePlayLogMapper.toEntity(testRequestDto)).thenReturn(testPlayLog);
            when(userGamePlayRepository.save(any(UserGamePlay.class))).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), testRequestDto);

            // Then
            verify(rateService, never()).rateGame(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("updatePlayLog()")
    class UpdatePlayLog {

        @Test
        @DisplayName("should update existing play log")
        void shouldUpdateExistingPlayLog() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));
            when(userGamePlayRepository.save(testPlayLog)).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);

            // When
            GamePlayLogResponseDto result = gamePlayLogService.updatePlayLog(testUser.getEmail(), testPlayLog.getId(), testRequestDto);

            // Then
            assertThat(result).isNotNull();
            verify(gamePlayLogMapper).updateEntityFromDto(testRequestDto, testPlayLog);
            verify(userGamePlayRepository).save(testPlayLog);
        }

        @Test
        @DisplayName("should throw when log not owned by user")
        void shouldThrowWhenLogNotOwnedByUser() {
            // Given
            User otherUser = new User();
            otherUser.setId(UUID.randomUUID());
            testPlayLog.setUser(otherUser);

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));

            // When + Then
            assertThatThrownBy(() -> gamePlayLogService.updatePlayLog(testUser.getEmail(), testPlayLog.getId(), testRequestDto))
                    .isInstanceOf(PlayLogNotFoundException.class);
        }

        @Test
        @DisplayName("should update global rating when updating most recent log score")
        void shouldUpdateGlobalRatingWhenUpdatingMostRecentScore() {
            // Given
            testPlayLog.setScore(3);
            GamePlayLogRequestDto requestWithNewScore = new GamePlayLogRequestDto(
                    testGame.getId(), testPlatform.getId(), PlayStatus.COMPLETED,
                    LocalDate.now(), LocalDate.now(), 2000, "owned", false, 5, null
            );

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));
            when(userGamePlayRepository.save(testPlayLog)).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);
            when(userGamePlayRepository.findMostRecentScoredPlay(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.of(testPlayLog));

            // When
            gamePlayLogService.updatePlayLog(testUser.getEmail(), testPlayLog.getId(), requestWithNewScore);

            // Then
            verify(rateService).rateGame(testUser.getEmail(), testGame.getId(), 5);
        }

        @Test
        @DisplayName("should not update global rating when updating non-latest log score")
        void shouldNotUpdateGlobalRatingWhenUpdatingNonLatestLog() {
            // Given
            testPlayLog.setScore(3);
            GamePlayLogRequestDto requestWithNewScore = new GamePlayLogRequestDto(
                    testGame.getId(), testPlatform.getId(), PlayStatus.COMPLETED,
                    LocalDate.now(), LocalDate.now(), 2000, "owned", false, 5, null
            );

            UserGamePlay moreRecentPlayLog = new UserGamePlay(testUser, testGame, testPlatform, PlayStatus.COMPLETED);
            moreRecentPlayLog.setId(UUID.randomUUID());
            moreRecentPlayLog.setScore(4);

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));
            when(userGamePlayRepository.save(testPlayLog)).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);
            when(userGamePlayRepository.findMostRecentScoredPlay(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.of(moreRecentPlayLog));

            // When
            gamePlayLogService.updatePlayLog(testUser.getEmail(), testPlayLog.getId(), requestWithNewScore);

            // Then
            verify(rateService, never()).rateGame(any(), any(), any());
            verify(rateService, never()).removeRating(any(), any());
        }

        @Test
        @DisplayName("should recalculate global rating when clearing score from most recent log")
        void shouldRecalculateRatingWhenClearingScoreFromMostRecentLog() {
            // Given
            testPlayLog.setScore(4);
            GamePlayLogRequestDto requestWithNoScore = new GamePlayLogRequestDto(
                    testGame.getId(), testPlatform.getId(), PlayStatus.COMPLETED,
                    LocalDate.now(), LocalDate.now(), 2000, "owned", false, null, null
            );

            UserGamePlay olderScoredPlayLog = new UserGamePlay(testUser, testGame, testPlatform, PlayStatus.COMPLETED);
            olderScoredPlayLog.setId(UUID.randomUUID());
            olderScoredPlayLog.setScore(3);

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));
            when(userGamePlayRepository.save(testPlayLog)).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);
            // First call: the updated play log now has no score, so it won't be returned
            // The most recent scored log is now the older one
            when(userGamePlayRepository.findMostRecentScoredPlay(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.of(olderScoredPlayLog));

            // When
            gamePlayLogService.updatePlayLog(testUser.getEmail(), testPlayLog.getId(), requestWithNoScore);

            // Then
            verify(rateService).rateGame(testUser.getEmail(), testGame.getId(), 3);
        }

        @Test
        @DisplayName("should remove global rating when clearing last scored log")
        void shouldRemoveGlobalRatingWhenClearingLastScoredLog() {
            // Given
            testPlayLog.setScore(4);
            GamePlayLogRequestDto requestWithNoScore = new GamePlayLogRequestDto(
                    testGame.getId(), testPlatform.getId(), PlayStatus.COMPLETED,
                    LocalDate.now(), LocalDate.now(), 2000, "owned", false, null, null
            );

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));
            when(userGamePlayRepository.save(testPlayLog)).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);
            when(userGamePlayRepository.findMostRecentScoredPlay(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.empty());

            // When
            gamePlayLogService.updatePlayLog(testUser.getEmail(), testPlayLog.getId(), requestWithNoScore);

            // Then
            verify(rateService).removeRating(testUser.getEmail(), testGame.getId());
        }
    }

    @Nested
    @DisplayName("deletePlayLog()")
    class DeletePlayLog {

        @Test
        @DisplayName("should delete play log")
        void shouldDeletePlayLog() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));

            // When
            gamePlayLogService.deletePlayLog(testUser.getEmail(), testPlayLog.getId());

            // Then
            verify(userGamePlayRepository).delete(testPlayLog);
        }

        @Test
        @DisplayName("should recalculate global rating when deleting most recent scored log")
        void shouldRecalculateRatingWhenDeletingMostRecentScoredLog() {
            // Given
            testPlayLog.setScore(5);
            UserGamePlay olderScoredPlayLog = new UserGamePlay(testUser, testGame, testPlatform, PlayStatus.COMPLETED);
            olderScoredPlayLog.setId(UUID.randomUUID());
            olderScoredPlayLog.setScore(3);

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));
            when(userGamePlayRepository.findMostRecentScoredPlay(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.of(olderScoredPlayLog));

            // When
            gamePlayLogService.deletePlayLog(testUser.getEmail(), testPlayLog.getId());

            // Then
            verify(userGamePlayRepository).delete(testPlayLog);
            verify(rateService).rateGame(testUser.getEmail(), testGame.getId(), 3);
        }

        @Test
        @DisplayName("should remove global rating when deleting last scored log")
        void shouldRemoveGlobalRatingWhenDeletingLastScoredLog() {
            // Given
            testPlayLog.setScore(4);

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));
            when(userGamePlayRepository.findMostRecentScoredPlay(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.empty());

            // When
            gamePlayLogService.deletePlayLog(testUser.getEmail(), testPlayLog.getId());

            // Then
            verify(userGamePlayRepository).delete(testPlayLog);
            verify(rateService).removeRating(testUser.getEmail(), testGame.getId());
        }

        @Test
        @DisplayName("should not affect rating when deleting unscored log")
        void shouldNotAffectRatingWhenDeletingUnscoredLog() {
            // Given
            testPlayLog.setScore(null);

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(testPlayLog.getId())).thenReturn(Optional.of(testPlayLog));

            // When
            gamePlayLogService.deletePlayLog(testUser.getEmail(), testPlayLog.getId());

            // Then
            verify(userGamePlayRepository).delete(testPlayLog);
            verify(rateService, never()).rateGame(any(), any(), any());
            verify(rateService, never()).removeRating(any(), any());
        }
    }

    @Nested
    @DisplayName("getUserPlayLog()")
    class GetUserPlayLog {

        @Test
        @DisplayName("should return paginated play logs")
        void shouldReturnPaginatedPlayLogs() {
            // Given
            Page<UserGamePlay> page = new PageImpl<>(List.of(testPlayLog));
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findByUserId(eq(testUser.getId()), any(Pageable.class))).thenReturn(page);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);

            // When
            Page<GamePlayLogResponseDto> result = gamePlayLogService.getUserPlayLog(testUser.getEmail(), Pageable.unpaged());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testResponseDto);
        }
    }

    @Nested
    @DisplayName("getGamePlayHistory()")
    class GetGamePlayHistory {

        @Test
        @DisplayName("should return list of play histories")
        void shouldReturnListOfPlayHistories() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(videoGameRepository.existsById(testGame.getId())).thenReturn(true);
            when(userGamePlayRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(List.of(testPlayLog));
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);

            // When
            List<GamePlayLogResponseDto> result = gamePlayLogService.getGamePlayHistory(testUser.getEmail(), testGame.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testResponseDto);
        }
    }

    @Nested
    @DisplayName("logPlay() — collection reconciliation")
    class CollectionReconciliation {

        private void stubLogPlayHappyPath(GamePlayLogRequestDto request) {
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(videoGameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
            when(platformRepository.findById(testPlatform.getId())).thenReturn(Optional.of(testPlatform));
            when(gamePlayLogMapper.toEntity(request)).thenReturn(testPlayLog);
            when(userGamePlayRepository.save(any(UserGamePlay.class))).thenReturn(testPlayLog);
            when(gamePlayLogMapper.toDto(testPlayLog)).thenReturn(testResponseDto);
        }

        private GamePlayLogRequestDto requestWithStatus(PlayStatus status) {
            return new GamePlayLogRequestDto(
                    testGame.getId(), testPlatform.getId(), status,
                    LocalDate.now(), LocalDate.now(), 2000, "owned", false, null, null
            );
        }

        private GameStatus capturedSavedStatus() {
            ArgumentCaptor<UserGame> captor = ArgumentCaptor.forClass(UserGame.class);
            verify(userGameRepository).save(captor.capture());
            return captor.getValue().getStatus();
        }

        @Test
        @DisplayName("should remove the game from wishlist and backlog on log")
        void shouldRemoveFromWishlistAndBacklog() {
            // Given
            stubLogPlayHappyPath(testRequestDto);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), testRequestDto);

            // Then
            verify(wishRepository).deleteByUserIdAndVideoGameId(testUser.getId(), testGame.getId());
            verify(backlogRepository).deleteByUserIdAndVideoGameId(testUser.getId(), testGame.getId());
        }

        @Test
        @DisplayName("should add the game to library when not already present")
        void shouldAddToLibraryWhenAbsent() {
            // Given
            stubLogPlayHappyPath(testRequestDto);
            when(userGameRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.empty());

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), testRequestDto);

            // Then
            ArgumentCaptor<UserGame> captor = ArgumentCaptor.forClass(UserGame.class);
            verify(userGameRepository).save(captor.capture());
            UserGame saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(testUser);
            assertThat(saved.getVideoGame()).isEqualTo(testGame);
            assertThat(saved.getStatus()).isEqualTo(GameStatus.COMPLETED);
        }

        @Test
        @DisplayName("should update existing library entry status without touching notes")
        void shouldUpdateExistingLibraryEntryStatusOnly() {
            // Given
            UserGame existing = new UserGame(testUser, testGame, GameStatus.PLAYING);
            existing.setNotes("my note");
            stubLogPlayHappyPath(testRequestDto);
            when(userGameRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId()))
                    .thenReturn(Optional.of(existing));

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), testRequestDto);

            // Then
            verify(userGameRepository).save(existing);
            assertThat(existing.getStatus()).isEqualTo(GameStatus.COMPLETED);
            assertThat(existing.getNotes()).isEqualTo("my note");
        }

        @Test
        @DisplayName("should map ARE_PLAYING to PLAYING")
        void shouldMapArePlayingToPlaying() {
            // Given
            GamePlayLogRequestDto request = requestWithStatus(PlayStatus.ARE_PLAYING);
            stubLogPlayHappyPath(request);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), request);

            // Then
            assertThat(capturedSavedStatus()).isEqualTo(GameStatus.PLAYING);
        }

        @Test
        @DisplayName("should map PLAYED to PLAYING")
        void shouldMapPlayedToPlaying() {
            // Given
            GamePlayLogRequestDto request = requestWithStatus(PlayStatus.PLAYED);
            stubLogPlayHappyPath(request);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), request);

            // Then
            assertThat(capturedSavedStatus()).isEqualTo(GameStatus.PLAYING);
        }

        @Test
        @DisplayName("should map SHELVED to PLAYING")
        void shouldMapShelvedToPlaying() {
            // Given
            GamePlayLogRequestDto request = requestWithStatus(PlayStatus.SHELVED);
            stubLogPlayHappyPath(request);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), request);

            // Then
            assertThat(capturedSavedStatus()).isEqualTo(GameStatus.PLAYING);
        }

        @Test
        @DisplayName("should map COMPLETED to COMPLETED")
        void shouldMapCompletedToCompleted() {
            // Given
            GamePlayLogRequestDto request = requestWithStatus(PlayStatus.COMPLETED);
            stubLogPlayHappyPath(request);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), request);

            // Then
            assertThat(capturedSavedStatus()).isEqualTo(GameStatus.COMPLETED);
        }

        @Test
        @DisplayName("should map RETIRED to DROPPED")
        void shouldMapRetiredToDropped() {
            // Given
            GamePlayLogRequestDto request = requestWithStatus(PlayStatus.RETIRED);
            stubLogPlayHappyPath(request);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), request);

            // Then
            assertThat(capturedSavedStatus()).isEqualTo(GameStatus.DROPPED);
        }

        @Test
        @DisplayName("should map ABANDONED to DROPPED")
        void shouldMapAbandonedToDropped() {
            // Given
            GamePlayLogRequestDto request = requestWithStatus(PlayStatus.ABANDONED);
            stubLogPlayHappyPath(request);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), request);

            // Then
            assertThat(capturedSavedStatus()).isEqualTo(GameStatus.DROPPED);
        }

        @Test
        @DisplayName("should default to PLAYING when play status is null")
        void shouldDefaultNullStatusToPlaying() {
            // Given
            GamePlayLogRequestDto request = requestWithStatus(null);
            stubLogPlayHappyPath(request);

            // When
            gamePlayLogService.logPlay(testUser.getEmail(), request);

            // Then
            assertThat(capturedSavedStatus()).isEqualTo(GameStatus.PLAYING);
        }
    }
}
