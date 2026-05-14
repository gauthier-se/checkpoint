package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.collection.GameInteractionStatusDto;
import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.GameInteractionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(GameInteractionController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameInteractionService gameInteractionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/me/games/{videoGameId}/status should return 200 and status")
    @WithMockUser(username = "user@example.com")
    void getGameInteractionStatus_shouldReturn200() throws Exception {
        // Given
        UUID videoGameId = UUID.randomUUID();

        GameInteractionStatusDto response = new GameInteractionStatusDto(
                true, null, false, null, true, GameStatus.COMPLETED, "My private notes", 1, 4, true, 4
        );

        when(gameInteractionService.getGameInteractionStatus(eq("user@example.com"), eq(videoGameId)))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/me/games/{videoGameId}/status", videoGameId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inWishlist").value(true))
                .andExpect(jsonPath("$.inBacklog").value(false))
                .andExpect(jsonPath("$.inLibrary").value(true))
                .andExpect(jsonPath("$.libraryStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.libraryNotes").value("My private notes"))
                .andExpect(jsonPath("$.playCount").value(1))
                .andExpect(jsonPath("$.userRating").value(4))
                .andExpect(jsonPath("$.hasReview").value(true))
                .andExpect(jsonPath("$.lastPlayRating").value(4));
    }
}
