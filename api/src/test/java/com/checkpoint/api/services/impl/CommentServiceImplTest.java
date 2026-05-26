package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.checkpoint.api.dto.social.CommentResponseDto;
import com.checkpoint.api.dto.social.CommentUserDto;
import com.checkpoint.api.entities.Comment;
import com.checkpoint.api.entities.GameList;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.exceptions.CommentNotFoundException;
import com.checkpoint.api.exceptions.GameListNotFoundException;
import com.checkpoint.api.exceptions.ReviewNotFoundException;
import com.checkpoint.api.exceptions.UnauthorizedCommentAccessException;
import com.checkpoint.api.mapper.CommentMapper;
import com.checkpoint.api.repositories.CommentRepository;
import com.checkpoint.api.repositories.GameListRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Unit tests for {@link CommentServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private GameListRepository gameListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CommentServiceImpl commentService;

    private User testUser;
    private User otherUser;
    private Review testReview;
    private GameList testList;

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(
                commentRepository, reviewRepository, gameListRepository,
                userRepository, likeRepository, commentMapper, eventPublisher);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setPseudo("testuser");
        testUser.setEmail("test@test.com");

        otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setPseudo("otheruser");
        otherUser.setEmail("other@test.com");

        testReview = new Review();
        testReview.setId(UUID.randomUUID());

        testList = new GameList("Test List", testUser);
        testList.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("addReviewComment()")
    class AddReviewComment {

        @Test
        @DisplayName("should create a comment on a review")
        void addReviewComment_shouldCreateComment() {
            // Given
            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));

            Comment savedComment = Comment.onReview("Nice!", testUser, testReview);
            savedComment.setId(UUID.randomUUID());
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

            CommentResponseDto expectedDto = new CommentResponseDto(
                    savedComment.getId(), "Nice!",
                    new CommentUserDto(testUser.getId(), testUser.getPseudo(), null),
                    null, null, null, 0, 0, false);
            when(commentMapper.toDto(savedComment)).thenReturn(expectedDto);

            // When
            CommentResponseDto result = commentService.addReviewComment("test@test.com", testReview.getId(), "Nice!");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("Nice!");
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("should throw ReviewNotFoundException when review does not exist")
        void addReviewComment_shouldThrowWhenReviewNotFound() {
            // Given
            UUID reviewId = UUID.randomUUID();
            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> commentService.addReviewComment("test@test.com", reviewId, "Nice!"))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addListComment()")
    class AddListComment {

        @Test
        @DisplayName("should create a comment on a list")
        void addListComment_shouldCreateComment() {
            // Given
            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(testList.getId())).thenReturn(Optional.of(testList));

            Comment savedComment = Comment.onList("Cool list!", testUser, testList);
            savedComment.setId(UUID.randomUUID());
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

            CommentResponseDto expectedDto = new CommentResponseDto(
                    savedComment.getId(), "Cool list!",
                    new CommentUserDto(testUser.getId(), testUser.getPseudo(), null),
                    null, null, null, 0, 0, false);
            when(commentMapper.toDto(savedComment)).thenReturn(expectedDto);

            // When
            CommentResponseDto result = commentService.addListComment("test@test.com", testList.getId(), "Cool list!");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("Cool list!");
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("should throw GameListNotFoundException when list does not exist")
        void addListComment_shouldThrowWhenListNotFound() {
            // Given
            UUID listId = UUID.randomUUID();
            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(gameListRepository.findById(listId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> commentService.addListComment("test@test.com", listId, "Nice!"))
                    .isInstanceOf(GameListNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getReviewComments()")
    class GetReviewComments {

        @Test
        @DisplayName("should return paginated top-level comments for a review")
        void getReviewComments_shouldReturnPaginatedComments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Comment comment = Comment.onReview("Great!", testUser, testReview);
            comment.setId(UUID.randomUUID());
            Page<Comment> commentPage = new PageImpl<>(List.of(comment));

            when(commentRepository.findByReviewIdAndParentCommentIsNull(testReview.getId(), pageable))
                    .thenReturn(commentPage);
            when(likeRepository.countByCommentId(comment.getId())).thenReturn(3L);
            when(commentRepository.countByParentCommentId(comment.getId())).thenReturn(2L);

            CommentResponseDto dto = new CommentResponseDto(
                    comment.getId(), "Great!",
                    new CommentUserDto(testUser.getId(), testUser.getPseudo(), null),
                    null, null, null, 2, 3, false);
            when(commentMapper.toDto(comment, 3L, false, 2L)).thenReturn(dto);

            // When
            Page<CommentResponseDto> result = commentService.getReviewComments(testReview.getId(), null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).content()).isEqualTo("Great!");
            assertThat(result.getContent().get(0).likesCount()).isEqualTo(3);
            assertThat(result.getContent().get(0).repliesCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should resolve hasLiked when viewer is authenticated")
        void getReviewComments_shouldResolveHasLikedForViewer() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Comment comment = Comment.onReview("Great!", testUser, testReview);
            comment.setId(UUID.randomUUID());
            Page<Comment> commentPage = new PageImpl<>(List.of(comment));

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(commentRepository.findByReviewIdAndParentCommentIsNull(testReview.getId(), pageable))
                    .thenReturn(commentPage);
            when(likeRepository.countByCommentId(comment.getId())).thenReturn(1L);
            when(commentRepository.countByParentCommentId(comment.getId())).thenReturn(0L);
            when(likeRepository.existsByUserIdAndCommentId(testUser.getId(), comment.getId())).thenReturn(true);

            CommentResponseDto dto = new CommentResponseDto(
                    comment.getId(), "Great!",
                    new CommentUserDto(testUser.getId(), testUser.getPseudo(), null),
                    null, null, null, 0, 1, true);
            when(commentMapper.toDto(comment, 1L, true, 0L)).thenReturn(dto);

            // When
            Page<CommentResponseDto> result = commentService.getReviewComments(
                    testReview.getId(), "test@test.com", pageable);

            // Then
            assertThat(result.getContent().get(0).hasLiked()).isTrue();
        }
    }

    @Nested
    @DisplayName("addReply()")
    class AddReply {

        @Test
        @DisplayName("should create a reply on a comment and publish notification")
        void addReply_shouldCreateReplyAndPublishNotification() {
            // Given
            Comment parentComment = Comment.onReview("Parent", otherUser, testReview);
            parentComment.setId(UUID.randomUUID());

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(parentComment.getId())).thenReturn(Optional.of(parentComment));

            Comment savedReply = Comment.asReply("Reply!", testUser, parentComment);
            savedReply.setId(UUID.randomUUID());
            when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);

            CommentResponseDto expectedDto = new CommentResponseDto(
                    savedReply.getId(), "Reply!",
                    new CommentUserDto(testUser.getId(), testUser.getPseudo(), null),
                    null, null, parentComment.getId(), 0, 0, false);
            when(commentMapper.toDto(savedReply)).thenReturn(expectedDto);

            // When
            CommentResponseDto result = commentService.addReply("test@test.com", parentComment.getId(), "Reply!");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("Reply!");
            assertThat(result.parentCommentId()).isEqualTo(parentComment.getId());
            verify(commentRepository).save(any(Comment.class));

            ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            NotificationEvent event = eventCaptor.getValue();
            assertThat(event.getRecipientId()).isEqualTo(otherUser.getId());
            assertThat(event.getSenderId()).isEqualTo(testUser.getId());
            assertThat(event.getType()).isEqualTo(NotificationType.COMMENT_REPLY);
            assertThat(event.getReferenceId()).isEqualTo(parentComment.getId());
            assertThat(event.getMessage()).contains("testuser");
        }

        @Test
        @DisplayName("should throw CommentNotFoundException when parent does not exist")
        void addReply_shouldThrowWhenParentNotFound() {
            // Given
            UUID commentId = UUID.randomUUID();
            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> commentService.addReply("test@test.com", commentId, "Reply"))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getReplies()")
    class GetReplies {

        @Test
        @DisplayName("should return paginated replies for a parent comment")
        void getReplies_shouldReturnPaginatedReplies() {
            // Given
            UUID parentId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            Comment reply = Comment.onReview("Reply", testUser, testReview);
            reply.setId(UUID.randomUUID());
            Page<Comment> replyPage = new PageImpl<>(List.of(reply));

            when(commentRepository.findByParentCommentId(parentId, pageable)).thenReturn(replyPage);
            when(likeRepository.countByCommentId(reply.getId())).thenReturn(1L);
            when(commentRepository.countByParentCommentId(reply.getId())).thenReturn(0L);

            CommentResponseDto dto = new CommentResponseDto(
                    reply.getId(), "Reply",
                    new CommentUserDto(testUser.getId(), testUser.getPseudo(), null),
                    null, null, parentId, 0, 1, false);
            when(commentMapper.toDto(reply, 1L, false, 0L)).thenReturn(dto);

            // When
            Page<CommentResponseDto> result = commentService.getReplies(parentId, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).content()).isEqualTo("Reply");
        }
    }

    @Nested
    @DisplayName("updateComment()")
    class UpdateComment {

        @Test
        @DisplayName("should update comment when user is owner")
        void updateComment_shouldUpdateWhenOwner() {
            // Given
            Comment comment = Comment.onReview("Old content", testUser, testReview);
            comment.setId(UUID.randomUUID());

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
            when(commentRepository.save(comment)).thenReturn(comment);

            CommentResponseDto expectedDto = new CommentResponseDto(
                    comment.getId(), "New content",
                    new CommentUserDto(testUser.getId(), testUser.getPseudo(), null),
                    null, null, null, 0, 0, false);
            when(commentMapper.toDto(comment)).thenReturn(expectedDto);

            // When
            CommentResponseDto result = commentService.updateComment("test@test.com", comment.getId(), "New content");

            // Then
            assertThat(result.content()).isEqualTo("New content");
            verify(commentRepository).save(comment);
        }

        @Test
        @DisplayName("should throw UnauthorizedCommentAccessException when user is not owner")
        void updateComment_shouldThrowWhenNotOwner() {
            // Given
            Comment comment = Comment.onReview("Content", otherUser, testReview);
            comment.setId(UUID.randomUUID());

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

            // When / Then
            assertThatThrownBy(() -> commentService.updateComment("test@test.com", comment.getId(), "Hacked"))
                    .isInstanceOf(UnauthorizedCommentAccessException.class);
        }

        @Test
        @DisplayName("should throw CommentNotFoundException when comment does not exist")
        void updateComment_shouldThrowWhenNotFound() {
            // Given
            UUID commentId = UUID.randomUUID();
            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> commentService.updateComment("test@test.com", commentId, "Updated"))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteComment()")
    class DeleteComment {

        @Test
        @DisplayName("should delete comment when user is owner")
        void deleteComment_shouldDeleteWhenOwner() {
            // Given
            Comment comment = Comment.onReview("Content", testUser, testReview);
            comment.setId(UUID.randomUUID());

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

            // When
            commentService.deleteComment("test@test.com", comment.getId());

            // Then
            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("should throw UnauthorizedCommentAccessException when user is not owner")
        void deleteComment_shouldThrowWhenNotOwner() {
            // Given
            Comment comment = Comment.onReview("Content", otherUser, testReview);
            comment.setId(UUID.randomUUID());

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

            // When / Then
            assertThatThrownBy(() -> commentService.deleteComment("test@test.com", comment.getId()))
                    .isInstanceOf(UnauthorizedCommentAccessException.class);
        }
    }

    @Nested
    @DisplayName("mention notifications")
    class MentionNotifications {

        private Comment stubSavedReviewComment(String content) {
            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
            when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));

            Comment savedComment = Comment.onReview(content, testUser, testReview);
            savedComment.setId(UUID.randomUUID());
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

            CommentResponseDto dto = new CommentResponseDto(
                    savedComment.getId(), content,
                    new CommentUserDto(testUser.getId(), testUser.getPseudo(), null),
                    null, null, null, 0, 0, false);
            when(commentMapper.toDto(savedComment)).thenReturn(dto);
            return savedComment;
        }

        @Test
        @DisplayName("should dispatch a MENTION notification for a valid mention")
        void shouldDispatchMentionNotification() {
            // Given
            Comment savedComment = stubSavedReviewComment("Hey @otheruser nice review");
            when(userRepository.findByPseudo("otheruser")).thenReturn(Optional.of(otherUser));

            // When
            commentService.addReviewComment("test@test.com", testReview.getId(), "Hey @otheruser nice review");

            // Then
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            NotificationEvent event = captor.getValue();
            assertThat(event.getRecipientId()).isEqualTo(otherUser.getId());
            assertThat(event.getSenderId()).isEqualTo(testUser.getId());
            assertThat(event.getType()).isEqualTo(NotificationType.MENTION);
            assertThat(event.getReferenceId()).isEqualTo(savedComment.getId());
            assertThat(event.getMessage()).contains("@otheruser");
        }

        @Test
        @DisplayName("should not notify when a user mentions themselves")
        void shouldNotNotifyOnSelfMention() {
            // Given
            stubSavedReviewComment("Note to self @testuser");
            when(userRepository.findByPseudo("testuser")).thenReturn(Optional.of(testUser));

            // When
            commentService.addReviewComment("test@test.com", testReview.getId(), "Note to self @testuser");

            // Then
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should silently ignore mentions of an unknown pseudo")
        void shouldIgnoreUnknownPseudo() {
            // Given
            stubSavedReviewComment("Hello @ghost");
            when(userRepository.findByPseudo("ghost")).thenReturn(Optional.empty());

            // When
            commentService.addReviewComment("test@test.com", testReview.getId(), "Hello @ghost");

            // Then
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should send only one notification for a duplicate mention")
        void shouldDeduplicateMentions() {
            // Given
            stubSavedReviewComment("@otheruser @otheruser hi");
            when(userRepository.findByPseudo("otheruser")).thenReturn(Optional.of(otherUser));

            // When
            commentService.addReviewComment("test@test.com", testReview.getId(), "@otheruser @otheruser hi");

            // Then
            verify(userRepository, times(1)).findByPseudo("otheruser");
            verify(eventPublisher, times(1)).publishEvent(any(NotificationEvent.class));
        }
    }
}
