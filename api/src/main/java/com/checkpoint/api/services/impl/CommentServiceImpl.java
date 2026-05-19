package com.checkpoint.api.services.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.social.CommentResponseDto;
import com.checkpoint.api.entities.Comment;
import com.checkpoint.api.entities.GameList;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.events.CommentReplyEvent;
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
import com.checkpoint.api.services.CommentService;

/**
 * Implementation of {@link CommentService}.
 * Manages CRUD operations for comments on reviews and game lists,
 * including nested replies and like enrichment.
 */
@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final GameListRepository gameListRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CommentMapper commentMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs a new CommentServiceImpl.
     *
     * @param commentRepository  the comment repository
     * @param reviewRepository   the review repository
     * @param gameListRepository the game list repository
     * @param userRepository     the user repository
     * @param likeRepository     the like repository
     * @param commentMapper      the comment mapper
     * @param eventPublisher     the application event publisher
     */
    public CommentServiceImpl(CommentRepository commentRepository,
                              ReviewRepository reviewRepository,
                              GameListRepository gameListRepository,
                              UserRepository userRepository,
                              LikeRepository likeRepository,
                              CommentMapper commentMapper,
                              ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.reviewRepository = reviewRepository;
        this.gameListRepository = gameListRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.commentMapper = commentMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommentResponseDto addReviewComment(String userEmail, UUID reviewId, String content) {
        User user = getUserByEmail(userEmail);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));

        Comment comment = Comment.onReview(content, user, review);
        Comment savedComment = commentRepository.save(comment);

        log.info("User {} commented on review {}", user.getPseudo(), reviewId);

        return commentMapper.toDto(savedComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommentResponseDto addListComment(String userEmail, UUID listId, String content) {
        User user = getUserByEmail(userEmail);

        GameList gameList = gameListRepository.findById(listId)
                .orElseThrow(() -> new GameListNotFoundException(listId));

        Comment comment = Comment.onList(content, user, gameList);
        Comment savedComment = commentRepository.save(comment);

        log.info("User {} commented on list {}", user.getPseudo(), listId);

        return commentMapper.toDto(savedComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getReviewComments(UUID reviewId, String viewerEmail, Pageable pageable) {
        User viewer = resolveViewer(viewerEmail);
        Page<Comment> comments = commentRepository.findByReviewIdAndParentCommentIsNull(reviewId, pageable);
        return comments.map(comment -> enrichComment(comment, viewer));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getListComments(UUID listId, String viewerEmail, Pageable pageable) {
        User viewer = resolveViewer(viewerEmail);
        Page<Comment> comments = commentRepository.findByGameListIdAndParentCommentIsNull(listId, pageable);
        return comments.map(comment -> enrichComment(comment, viewer));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommentResponseDto addReply(String userEmail, UUID parentCommentId, String content) {
        User user = getUserByEmail(userEmail);

        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new CommentNotFoundException(parentCommentId));

        Comment reply = Comment.asReply(content, user, parentComment);
        Comment savedReply = commentRepository.save(reply);

        log.info("User {} replied to comment {}", user.getPseudo(), parentCommentId);

        Comment effectiveParent = savedReply.getParentComment();
        UUID effectiveParentId = effectiveParent.getId();
        UUID effectiveParentAuthorId = effectiveParent.getUser().getId();

        if (!effectiveParentAuthorId.equals(user.getId())) {
            eventPublisher.publishEvent(new CommentReplyEvent(
                    user.getId(), effectiveParentAuthorId, effectiveParentId));
        }

        String message = user.getPseudo() + " replied to your comment";
        eventPublisher.publishEvent(new NotificationEvent(
                parentComment.getUser().getId(), user.getId(),
                NotificationType.COMMENT_REPLY, parentCommentId, message));

        return commentMapper.toDto(savedReply);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getReplies(UUID parentCommentId, String viewerEmail, Pageable pageable) {
        User viewer = resolveViewer(viewerEmail);
        Page<Comment> replies = commentRepository.findByParentCommentId(parentCommentId, pageable);
        return replies.map(reply -> enrichComment(reply, viewer));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommentResponseDto updateComment(String userEmail, UUID commentId, String content) {
        User user = getUserByEmail(userEmail);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedCommentAccessException(commentId);
        }

        comment.setContent(content);
        Comment updatedComment = commentRepository.save(comment);

        log.info("User {} updated comment {}", user.getPseudo(), commentId);

        return commentMapper.toDto(updatedComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteComment(String userEmail, UUID commentId) {
        User user = getUserByEmail(userEmail);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedCommentAccessException(commentId);
        }

        commentRepository.delete(comment);

        log.info("User {} deleted comment {}", user.getPseudo(), commentId);
    }

    /**
     * Enriches a comment entity with like count, reply count, and viewer's like status.
     *
     * @param comment the comment entity
     * @param viewer  the current viewer (nullable)
     * @return the enriched comment response DTO
     */
    private CommentResponseDto enrichComment(Comment comment, User viewer) {
        long likesCount = likeRepository.countByCommentId(comment.getId());
        long repliesCount = commentRepository.countByParentCommentId(comment.getId());
        boolean hasLiked = viewer != null
                && likeRepository.existsByUserIdAndCommentId(viewer.getId(), comment.getId());
        return commentMapper.toDto(comment, likesCount, hasLiked, repliesCount);
    }

    /**
     * Resolves a viewer from their email. Returns null if email is null or user not found.
     *
     * @param viewerEmail the viewer's email (nullable)
     * @return the user entity, or null
     */
    private User resolveViewer(String viewerEmail) {
        if (viewerEmail == null) {
            return null;
        }
        return userRepository.findByEmail(viewerEmail).orElse(null);
    }

    /**
     * Retrieves a user by email.
     *
     * @param email the user's email
     * @return the user entity
     * @throws IllegalArgumentException if the user is not found
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
