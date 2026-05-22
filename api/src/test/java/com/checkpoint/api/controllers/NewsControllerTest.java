package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import com.checkpoint.api.dto.catalog.NewsSearchCriteria;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.exceptions.NewsNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.NewsSearchService;
import com.checkpoint.api.services.NewsService;

@WebMvcTest(NewsController.class)
@AutoConfigureMockMvc(addFilters = false)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @MockitoBean
    private NewsSearchService newsSearchService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    private NewsResponseDto sampleDto(UUID id) {
        UUID authorId = UUID.randomUUID();
        NewsAuthorDto author = new NewsAuthorDto(authorId, "admin", "admin.jpg");
        return new NewsResponseDto(
                id, "Test News", "Description", "pic.jpg",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), author,
                NewsSource.MANUAL, null, null, null
        );
    }

    @Test
    @DisplayName("GET /api/news should delegate to NewsSearchService with empty criteria by default")
    void getPublishedNews_shouldDelegateWithDefaults() throws Exception {
        UUID newsId = UUID.randomUUID();
        Page<NewsResponseDto> page = new PageImpl<>(List.of(sampleDto(newsId)));

        when(newsSearchService.search(any(NewsSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/news")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(newsId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Test News"));

        verify(newsSearchService).search(any(NewsSearchCriteria.class), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/news should pass q, source, feedName, dates, sort to criteria")
    void getPublishedNews_shouldThreadCriteria() throws Exception {
        Page<NewsResponseDto> page = new PageImpl<>(List.of());
        when(newsSearchService.search(any(NewsSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/news")
                        .param("q", "witcher")
                        .param("source", "RSS")
                        .param("feedName", "IGN")
                        .param("publishedFrom", "2026-01-01")
                        .param("publishedTo", "2026-04-01")
                        .param("sort", "relevance"))
                .andExpect(status().isOk());

        verify(newsSearchService).search(argThat((NewsSearchCriteria c) ->
                "witcher".equals(c.q())
                        && c.source() == NewsSource.RSS
                        && "IGN".equals(c.feedName())
                        && "2026-01-01".equals(c.publishedFrom().toString())
                        && "2026-04-01".equals(c.publishedTo().toString())
                        && "relevance".equals(c.sort())
        ), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/news should return 400 when publishedFrom > publishedTo")
    void getPublishedNews_shouldRejectInvertedDateRange() throws Exception {
        mockMvc.perform(get("/api/news")
                        .param("publishedFrom", "2026-04-01")
                        .param("publishedTo", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/news/search should return fuzzy-matched results")
    void quickSearch_shouldReturnResults() throws Exception {
        UUID newsId = UUID.randomUUID();
        when(newsSearchService.quickSearch(eq("elden"), anyInt()))
                .thenReturn(List.of(sampleDto(newsId)));

        mockMvc.perform(get("/api/news/search")
                        .param("q", "elden")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(newsId.toString()));

        verify(newsSearchService).quickSearch("elden", 3);
    }

    @Test
    @DisplayName("GET /api/news/search should clamp limit to max 10")
    void quickSearch_shouldClampLimit() throws Exception {
        when(newsSearchService.quickSearch(eq("elden"), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/news/search")
                        .param("q", "elden")
                        .param("limit", "99"))
                .andExpect(status().isOk());

        verify(newsSearchService).quickSearch("elden", 10);
    }

    @Test
    @DisplayName("GET /api/news/search should default limit to 5 when omitted")
    void quickSearch_shouldDefaultLimit() throws Exception {
        when(newsSearchService.quickSearch(eq("elden"), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/news/search")
                        .param("q", "elden"))
                .andExpect(status().isOk());

        verify(newsSearchService).quickSearch("elden", 5);
    }

    @Test
    @DisplayName("GET /api/news/{newsId} should return a single published news")
    void getNewsById_shouldReturnNews() throws Exception {
        UUID newsId = UUID.randomUUID();
        when(newsService.getNewsById(newsId)).thenReturn(sampleDto(newsId));

        mockMvc.perform(get("/api/news/{newsId}", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newsId.toString()))
                .andExpect(jsonPath("$.title").value("Test News"));

        verify(newsService).getNewsById(newsId);
    }

    @Test
    @DisplayName("GET /api/news/{newsId} should return 404 when news not found")
    void getNewsById_shouldReturn404WhenNotFound() throws Exception {
        UUID newsId = UUID.randomUUID();
        when(newsService.getNewsById(newsId)).thenThrow(new NewsNotFoundException(newsId));

        mockMvc.perform(get("/api/news/{newsId}", newsId))
                .andExpect(status().isNotFound());

        verify(newsService).getNewsById(newsId);
    }
}
