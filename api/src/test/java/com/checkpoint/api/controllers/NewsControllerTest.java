package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.NewsAuthorDto;
import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.exceptions.NewsNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.NewsService;

@WebMvcTest(NewsController.class)
@AutoConfigureMockMvc(addFilters = false)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/news should return paginated published news")
    void getPublishedNews_shouldReturnPaginatedList() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        NewsAuthorDto author = new NewsAuthorDto(authorId, "admin", "admin.jpg");
        NewsResponseDto dto = new NewsResponseDto(
                newsId, "Test News", "Description", "pic.jpg",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), author,
                NewsSource.MANUAL, null, null, null
        );
        Page<NewsResponseDto> page = new PageImpl<>(List.of(dto));

        when(newsService.getPublishedNews(any(Pageable.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(get("/api/news")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(newsId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Test News"))
                .andExpect(jsonPath("$.content[0].author.pseudo").value("admin"));

        verify(newsService).getPublishedNews(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/news/{newsId} should return a single published news")
    void getNewsById_shouldReturnNews() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        NewsAuthorDto author = new NewsAuthorDto(authorId, "admin", "admin.jpg");
        NewsResponseDto dto = new NewsResponseDto(
                newsId, "Test News", "Description", "pic.jpg",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), author,
                NewsSource.MANUAL, null, null, null
        );

        when(newsService.getNewsById(newsId)).thenReturn(dto);

        // When / Then
        mockMvc.perform(get("/api/news/{newsId}", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newsId.toString()))
                .andExpect(jsonPath("$.title").value("Test News"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.author.pseudo").value("admin"));

        verify(newsService).getNewsById(newsId);
    }

    @Test
    @DisplayName("GET /api/news/{newsId} should return 404 when news not found")
    void getNewsById_shouldReturn404WhenNotFound() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();

        when(newsService.getNewsById(newsId)).thenThrow(new NewsNotFoundException(newsId));

        // When / Then
        mockMvc.perform(get("/api/news/{newsId}", newsId))
                .andExpect(status().isNotFound());

        verify(newsService).getNewsById(newsId);
    }
}
