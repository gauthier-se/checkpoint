package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.NewsAuthorDto;
import com.checkpoint.api.dto.catalog.NewsRequestDto;
import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.NewsImportService;
import com.checkpoint.api.services.NewsService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AdminNewsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NewsService newsService;

    @MockitoBean
    private NewsImportService newsImportService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    private NewsResponseDto createSampleNewsResponse(UUID newsId, LocalDateTime publishedAt) {
        UUID authorId = UUID.randomUUID();
        NewsAuthorDto author = new NewsAuthorDto(authorId, "admin", "admin.jpg");
        return new NewsResponseDto(
                newsId, "Test News", "Description", "pic.jpg",
                publishedAt, LocalDateTime.now(), LocalDateTime.now(), author,
                NewsSource.MANUAL, null, null, null
        );
    }

    @Test
    @DisplayName("POST /api/admin/news should create a draft news and return 201")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void createNews_shouldReturn201() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        NewsRequestDto request = new NewsRequestDto("Test News", "Description", "pic.jpg");
        NewsResponseDto response = createSampleNewsResponse(newsId, null);

        when(newsService.createNews(eq("admin@test.com"), any(NewsRequestDto.class))).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/admin/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(newsId.toString()))
                .andExpect(jsonPath("$.title").value("Test News"));

        verify(newsService).createNews(eq("admin@test.com"), any(NewsRequestDto.class));
    }

    @Test
    @DisplayName("GET /api/admin/news should return paginated list of all news")
    void getAllNews_shouldReturnPaginatedList() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        NewsResponseDto dto = createSampleNewsResponse(newsId, null);
        Page<NewsResponseDto> page = new PageImpl<>(List.of(dto));

        when(newsService.getAllNews(any(Pageable.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(get("/api/admin/news")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(newsId.toString()));

        verify(newsService).getAllNews(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/admin/news/{newsId} should return a single news")
    void getNewsById_shouldReturnNews() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        NewsResponseDto dto = createSampleNewsResponse(newsId, null);

        when(newsService.getNewsByIdAdmin(newsId)).thenReturn(dto);

        // When / Then
        mockMvc.perform(get("/api/admin/news/{newsId}", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newsId.toString()))
                .andExpect(jsonPath("$.title").value("Test News"));

        verify(newsService).getNewsByIdAdmin(newsId);
    }

    @Test
    @DisplayName("PUT /api/admin/news/{newsId} should update and return news")
    void updateNews_shouldReturnUpdatedNews() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        NewsRequestDto request = new NewsRequestDto("Updated Title", "Updated Desc", "new-pic.jpg");
        NewsResponseDto response = createSampleNewsResponse(newsId, null);

        when(newsService.updateNews(eq(newsId), any(NewsRequestDto.class))).thenReturn(response);

        // When / Then
        mockMvc.perform(put("/api/admin/news/{newsId}", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newsId.toString()));

        verify(newsService).updateNews(eq(newsId), any(NewsRequestDto.class));
    }

    @Test
    @DisplayName("DELETE /api/admin/news/{newsId} should return 204 No Content")
    void deleteNews_shouldReturn204() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        doNothing().when(newsService).deleteNews(newsId);

        // When / Then
        mockMvc.perform(delete("/api/admin/news/{newsId}", newsId))
                .andExpect(status().isNoContent());

        verify(newsService).deleteNews(newsId);
    }

    @Test
    @DisplayName("POST /api/admin/news/{newsId}/publish should publish and return news")
    void publishNews_shouldReturnPublishedNews() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        NewsResponseDto response = createSampleNewsResponse(newsId, LocalDateTime.now());

        when(newsService.publishNews(newsId)).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/admin/news/{newsId}/publish", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newsId.toString()))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        verify(newsService).publishNews(newsId);
    }

    @Test
    @DisplayName("POST /api/admin/news/{newsId}/unpublish should unpublish and return news")
    void unpublishNews_shouldReturnUnpublishedNews() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        NewsResponseDto response = createSampleNewsResponse(newsId, null);

        when(newsService.unpublishNews(newsId)).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/admin/news/{newsId}/unpublish", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newsId.toString()));

        verify(newsService).unpublishNews(newsId);
    }

    @Test
    @DisplayName("POST /api/admin/news/import/STEAM should run the Steam pass and return count")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void triggerImport_steam_shouldReturnCount() throws Exception {
        when(newsImportService.importFromSource(NewsSource.STEAM)).thenReturn(4);

        mockMvc.perform(post("/api/admin/news/import/{source}", "STEAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(4));

        verify(newsImportService).importFromSource(NewsSource.STEAM);
    }

    @Test
    @DisplayName("POST /api/admin/news/import/RSS should run the RSS pass and return count")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void triggerImport_rss_shouldReturnCount() throws Exception {
        when(newsImportService.importFromSource(NewsSource.RSS)).thenReturn(0);

        mockMvc.perform(post("/api/admin/news/import/{source}", "RSS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0));

        verify(newsImportService).importFromSource(NewsSource.RSS);
    }
}
