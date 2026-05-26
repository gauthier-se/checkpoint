package com.checkpoint.api.services.impl;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.ReviewCardDto;
import com.checkpoint.api.dto.catalog.ReviewRequestDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.events.ReviewCreatedEvent;
import com.checkpoint.api.events.UserActivityEvent;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.PlayLogNotFoundException;
import com.checkpoint.api.exceptions.ReviewAlreadyExistsException;
import com.checkpoint.api.exceptions.ReviewNotFoundException;
import com.checkpoint.api.mapper.ReviewMapper;
import com.checkpoint.api.repositories.CommentRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.ReviewService;
import com.checkpoint.api.utils.MentionParser;

/**
 * Implementation of {@link ReviewService}.
 * Manages game reviews tied to play log entries.
 */
@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final VideoGameRepository videoGameRepository;
    private final UserRepository userRepository;
    private final UserGamePlayRepository userGamePlayRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ReviewMapper reviewMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs a new ReviewServiceImpl.
     *
     * @param reviewRepository       the review repository
     * @param videoGameRepository    the video game repository
     * @param userRepository         the user repository
     * @param userGamePlayRepository the play log repository
     * @param likeRepository         the like repository
     * @param commentRepository      the comment repository
     * @param reviewMapper           the review mapper
     * @param eventPublisher         the application event publisher
     */
    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             VideoGameRepository videoGameRepository,
                             UserRepository userRepository,
                             UserGamePlayRepository userGamePlayRepository,
                             LikeRepository likeRepository,
                             CommentRepository commentRepository,
                             ReviewMapper reviewMapper,
                             ApplicationEventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.videoGameRepository = videoGameRepository;
        this.userRepository = userRepository;
        this.userGamePlayRepository = userGamePlayRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.reviewMapper = reviewMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getGameReviews(UUID videoGameId, String viewerEmail, Pageable pageable) {
        if (!videoGameRepository.existsById(videoGameId)) {
            throw new GameNotFoundException(videoGameId);
        }

        User viewer = null;
        if (viewerEmail != null) {
            viewer = userRepository.findByEmail(viewerEmail).orElse(null);
        }

        User resolvedViewer = viewer;
        Page<Review> reviews = reviewRepository.findByVideoGameId(videoGameId, pageable);

        return reviews.map(review -> {
            long likesCount = likeRepository.countByReviewId(review.getId());
            boolean hasLiked = resolvedViewer != null
                    && likeRepository.existsByUserIdAndReviewId(resolvedViewer.getId(), review.getId());
            long commentsCount = commentRepository.countByReviewId(review.getId());
            return reviewMapper.toDto(review, likesCount, hasLiked, commentsCount);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReviewResponseDto createPlayLogReview(String userEmail, UUID playId, ReviewRequestDto request) {
        User user = getUserByEmail(userEmail);
        UserGamePlay playLog = getPlayLogOwnedByUser(playId, user);

        if (reviewRepository.existsByUserGamePlayId(playId)) {
            throw new ReviewAlreadyExistsException(playId);
        }

        Review review = new Review(
                request.content(),
                request.haveSpoilers(),
                user,
                playLog.getVideoGame(),
                playLog
        );

        Review savedReview = reviewRepository.save(review);
        log.info("Created review for play log {} by user {}", playId, user.getPseudo());

        eventPublisher.publishEvent(new ReviewCreatedEvent(user.getId()));
        eventPublisher.publishEvent(new UserActivityEvent(user.getId()));

        dispatchMentionNotifications(request.content(), user.getId(), savedReview.getId());

        return reviewMapper.toDto(savedReview);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReviewResponseDto updatePlayLogReview(String userEmail, UUID playId, ReviewRequestDto request) {
        User user = getUserByEmail(userEmail);
        getPlayLogOwnedByUser(playId, user);

        Review review = reviewRepository.findByUserGamePlayId(playId)
                .orElseThrow(() -> new ReviewNotFoundException(playId));

        review.setContent(request.content());
        review.setHaveSpoilers(request.haveSpoilers());
        Review savedReview = reviewRepository.save(review);
        log.info("Updated review for play log {} by user {}", playId, user.getPseudo());

        eventPublisher.publishEvent(new UserActivityEvent(user.getId()));

        dispatchMentionNotifications(request.content(), user.getId(), savedReview.getId());

        return reviewMapper.toDto(savedReview);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deletePlayLogReview(String userEmail, UUID playId) {
        User user = getUserByEmail(userEmail);
        getPlayLogOwnedByUser(playId, user);

        Review review = reviewRepository.findByUserGamePlayId(playId)
                .orElseThrow(() -> new ReviewNotFoundException(playId));

        reviewRepository.delete(review);
        log.info("Deleted review for play log {} by user {}", playId, user.getPseudo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getPlayLogReview(String userEmail, UUID playId) {
        User user = getUserByEmail(userEmail);
        getPlayLogOwnedByUser(playId, user);

        Review review = reviewRepository.findByUserGamePlayId(playId)
                .orElseThrow(() -> new ReviewNotFoundException(playId));

        return reviewMapper.toDto(review);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewCardDto> getPopularReviews(int size, String viewerEmail) {
        User viewer = resolveViewer(viewerEmail);
        List<Review> reviews = reviewRepository.findPopularReviews(size);
        return reviews.stream()
                .map(review -> toCardDto(review, viewer))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewCardDto> getRecentReviews(int size, String viewerEmail) {
        User viewer = resolveViewer(viewerEmail);
        Page<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size));
        return reviews.getContent().stream()
                .map(review -> toCardDto(review, viewer))
                .toList();
    }

    /**
     * Parses {@code @username} mentions from the given content and publishes a
     * {@link NotificationType#MENTION} notification for each mentioned user that exists
     * and is not the author. Unknown pseudos are silently ignored, and duplicate
     * mentions of the same user produce a single notification (deduplicated by the
     * {@link Set} of pseudos and, ultimately, by the notification service).
     *
     * @param content     the review content to scan for mentions
     * @param authorId    the review author's ID (never notified about their own mention)
     * @param referenceId the saved review's ID, used as the notification reference
     */
    private void dispatchMentionNotifications(String content, UUID authorId, UUID referenceId) {
        Set<String> mentioned = MentionParser.extractMentions(content);
        for (String pseudo : mentioned) {
            userRepository.findByPseudo(pseudo).ifPresent(target -> {
                if (!target.getId().equals(authorId)) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            target.getId(), authorId, NotificationType.MENTION,
                            referenceId, "@" + pseudo + " mentioned you in a review"));
                }
            });
        }
    }

    /**
     * Resolves a viewer user from an optional email.
     *
     * @param viewerEmail the viewer's email, or null
     * @return the viewer entity, or null if no email or no matching user
     */
    private User resolveViewer(String viewerEmail) {
        if (viewerEmail == null) {
            return null;
        }
        return userRepository.findByEmail(viewerEmail).orElse(null);
    }

    /**
     * Maps a review to a card DTO with likes/comments context for the given viewer.
     *
     * @param review the review entity
     * @param viewer the viewer (may be null for anonymous requests)
     * @return the review card DTO
     */
    private ReviewCardDto toCardDto(Review review, User viewer) {
        long likesCount = likeRepository.countByReviewId(review.getId());
        boolean hasLiked = viewer != null
                && likeRepository.existsByUserIdAndReviewId(viewer.getId(), review.getId());
        long commentsCount = commentRepository.countByReviewId(review.getId());
        return reviewMapper.toCardDto(review, likesCount, hasLiked, commentsCount);
    }

    /**
     * Retrieves a user by email.
     *
     * @param email the user's email
     * @return the user entity
     * @throws UsernameNotFoundException if the user is not found
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    /**
     * Retrieves a play log and verifies it belongs to the given user.
     *
     * @param playId the play log ID
     * @param user   the user who should own the play log
     * @return the play log entity
     * @throws PlayLogNotFoundException if the play log does not exist or does not belong to the user
     */
    private UserGamePlay getPlayLogOwnedByUser(UUID playId, User user) {
        UserGamePlay playLog = userGamePlayRepository.findById(playId)
                .orElseThrow(() -> new PlayLogNotFoundException("Play log not found with ID: " + playId));

        if (!playLog.getUser().getId().equals(user.getId())) {
            throw new PlayLogNotFoundException("Play log not found with ID: " + playId);
        }

        return playLog;
    }
}
