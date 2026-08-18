package com.checkpoint.api.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.checkpoint.api.entities.ReviewView;

/**
 * Repository for {@link ReviewView}: distinct reviews opened by a user.
 */
public interface ReviewViewRepository extends JpaRepository<ReviewView, ReviewView.PK> {

    /**
     * Counts the distinct reviews the user has viewed. Powers the
     * STAY_AWHILE_REVIEWS easter-egg badge.
     */
    long countByUserId(UUID userId);

    boolean existsByUserIdAndReviewId(UUID userId, UUID reviewId);
}
