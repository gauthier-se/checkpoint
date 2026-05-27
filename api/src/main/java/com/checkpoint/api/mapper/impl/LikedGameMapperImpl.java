package com.checkpoint.api.mapper.impl;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.entities.Like;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.mapper.LikedGameMapper;

/**
 * Implementation of {@link LikedGameMapper}.
 */
@Component
public class LikedGameMapperImpl implements LikedGameMapper {

    @Override
    public LikedGameResponseDto toResponseDto(Like like) {
        VideoGame videoGame = like.getVideoGame();

        return new LikedGameResponseDto(
                like.getId(),
                videoGame.getId(),
                videoGame.getTitle(),
                videoGame.getCoverUrl(),
                videoGame.getReleaseDate(),
                like.getCreatedAt()
        );
    }
}
