package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.ReviewCardDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.dto.catalog.ReviewUserDto;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.ReviewService;

/**
 * Unit tests for {@link ReviewController}.
 */
@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    private UUID gameId;
    private ReviewResponseDto reviewResponseDto;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        reviewResponseDto = new ReviewResponseDto(
                UUID.randomUUID(),
                "Great game!",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                new ReviewUserDto(UUID.randomUUID(), "testuser", null),
                UUID.randomUUID(),
                "PC",
                PlayStatus.COMPLETED,
                false,
                0,
                false,
                0
        );
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/reviews")
    class GetReviews {

        @Test
        @DisplayName("should return 200 OK with paginated reviews")
        void getReviews_shouldReturnPaginatedReviews() throws Exception {
            // Given
            when(reviewService.getGameReviews(eq(gameId), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(reviewResponseDto)));

            // When / Then
            mockMvc.perform(get("/api/games/{gameId}/reviews", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].content").value("Great game!"))
                    .andExpect(jsonPath("$.content[0].haveSpoilers").value(false))
                    .andExpect(jsonPath("$.content[0].platformName").value("PC"))
                    .andExpect(jsonPath("$.content[0].playStatus").value("COMPLETED"));
        }

        @Test
        @DisplayName("should return 200 OK with empty page when no reviews exist")
        void getReviews_shouldReturnEmptyPage() throws Exception {
            // Given
            when(reviewService.getGameReviews(eq(gameId), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            // When / Then
            mockMvc.perform(get("/api/games/{gameId}/reviews", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("should return 404 when game does not exist")
        void getReviews_shouldReturn404WhenGameNotFound() throws Exception {
            // Given
            when(reviewService.getGameReviews(eq(gameId), any(), any()))
                    .thenThrow(new GameNotFoundException(gameId));

            // When / Then
            mockMvc.perform(get("/api/games/{gameId}/reviews", gameId))
                    .andExpect(status().isNotFound());
        }
    }

    private ReviewCardDto sampleCard(String content, String gameTitle) {
        return new ReviewCardDto(
                UUID.randomUUID(),
                content,
                false,
                LocalDateTime.now(),
                new ReviewUserDto(UUID.randomUUID(), "alice", null),
                UUID.randomUUID(),
                "PC",
                PlayStatus.COMPLETED,
                false,
                3,
                false,
                0,
                UUID.randomUUID(),
                gameTitle,
                "cover.jpg"
        );
    }

    @Nested
    @DisplayName("GET /api/reviews/popular")
    class GetPopular {

        @Test
        @DisplayName("should return 200 OK with up to N popular reviews")
        void shouldReturnPopularReviews() throws Exception {
            when(reviewService.getPopularReviews(eq(7), any()))
                    .thenReturn(List.of(sampleCard("Best game ever", "Elden Ring")));

            mockMvc.perform(get("/api/reviews/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].content").value("Best game ever"))
                    .andExpect(jsonPath("$[0].gameTitle").value("Elden Ring"))
                    .andExpect(jsonPath("$[0].gameCoverUrl").value("cover.jpg"));
        }

        @Test
        @DisplayName("should return empty array when no reviews exist")
        void shouldReturnEmptyArrayWhenNoReviews() throws Exception {
            when(reviewService.getPopularReviews(anyInt(), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/reviews/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("should accept custom size parameter")
        void shouldAcceptCustomSize() throws Exception {
            when(reviewService.getPopularReviews(eq(3), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/reviews/popular").param("size", "3"))
                    .andExpect(status().isOk());

            verify(reviewService).getPopularReviews(eq(3), any());
        }

        @Test
        @DisplayName("should cap size to maximum 20")
        void shouldCapSizeToMaximum() throws Exception {
            when(reviewService.getPopularReviews(anyInt(), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/reviews/popular").param("size", "100"))
                    .andExpect(status().isOk());

            verify(reviewService).getPopularReviews(eq(20), any());
        }
    }

    @Nested
    @DisplayName("GET /api/reviews/recent")
    class GetRecent {

        @Test
        @DisplayName("should return 200 OK with up to N recent reviews")
        void shouldReturnRecentReviews() throws Exception {
            when(reviewService.getRecentReviews(eq(7), any()))
                    .thenReturn(List.of(
                            sampleCard("Just finished it!", "Hollow Knight"),
                            sampleCard("Played all weekend", "Celeste")
                    ));

            mockMvc.perform(get("/api/reviews/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].gameTitle").value("Hollow Knight"))
                    .andExpect(jsonPath("$[1].gameTitle").value("Celeste"));
        }

        @Test
        @DisplayName("should return empty array when no reviews exist")
        void shouldReturnEmptyArrayWhenNoReviews() throws Exception {
            when(reviewService.getRecentReviews(anyInt(), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/reviews/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("should accept custom size parameter")
        void shouldAcceptCustomSize() throws Exception {
            when(reviewService.getRecentReviews(eq(5), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/reviews/recent").param("size", "5"))
                    .andExpect(status().isOk());

            verify(reviewService).getRecentReviews(eq(5), any());
        }

        @Test
        @DisplayName("should cap size to maximum 20")
        void shouldCapSizeToMaximum() throws Exception {
            when(reviewService.getRecentReviews(anyInt(), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/reviews/recent").param("size", "999"))
                    .andExpect(status().isOk());

            verify(reviewService).getRecentReviews(eq(20), any());
        }
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/reviews/popular")
    class GetPopularForGame {

        @Test
        @DisplayName("should return 200 OK with popular reviews scoped to the game")
        void shouldReturnPopularGameReviews() throws Exception {
            when(reviewService.getPopularGameReviews(eq(gameId), eq(7), any()))
                    .thenReturn(List.of(sampleCard("Loved it", "Elden Ring")));

            mockMvc.perform(get("/api/games/{gameId}/reviews/popular", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].content").value("Loved it"));
        }

        @Test
        @DisplayName("should cap size to maximum 20")
        void shouldCapSizeToMaximum() throws Exception {
            when(reviewService.getPopularGameReviews(eq(gameId), anyInt(), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/games/{gameId}/reviews/popular", gameId).param("size", "1000"))
                    .andExpect(status().isOk());

            verify(reviewService).getPopularGameReviews(eq(gameId), eq(20), any());
        }

        @Test
        @DisplayName("should return 404 when the game does not exist")
        void shouldReturn404WhenGameMissing() throws Exception {
            when(reviewService.getPopularGameReviews(eq(gameId), anyInt(), any()))
                    .thenThrow(new GameNotFoundException(gameId));

            mockMvc.perform(get("/api/games/{gameId}/reviews/popular", gameId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/reviews/from-friends")
    class GetFriendReviewsForGame {

        @Test
        @DisplayName("should return 200 OK with paginated friend reviews")
        void shouldReturnFriendReviews() throws Exception {
            when(reviewService.getFriendReviewsForGame(eq(gameId), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(reviewResponseDto)));

            mockMvc.perform(get("/api/games/{gameId}/reviews/from-friends", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].content").value("Great game!"));
        }

        @Test
        @DisplayName("should return 200 OK with an empty page when viewer is anonymous")
        void shouldReturnEmptyPageWhenAnonymous() throws Exception {
            when(reviewService.getFriendReviewsForGame(eq(gameId), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/games/{gameId}/reviews/from-friends", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("should return 404 when the game does not exist")
        void shouldReturn404WhenGameMissing() throws Exception {
            when(reviewService.getFriendReviewsForGame(eq(gameId), any(), any()))
                    .thenThrow(new GameNotFoundException(gameId));

            mockMvc.perform(get("/api/games/{gameId}/reviews/from-friends", gameId))
                    .andExpect(status().isNotFound());
        }
    }
}
