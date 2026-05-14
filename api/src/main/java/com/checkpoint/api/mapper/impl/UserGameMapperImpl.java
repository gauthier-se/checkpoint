package com.checkpoint.api.mapper.impl;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.mapper.UserGameMapper;

/**
 * Implementation of {@link UserGameMapper}.
 */
@Component
public class UserGameMapperImpl implements UserGameMapper {

    @Override
    public UserGameResponseDto toResponseDto(UserGame userGame) {
        VideoGame videoGame = userGame.getVideoGame();

        return new UserGameResponseDto(
                userGame.getId(),
                videoGame.getId(),
                videoGame.getTitle(),
                videoGame.getCoverUrl(),
                videoGame.getReleaseDate(),
                userGame.getStatus(),
                userGame.getCreatedAt(),
                userGame.getUpdatedAt(),
                userGame.getNotes()
        );
    }
}
