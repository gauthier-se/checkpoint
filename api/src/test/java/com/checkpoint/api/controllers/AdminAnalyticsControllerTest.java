package com.checkpoint.api.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.admin.AdminAnalyticsDto;
import com.checkpoint.api.dto.admin.AdminAnalyticsDto.TopGame;
import com.checkpoint.api.dto.admin.AdminAnalyticsDto.TopReviewer;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.AdminAnalyticsService;

/**
 * Unit tests for {@link AdminAnalyticsController}.
 */
@WebMvcTest(AdminAnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAnalyticsService adminAnalyticsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @DisplayName("Should return the full analytics payload")
    void shouldReturnAnalyticsPayload() throws Exception {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AdminAnalyticsDto analytics = new AdminAnalyticsDto(
                42L,
                40L,
                100L,
                250L,
                3L,
                List.of(new TopGame(gameId, "Hades", 17L)),
                List.of(new TopReviewer(userId, "alice", 9L))
        );
        when(adminAnalyticsService.getAnalytics()).thenReturn(analytics);

        // When & Then
        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(42))
                .andExpect(jsonPath("$.activeUsers").value(40))
                .andExpect(jsonPath("$.totalGames").value(100))
                .andExpect(jsonPath("$.totalReviews").value(250))
                .andExpect(jsonPath("$.openReports").value(3))
                .andExpect(jsonPath("$.topReviewedGames.length()").value(1))
                .andExpect(jsonPath("$.topReviewedGames[0].id").value(gameId.toString()))
                .andExpect(jsonPath("$.topReviewedGames[0].title").value("Hades"))
                .andExpect(jsonPath("$.topReviewedGames[0].reviewCount").value(17))
                .andExpect(jsonPath("$.topReviewers.length()").value(1))
                .andExpect(jsonPath("$.topReviewers[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.topReviewers[0].username").value("alice"))
                .andExpect(jsonPath("$.topReviewers[0].reviewCount").value(9));

        verify(adminAnalyticsService).getAnalytics();
    }

    @Test
    @DisplayName("Should return empty top lists gracefully")
    void shouldReturnEmptyTopLists() throws Exception {
        // Given
        AdminAnalyticsDto analytics = new AdminAnalyticsDto(
                0L, 0L, 0L, 0L, 0L, List.of(), List.of()
        );
        when(adminAnalyticsService.getAnalytics()).thenReturn(analytics);

        // When & Then
        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(0))
                .andExpect(jsonPath("$.topReviewedGames.length()").value(0))
                .andExpect(jsonPath("$.topReviewers.length()").value(0));

        verify(adminAnalyticsService).getAnalytics();
    }
}
