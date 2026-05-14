package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.dto.collection.GameInteractionStatusDto;
import com.checkpoint.api.entities.Backlog;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.entities.Wish;
import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.enums.Priority;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.WishRepository;

@ExtendWith(MockitoExtension.class)
class GameInteractionServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private BacklogRepository backlogRepository;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private UserGamePlayRepository userGamePlayRepository;

    @Mock
    private RateRepository rateRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private GameInteractionServiceImpl gameInteractionService;

    private User testUser;
    private VideoGame testGame;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@example.com");
        testUser.setPseudo("user");

        testGame = new VideoGame();
        testGame.setId(UUID.randomUUID());
        testGame.setTitle("The Witcher 3");
    }

    @Test
    @DisplayName("should return aggregate status when all interactions exist")
    void shouldReturnStatusWhenAllExist() {
        // Given
        UserGame userGame = new UserGame(testUser, testGame, GameStatus.PLAYING);
        userGame.setNotes("Currently in act 2");
        Rate rate = new Rate(testUser, testGame, 5);

        Wish wish = new Wish(testUser, testGame);
        wish.setPriority(Priority.HIGH);
        Backlog backlog = new Backlog(testUser, testGame);
        backlog.setPriority(Priority.MEDIUM);

        UserGamePlay mostRecentScoredPlay = new UserGamePlay(testUser, testGame, null, PlayStatus.COMPLETED);
        mostRecentScoredPlay.setScore(4);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(wishRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(Optional.of(wish));
        when(backlogRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(Optional.of(backlog));
        when(userGameRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(Optional.of(userGame));
        when(userGamePlayRepository.countByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(3L);
        when(rateRepository.findByUserPseudoAndVideoGameId(testUser.getPseudo(), testGame.getId())).thenReturn(Optional.of(rate));
        when(reviewRepository.existsByUserPseudoAndVideoGameId(testUser.getPseudo(), testGame.getId())).thenReturn(true);
        when(userGamePlayRepository.findMostRecentScoredPlay(testUser.getId(), testGame.getId()))
                .thenReturn(Optional.of(mostRecentScoredPlay));

        // When
        GameInteractionStatusDto result = gameInteractionService.getGameInteractionStatus(testUser.getEmail(), testGame.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.inWishlist()).isTrue();
        assertThat(result.wishlistPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.inBacklog()).isTrue();
        assertThat(result.backlogPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(result.inLibrary()).isTrue();
        assertThat(result.libraryStatus()).isEqualTo(GameStatus.PLAYING);
        assertThat(result.libraryNotes()).isEqualTo("Currently in act 2");
        assertThat(result.playCount()).isEqualTo(3);
        assertThat(result.userRating()).isEqualTo(5);
        assertThat(result.hasReview()).isTrue();
        assertThat(result.lastPlayRating()).isEqualTo(4);
    }

    @Test
    @DisplayName("should return status with defaults when no interactions exist")
    void shouldReturnStatusWhenNoneExist() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(wishRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(Optional.empty());
        when(backlogRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(Optional.empty());
        when(userGameRepository.findByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(Optional.empty());
        when(userGamePlayRepository.countByUserIdAndVideoGameId(testUser.getId(), testGame.getId())).thenReturn(0L);
        when(rateRepository.findByUserPseudoAndVideoGameId(testUser.getPseudo(), testGame.getId())).thenReturn(Optional.empty());
        when(reviewRepository.existsByUserPseudoAndVideoGameId(testUser.getPseudo(), testGame.getId())).thenReturn(false);
        when(userGamePlayRepository.findMostRecentScoredPlay(testUser.getId(), testGame.getId())).thenReturn(Optional.empty());

        // When
        GameInteractionStatusDto result = gameInteractionService.getGameInteractionStatus(testUser.getEmail(), testGame.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.inWishlist()).isFalse();
        assertThat(result.wishlistPriority()).isNull();
        assertThat(result.inBacklog()).isFalse();
        assertThat(result.backlogPriority()).isNull();
        assertThat(result.inLibrary()).isFalse();
        assertThat(result.libraryStatus()).isNull();
        assertThat(result.libraryNotes()).isNull();
        assertThat(result.playCount()).isEqualTo(0);
        assertThat(result.userRating()).isNull();
        assertThat(result.hasReview()).isFalse();
        assertThat(result.lastPlayRating()).isNull();
    }

    @Test
    @DisplayName("should throw error when user does not exist")
    void shouldThrowErrorWhenUserDoesNotExist() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> gameInteractionService.getGameInteractionStatus(testUser.getEmail(), testGame.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with email");
    }
}
