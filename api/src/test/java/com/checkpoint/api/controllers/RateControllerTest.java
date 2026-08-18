package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.RateRequestDto;
import com.checkpoint.api.dto.catalog.RateResponseDto;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.RateService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = RateController.class)
@AutoConfigureMockMvc(addFilters = false)
class RateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RateService rateService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    private UUID videoGameId;
    private RateResponseDto rateResponseDto;

    @BeforeEach
    void setUp() {
        videoGameId = UUID.randomUUID();
        rateResponseDto = new RateResponseDto(
                UUID.randomUUID(),
                4,
                videoGameId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/v1/me/games/{videoGameId}/rate should return 200 OK with the rating")
    void rateGame_shouldReturnRating() throws Exception {
        // Given
        RateRequestDto request = new RateRequestDto(4);
        when(rateService.rateGame(eq("testuser"), eq(videoGameId), eq(4)))
                .thenReturn(rateResponseDto);

        // When & Then
        mockMvc.perform(put("/api/v1/me/games/{videoGameId}/rate", videoGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(4))
                .andExpect(jsonPath("$.videoGameId").value(videoGameId.toString()));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/v1/me/games/{videoGameId}/rate should return 400 when score is missing")
    void rateGame_shouldReturn400WhenScoreMissing() throws Exception {
        // Given
        String requestBody = "{}";

        // When & Then
        mockMvc.perform(put("/api/v1/me/games/{videoGameId}/rate", videoGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/v1/me/games/{videoGameId}/rate should return 400 when score is out of range")
    void rateGame_shouldReturn400WhenScoreOutOfRange() throws Exception {
        // Given
        RateRequestDto request = new RateRequestDto(11);

        // When & Then
        mockMvc.perform(put("/api/v1/me/games/{videoGameId}/rate", videoGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/v1/me/games/{videoGameId}/rate should accept half-star scores (1-10)")
    void rateGame_shouldAcceptHalfStarScore() throws Exception {
        // Given: score 7 represents 3.5★
        RateRequestDto request = new RateRequestDto(7);
        RateResponseDto halfStarResponse = new RateResponseDto(
                UUID.randomUUID(),
                7,
                videoGameId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(rateService.rateGame(eq("testuser"), eq(videoGameId), eq(7)))
                .thenReturn(halfStarResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/me/games/{videoGameId}/rate", videoGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(7));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/v1/me/games/{videoGameId}/rate should return 204 No Content")
    void removeRating_shouldReturn204() throws Exception {
        // Given
        doNothing().when(rateService).removeRating(eq("testuser"), eq(videoGameId));

        // When & Then
        mockMvc.perform(delete("/api/v1/me/games/{videoGameId}/rate", videoGameId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/v1/me/games/{videoGameId}/rate should return 200 OK when rating exists")
    void getMyRating_shouldReturnRating() throws Exception {
        // Given
        when(rateService.getUserRating(eq("testuser"), eq(videoGameId)))
                .thenReturn(rateResponseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/me/games/{videoGameId}/rate", videoGameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(4))
                .andExpect(jsonPath("$.videoGameId").value(videoGameId.toString()));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/v1/me/games/{videoGameId}/rate should return 404 when no rating exists")
    void getMyRating_shouldReturn404WhenNoRatingExists() throws Exception {
        // Given
        when(rateService.getUserRating(eq("testuser"), eq(videoGameId)))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/me/games/{videoGameId}/rate", videoGameId))
                .andExpect(status().isNotFound());
    }
}
