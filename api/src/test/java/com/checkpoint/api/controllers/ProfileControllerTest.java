package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.dto.catalog.ReviewUserDto;
import com.checkpoint.api.dto.collection.WishResponseDto;
import com.checkpoint.api.dto.profile.BadgeDto;
import com.checkpoint.api.dto.profile.UserProfileDto;
import com.checkpoint.api.exceptions.ProfilePrivateException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.ProfileService;

/**
 * Unit tests for {@link ProfileController}.
 */
@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("GET /api/users/{username}")
    class GetUserProfile {

        @Test
        @DisplayName("should return profile for existing user")
        void getUserProfile_shouldReturnProfile() throws Exception {
            // Given
            UserProfileDto profile = new UserProfileDto(
                    UUID.randomUUID(), "testuser", "A bio", null,
                    5, 4500, 5000, false,
                    List.of(new BadgeDto(UUID.randomUUID(), "First Review", null, "Write your first review")),
                    List.of(),
                    10L, 5L, 3L, 7L,
                    null, false, LocalDateTime.now()
            );

            when(profileService.getUserProfile(eq("testuser"), isNull()))
                    .thenReturn(profile);

            // When / Then
            mockMvc.perform(get("/api/users/{username}", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.bio").value("A bio"))
                    .andExpect(jsonPath("$.level").value(5))
                    .andExpect(jsonPath("$.xpPoint").value(4500))
                    .andExpect(jsonPath("$.xpThreshold").value(5000))
                    .andExpect(jsonPath("$.followerCount").value(10))
                    .andExpect(jsonPath("$.followingCount").value(5))
                    .andExpect(jsonPath("$.reviewCount").value(3))
                    .andExpect(jsonPath("$.wishlistCount").value(7))
                    .andExpect(jsonPath("$.badges[0].name").value("First Review"))
                    .andExpect(jsonPath("$.isOwner").value(false));
        }

        @Test
        @DisplayName("should return profile with isFollowing when authenticated")
        @WithMockUser(username = "viewer@example.com")
        void getUserProfile_shouldReturnIsFollowingWhenAuthenticated() throws Exception {
            // Given
            UserProfileDto profile = new UserProfileDto(
                    UUID.randomUUID(), "testuser", null, null,
                    1, 0, 1000, false,
                    List.of(), List.of(), 0L, 0L, 0L, 0L,
                    true, false, LocalDateTime.now()
            );

            when(profileService.getUserProfile(eq("testuser"), eq("viewer@example.com")))
                    .thenReturn(profile);

            // When / Then
            mockMvc.perform(get("/api/users/{username}", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isFollowing").value(true))
                    .andExpect(jsonPath("$.isOwner").value(false));
        }

        @Test
        @DisplayName("should return 404 for non-existing user")
        void getUserProfile_shouldReturn404ForNonExistingUser() throws Exception {
            // Given
            when(profileService.getUserProfile(eq("unknown"), isNull()))
                    .thenThrow(new UserNotFoundException("unknown"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}", "unknown"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}/reviews")
    class GetUserReviews {

        @Test
        @DisplayName("should return paginated reviews for public profile")
        void getUserReviews_shouldReturnPaginatedReviews() throws Exception {
            // Given
            ReviewResponseDto review = new ReviewResponseDto(
                    UUID.randomUUID(), "Great game!", false,
                    LocalDateTime.now(), LocalDateTime.now(),
                    new ReviewUserDto(UUID.randomUUID(), "testuser", null),
                    null, null, null, null, 0, false, 0
            );
            Page<ReviewResponseDto> page = new PageImpl<>(List.of(review));

            when(profileService.getUserReviews(eq("testuser"), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/reviews", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].content").value("Great game!"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 403 for private profile reviews")
        void getUserReviews_shouldReturn403ForPrivateProfile() throws Exception {
            // Given
            when(profileService.getUserReviews(eq("privateuser"), isNull(), any(Pageable.class)))
                    .thenThrow(new ProfilePrivateException("privateuser"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/reviews", "privateuser"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}/wishlist")
    class GetUserWishlist {

        @Test
        @DisplayName("should return paginated wishlist for public profile")
        void getUserWishlist_shouldReturnPaginatedWishlist() throws Exception {
            // Given
            WishResponseDto wish = new WishResponseDto(
                    UUID.randomUUID(), UUID.randomUUID(),
                    "Elden Ring", "https://example.com/cover.jpg",
                    null, null, LocalDateTime.now()
            );
            Page<WishResponseDto> page = new PageImpl<>(List.of(wish));

            when(profileService.getUserWishlist(eq("testuser"), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/wishlist", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Elden Ring"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 403 for private profile wishlist")
        void getUserWishlist_shouldReturn403ForPrivateProfile() throws Exception {
            // Given
            when(profileService.getUserWishlist(eq("privateuser"), isNull(), any(Pageable.class)))
                    .thenThrow(new ProfilePrivateException("privateuser"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/wishlist", "privateuser"))
                    .andExpect(status().isForbidden());
        }
    }
}
