package com.checkpoint.api.services.impl;

import java.util.Optional;
import java.util.UUID;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.checkpoint.api.events.ReviewLikedEvent;
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
import com.checkpoint.api.services.LikeService;

/**
 * Implementation of {@link LikeService}.
 * Manages like/unlike toggle operations on reviews, game lists, and comments.
 */
@Service
@Transactional
public class LikeServiceImpl implements LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeServiceImpl.class);

    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final GameListRepository gameListRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final VideoGameRepository videoGameRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final LikedGameMapper likedGameMapper;

    /**
     * Constructs a new LikeServiceImpl.
     *
     * @param likeRepository      the like repository
     * @param reviewRepository    the review repository
     * @param gameListRepository  the game list repository
     * @param commentRepository   the comment repository
     * @param userRepository      the user repository
     * @param videoGameRepository the video game repository
     * @param eventPublisher      the application event publisher
     * @param entityManager       the JPA entity manager (used to refresh search index)
     * @param likedGameMapper     the liked-game mapper
     */
    public LikeServiceImpl(LikeRepository likeRepository,
                           ReviewRepository reviewRepository,
                           GameListRepository gameListRepository,
                           CommentRepository commentRepository,
                           UserRepository userRepository,
                           VideoGameRepository videoGameRepository,
                           ApplicationEventPublisher eventPublisher,
                           EntityManager entityManager,
                           LikedGameMapper likedGameMapper) {
        this.likeRepository = likeRepository;
        this.reviewRepository = reviewRepository;
        this.gameListRepository = gameListRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.videoGameRepository = videoGameRepository;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.likedGameMapper = likedGameMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LikeResponseDto toggleReviewLike(String userEmail, UUID reviewId) {
        User user = getUserByEmail(userEmail);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));

        Optional<Like> existingLike = likeRepository.findByUserIdAndReviewId(user.getId(), reviewId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            long likesCount = likeRepository.countByReviewId(reviewId) - 1;
            log.info("User {} unliked review {}", user.getPseudo(), reviewId);
            return new LikeResponseDto(false, Math.max(0, likesCount));
        } else {
            Like like = Like.forReview(user, review);
            Like savedLike = likeRepository.save(like);
            long likesCount = likeRepository.countByReviewId(reviewId) + 1;
            log.info("User {} liked review {}", user.getPseudo(), reviewId);

            UUID reviewAuthorId = review.getUser().getId();
            if (!reviewAuthorId.equals(user.getId())) {
                eventPublisher.publishEvent(new ReviewLikedEvent(
                        user.getId(), reviewAuthorId, reviewId, savedLike.getId()));
            }

            String message = user.getPseudo() + " liked your review of " + review.getVideoGame().getTitle();
            eventPublisher.publishEvent(new NotificationEvent(
                    reviewAuthorId, user.getId(),
                    NotificationType.LIKE_REVIEW, reviewId, message));

            return new LikeResponseDto(true, likesCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LikeResponseDto toggleListLike(String userEmail, UUID listId) {
        User user = getUserByEmail(userEmail);

        GameList gameList = gameListRepository.findById(listId)
                .orElseThrow(() -> new GameListNotFoundException(listId));

        Optional<Like> existingLike = likeRepository.findByUserIdAndGameListId(user.getId(), listId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            long likesCount = likeRepository.countByGameListId(listId) - 1;
            refreshListSearchIndex(gameList);
            log.info("User {} unliked list {}", user.getPseudo(), listId);
            return new LikeResponseDto(false, Math.max(0, likesCount));
        } else {
            Like like = Like.forGameList(user, gameList);
            likeRepository.save(like);
            long likesCount = likeRepository.countByGameListId(listId) + 1;
            refreshListSearchIndex(gameList);
            log.info("User {} liked list {}", user.getPseudo(), listId);

            String message = user.getPseudo() + " liked your list \"" + gameList.getTitle() + "\"";
            eventPublisher.publishEvent(new NotificationEvent(
                    gameList.getUser().getId(), user.getId(),
                    NotificationType.LIKE_LIST, listId, message));

            return new LikeResponseDto(true, likesCount);
        }
    }

    /**
     * Refreshes the Hibernate Search index for the given list after a like toggle.
     * {@code GameList.likesCount} is a {@code @Formula} field indexed with
     * {@code reindexOnUpdate = NO}, so it must be re-indexed manually. {@code refresh()}
     * forces the formula to recompute before indexing.
     */
    private void refreshListSearchIndex(GameList gameList) {
        entityManager.refresh(gameList);
        SearchSession session = Search.session(entityManager);
        session.indexingPlan().addOrUpdate(gameList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LikeResponseDto toggleCommentLike(String userEmail, UUID commentId) {
        User user = getUserByEmail(userEmail);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        Optional<Like> existingLike = likeRepository.findByUserIdAndCommentId(user.getId(), commentId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            long likesCount = likeRepository.countByCommentId(commentId) - 1;
            log.info("User {} unliked comment {}", user.getPseudo(), commentId);
            return new LikeResponseDto(false, Math.max(0, likesCount));
        } else {
            Like like = Like.forComment(user, comment);
            likeRepository.save(like);
            long likesCount = likeRepository.countByCommentId(commentId) + 1;
            log.info("User {} liked comment {}", user.getPseudo(), commentId);
            return new LikeResponseDto(true, likesCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LikeResponseDto toggleGameLike(String userEmail, UUID videoGameId) {
        User user = getUserByEmail(userEmail);

        VideoGame videoGame = videoGameRepository.findById(videoGameId)
                .orElseThrow(() -> new GameNotFoundException(videoGameId));

        Optional<Like> existingLike = likeRepository.findByUserIdAndVideoGameId(user.getId(), videoGameId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            long likesCount = likeRepository.countByVideoGameId(videoGameId) - 1;
            log.info("User {} unliked game {}", user.getPseudo(), videoGameId);
            return new LikeResponseDto(false, Math.max(0, likesCount));
        } else {
            Like like = Like.forVideoGame(user, videoGame);
            likeRepository.save(like);
            long likesCount = likeRepository.countByVideoGameId(videoGameId) + 1;
            log.info("User {} liked game {}", user.getPseudo(), videoGameId);
            return new LikeResponseDto(true, likesCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LikedGameResponseDto> getLikedGames(String userEmail, Pageable pageable) {
        log.debug("Fetching liked games for user {} - page: {}, size: {}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());

        User user = getUserByEmail(userEmail);

        return likeRepository.findGameLikesByUserId(user.getId(), pageable)
                .map(likedGameMapper::toResponseDto);
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
