package com.checkpoint.api.mapper;

import com.checkpoint.api.dto.profile.BadgeDto;
import com.checkpoint.api.dto.profile.FavoriteDto;
import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.UserProfileDto;
import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.entities.Favorite;
import com.checkpoint.api.entities.User;

/**
 * Mapper for converting User entities to profile DTOs.
 */
public interface ProfileMapper {

    /**
     * Maps a User entity and computed stats to a UserProfileDto.
     *
     * @param user           the user entity (with badges loaded)
     * @param followerCount  the number of followers
     * @param followingCount the number of users being followed
     * @param reviewCount    the number of reviews written
     * @param wishlistCount  the number of games in the wishlist
     * @param isFollowing    whether the viewer is following this user (null if not authenticated)
     * @param isOwner        whether the viewer is the profile owner
     * @return the user profile DTO
     */
    UserProfileDto toUserProfileDto(User user, Long followerCount, Long followingCount,
                                     Long reviewCount, Long wishlistCount,
                                     Boolean isFollowing, Boolean isOwner);

    /**
     * Maps a Badge entity to a BadgeDto.
     *
     * @param badge the badge entity
     * @return the badge DTO
     */
    BadgeDto toBadgeDto(Badge badge);

    /**
     * Maps a Favorite entity (with its video game) to a FavoriteDto.
     *
     * @param favorite the favorite entity
     * @return the favorite DTO
     */
    FavoriteDto toFavoriteDto(Favorite favorite);

    /**
     * Maps a User entity to a ProfileUpdatedDto after a profile update.
     *
     * @param user the updated user entity
     * @return the profile updated DTO
     */
    ProfileUpdatedDto toProfileUpdatedDto(User user);
}
