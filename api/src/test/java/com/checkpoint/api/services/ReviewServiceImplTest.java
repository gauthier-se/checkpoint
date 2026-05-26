package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.ReviewRequestDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.dto.catalog.ReviewUserDto;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.PlayLogNotFoundException;
import com.checkpoint.api.exceptions.ReviewAlreadyExistsException;
import com.checkpoint.api.exceptions.ReviewNotFoundException;
import com.checkpoint.api.events.ReviewCreatedEvent;
import com.checkpoint.api.mapper.ReviewMapper;
import com.checkpoint.api.repositories.CommentRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.impl.ReviewServiceImpl;

/**
 * Unit tests for {@link ReviewServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGamePlayRepository userGamePlayRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReviewServiceImpl reviewService;

    private User testUser;
    private VideoGame testGame;
    private Platform testPlatform;
    private UserGamePlay testPlayLog;
    private UUID gameId;
    private UUID playId;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository, videoGameRepository, userRepository,
                userGamePlayRepository, likeRepository, commentRepository, reviewMapper, eventPublisher);

        gameId = UUID.randomUUID();
        playId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setPseudo("testuser");
        testUser.setEmail("test@test.com");

        testGame = new VideoGame();
        testGame.setId(gameId);
        testGame.setTitle("Test Game");

        testPlatform = new Platform();
        testPlatform.setId(UUID.randomUUID());
        testPlatform.setName("PC");

        testPlayLog = new UserGamePlay(testUser, testGame, testPlatform, PlayStatus.COMPLETED);
        testPlayLog.setId(playId);
    }

    @Nested
    @DisplayName("getGameReviews()")
    class GetGameReviews {

        @Test
        @DisplayName("Should return a paginated list of reviews")
        void getGameReviews_shouldReturnPaginatedReviews() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Review review = new Review("Good", false, testUser, testGame, testPlayLog);
            Page<Review> reviewPage = new PageImpl<>(List.of(review));

            when(videoGameRepository.existsById(gameId)).thenReturn(true);
            when(reviewRepository.findByVideoGameId(gameId, pageable)).thenReturn(reviewPage);
            when(likeRepository.countByReviewId(any())).thenReturn(0L);
            when(commentRepository.countByReviewId(any())).thenReturn(0L);

            ReviewResponseDto responseDto = new ReviewResponseDto(
                    UUID.randomUUID(), "Good", false,
                    LocalDateTime.now(), LocalDateTime.now(),
                    new ReviewUserDto(testUser.getId(), testUser.getPseudo(), null),
                    playId, "PC", PlayStatus.COMPLETED, false, 0, false, 0);
            when(reviewMapper.toDto(review, 0L, false, 0L)).thenReturn(responseDto);

            // When
            Page<ReviewResponseDto> result = reviewService.getGameReviews(gameId, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).content()).isEqualTo("Good");
        }

        @Test
        @DisplayName("Should throw GameNotFoundException when game does not exist")
        void getGameReviews_shouldThrowWhenGameNotFound() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(videoGameRepository.existsById(gameId)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> reviewService.getGameReviews(gameId, null, pageable))
                    .isInstanceOf(GameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createPlayLogReview()")
    class CreatePlayLogReview {

        @Test
        @DisplayName("Should create a review for a play log")
        void createPlayLogReview_shouldCreateReview() {
            // Given
            ReviewRequestDto requestDto = new ReviewRequestDto("Great game!", false);
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.existsByUserGamePlayId(playId)).thenReturn(false);

            Review savedReview = new Review("Great game!", false, testUser, testGame, testPlayLog);
            savedReview.setId(UUID.randomUUID());
            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            ReviewResponseDto responseDto = new ReviewResponseDto(
                    savedReview.getId(), "Great game!", false,
                    LocalDateTime.now(), LocalDateTime.now(),
                    new ReviewUserDto(testUser.getId(), testUser.getPseudo(), null),
                    playId, "PC", PlayStatus.COMPLETED, false, 0, false, 0);
            when(reviewMapper.toDto(savedReview)).thenReturn(responseDto);

            // When
            ReviewResponseDto result = reviewService.createPlayLogReview(
                    testUser.getEmail(), playId, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("Great game!");
            assertThat(result.playLogId()).isEqualTo(playId);
            verify(reviewRepository).save(any(Review.class));
            verify(eventPublisher).publishEvent(any(ReviewCreatedEvent.class));
        }

        @Test
        @DisplayName("Should throw ReviewAlreadyExistsException when play log already has a review")
        void createPlayLogReview_shouldThrowWhenReviewAlreadyExists() {
            // Given
            ReviewRequestDto requestDto = new ReviewRequestDto("Great game!", false);
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.existsByUserGamePlayId(playId)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> reviewService.createPlayLogReview(
                    testUser.getEmail(), playId, requestDto))
                    .isInstanceOf(ReviewAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Should throw PlayLogNotFoundException when play log does not exist")
        void createPlayLogReview_shouldThrowWhenPlayLogNotFound() {
            // Given
            ReviewRequestDto requestDto = new ReviewRequestDto("Great game!", false);
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> reviewService.createPlayLogReview(
                    testUser.getEmail(), playId, requestDto))
                    .isInstanceOf(PlayLogNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw PlayLogNotFoundException when play log belongs to another user")
        void createPlayLogReview_shouldThrowWhenPlayLogNotOwnedByUser() {
            // Given
            ReviewRequestDto requestDto = new ReviewRequestDto("Great game!", false);
            User otherUser = new User();
            otherUser.setId(UUID.randomUUID());
            testPlayLog.setUser(otherUser);

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));

            // When / Then
            assertThatThrownBy(() -> reviewService.createPlayLogReview(
                    testUser.getEmail(), playId, requestDto))
                    .isInstanceOf(PlayLogNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updatePlayLogReview()")
    class UpdatePlayLogReview {

        @Test
        @DisplayName("Should update the review of a play log")
        void updatePlayLogReview_shouldUpdateReview() {
            // Given
            ReviewRequestDto requestDto = new ReviewRequestDto("Updated review", true);
            Review existingReview = new Review("Old content", false, testUser, testGame, testPlayLog);
            existingReview.setId(UUID.randomUUID());

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.findByUserGamePlayId(playId)).thenReturn(Optional.of(existingReview));
            when(reviewRepository.save(existingReview)).thenReturn(existingReview);

            ReviewResponseDto responseDto = new ReviewResponseDto(
                    existingReview.getId(), "Updated review", true,
                    LocalDateTime.now(), LocalDateTime.now(),
                    new ReviewUserDto(testUser.getId(), testUser.getPseudo(), null),
                    playId, "PC", PlayStatus.COMPLETED, false, 0, false, 0);
            when(reviewMapper.toDto(existingReview)).thenReturn(responseDto);

            // When
            ReviewResponseDto result = reviewService.updatePlayLogReview(
                    testUser.getEmail(), playId, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("Updated review");
            assertThat(result.haveSpoilers()).isTrue();
            assertThat(existingReview.getContent()).isEqualTo("Updated review");
            assertThat(existingReview.getHaveSpoilers()).isTrue();
            verify(reviewRepository).save(existingReview);
        }

        @Test
        @DisplayName("Should throw ReviewNotFoundException when no review exists for play log")
        void updatePlayLogReview_shouldThrowWhenReviewNotFound() {
            // Given
            ReviewRequestDto requestDto = new ReviewRequestDto("Updated review", true);
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.findByUserGamePlayId(playId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> reviewService.updatePlayLogReview(
                    testUser.getEmail(), playId, requestDto))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deletePlayLogReview()")
    class DeletePlayLogReview {

        @Test
        @DisplayName("Should delete the review of a play log")
        void deletePlayLogReview_shouldDeleteReview() {
            // Given
            Review existingReview = new Review("Some review", false, testUser, testGame, testPlayLog);
            existingReview.setId(UUID.randomUUID());

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.findByUserGamePlayId(playId)).thenReturn(Optional.of(existingReview));

            // When
            reviewService.deletePlayLogReview(testUser.getEmail(), playId);

            // Then
            verify(reviewRepository).delete(existingReview);
        }

        @Test
        @DisplayName("Should throw ReviewNotFoundException when no review exists for play log")
        void deletePlayLogReview_shouldThrowWhenReviewNotFound() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.findByUserGamePlayId(playId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> reviewService.deletePlayLogReview(testUser.getEmail(), playId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPlayLogReview()")
    class GetPlayLogReview {

        @Test
        @DisplayName("Should return the review of a play log")
        void getPlayLogReview_shouldReturnReview() {
            // Given
            Review review = new Review("Great game!", false, testUser, testGame, testPlayLog);
            review.setId(UUID.randomUUID());

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.findByUserGamePlayId(playId)).thenReturn(Optional.of(review));

            ReviewResponseDto responseDto = new ReviewResponseDto(
                    review.getId(), "Great game!", false,
                    LocalDateTime.now(), LocalDateTime.now(),
                    new ReviewUserDto(testUser.getId(), testUser.getPseudo(), null),
                    playId, "PC", PlayStatus.COMPLETED, false, 0, false, 0);
            when(reviewMapper.toDto(review)).thenReturn(responseDto);

            // When
            ReviewResponseDto result = reviewService.getPlayLogReview(
                    testUser.getEmail(), playId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("Great game!");
            assertThat(result.playLogId()).isEqualTo(playId);
        }

        @Test
        @DisplayName("Should throw ReviewNotFoundException when no review exists for play log")
        void getPlayLogReview_shouldThrowWhenReviewNotFound() {
            // Given
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.findByUserGamePlayId(playId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> reviewService.getPlayLogReview(testUser.getEmail(), playId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("mention notifications")
    class MentionNotifications {

        private User mentionedUser() {
            User other = new User();
            other.setId(UUID.randomUUID());
            other.setPseudo("otheruser");
            other.setEmail("other@test.com");
            return other;
        }

        private NotificationEvent captureMentionEvent() {
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
            return captor.getAllValues().stream()
                    .filter(NotificationEvent.class::isInstance)
                    .map(NotificationEvent.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No MENTION notification event was published"));
        }

        @Test
        @DisplayName("Should dispatch a MENTION notification when creating a review")
        void createReview_shouldDispatchMention() {
            // Given
            User other = mentionedUser();
            ReviewRequestDto requestDto = new ReviewRequestDto("Played with @otheruser", false);
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.existsByUserGamePlayId(playId)).thenReturn(false);

            Review savedReview = new Review("Played with @otheruser", false, testUser, testGame, testPlayLog);
            savedReview.setId(UUID.randomUUID());
            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
            when(userRepository.findByPseudo("otheruser")).thenReturn(Optional.of(other));

            // When
            reviewService.createPlayLogReview(testUser.getEmail(), playId, requestDto);

            // Then
            NotificationEvent event = captureMentionEvent();
            assertThat(event.getRecipientId()).isEqualTo(other.getId());
            assertThat(event.getSenderId()).isEqualTo(testUser.getId());
            assertThat(event.getType()).isEqualTo(NotificationType.MENTION);
            assertThat(event.getReferenceId()).isEqualTo(savedReview.getId());
        }

        @Test
        @DisplayName("Should dispatch a MENTION notification when updating a review")
        void updateReview_shouldDispatchMention() {
            // Given
            User other = mentionedUser();
            ReviewRequestDto requestDto = new ReviewRequestDto("Now mentioning @otheruser", false);
            Review existingReview = new Review("Old content", false, testUser, testGame, testPlayLog);
            existingReview.setId(UUID.randomUUID());

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.findByUserGamePlayId(playId)).thenReturn(Optional.of(existingReview));
            when(reviewRepository.save(existingReview)).thenReturn(existingReview);
            when(userRepository.findByPseudo("otheruser")).thenReturn(Optional.of(other));

            // When
            reviewService.updatePlayLogReview(testUser.getEmail(), playId, requestDto);

            // Then
            NotificationEvent event = captureMentionEvent();
            assertThat(event.getRecipientId()).isEqualTo(other.getId());
            assertThat(event.getType()).isEqualTo(NotificationType.MENTION);
            assertThat(event.getReferenceId()).isEqualTo(existingReview.getId());
        }

        @Test
        @DisplayName("Should not dispatch a MENTION notification for a self-mention")
        void createReview_shouldNotDispatchSelfMention() {
            // Given
            ReviewRequestDto requestDto = new ReviewRequestDto("Note to @testuser", false);
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userGamePlayRepository.findById(playId)).thenReturn(Optional.of(testPlayLog));
            when(reviewRepository.existsByUserGamePlayId(playId)).thenReturn(false);

            Review savedReview = new Review("Note to @testuser", false, testUser, testGame, testPlayLog);
            savedReview.setId(UUID.randomUUID());
            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
            when(userRepository.findByPseudo("testuser")).thenReturn(Optional.of(testUser));

            // When
            reviewService.createPlayLogReview(testUser.getEmail(), playId, requestDto);

            // Then
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
            assertThat(captor.getAllValues()).noneMatch(NotificationEvent.class::isInstance);
        }
    }
}
