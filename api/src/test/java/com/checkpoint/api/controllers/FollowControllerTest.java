package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.checkpoint.api.dto.social.FollowResponseDto;
import com.checkpoint.api.dto.social.FollowUserDto;
import com.checkpoint.api.exceptions.SelfFollowException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.FollowService;

/**
 * Unit tests for {@link FollowController}.
 */
@WebMvcTest(FollowController.class)
@AutoConfigureMockMvc(addFilters = false)
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("POST /api/users/{userId}/follow")
    class ToggleFollow {

        @Test
        @DisplayName("should follow user and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleFollow_shouldFollowUser() throws Exception {
            // Given
            UUID targetUserId = UUID.randomUUID();
            FollowResponseDto response = new FollowResponseDto(true, "Successfully followed TestUser");

            when(followService.toggleFollow(eq("user@example.com"), eq(targetUserId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/users/{userId}/follow", targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.following").value(true))
                    .andExpect(jsonPath("$.message").value("Successfully followed TestUser"));
        }

        @Test
        @DisplayName("should unfollow user and return 200")
        @WithMockUser(username = "user@example.com")
        void toggleFollow_shouldUnfollowUser() throws Exception {
            // Given
            UUID targetUserId = UUID.randomUUID();
            FollowResponseDto response = new FollowResponseDto(false, "Successfully unfollowed TestUser");

            when(followService.toggleFollow(eq("user@example.com"), eq(targetUserId)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/users/{userId}/follow", targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.following").value(false))
                    .andExpect(jsonPath("$.message").value("Successfully unfollowed TestUser"));
        }

        @Test
        @DisplayName("should return 400 when user tries to follow themselves")
        @WithMockUser(username = "user@example.com")
        void toggleFollow_shouldReturn400WhenSelfFollow() throws Exception {
            // Given
            UUID targetUserId = UUID.randomUUID();

            when(followService.toggleFollow(eq("user@example.com"), eq(targetUserId)))
                    .thenThrow(new SelfFollowException());

            // When / Then
            mockMvc.perform(post("/api/users/{userId}/follow", targetUserId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("should return 404 when target user not found")
        @WithMockUser(username = "user@example.com")
        void toggleFollow_shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            UUID targetUserId = UUID.randomUUID();

            when(followService.toggleFollow(eq("user@example.com"), eq(targetUserId)))
                    .thenThrow(new UserNotFoundException(targetUserId));

            // When / Then
            mockMvc.perform(post("/api/users/{userId}/follow", targetUserId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{userId}/followers")
    class GetFollowers {

        @Test
        @DisplayName("should return paginated followers")
        void getFollowers_shouldReturnPaginatedFollowers() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            List<FollowUserDto> followers = List.of(
                    new FollowUserDto(UUID.randomUUID(), "follower1", "pic1.jpg"),
                    new FollowUserDto(UUID.randomUUID(), "follower2", "pic2.jpg")
            );
            Page<FollowUserDto> page = new PageImpl<>(followers);

            when(followService.getFollowers(eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/users/{userId}/followers", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].pseudo").value("follower1"))
                    .andExpect(jsonPath("$.content[1].pseudo").value("follower2"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(2));
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void getFollowers_shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();

            when(followService.getFollowers(eq(userId), any(Pageable.class)))
                    .thenThrow(new UserNotFoundException(userId));

            // When / Then
            mockMvc.perform(get("/api/users/{userId}/followers", userId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should accept pagination parameters")
        void getFollowers_shouldAcceptPaginationParams() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            Page<FollowUserDto> emptyPage = new PageImpl<>(List.of());

            when(followService.getFollowers(eq(userId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When / Then
            mockMvc.perform(get("/api/users/{userId}/followers", userId)
                            .param("page", "1")
                            .param("size", "10")
                            .param("sort", "pseudo,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{userId}/following")
    class GetFollowing {

        @Test
        @DisplayName("should return paginated following list")
        void getFollowing_shouldReturnPaginatedFollowing() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            List<FollowUserDto> following = List.of(
                    new FollowUserDto(UUID.randomUUID(), "followed1", "pic1.jpg")
            );
            Page<FollowUserDto> page = new PageImpl<>(following);

            when(followService.getFollowing(eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/users/{userId}/following", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].pseudo").value("followed1"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void getFollowing_shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();

            when(followService.getFollowing(eq(userId), any(Pageable.class)))
                    .thenThrow(new UserNotFoundException(userId));

            // When / Then
            mockMvc.perform(get("/api/users/{userId}/following", userId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/me/followers/{followerId}")
    class RemoveFollower {

        @Test
        @DisplayName("should remove follower and return 204")
        @WithMockUser(username = "user@example.com")
        void removeFollower_shouldReturn204() throws Exception {
            // Given
            UUID followerId = UUID.randomUUID();
            doNothing().when(followService).removeFollower(eq("user@example.com"), eq(followerId));

            // When / Then
            mockMvc.perform(delete("/api/users/me/followers/{followerId}", followerId))
                    .andExpect(status().isNoContent());

            verify(followService).removeFollower(eq("user@example.com"), eq(followerId));
        }

        @Test
        @DisplayName("should return 404 when follower not found")
        @WithMockUser(username = "user@example.com")
        void removeFollower_shouldReturn404WhenFollowerNotFound() throws Exception {
            // Given
            UUID followerId = UUID.randomUUID();
            doThrow(new UserNotFoundException(followerId))
                    .when(followService).removeFollower(eq("user@example.com"), eq(followerId));

            // When / Then
            mockMvc.perform(delete("/api/users/me/followers/{followerId}", followerId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
