package com.checkpoint.api.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.social.FollowResponseDto;
import com.checkpoint.api.dto.social.FollowUserDto;

/**
 * Service for managing the social graph (follow/unfollow system).
 */
public interface FollowService {

    /**
     * Toggles the follow status between the authenticated user and the target user.
     * If the user is already following the target, they will unfollow.
     * If not, they will follow.
     *
     * @param userEmail    the authenticated user's email
     * @param targetUserId the target user's ID
     * @return a response indicating the new follow status
     */
    FollowResponseDto toggleFollow(String userEmail, UUID targetUserId);

    /**
     * Returns a paginated list of users who follow the given user.
     *
     * @param userId   the user's ID
     * @param pageable pagination parameters
     * @return a page of follower user DTOs
     */
    Page<FollowUserDto> getFollowers(UUID userId, Pageable pageable);

    /**
     * Returns a paginated list of users that the given user follows.
     *
     * @param userId   the user's ID
     * @param pageable pagination parameters
     * @return a page of following user DTOs
     */
    Page<FollowUserDto> getFollowing(UUID userId, Pageable pageable);

    /**
     * Removes a follower from the authenticated user: the given follower will no
     * longer follow the authenticated user. Idempotent — does nothing if the user
     * is not currently a follower. No notification is sent to the removed follower.
     *
     * @param userEmail  the authenticated user's email
     * @param followerId the ID of the follower to remove
     */
    void removeFollower(String userEmail, UUID followerId);
}
