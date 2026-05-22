package com.checkpoint.api.services.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.NewsRequestDto;
import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.entities.News;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.NewsNotFoundException;
import com.checkpoint.api.mapper.NewsMapper;
import com.checkpoint.api.repositories.NewsRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.NewsService;

/**
 * Implementation of {@link NewsService}.
 * Manages news article CRUD operations including draft/publish workflow.
 */
@Service
@Transactional
public class NewsServiceImpl implements NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);

    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final NewsMapper newsMapper;

    public NewsServiceImpl(
            NewsRepository newsRepository,
            UserRepository userRepository,
            NewsMapper newsMapper
    ) {
        this.newsRepository = newsRepository;
        this.userRepository = userRepository;
        this.newsMapper = newsMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NewsResponseDto createNews(String userEmail, NewsRequestDto request) {
        User author = getUserByEmail(userEmail);

        News news = new News(request.title(), request.description(), author);
        news.setPicture(request.picture());

        News savedNews = newsRepository.save(news);

        log.info("Created news draft '{}' by {}", savedNews.getTitle(), userEmail);
        return newsMapper.toDto(savedNews);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NewsResponseDto updateNews(UUID newsId, NewsRequestDto request) {
        News news = getNewsOrThrow(newsId);

        news.setTitle(request.title());
        news.setDescription(request.description());
        news.setPicture(request.picture());

        News updatedNews = newsRepository.save(news);

        log.info("Updated news '{}'", updatedNews.getTitle());
        return newsMapper.toDto(updatedNews);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNews(UUID newsId) {
        News news = getNewsOrThrow(newsId);

        newsRepository.delete(news);
        log.info("Deleted news '{}' (ID: {})", news.getTitle(), newsId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NewsResponseDto publishNews(UUID newsId) {
        News news = getNewsOrThrow(newsId);

        news.setPublishedAt(LocalDateTime.now());
        News publishedNews = newsRepository.save(news);

        log.info("Published news '{}' (ID: {})", publishedNews.getTitle(), newsId);
        return newsMapper.toDto(publishedNews);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NewsResponseDto unpublishNews(UUID newsId) {
        News news = getNewsOrThrow(newsId);

        news.setPublishedAt(null);
        News unpublishedNews = newsRepository.save(news);

        log.info("Unpublished news '{}' (ID: {})", unpublishedNews.getTitle(), newsId);
        return newsMapper.toDto(unpublishedNews);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public NewsResponseDto getNewsById(UUID newsId) {
        News news = newsRepository.findByIdAndPublishedAtIsNotNull(newsId)
                .orElseThrow(() -> new NewsNotFoundException(newsId));
        return newsMapper.toDto(news);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NewsResponseDto> getAllNews(Pageable pageable) {
        Page<News> newsPage = newsRepository.findAllByOrderByCreatedAtDesc(pageable);
        return newsPage.map(newsMapper::toDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public NewsResponseDto getNewsByIdAdmin(UUID newsId) {
        News news = getNewsOrThrow(newsId);
        return newsMapper.toDto(news);
    }

    /**
     * Retrieves a news article by ID or throws if not found.
     *
     * @param newsId the news ID
     * @return the news entity
     */
    private News getNewsOrThrow(UUID newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException(newsId));
    }

    /**
     * Retrieves a user by email or throws if not found.
     *
     * @param email the user email
     * @return the user entity
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
