package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.social.FeedGameDto;
import com.checkpoint.api.dto.social.FeedItemDto;
import com.checkpoint.api.dto.social.FeedUserDto;
import com.checkpoint.api.enums.FeedItemType;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.FeedService;

/**
 * Unit tests for {@link FeedController}.
 */
@WebMvcTest(FeedController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/me/feed should return paginated feed")
    @WithMockUser(username = "user@test.com")
    void getFeed_shouldReturnPaginatedFeed() throws Exception {
        // Given
        FeedUserDto feedUser = new FeedUserDto(UUID.randomUUID(), "gamer42", "avatar.jpg");
        FeedGameDto feedGame = new FeedGameDto(UUID.randomUUID(), "Elden Ring", "cover.jpg", null);
        FeedItemDto item = new FeedItemDto(
                UUID.randomUUID(), FeedItemType.RATING, LocalDateTime.now(),
                feedUser, feedGame, null, 5, null, null, null, null
        );

        List<FeedItemDto> items = List.of(item);
        Page<FeedItemDto> page = new PageImpl<>(items, PageRequest.of(0, 20), 1);
        PagedResponseDto<FeedItemDto> response = PagedResponseDto.from(page);

        when(feedService.getFeed(eq("user@test.com"), anyInt(), anyInt())).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/me/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").value("RATING"))
                .andExpect(jsonPath("$.content[0].score").value(5))
                .andExpect(jsonPath("$.content[0].user.pseudo").value("gamer42"))
                .andExpect(jsonPath("$.content[0].game.title").value("Elden Ring"))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/me/feed should return empty when user follows nobody")
    @WithMockUser(username = "lonely@test.com")
    void getFeed_shouldReturnEmptyWhenNoFollowing() throws Exception {
        // Given
        Page<FeedItemDto> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        PagedResponseDto<FeedItemDto> response = PagedResponseDto.from(emptyPage);

        when(feedService.getFeed(eq("lonely@test.com"), anyInt(), anyInt())).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/me/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.metadata.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/me/friends/trending-games should return games")
    @WithMockUser(username = "user@test.com")
    void getFriendsTrendingGames_shouldReturnGames() throws Exception {
        // Given
        GameCardDto game = new GameCardDto(
                UUID.randomUUID(), "Elden Ring", "cover.jpg",
                LocalDate.of(2022, 2, 25), 4.9, 5000L
        );

        when(feedService.getFriendsTrendingGames(eq("user@test.com"), anyInt()))
                .thenReturn(List.of(game));

        // When / Then
        mockMvc.perform(get("/api/me/friends/trending-games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Elden Ring"))
                .andExpect(jsonPath("$[0].ratingCount").value(5000));
    }

    @Test
    @DisplayName("GET /api/me/friends/trending-games should return empty when user follows nobody")
    @WithMockUser(username = "lonely@test.com")
    void getFriendsTrendingGames_shouldReturnEmptyWhenNoFollowing() throws Exception {
        // Given
        when(feedService.getFriendsTrendingGames(eq("lonely@test.com"), anyInt()))
                .thenReturn(Collections.emptyList());

        // When / Then
        mockMvc.perform(get("/api/me/friends/trending-games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/me/friends/popular-games should return paginated games")
    @WithMockUser(username = "user@test.com")
    void getFriendsPopularGames_shouldReturnPaginatedGames() throws Exception {
        // Given
        GameCardDto game = new GameCardDto(
                UUID.randomUUID(), "Elden Ring", "cover.jpg",
                LocalDate.of(2022, 2, 25), 4.9, 5000L
        );
        Page<GameCardDto> page = new PageImpl<>(List.of(game), PageRequest.of(0, 32), 1);
        PagedResponseDto<GameCardDto> response = PagedResponseDto.from(page);

        when(feedService.getFriendsPopularGames(eq("user@test.com"), anyInt(), anyInt()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/me/friends/popular-games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Elden Ring"))
                .andExpect(jsonPath("$.content[0].ratingCount").value(5000))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/me/friends/popular-games should return empty when user follows nobody")
    @WithMockUser(username = "lonely@test.com")
    void getFriendsPopularGames_shouldReturnEmptyWhenNoFollowing() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 32), 0);
        PagedResponseDto<GameCardDto> response = PagedResponseDto.from(emptyPage);

        when(feedService.getFriendsPopularGames(eq("lonely@test.com"), anyInt(), anyInt()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/me/friends/popular-games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.metadata.totalElements").value(0));
    }
}
