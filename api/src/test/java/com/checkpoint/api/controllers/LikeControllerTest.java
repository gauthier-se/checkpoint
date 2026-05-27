package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.dto.social.LikeResponseDto;
import com.checkpoint.api.exceptions.CommentNotFoundException;
import com.checkpoint.api.exceptions.GameListNotFoundException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.ReviewNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.LikeService;

/**
 * Unit tests for {@link LikeController}.
 */
@WebMvcTest(LikeController.class)
@AutoConfigureMockMvc(addFilters = false)
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LikeService likeService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("POST /api/reviews/{reviewId}/like")
    class ToggleReviewLike {

        @Test
        @DisplayName("should like review and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleReviewLike_shouldLikeReview() throws Exception {
            // Given
            UUID reviewId = UUID.randomUUID();
            LikeResponseDto response = new LikeResponseDto(true, 5);

            when(likeService.toggleReviewLike(eq("user@example.com"), eq(reviewId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/reviews/{reviewId}/like", reviewId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true))
                    .andExpect(jsonPath("$.likesCount").value(5));
        }

        @Test
        @DisplayName("should unlike review and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleReviewLike_shouldUnlikeReview() throws Exception {
            // Given
            UUID reviewId = UUID.randomUUID();
            LikeResponseDto response = new LikeResponseDto(false, 4);

            when(likeService.toggleReviewLike(eq("user@example.com"), eq(reviewId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/reviews/{reviewId}/like", reviewId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(false))
                    .andExpect(jsonPath("$.likesCount").value(4));
        }

        @Test
        @DisplayName("should return 404 when review not found")
        @WithMockUser(username = "user@example.com")
        void toggleReviewLike_shouldReturn404WhenReviewNotFound() throws Exception {
            // Given
            UUID reviewId = UUID.randomUUID();

            when(likeService.toggleReviewLike(eq("user@example.com"), eq(reviewId)))
                    .thenThrow(new ReviewNotFoundException("Review not found with ID: " + reviewId));

            // When / Then
            mockMvc.perform(post("/api/reviews/{reviewId}/like", reviewId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("POST /api/lists/{listId}/like")
    class ToggleListLike {

        @Test
        @DisplayName("should like list and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleListLike_shouldLikeList() throws Exception {
            // Given
            UUID listId = UUID.randomUUID();
            LikeResponseDto response = new LikeResponseDto(true, 10);

            when(likeService.toggleListLike(eq("user@example.com"), eq(listId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/lists/{listId}/like", listId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true))
                    .andExpect(jsonPath("$.likesCount").value(10));
        }

        @Test
        @DisplayName("should unlike list and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleListLike_shouldUnlikeList() throws Exception {
            // Given
            UUID listId = UUID.randomUUID();
            LikeResponseDto response = new LikeResponseDto(false, 9);

            when(likeService.toggleListLike(eq("user@example.com"), eq(listId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/lists/{listId}/like", listId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(false))
                    .andExpect(jsonPath("$.likesCount").value(9));
        }

        @Test
        @DisplayName("should return 404 when list not found")
        @WithMockUser(username = "user@example.com")
        void toggleListLike_shouldReturn404WhenListNotFound() throws Exception {
            // Given
            UUID listId = UUID.randomUUID();

            when(likeService.toggleListLike(eq("user@example.com"), eq(listId)))
                    .thenThrow(new GameListNotFoundException(listId));

            // When / Then
            mockMvc.perform(post("/api/lists/{listId}/like", listId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("POST /api/comments/{commentId}/like")
    class ToggleCommentLike {

        @Test
        @DisplayName("should like comment and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleCommentLike_shouldLikeComment() throws Exception {
            // Given
            UUID commentId = UUID.randomUUID();
            LikeResponseDto response = new LikeResponseDto(true, 3);

            when(likeService.toggleCommentLike(eq("user@example.com"), eq(commentId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/comments/{commentId}/like", commentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true))
                    .andExpect(jsonPath("$.likesCount").value(3));
        }

        @Test
        @DisplayName("should unlike comment and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleCommentLike_shouldUnlikeComment() throws Exception {
            // Given
            UUID commentId = UUID.randomUUID();
            LikeResponseDto response = new LikeResponseDto(false, 2);

            when(likeService.toggleCommentLike(eq("user@example.com"), eq(commentId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/comments/{commentId}/like", commentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(false))
                    .andExpect(jsonPath("$.likesCount").value(2));
        }

        @Test
        @DisplayName("should return 404 when comment not found")
        @WithMockUser(username = "user@example.com")
        void toggleCommentLike_shouldReturn404WhenCommentNotFound() throws Exception {
            // Given
            UUID commentId = UUID.randomUUID();

            when(likeService.toggleCommentLike(eq("user@example.com"), eq(commentId)))
                    .thenThrow(new CommentNotFoundException(commentId));

            // When / Then
            mockMvc.perform(post("/api/comments/{commentId}/like", commentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("POST /api/me/games/{videoGameId}/like")
    class ToggleGameLike {

        @Test
        @DisplayName("should like game and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleGameLike_shouldLikeGame() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            LikeResponseDto response = new LikeResponseDto(true, 7);

            when(likeService.toggleGameLike(eq("user@example.com"), eq(videoGameId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/me/games/{videoGameId}/like", videoGameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true))
                    .andExpect(jsonPath("$.likesCount").value(7));
        }

        @Test
        @DisplayName("should unlike game and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleGameLike_shouldUnlikeGame() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();
            LikeResponseDto response = new LikeResponseDto(false, 6);

            when(likeService.toggleGameLike(eq("user@example.com"), eq(videoGameId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/me/games/{videoGameId}/like", videoGameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(false))
                    .andExpect(jsonPath("$.likesCount").value(6));
        }

        @Test
        @DisplayName("should return 404 when game not found")
        @WithMockUser(username = "user@example.com")
        void toggleGameLike_shouldReturn404WhenGameNotFound() throws Exception {
            // Given
            UUID videoGameId = UUID.randomUUID();

            when(likeService.toggleGameLike(eq("user@example.com"), eq(videoGameId)))
                    .thenThrow(new GameNotFoundException(videoGameId));

            // When / Then
            mockMvc.perform(post("/api/me/games/{videoGameId}/like", videoGameId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/me/likes")
    class GetLikedGames {

        @Test
        @DisplayName("should return the user's liked games as a paginated response")
        @WithMockUser(username = "user@example.com")
        void getLikedGames_shouldReturnPaginatedLikes() throws Exception {
            // Given
            LikedGameResponseDto dto = new LikedGameResponseDto(
                    UUID.randomUUID(), UUID.randomUUID(), "Elden Ring",
                    "https://example.com/cover.jpg", LocalDate.of(2022, 2, 25), LocalDateTime.now());
            Page<LikedGameResponseDto> page = new PageImpl<>(List.of(dto));

            when(likeService.getLikedGames(eq("user@example.com"), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/me/likes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].title").value("Elden Ring"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should return an empty page when the user has no liked games")
        @WithMockUser(username = "user@example.com")
        void getLikedGames_shouldReturnEmptyPage() throws Exception {
            // Given
            when(likeService.getLikedGames(eq("user@example.com"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // When / Then
            mockMvc.perform(get("/api/me/likes")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
