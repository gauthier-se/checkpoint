package com.checkpoint.api.mapper.impl;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.catalog.NewsAuthorDto;
import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.entities.News;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.mapper.NewsMapper;

/**
 * Implementation of {@link NewsMapper}.
 */
@Component
public class NewsMapperImpl implements NewsMapper {

    /**
     * {@inheritDoc}
     */
    @Override
    public NewsResponseDto toDto(News news) {
        if (news == null) {
            return null;
        }

        NewsAuthorDto authorDto = null;
        User author = news.getAuthor();
        if (author != null) {
            authorDto = new NewsAuthorDto(
                    author.getId(),
                    author.getPseudo(),
                    author.getPicture()
            );
        }

        VideoGame videoGame = news.getVideoGame();

        return new NewsResponseDto(
                news.getId(),
                news.getTitle(),
                news.getDescription(),
                news.getPicture(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getUpdatedAt(),
                authorDto,
                news.getSource(),
                news.getExternalUrl(),
                news.getFeedName(),
                videoGame != null ? videoGame.getId() : null
        );
    }
}
