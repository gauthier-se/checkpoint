package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

import com.checkpoint.api.dto.profile.FavoriteDto;
import com.checkpoint.api.dto.profile.UpdateFavoritesDto;
import com.checkpoint.api.exceptions.InvalidFavoritesException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.FavoriteService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link FavoriteController}.
 */
@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @DisplayName("PUT /api/me/favorites returns 200 with the ordered list")
    @WithMockUser(username = "alice@test.com")
    void replaceFavorites_shouldReturnOrderedList() throws Exception {
        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();
        UpdateFavoritesDto request = new UpdateFavoritesDto(List.of(g1, g2));

        List<FavoriteDto> response = List.of(
                new FavoriteDto(g1, "Game One", "/covers/1.jpg", 0),
                new FavoriteDto(g2, "Game Two", "/covers/2.jpg", 1)
        );

        when(favoriteService.replaceFavorites(eq("alice@test.com"), anyList())).thenReturn(response);

        mockMvc.perform(put("/api/me/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gameId").value(g1.toString()))
                .andExpect(jsonPath("$[0].displayOrder").value(0))
                .andExpect(jsonPath("$[1].gameId").value(g2.toString()))
                .andExpect(jsonPath("$[1].displayOrder").value(1));

        verify(favoriteService).replaceFavorites(eq("alice@test.com"), anyList());
    }

    @Test
    @DisplayName("PUT /api/me/favorites returns 400 when more than 5 ids are sent")
    @WithMockUser(username = "alice@test.com")
    void replaceFavorites_shouldReturn400WhenMoreThanFive() throws Exception {
        List<UUID> ids = List.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        UpdateFavoritesDto request = new UpdateFavoritesDto(ids);

        mockMvc.perform(put("/api/me/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/me/favorites returns 400 when service flags duplicate ids")
    @WithMockUser(username = "alice@test.com")
    void replaceFavorites_shouldReturn400WhenDuplicates() throws Exception {
        UUID g = UUID.randomUUID();
        UpdateFavoritesDto request = new UpdateFavoritesDto(List.of(g, g));

        when(favoriteService.replaceFavorites(eq("alice@test.com"), anyList()))
                .thenThrow(new InvalidFavoritesException("Duplicate gameIds are not allowed"));

        mockMvc.perform(put("/api/me/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/me/favorites returns 400 when service flags unknown gameId")
    @WithMockUser(username = "alice@test.com")
    void replaceFavorites_shouldReturn400WhenUnknownGameId() throws Exception {
        UpdateFavoritesDto request = new UpdateFavoritesDto(List.of(UUID.randomUUID()));

        when(favoriteService.replaceFavorites(eq("alice@test.com"), anyList()))
                .thenThrow(new InvalidFavoritesException("One or more gameIds do not exist"));

        mockMvc.perform(put("/api/me/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
