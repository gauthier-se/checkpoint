package com.checkpoint.api.mapper.impl;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.profile.BadgeDto;
import com.checkpoint.api.dto.profile.FavoriteDto;
import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.UserProfileDto;
import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.entities.Favorite;
import com.checkpoint.api.entities.User;
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
    public UserProfileDto toUserProfileDto(User user, Long followerCount, Long followingCount,
                                            Long reviewCount, Long wishlistCount,
                                            Boolean isFollowing, Boolean isOwner) {
        List<BadgeDto> badgeDtos = user.getBadges().stream()
                .map(this::toBadgeDto)
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
                followerCount,
                followingCount,
                reviewCount,
                wishlistCount,
                isFollowing,
                isOwner,
                user.getCreatedAt()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BadgeDto toBadgeDto(Badge badge) {
        return new BadgeDto(
                badge.getId(),
                badge.getName(),
                badge.getPicture(),
                badge.getDescription()
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
