package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;

import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.dto.social.LikeResponseDto;
import com.checkpoint.api.entities.Comment;
import com.checkpoint.api.entities.GameList;
import com.checkpoint.api.entities.Like;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.exceptions.CommentNotFoundException;
import com.checkpoint.api.exceptions.GameListNotFoundException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.ReviewNotFoundException;
import com.checkpoint.api.mapper.LikedGameMapper;
import com.checkpoint.api.repositories.CommentRepository;
import com.checkpoint.api.repositories.GameListRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

/**
 * Unit tests for {@link LikeServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private GameListRepository gameListRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EntityManager entityManager;

    @Mock
    private LikedGameMapper likedGameMapper;

    private LikeServiceImpl likeService;

    private User user;
    private User reviewAuthor;
    private User listOwner;
    private Review review;
    private VideoGame videoGame;
    private GameList gameList;

    @BeforeEach
    void setUp() {
        likeService = new LikeServiceImpl(
                likeRepository, reviewRepository, gameListRepository,
                commentRepository, userRepository, videoGameRepository,
                eventPublisher, entityManager, likedGameMapper);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPseudo("testUser");

        reviewAuthor = new User();
        reviewAuthor.setId(UUID.randomUUID());
        reviewAuthor.setPseudo("reviewAuthor");

        listOwner = new User();
        listOwner.setId(UUID.randomUUID());
        listOwner.setPseudo("listOwner");

        videoGame = new VideoGame();
        videoGame.setId(UUID.randomUUID());
        videoGame.setTitle("The Last of Us");

        review = new Review();
        review.setId(UUID.randomUUID());
        review.setUser(reviewAuthor);
        review.setVideoGame(videoGame);

        gameList = new GameList();
        gameList.setId(UUID.randomUUID());
        gameList.setUser(listOwner);
        gameList.setTitle("My Top Games");
    }

    @Nested
    @DisplayName("toggleReviewLike")
    class ToggleReviewLike {

        @Test
        @DisplayName("should like when not already liked")
        void toggleReviewLike_shouldLikeWhenNotLiked() {
            // Given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(reviewRepository.findById(review.getId()))
                    .thenReturn(Optional.of(review));
            when(likeRepository.findByUserIdAndReviewId(user.getId(), review.getId()))
                    .thenReturn(Optional.empty());
            when(likeRepository.countByReviewId(review.getId()))
                    .thenReturn(3L);
            when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> {
                Like like = invocation.getArgument(0);
                like.setId(UUID.randomUUID());
                return like;
            });

            // When
            LikeResponseDto result = likeService.toggleReviewLike("user@example.com", review.getId());

            // Then
            assertThat(result.liked()).isTrue();
            assertThat(result.likesCount()).isEqualTo(4);
            verify(likeRepository).save(any(Like.class));

            ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            NotificationEvent event = eventCaptor.getValue();
            assertThat(event.getRecipientId()).isEqualTo(reviewAuthor.getId());
            assertThat(event.getSenderId()).isEqualTo(user.getId());
            assertThat(event.getType()).isEqualTo(NotificationType.LIKE_REVIEW);
            assertThat(event.getReferenceId()).isEqualTo(review.getId());
            assertThat(event.getMessage()).contains("The Last of Us");
        }

        @Test
        @DisplayName("should unlike when already liked")
        void toggleReviewLike_shouldUnlikeWhenAlreadyLiked() {
            // Given
            Like existingLike = Like.forReview(user, review);

            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(reviewRepository.findById(review.getId()))
                    .thenReturn(Optional.of(review));
            when(likeRepository.findByUserIdAndReviewId(user.getId(), review.getId()))
                    .thenReturn(Optional.of(existingLike));
            when(likeRepository.countByReviewId(review.getId()))
                    .thenReturn(4L);

            // When
            LikeResponseDto result = likeService.toggleReviewLike("user@example.com", review.getId());

            // Then
            assertThat(result.liked()).isFalse();
            assertThat(result.likesCount()).isEqualTo(3);
            verify(likeRepository).delete(existingLike);
            verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("should throw ReviewNotFoundException when review does not exist")
        void toggleReviewLike_shouldThrowWhenReviewNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(reviewRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> likeService.toggleReviewLike("user@example.com", unknownId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleListLike")
    class ToggleListLike {

        @Test
        @DisplayName("should like when not already liked")
        void toggleListLike_shouldLikeWhenNotLiked() {
            // Given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(gameListRepository.findById(gameList.getId()))
                    .thenReturn(Optional.of(gameList));
            when(likeRepository.findByUserIdAndGameListId(user.getId(), gameList.getId()))
                    .thenReturn(Optional.empty());
            when(likeRepository.countByGameListId(gameList.getId()))
                    .thenReturn(7L);

            // When / Then
            try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                         Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
                searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                        .thenReturn(Mockito.mock(org.hibernate.search.mapper.orm.session.SearchSession.class,
                                Mockito.RETURNS_DEEP_STUBS));

                LikeResponseDto result = likeService.toggleListLike("user@example.com", gameList.getId());

                assertThat(result.liked()).isTrue();
                assertThat(result.likesCount()).isEqualTo(8);
                verify(likeRepository).save(any(Like.class));

                ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
                verify(eventPublisher).publishEvent(eventCaptor.capture());
                NotificationEvent event = eventCaptor.getValue();
                assertThat(event.getRecipientId()).isEqualTo(listOwner.getId());
                assertThat(event.getSenderId()).isEqualTo(user.getId());
                assertThat(event.getType()).isEqualTo(NotificationType.LIKE_LIST);
                assertThat(event.getReferenceId()).isEqualTo(gameList.getId());
                assertThat(event.getMessage()).contains("My Top Games");
            }
        }

        @Test
        @DisplayName("should unlike when already liked")
        void toggleListLike_shouldUnlikeWhenAlreadyLiked() {
            // Given
            Like existingLike = Like.forGameList(user, gameList);

            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(gameListRepository.findById(gameList.getId()))
                    .thenReturn(Optional.of(gameList));
            when(likeRepository.findByUserIdAndGameListId(user.getId(), gameList.getId()))
                    .thenReturn(Optional.of(existingLike));
            when(likeRepository.countByGameListId(gameList.getId()))
                    .thenReturn(8L);

            // When / Then
            try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                         Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
                searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                        .thenReturn(Mockito.mock(org.hibernate.search.mapper.orm.session.SearchSession.class,
                                Mockito.RETURNS_DEEP_STUBS));

                LikeResponseDto result = likeService.toggleListLike("user@example.com", gameList.getId());

                assertThat(result.liked()).isFalse();
                assertThat(result.likesCount()).isEqualTo(7);
                verify(likeRepository).delete(existingLike);
                verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
            }
        }

        @Test
        @DisplayName("should throw GameListNotFoundException when list does not exist")
        void toggleListLike_shouldThrowWhenListNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(gameListRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> likeService.toggleListLike("user@example.com", unknownId))
                    .isInstanceOf(GameListNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleCommentLike")
    class ToggleCommentLike {

        @Test
        @DisplayName("should like when not already liked")
        void toggleCommentLike_shouldLikeWhenNotLiked() {
            // Given
            Comment comment = new Comment();
            comment.setId(UUID.randomUUID());

            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(commentRepository.findById(comment.getId()))
                    .thenReturn(Optional.of(comment));
            when(likeRepository.findByUserIdAndCommentId(user.getId(), comment.getId()))
                    .thenReturn(Optional.empty());
            when(likeRepository.countByCommentId(comment.getId()))
                    .thenReturn(2L);

            // When
            LikeResponseDto result = likeService.toggleCommentLike("user@example.com", comment.getId());

            // Then
            assertThat(result.liked()).isTrue();
            assertThat(result.likesCount()).isEqualTo(3);
            verify(likeRepository).save(any(Like.class));
        }

        @Test
        @DisplayName("should unlike when already liked")
        void toggleCommentLike_shouldUnlikeWhenAlreadyLiked() {
            // Given
            Comment comment = new Comment();
            comment.setId(UUID.randomUUID());
            Like existingLike = Like.forComment(user, comment);

            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(commentRepository.findById(comment.getId()))
                    .thenReturn(Optional.of(comment));
            when(likeRepository.findByUserIdAndCommentId(user.getId(), comment.getId()))
                    .thenReturn(Optional.of(existingLike));
            when(likeRepository.countByCommentId(comment.getId()))
                    .thenReturn(3L);

            // When
            LikeResponseDto result = likeService.toggleCommentLike("user@example.com", comment.getId());

            // Then
            assertThat(result.liked()).isFalse();
            assertThat(result.likesCount()).isEqualTo(2);
            verify(likeRepository).delete(existingLike);
        }

        @Test
        @DisplayName("should throw CommentNotFoundException when comment does not exist")
        void toggleCommentLike_shouldThrowWhenCommentNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(commentRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> likeService.toggleCommentLike("user@example.com", unknownId))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleGameLike")
    class ToggleGameLike {

        @Test
        @DisplayName("should like when not already liked")
        void toggleGameLike_shouldLikeWhenNotLiked() {
            // Given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(videoGameRepository.findById(videoGame.getId()))
                    .thenReturn(Optional.of(videoGame));
            when(likeRepository.findByUserIdAndVideoGameId(user.getId(), videoGame.getId()))
                    .thenReturn(Optional.empty());
            when(likeRepository.countByVideoGameId(videoGame.getId()))
                    .thenReturn(5L);

            // When
            LikeResponseDto result = likeService.toggleGameLike("user@example.com", videoGame.getId());

            // Then
            assertThat(result.liked()).isTrue();
            assertThat(result.likesCount()).isEqualTo(6);
            verify(likeRepository).save(any(Like.class));
            verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("should unlike when already liked")
        void toggleGameLike_shouldUnlikeWhenAlreadyLiked() {
            // Given
            Like existingLike = Like.forVideoGame(user, videoGame);

            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(videoGameRepository.findById(videoGame.getId()))
                    .thenReturn(Optional.of(videoGame));
            when(likeRepository.findByUserIdAndVideoGameId(user.getId(), videoGame.getId()))
                    .thenReturn(Optional.of(existingLike));
            when(likeRepository.countByVideoGameId(videoGame.getId()))
                    .thenReturn(6L);

            // When
            LikeResponseDto result = likeService.toggleGameLike("user@example.com", videoGame.getId());

            // Then
            assertThat(result.liked()).isFalse();
            assertThat(result.likesCount()).isEqualTo(5);
            verify(likeRepository).delete(existingLike);
        }

        @Test
        @DisplayName("should throw GameNotFoundException when game does not exist")
        void toggleGameLike_shouldThrowWhenGameNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(videoGameRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> likeService.toggleGameLike("user@example.com", unknownId))
                    .isInstanceOf(GameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getLikedGames")
    class GetLikedGames {

        @Test
        @DisplayName("should return the user's liked games mapped to DTOs")
        void getLikedGames_shouldReturnMappedPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Like like = Like.forVideoGame(user, videoGame);
            like.setId(UUID.randomUUID());
            Page<Like> likePage = new PageImpl<>(List.of(like), pageable, 1);

            LikedGameResponseDto dto = new LikedGameResponseDto(
                    like.getId(), videoGame.getId(), videoGame.getTitle(),
                    null, null, LocalDateTime.now());

            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(likeRepository.findGameLikesByUserId(user.getId(), pageable))
                    .thenReturn(likePage);
            when(likedGameMapper.toResponseDto(like)).thenReturn(dto);

            // When
            Page<LikedGameResponseDto> result = likeService.getLikedGames("user@example.com", pageable);

            // Then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).containsExactly(dto);
            verify(likeRepository).findGameLikesByUserId(user.getId(), pageable);
        }

        @Test
        @DisplayName("should return an empty page when the user has no liked games")
        void getLikedGames_shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(user));
            when(likeRepository.findGameLikesByUserId(user.getId(), pageable))
                    .thenReturn(Page.empty(pageable));

            // When
            Page<LikedGameResponseDto> result = likeService.getLikedGames("user@example.com", pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }
}
