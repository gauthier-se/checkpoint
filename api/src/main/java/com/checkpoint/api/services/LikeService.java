package com.checkpoint.api.services;

import java.util.UUID;

import com.checkpoint.api.dto.social.LikeResponseDto;

/**
 * Service for managing likes on reviews, game lists, and comments.
 */
public interface LikeService {

    /**
     * Toggles a like on a review. If the user already liked the review, the like is removed.
     * Otherwise, a new like is created.
     *
     * @param userEmail the authenticated user's email
     * @param reviewId  the review ID
     * @return a response indicating the new like status and updated count
     * @throws com.checkpoint.api.exceptions.ReviewNotFoundException if the review does not exist
     */
    LikeResponseDto toggleReviewLike(String userEmail, UUID reviewId);

    /**
     * Toggles a like on a game list. If the user already liked the list, the like is removed.
     * Otherwise, a new like is created.
     *
     * @param userEmail the authenticated user's email
     * @param listId    the game list ID
     * @return a response indicating the new like status and updated count
     * @throws com.checkpoint.api.exceptions.GameListNotFoundException if the game list does not exist
     */
    LikeResponseDto toggleListLike(String userEmail, UUID listId);

    /**
     * Toggles a like on a comment. If the user already liked the comment, the like is removed.
     * Otherwise, a new like is created.
     *
     * @param userEmail the authenticated user's email
     * @param commentId the comment ID
     * @return a response indicating the new like status and updated count
     * @throws com.checkpoint.api.exceptions.CommentNotFoundException if the comment does not exist
     */
    LikeResponseDto toggleCommentLike(String userEmail, UUID commentId);

    /**
     * Toggles a like on a video game. If the user already likes the game, the like is removed.
     * Otherwise, a new like is created. A "like" marks a game the user loves — distinct from
     * the wishlist, which tracks games the user wants to buy.
     *
     * @param userEmail   the authenticated user's email
     * @param videoGameId the video game ID
     * @return a response indicating the new like status and updated count
     * @throws com.checkpoint.api.exceptions.GameNotFoundException if the video game does not exist
     */
    LikeResponseDto toggleGameLike(String userEmail, UUID videoGameId);
}
