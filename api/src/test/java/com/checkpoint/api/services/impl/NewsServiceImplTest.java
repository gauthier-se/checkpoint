package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.NewsAuthorDto;
import com.checkpoint.api.dto.catalog.NewsRequestDto;
import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.entities.News;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.NewsNotFoundException;
import com.checkpoint.api.mapper.NewsMapper;
import com.checkpoint.api.repositories.NewsRepository;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Unit tests for {@link NewsServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NewsMapper newsMapper;

    private NewsServiceImpl newsService;

    private User testUser;
    private News testNews;
    private NewsResponseDto testResponseDto;

    @BeforeEach
    void setUp() {
        newsService = new NewsServiceImpl(newsRepository, userRepository, newsMapper);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setPseudo("admin");
        testUser.setEmail("admin@test.com");
        testUser.setPicture("admin.jpg");

        testNews = new News("Test News", "Description", testUser);
        testNews.setId(UUID.randomUUID());
        testNews.setPicture("pic.jpg");

        NewsAuthorDto authorDto = new NewsAuthorDto(testUser.getId(), "admin", "admin.jpg");
        testResponseDto = new NewsResponseDto(
                testNews.getId(), "Test News", "Description", "pic.jpg",
                null, LocalDateTime.now(), LocalDateTime.now(), authorDto,
                NewsSource.MANUAL, null, null, null
        );
    }

    @Nested
    @DisplayName("createNews")
    class CreateNews {

        @Test
        @DisplayName("should create a draft news article")
        void createNews_shouldCreateDraft() {
            // Given
            NewsRequestDto request = new NewsRequestDto("Test News", "Description", "pic.jpg");

            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testUser));
            when(newsRepository.save(any(News.class))).thenReturn(testNews);
            when(newsMapper.toDto(testNews)).thenReturn(testResponseDto);

            // When
            NewsResponseDto result = newsService.createNews("admin@test.com", request);

            // Then
            assertThat(result.title()).isEqualTo("Test News");
            assertThat(result.publishedAt()).isNull();
            verify(newsRepository).save(any(News.class));
        }
    }

    @Nested
    @DisplayName("updateNews")
    class UpdateNews {

        @Test
        @DisplayName("should update news article fields")
        void updateNews_shouldUpdateFields() {
            // Given
            UUID newsId = testNews.getId();
            NewsRequestDto request = new NewsRequestDto("Updated Title", "Updated Desc", "new-pic.jpg");

            when(newsRepository.findById(newsId)).thenReturn(Optional.of(testNews));
            when(newsRepository.save(testNews)).thenReturn(testNews);
            when(newsMapper.toDto(testNews)).thenReturn(testResponseDto);

            // When
            NewsResponseDto result = newsService.updateNews(newsId, request);

            // Then
            assertThat(result).isNotNull();
            verify(newsRepository).save(testNews);
        }

        @Test
        @DisplayName("should throw NewsNotFoundException when news does not exist")
        void updateNews_shouldThrowWhenNotFound() {
            // Given
            UUID newsId = UUID.randomUUID();
            NewsRequestDto request = new NewsRequestDto("Title", "Desc", null);

            when(newsRepository.findById(newsId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> newsService.updateNews(newsId, request))
                    .isInstanceOf(NewsNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteNews")
    class DeleteNews {

        @Test
        @DisplayName("should delete news article")
        void deleteNews_shouldDelete() {
            // Given
            UUID newsId = testNews.getId();

            when(newsRepository.findById(newsId)).thenReturn(Optional.of(testNews));

            // When
            newsService.deleteNews(newsId);

            // Then
            verify(newsRepository).delete(testNews);
        }

        @Test
        @DisplayName("should throw NewsNotFoundException when news does not exist")
        void deleteNews_shouldThrowWhenNotFound() {
            // Given
            UUID newsId = UUID.randomUUID();

            when(newsRepository.findById(newsId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> newsService.deleteNews(newsId))
                    .isInstanceOf(NewsNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("publishNews")
    class PublishNews {

        @Test
        @DisplayName("should set publishedAt to now")
        void publishNews_shouldSetPublishedAt() {
            // Given
            UUID newsId = testNews.getId();

            when(newsRepository.findById(newsId)).thenReturn(Optional.of(testNews));
            when(newsRepository.save(testNews)).thenReturn(testNews);
            when(newsMapper.toDto(testNews)).thenReturn(testResponseDto);

            // When
            newsService.publishNews(newsId);

            // Then
            assertThat(testNews.getPublishedAt()).isNotNull();
            verify(newsRepository).save(testNews);
        }
    }

    @Nested
    @DisplayName("unpublishNews")
    class UnpublishNews {

        @Test
        @DisplayName("should set publishedAt to null")
        void unpublishNews_shouldClearPublishedAt() {
            // Given
            UUID newsId = testNews.getId();
            testNews.setPublishedAt(LocalDateTime.now());

            when(newsRepository.findById(newsId)).thenReturn(Optional.of(testNews));
            when(newsRepository.save(testNews)).thenReturn(testNews);
            when(newsMapper.toDto(testNews)).thenReturn(testResponseDto);

            // When
            newsService.unpublishNews(newsId);

            // Then
            assertThat(testNews.getPublishedAt()).isNull();
            verify(newsRepository).save(testNews);
        }
    }

    @Nested
    @DisplayName("getPublishedNews")
    class GetPublishedNews {

        @Test
        @DisplayName("should return paginated published news")
        void getPublishedNews_shouldReturnPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<News> newsPage = new PageImpl<>(List.of(testNews));

            when(newsRepository.findByPublishedAtIsNotNullOrderByPublishedAtDesc(pageable)).thenReturn(newsPage);
            when(newsMapper.toDto(testNews)).thenReturn(testResponseDto);

            // When
            Page<NewsResponseDto> result = newsService.getPublishedNews(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("Test News");
        }
    }

    @Nested
    @DisplayName("getNewsById")
    class GetNewsById {

        @Test
        @DisplayName("should return published news by ID")
        void getNewsById_shouldReturnNews() {
            // Given
            UUID newsId = testNews.getId();

            when(newsRepository.findByIdAndPublishedAtIsNotNull(newsId)).thenReturn(Optional.of(testNews));
            when(newsMapper.toDto(testNews)).thenReturn(testResponseDto);

            // When
            NewsResponseDto result = newsService.getNewsById(newsId);

            // Then
            assertThat(result.title()).isEqualTo("Test News");
        }

        @Test
        @DisplayName("should throw NewsNotFoundException when not published")
        void getNewsById_shouldThrowWhenNotPublished() {
            // Given
            UUID newsId = UUID.randomUUID();

            when(newsRepository.findByIdAndPublishedAtIsNotNull(newsId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> newsService.getNewsById(newsId))
                    .isInstanceOf(NewsNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllNews")
    class GetAllNews {

        @Test
        @DisplayName("should return all news for admin")
        void getAllNews_shouldReturnAllNews() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<News> newsPage = new PageImpl<>(List.of(testNews));

            when(newsRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(newsPage);
            when(newsMapper.toDto(testNews)).thenReturn(testResponseDto);

            // When
            Page<NewsResponseDto> result = newsService.getAllNews(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getNewsByIdAdmin")
    class GetNewsByIdAdmin {

        @Test
        @DisplayName("should return any news by ID for admin")
        void getNewsByIdAdmin_shouldReturnNews() {
            // Given
            UUID newsId = testNews.getId();

            when(newsRepository.findById(newsId)).thenReturn(Optional.of(testNews));
            when(newsMapper.toDto(testNews)).thenReturn(testResponseDto);

            // When
            NewsResponseDto result = newsService.getNewsByIdAdmin(newsId);

            // Then
            assertThat(result.title()).isEqualTo("Test News");
        }

        @Test
        @DisplayName("should throw NewsNotFoundException when not found")
        void getNewsByIdAdmin_shouldThrowWhenNotFound() {
            // Given
            UUID newsId = UUID.randomUUID();

            when(newsRepository.findById(newsId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> newsService.getNewsByIdAdmin(newsId))
                    .isInstanceOf(NewsNotFoundException.class);
        }
    }
}
