package com.checkpoint.api.mapper.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

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
import com.checkpoint.api.mapper.ProfileMapper;

/**
 * Implementation of {@link ProfileMapper}.
 */
@Component
public class ProfileMapperImpl implements ProfileMapper {

    /**
     * {@inheritDoc}
     */
    @Override
    public UserProfileDto toUserProfileDto(User user, List<Badge> catalog, List<RecentPlayDto> recentPlays,
                                            Long followerCount, Long followingCount,
                                            Long reviewCount, Long wishlistCount,
                                            List<RatingDistributionEntryDto> ratingDistribution,
                                            Boolean isFollowing, Boolean isOwner) {
        Set<UUID> earnedIds = user.getBadges().stream()
                .map(Badge::getId)
                .collect(Collectors.toSet());

        // Visible badges first (so the earned set leads), then hidden ones; within
        // each group sort by code so the order is stable across requests.
        List<BadgeDto> badgeDtos = catalog.stream()
                .sorted(Comparator.comparing(Badge::isHidden).thenComparing(Badge::getCode))
                .map(badge -> toBadgeDto(badge, earnedIds.contains(badge.getId())))
                .toList();

        List<FavoriteDto> favoriteDtos = user.getFavorites().stream()
                .sorted(Comparator.comparing(Favorite::getDisplayOrder))
                .map(this::toFavoriteDto)
                .toList();

        Integer xpThreshold = user.getLevel() * 1000;

        return new UserProfileDto(
                user.getId(),
                user.getPseudo(),
                user.getBio(),
                user.getPicture(),
                user.getLevel(),
                user.getXpPoint(),
                xpThreshold,
                user.getIsPrivate(),
                badgeDtos,
                favoriteDtos,
                recentPlays,
                followerCount,
                followingCount,
                reviewCount,
                wishlistCount,
                ratingDistribution,
                isFollowing,
                isOwner,
                user.getCreatedAt()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecentPlayDto toRecentPlayDto(UserGamePlay play, boolean isLiked) {
        return new RecentPlayDto(
                play.getId(),
                play.getVideoGame().getId(),
                play.getVideoGame().getTitle(),
                play.getVideoGame().getCoverUrl(),
                play.getScore(),
                play.getReview() != null,
                Boolean.TRUE.equals(play.getIsReplay()),
                isLiked,
                play.getCreatedAt()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BadgeDto toBadgeDto(Badge badge, boolean earned) {
        return new BadgeDto(
                badge.getId(),
                badge.getCode(),
                badge.getName(),
                badge.getPicture(),
                badge.getDescription(),
                badge.isHidden(),
                earned
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FavoriteDto toFavoriteDto(Favorite favorite) {
        return new FavoriteDto(
                favorite.getVideoGame().getId(),
                favorite.getVideoGame().getTitle(),
                favorite.getVideoGame().getCoverUrl(),
                favorite.getDisplayOrder()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProfileUpdatedDto toProfileUpdatedDto(User user) {
        return new ProfileUpdatedDto(
                user.getPseudo(),
                user.getBio(),
                user.getPicture(),
                user.getIsPrivate()
        );
    }
}
