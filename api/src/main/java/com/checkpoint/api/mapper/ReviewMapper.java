package com.checkpoint.api.mapper;

import com.checkpoint.api.dto.catalog.ReviewCardDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.entities.Review;

/**
 * Mapper for converting Review entities to DTOs.
 */
public interface ReviewMapper {

    /**
     * Maps a Review entity to a ReviewResponseDto with default like values (0, false).
     *
     * @param review the review entity
     * @return the review response DTO
     */
    ReviewResponseDto toDto(Review review);

    /**
     * Maps a Review entity to a ReviewResponseDto with like and comment context.
     *
     * @param review        the review entity
     * @param likesCount    the number of likes on this review
     * @param hasLiked      whether the current viewer has liked this review
     * @param commentsCount the number of comments on this review
     * @return the review response DTO
     */
    ReviewResponseDto toDto(Review review, long likesCount, boolean hasLiked, long commentsCount);

    /**
     * Maps a Review entity to a ReviewCardDto, bundling minimal game information.
     * Used by cross-game review listings (popular / recent).
     *
     * @param review        the review entity
     * @param likesCount    the number of likes on this review
     * @param hasLiked      whether the current viewer has liked this review
     * @param commentsCount the number of comments on this review
     * @return the review card DTO
     */
    ReviewCardDto toCardDto(Review review, long likesCount, boolean hasLiked, long commentsCount);
}
