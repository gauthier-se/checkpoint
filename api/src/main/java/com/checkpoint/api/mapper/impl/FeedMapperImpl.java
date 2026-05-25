package com.checkpoint.api.mapper.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.social.FeedGameDto;
import com.checkpoint.api.dto.social.FeedItemDto;
import com.checkpoint.api.dto.social.FeedUserDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.FeedItemType;
import com.checkpoint.api.mapper.FeedMapper;

/**
 * Implementation of {@link FeedMapper}.
 */
@Component
public class FeedMapperImpl implements FeedMapper {

    @Override
    public FeedItemDto toFeedItemDto(Object[] row, Map<UUID, User> userCache, Map<UUID, VideoGame> gameCache) {
        UUID id = (UUID) row[0];
        String typeStr = (String) row[1];
        LocalDateTime createdAt = ((java.sql.Timestamp) row[2]).toLocalDateTime();
        UUID userId = (UUID) row[3];
        UUID videoGameId = row[4] != null ? (UUID) row[4] : null;
        String extra1 = row[5] != null ? row[5].toString() : null;
        String extra2 = row[6] != null ? row[6].toString() : null;

        FeedItemType type = FeedItemType.valueOf(typeStr);

        User user = userCache.get(userId);
        if (user == null) {
            return null;
        }
        FeedUserDto feedUser = new FeedUserDto(user.getId(), user.getPseudo(), user.getPicture());

        FeedGameDto feedGame = null;
        if (videoGameId != null) {
            VideoGame game = gameCache.get(videoGameId);
            if (game != null) {
                feedGame = new FeedGameDto(game.getId(), game.getTitle(), game.getCoverUrl(), game.getReleaseDate());
            }
        }

        String playStatus = null;
        Integer score = null;
        String reviewContent = null;
        Boolean haveSpoilers = null;
        String listTitle = null;
        Integer listGameCount = null;

        switch (type) {
            case PLAY:
                playStatus = extra1;
                break;
            case RATING:
                score = extra1 != null ? Integer.parseInt(extra1) : null;
                break;
            case REVIEW:
                reviewContent = extra1;
                haveSpoilers = extra2 != null ? Boolean.parseBoolean(extra2) : null;
                break;
            case LIST:
                listTitle = extra1;
                listGameCount = extra2 != null ? Integer.parseInt(extra2) : null;
                break;
            case LIKE_GAME:
                // No type-specific fields; the liked game is carried by the common `game` field.
                break;
        }

        return new FeedItemDto(
                id, type, createdAt, feedUser, feedGame,
                playStatus, score, reviewContent, haveSpoilers,
                listTitle, listGameCount
        );
    }
}
