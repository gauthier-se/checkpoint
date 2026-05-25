package com.checkpoint.api.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.Like;

/**
 * Repository for Like entity.
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    /**
     * Counts the number of likes for a game list.
     */
    long countByGameListId(UUID gameListId);

    /**
     * Checks if a user has liked a game list.
     */
    boolean existsByUserIdAndGameListId(UUID userId, UUID gameListId);

    /**
     * Finds a like by user and game list.
     */
    Optional<Like> findByUserIdAndGameListId(UUID userId, UUID gameListId);

    /**
     * Counts the number of likes for a review.
     */
    long countByReviewId(UUID reviewId);

    /**
     * Checks if a user has liked a review.
     */
    boolean existsByUserIdAndReviewId(UUID userId, UUID reviewId);

    /**
     * Finds a like by user and review.
     */
    Optional<Like> findByUserIdAndReviewId(UUID userId, UUID reviewId);

    /**
     * Checks if a user has liked a specific video game (top-level like).
     */
    boolean existsByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Finds a user's like on a specific video game (top-level like).
     * Used by the like/unlike toggle to locate the row to remove.
     */
    Optional<Like> findByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Returns the subset of {@code videoGameIds} that the given user has liked.
     * Batched single-query lookup used to populate the {@code isLiked} flag for
     * a list of plays without triggering N+1.
     *
     * @param userId       the user ID
     * @param videoGameIds the candidate video game IDs (must be non-empty)
     */
    @Query("""
            SELECT l.videoGame.id FROM Like l
            WHERE l.user.id = :userId
              AND l.videoGame.id IN :videoGameIds
            """)
    List<UUID> findVideoGameIdsLikedByUser(@Param("userId") UUID userId,
                                            @Param("videoGameIds") Collection<UUID> videoGameIds);

    /**
     * Counts the number of likes for a comment.
     */
    long countByCommentId(UUID commentId);

    /**
     * Checks if a user has liked a comment.
     */
    boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

    /**
     * Finds a like by user and comment.
     */
    Optional<Like> findByUserIdAndCommentId(UUID userId, UUID commentId);

    /**
     * Counts every like (across reviews, lists, comments, games) given by the user.
     * Used by the badge system to evaluate the {@code PRAISE_THE_SUN} threshold.
     */
    long countByUserId(UUID userId);

    /**
     * Counts the total number of likes received across all of the user's reviews.
     * Used by the badge system to evaluate the {@code BELOVED_REVIEWER} threshold.
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.review.user.id = :userId")
    long countLikesReceivedOnReviewsByUserId(@Param("userId") UUID userId);

    /**
     * Counts how many likes reference a given video game (top-level game likes).
     * Used by the admin delete-game integrity check.
     */
    long countByVideoGameId(UUID videoGameId);
}
