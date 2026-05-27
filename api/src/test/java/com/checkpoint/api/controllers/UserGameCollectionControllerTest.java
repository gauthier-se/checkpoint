package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import com.checkpoint.api.dto.collection.UserGameRequestDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.exceptions.GameAlreadyInLibraryException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameNotInLibraryException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.UserGameCollectionService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link UserGameCollectionController}.
 */
@WebMvcTest(UserGameCollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserGameCollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserGameCollectionService userGameCollectionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("POST /api/me/library")
    class AddGameToLibrary {

        @Test
        @DisplayName("should add a game to library and return 201")
        @WithMockUser(username = "user@example.com")
        void addGame_shouldReturn201() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UUID userGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, GameStatus.PLAYING, null);
            UserGameResponseDto response = new UserGameResponseDto(
                    userGameId, videoGameId, "The Witcher 3", "cover.jpg",
                    LocalDate.of(2015, 5, 19), GameStatus.PLAYING,
                    LocalDateTime.now(), LocalDateTime.now(), null);

            when(userGameCollectionService.addGameToLibrary(eq("user@example.com"), any(UserGameRequestDto.class)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/me/library")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(userGameId.toString()))
                    .andExpect(jsonPath("$.videoGameId").value(videoGameId.toString()))
                    .andExpect(jsonPath("$.title").value("The Witcher 3"))
                    .andExpect(jsonPath("$.status").value("PLAYING"))
                    .andExpect(jsonPath("$.notes").doesNotExist());
        }

        @Test
        @DisplayName("should add a game with notes and return 201")
        @WithMockUser(username = "user@example.com")
        void addGame_withNotes_shouldReturn201() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UUID userGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, GameStatus.PLAYING, "Strategy note");
            UserGameResponseDto response = new UserGameResponseDto(
                    userGameId, videoGameId, "The Witcher 3", "cover.jpg",
                    LocalDate.of(2015, 5, 19), GameStatus.PLAYING,
                    LocalDateTime.now(), LocalDateTime.now(), "Strategy note");

            when(userGameCollectionService.addGameToLibrary(eq("user@example.com"), any(UserGameRequestDto.class)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/me/library")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.notes").value("Strategy note"));
        }

        @Test
        @DisplayName("should return 409 when game already in library")
        @WithMockUser(username = "user@example.com")
        void addGame_shouldReturn409WhenAlreadyInLibrary() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, GameStatus.PLAYING, null);

            when(userGameCollectionService.addGameToLibrary(eq("user@example.com"), any(UserGameRequestDto.class)))
                    .thenThrow(new GameAlreadyInLibraryException(videoGameId));

            // When / Then
            mockMvc.perform(post("/api/me/library")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"));
        }

        @Test
        @DisplayName("should return 404 when video game not found")
        @WithMockUser(username = "user@example.com")
        void addGame_shouldReturn404WhenGameNotFound() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, GameStatus.PLAYING, null);

            when(userGameCollectionService.addGameToLibrary(eq("user@example.com"), any(UserGameRequestDto.class)))
                    .thenThrow(new GameNotFoundException(videoGameId));

            // When / Then
            mockMvc.perform(post("/api/me/library")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should return 400 when request body is invalid")
        @WithMockUser(username = "user@example.com")
        void addGame_shouldReturn400WhenInvalidBody() throws Exception {
            // Given — missing required fields
            String invalidJson = "{}";

            // When / Then
            mockMvc.perform(post("/api/me/library")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/me/library/{videoGameId}")
    class UpdateGameStatus {

        @Test
        @DisplayName("should update game status and return 200")
        @WithMockUser(username = "user@example.com")
        void updateStatus_shouldReturn200() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UUID userGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, GameStatus.COMPLETED, "Finished it!");
            UserGameResponseDto response = new UserGameResponseDto(
                    userGameId, videoGameId, "The Witcher 3", "cover.jpg",
                    LocalDate.of(2015, 5, 19), GameStatus.COMPLETED,
                    LocalDateTime.now(), LocalDateTime.now(), "Finished it!");

            when(userGameCollectionService.updateGameStatus(eq("user@example.com"), eq(videoGameId), any(UserGameRequestDto.class)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(put("/api/me/library/{videoGameId}", videoGameId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.notes").value("Finished it!"));
        }

        @Test
        @DisplayName("should clear notes when request notes is null")
        @WithMockUser(username = "user@example.com")
        void updateStatus_shouldClearNotesWhenNull() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UUID userGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, GameStatus.PLAYING, null);
            UserGameResponseDto response = new UserGameResponseDto(
                    userGameId, videoGameId, "The Witcher 3", "cover.jpg",
                    LocalDate.of(2015, 5, 19), GameStatus.PLAYING,
                    LocalDateTime.now(), LocalDateTime.now(), null);

            when(userGameCollectionService.updateGameStatus(eq("user@example.com"), eq(videoGameId), any(UserGameRequestDto.class)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(put("/api/me/library/{videoGameId}", videoGameId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").doesNotExist());
        }

        @Test
        @DisplayName("should return 400 when notes exceed 2000 characters")
        @WithMockUser(username = "user@example.com")
        void updateStatus_shouldReturn400WhenNotesTooLong() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            String tooLong = "x".repeat(2001);
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, GameStatus.PLAYING, tooLong);

            // When / Then
            mockMvc.perform(put("/api/me/library/{videoGameId}", videoGameId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when game not in library")
        @WithMockUser(username = "user@example.com")
        void updateStatus_shouldReturn404WhenNotInLibrary() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UserGameRequestDto request = new UserGameRequestDto(videoGameId, GameStatus.DROPPED, null);

            when(userGameCollectionService.updateGameStatus(eq("user@example.com"), eq(videoGameId), any(UserGameRequestDto.class)))
                    .thenThrow(new GameNotInLibraryException(videoGameId));

            // When / Then
            mockMvc.perform(put("/api/me/library/{videoGameId}", videoGameId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/me/library")
    class GetUserLibrary {

        @Test
        @DisplayName("should return paginated library")
        @WithMockUser(username = "user@example.com")
        void getLibrary_shouldReturnPaginatedLibrary() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            UUID userGameId = UUID.randomUUID();
            List<UserGameResponseDto> items = List.of(
                    new UserGameResponseDto(userGameId, videoGameId, "Elden Ring", "cover.jpg",
                            LocalDate.of(2022, 2, 25), GameStatus.PLAYING,
                            LocalDateTime.now(), LocalDateTime.now(), null)
            );
            Page<UserGameResponseDto> page = new PageImpl<>(items);

            when(userGameCollectionService.getUserLibrary(eq("user@example.com"), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/me/library"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].title").value("Elden Ring"))
                    .andExpect(jsonPath("$.content[0].status").value("PLAYING"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should accept pagination parameters")
        @WithMockUser(username = "user@example.com")
        void getLibrary_shouldAcceptPaginationParams() throws Exception {
            // Given
            Page<UserGameResponseDto> emptyPage = new PageImpl<>(List.of());
            when(userGameCollectionService.getUserLibrary(eq("user@example.com"), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When / Then
            mockMvc.perform(get("/api/me/library")
                            .param("page", "1")
                            .param("size", "10")
                            .param("sort", "status,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("DELETE /api/me/library/{videoGameId}")
    class RemoveGameFromLibrary {

        @Test
        @DisplayName("should remove game and return 204")
        @WithMockUser(username = "user@example.com")
        void removeGame_shouldReturn204() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            doNothing().when(userGameCollectionService)
                    .removeGameFromLibrary("user@example.com", videoGameId);

            // When / Then
            mockMvc.perform(delete("/api/me/library/{videoGameId}", videoGameId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when game not in library")
        @WithMockUser(username = "user@example.com")
        void removeGame_shouldReturn404WhenNotInLibrary() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            doThrow(new GameNotInLibraryException(videoGameId))
                    .when(userGameCollectionService)
                    .removeGameFromLibrary("user@example.com", videoGameId);

            // When / Then
            mockMvc.perform(delete("/api/me/library/{videoGameId}", videoGameId))
                    .andExpect(status().isNotFound());
        }
    }
}
