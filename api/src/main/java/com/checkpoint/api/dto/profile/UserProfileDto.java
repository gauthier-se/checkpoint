package com.checkpoint.api.dto.profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a public user profile.
 *
 * @param id             the user's UUID
 * @param username       the user's display name (pseudo)
 * @param bio            the user's biography
 * @param picture        the user's profile picture URL
 * @param level          the user's current level
 * @param xpPoint        the user's current XP
 * @param xpThreshold    the XP required for the next level
 * @param isPrivate      whether the profile is private
 * @param badges         the list of earned badges
 * @param favorites      the user's favorite games (max 5, ordered by displayOrder)
 * @param recentPlays    the user's 5 most recent play logs (empty if private and viewer is not owner)
 * @param followerCount  the number of followers
 * @param followingCount the number of users being followed
 * @param reviewCount    the number of reviews written
 * @param wishlistCount  the number of games in the wishlist
 * @param isFollowing    whether the viewer is following this user (null if not authenticated)
 * @param isOwner        whether the viewer is the profile owner
 * @param createdAt      when the user account was created
 */
public record UserProfileDto(
        UUID id,
        String username,
        String bio,
        String picture,
        Integer level,
        Integer xpPoint,
        Integer xpThreshold,
        Boolean isPrivate,
        List<BadgeDto> badges,
        List<FavoriteDto> favorites,
        List<RecentPlayDto> recentPlays,
        Long followerCount,
        Long followingCount,
        Long reviewCount,
        Long wishlistCount,
        Boolean isFollowing,
        Boolean isOwner,
        LocalDateTime createdAt
) {}
