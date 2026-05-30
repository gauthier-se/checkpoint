package com.checkpoint.api.mapper;

import java.util.List;

import com.checkpoint.api.dto.profile.BadgeDto;
import com.checkpoint.api.dto.profile.FavoriteDto;
import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.RatingDistributionEntryDto;
import com.checkpoint.api.dto.profile.RecentPlayDto;
import com.checkpoint.api.dto.profile.UserProfileDto;
import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.entities.Favorite;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGamePlay;

/**
 * Mapper for converting User entities to profile DTOs.
 */
public interface ProfileMapper {

    /**
     * Maps a User entity and computed stats to a UserProfileDto.
     *
     * @param user           the user entity (with badges loaded)
     * @param catalog        the full badge catalog, used to mark earned vs. not
     *                       and to surface hidden silhouettes
     * @param recentPlays    the user's recent play projections (pre-computed by the service)
     * @param followerCount  the number of followers
     * @param followingCount the number of users being followed
     * @param reviewCount        the number of reviews written
     * @param wishlistCount      the number of games in the wishlist
     * @param ratingDistribution the user's ratings grouped by score (sparse, 1&ndash;10)
     * @param isFollowing        whether the viewer is following this user (null if not authenticated)
     * @param isOwner            whether the viewer is the profile owner
     * @return the user profile DTO
     */
    UserProfileDto toUserProfileDto(User user, List<Badge> catalog, List<RecentPlayDto> recentPlays,
                                     Long followerCount, Long followingCount,
                                     Long reviewCount, Long wishlistCount,
                                     List<RatingDistributionEntryDto> ratingDistribution,
                                     Boolean isFollowing, Boolean isOwner);

    /**
     * Maps a play log to a compact {@link RecentPlayDto}.
     *
     * @param play    the play log entity (with {@code videoGame} and {@code review} fetched)
     * @param isLiked whether the profile owner has liked this play's video game
     * @return the recent play DTO
     */
    RecentPlayDto toRecentPlayDto(UserGamePlay play, boolean isLiked);

    /**
     * Maps a Badge entity to a BadgeDto with an explicit earned flag.
     *
     * @param badge  the badge entity
     * @param earned whether the profile owner has earned this badge
     * @return the badge DTO
     */
    BadgeDto toBadgeDto(Badge badge, boolean earned);

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
