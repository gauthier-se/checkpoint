package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.RecommendedGameDto;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.GameRecommendationService;

@WebMvcTest(controllers = RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameRecommendationService recommendationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/me/games/recommended returns 200 with recommendation list")
    void getRecommended_shouldReturnRecommendations() throws Exception {
        UUID gameId = UUID.randomUUID();
        RecommendedGameDto dto = new RecommendedGameDto(
                gameId, "Cool RPG", "https://cdn/cool.jpg",
                LocalDate.of(2024, 5, 10), 4.5, "Matches your favorite genres: RPG");
        when(recommendationService.getRecommendationsFor(eq("testuser"), eq(10)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/me/games/recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(gameId.toString()))
                .andExpect(jsonPath("$[0].title").value("Cool RPG"))
                .andExpect(jsonPath("$[0].coverUrl").value("https://cdn/cool.jpg"))
                .andExpect(jsonPath("$[0].averageRating").value(4.5))
                .andExpect(jsonPath("$[0].reason").value("Matches your favorite genres: RPG"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/me/games/recommended forwards the size query parameter")
    void getRecommended_shouldForwardSize() throws Exception {
        when(recommendationService.getRecommendationsFor(eq("testuser"), eq(6)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/me/games/recommended").param("size", "6"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/me/games/recommended returns empty array when service returns empty")
    void getRecommended_shouldReturnEmptyArray() throws Exception {
        when(recommendationService.getRecommendationsFor(eq("testuser"), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/me/games/recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
