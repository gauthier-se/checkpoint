package com.checkpoint.api.mapper.impl;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.catalog.ReviewCardDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.dto.catalog.ReviewUserDto;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.mapper.ReviewMapper;

import java.util.UUID;

/**
 * Implementation of {@link ReviewMapper}.
 */
@Component
public class ReviewMapperImpl implements ReviewMapper {

    /**
     * {@inheritDoc}
     */
    @Override
    public ReviewResponseDto toDto(Review review) {
        return toDto(review, 0, false, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReviewResponseDto toDto(Review review, long likesCount, boolean hasLiked, long commentsCount) {
        if (review == null) {
            return null;
        }

        ReviewUserDto userDto = null;
        if (review.getUser() != null) {
            User user = review.getUser();
            userDto = new ReviewUserDto(
                    user.getId(),
                    user.getPseudo(),
                    user.getPicture()
            );
        }

        UUID playLogId = null;
        String platformName = null;
        PlayStatus playStatus = null;
        Boolean isReplay = null;

        UserGamePlay playLog = review.getUserGamePlay();
        if (playLog != null) {
            playLogId = playLog.getId();
            platformName = playLog.getPlatform() != null ? playLog.getPlatform().getName() : null;
            playStatus = playLog.getStatus();
            isReplay = playLog.getIsReplay();
        }

        return new ReviewResponseDto(
                review.getId(),
                review.getContent(),
                review.getHaveSpoilers(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                userDto,
                playLogId,
                platformName,
                playStatus,
                isReplay,
                likesCount,
                hasLiked,
                commentsCount
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReviewCardDto toCardDto(Review review, long likesCount, boolean hasLiked, long commentsCount) {
        if (review == null) {
            return null;
        }

        ReviewUserDto userDto = null;
        if (review.getUser() != null) {
            User user = review.getUser();
            userDto = new ReviewUserDto(
                    user.getId(),
                    user.getPseudo(),
                    user.getPicture()
            );
        }

        UUID playLogId = null;
        String platformName = null;
        PlayStatus playStatus = null;
        Boolean isReplay = null;

        UserGamePlay playLog = review.getUserGamePlay();
        if (playLog != null) {
            playLogId = playLog.getId();
            platformName = playLog.getPlatform() != null ? playLog.getPlatform().getName() : null;
            playStatus = playLog.getStatus();
            isReplay = playLog.getIsReplay();
        }

        UUID gameId = null;
        String gameTitle = null;
        String gameCoverUrl = null;
        VideoGame videoGame = review.getVideoGame();
        if (videoGame != null) {
            gameId = videoGame.getId();
            gameTitle = videoGame.getTitle();
            gameCoverUrl = videoGame.getCoverUrl();
        }

        return new ReviewCardDto(
                review.getId(),
                review.getContent(),
                review.getHaveSpoilers(),
                review.getCreatedAt(),
                userDto,
                playLogId,
                platformName,
                playStatus,
                isReplay,
                likesCount,
                hasLiked,
                commentsCount,
                gameId,
                gameTitle,
                gameCoverUrl
        );
    }
}
