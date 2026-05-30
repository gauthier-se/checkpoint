package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.dto.catalog.ReviewUserDto;
import com.checkpoint.api.dto.collection.BacklogResponseDto;
import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.dto.collection.WishResponseDto;
import com.checkpoint.api.dto.playlog.GamePlayLogResponseDto;
import com.checkpoint.api.dto.profile.BadgeDto;
import com.checkpoint.api.dto.profile.CommonGameEntryDto;
import com.checkpoint.api.dto.profile.ProfileComparisonDto;
import com.checkpoint.api.dto.profile.RatingDistributionEntryDto;
import com.checkpoint.api.dto.profile.RecentPlayDto;
import com.checkpoint.api.dto.profile.UserProfileDto;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.enums.Priority;
import com.checkpoint.api.exceptions.ProfilePrivateException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.ProfileComparisonService;
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
    private ProfileComparisonService profileComparisonService;

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
            UUID playId = UUID.randomUUID();
            UUID gameId = UUID.randomUUID();
            RecentPlayDto recentPlay = new RecentPlayDto(
                    playId, gameId, "Elden Ring", "/covers/elden.png",
                    9, true, false, true, LocalDateTime.now()
            );
            UserProfileDto profile = new UserProfileDto(
                    UUID.randomUUID(), "testuser", "A bio", null,
                    5, 4500, 5000, false,
                    List.of(new BadgeDto(UUID.randomUUID(), "FIRST_REVIEW", "First Review",
                            null, "Write your first review", false, true)),
                    List.of(),
                    List.of(recentPlay),
                    10L, 5L, 3L, 7L,
                    List.of(new RatingDistributionEntryDto(9, 2L)),
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
                    .andExpect(jsonPath("$.recentPlays[0].id").value(playId.toString()))
                    .andExpect(jsonPath("$.recentPlays[0].videoGameId").value(gameId.toString()))
                    .andExpect(jsonPath("$.recentPlays[0].title").value("Elden Ring"))
                    .andExpect(jsonPath("$.recentPlays[0].score").value(9))
                    .andExpect(jsonPath("$.recentPlays[0].hasReview").value(true))
                    .andExpect(jsonPath("$.recentPlays[0].isReplay").value(false))
                    .andExpect(jsonPath("$.recentPlays[0].isLiked").value(true))
                    .andExpect(jsonPath("$.ratingDistribution[0].score").value(9))
                    .andExpect(jsonPath("$.ratingDistribution[0].count").value(2))
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
                    List.of(), List.of(), List.of(), 0L, 0L, 0L, 0L,
                    List.of(),
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

    @Nested
    @DisplayName("GET /api/users/{username}/likes")
    class GetUserLikedGames {

        @Test
        @DisplayName("should return paginated liked games for public profile")
        void getUserLikedGames_shouldReturnPaginatedLikes() throws Exception {
            // Given
            LikedGameResponseDto liked = new LikedGameResponseDto(
                    UUID.randomUUID(), UUID.randomUUID(),
                    "Hollow Knight", "https://example.com/cover.jpg",
                    LocalDate.of(2017, 2, 24), LocalDateTime.now()
            );
            Page<LikedGameResponseDto> page = new PageImpl<>(List.of(liked));

            when(profileService.getUserLikedGames(eq("testuser"), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/likes", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Hollow Knight"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 403 for private profile liked games")
        void getUserLikedGames_shouldReturn403ForPrivateProfile() throws Exception {
            // Given
            when(profileService.getUserLikedGames(eq("privateuser"), isNull(), any(Pageable.class)))
                    .thenThrow(new ProfilePrivateException("privateuser"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/likes", "privateuser"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 for unknown user")
        void getUserLikedGames_shouldReturn404ForUnknownUser() throws Exception {
            // Given
            when(profileService.getUserLikedGames(eq("ghost"), isNull(), any(Pageable.class)))
                    .thenThrow(new UserNotFoundException("ghost"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/likes", "ghost"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}/library")
    class GetUserLibrary {

        @Test
        @DisplayName("should return paginated library for public profile")
        void getUserLibrary_shouldReturnPaginatedLibrary() throws Exception {
            // Given
            UserGameResponseDto game = new UserGameResponseDto(
                    UUID.randomUUID(), UUID.randomUUID(),
                    "Celeste", "https://example.com/celeste.jpg",
                    LocalDate.of(2018, 1, 25), PlayStatus.COMPLETED,
                    LocalDateTime.now(), LocalDateTime.now(), null, 5.0
            );
            Page<UserGameResponseDto> page = new PageImpl<>(List.of(game));

            when(profileService.getUserLibrary(eq("testuser"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/library", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Celeste"))
                    .andExpect(jsonPath("$.content[0].userRating").value(5.0))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should pass status filter to the service")
        void getUserLibrary_shouldPassStatusFilter() throws Exception {
            // Given
            Page<UserGameResponseDto> emptyPage = new PageImpl<>(List.of());
            when(profileService.getUserLibrary(eq("testuser"), isNull(), eq(PlayStatus.COMPLETED), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/library", "testuser").param("status", "COMPLETED"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 for private profile library")
        void getUserLibrary_shouldReturn403ForPrivateProfile() throws Exception {
            // Given
            when(profileService.getUserLibrary(eq("privateuser"), isNull(), isNull(), any(Pageable.class)))
                    .thenThrow(new ProfilePrivateException("privateuser"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/library", "privateuser"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 for unknown user")
        void getUserLibrary_shouldReturn404ForUnknownUser() throws Exception {
            // Given
            when(profileService.getUserLibrary(eq("ghost"), isNull(), isNull(), any(Pageable.class)))
                    .thenThrow(new UserNotFoundException("ghost"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/library", "ghost"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}/backlog")
    class GetUserBacklog {

        @Test
        @DisplayName("should return paginated backlog for public profile")
        void getUserBacklog_shouldReturnPaginatedBacklog() throws Exception {
            // Given
            BacklogResponseDto game = new BacklogResponseDto(
                    UUID.randomUUID(), UUID.randomUUID(),
                    "Hades", "https://example.com/hades.jpg",
                    LocalDate.of(2020, 9, 17), Priority.HIGH, LocalDateTime.now()
            );
            Page<BacklogResponseDto> page = new PageImpl<>(List.of(game));

            when(profileService.getUserBacklog(eq("testuser"), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/backlog", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Hades"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 403 for private profile backlog")
        void getUserBacklog_shouldReturn403ForPrivateProfile() throws Exception {
            // Given
            when(profileService.getUserBacklog(eq("privateuser"), isNull(), any(Pageable.class)))
                    .thenThrow(new ProfilePrivateException("privateuser"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/backlog", "privateuser"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 for unknown user")
        void getUserBacklog_shouldReturn404ForUnknownUser() throws Exception {
            // Given
            when(profileService.getUserBacklog(eq("ghost"), isNull(), any(Pageable.class)))
                    .thenThrow(new UserNotFoundException("ghost"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/backlog", "ghost"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}/plays")
    class GetUserPlayLog {

        @Test
        @DisplayName("should return paginated play log for public profile")
        void getUserPlayLog_shouldReturnPaginatedPlayLog() throws Exception {
            // Given
            GamePlayLogResponseDto play = new GamePlayLogResponseDto(
                    UUID.randomUUID(), UUID.randomUUID(),
                    "Stardew Valley", "https://example.com/stardew.jpg",
                    LocalDate.of(2016, 2, 26), null, null,
                    PlayStatus.COMPLETED, false, 1200, null, null, null,
                    LocalDateTime.now(), LocalDateTime.now(), false, null, null, List.of()
            );
            Page<GamePlayLogResponseDto> page = new PageImpl<>(List.of(play));

            when(profileService.getUserPlayLog(eq("testuser"), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/plays", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Stardew Valley"))
                    .andExpect(jsonPath("$.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 403 for private profile play log")
        void getUserPlayLog_shouldReturn403ForPrivateProfile() throws Exception {
            // Given
            when(profileService.getUserPlayLog(eq("privateuser"), isNull(), any(Pageable.class)))
                    .thenThrow(new ProfilePrivateException("privateuser"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/plays", "privateuser"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 for unknown user")
        void getUserPlayLog_shouldReturn404ForUnknownUser() throws Exception {
            // Given
            when(profileService.getUserPlayLog(eq("ghost"), isNull(), any(Pageable.class)))
                    .thenThrow(new UserNotFoundException("ghost"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/plays", "ghost"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}/compare")
    class CompareProfiles {

        @Test
        @DisplayName("should return comparison for an authenticated viewer")
        @WithMockUser(username = "viewer@example.com")
        void compareProfiles_shouldReturnComparison() throws Exception {
            // Given
            UUID gameId = UUID.randomUUID();
            CommonGameEntryDto entry = new CommonGameEntryDto(
                    gameId, "Elden Ring", "/covers/elden.png", LocalDate.of(2022, 2, 25),
                    PlayStatus.COMPLETED, PlayStatus.ARE_PLAYING, 5.0, 4.0, 1.0);
            Page<CommonGameEntryDto> page = new PageImpl<>(
                    List.of(entry), PageRequest.of(0, 20), 1);
            ProfileComparisonDto comparison = new ProfileComparisonDto(
                    71, 1, 4, 4, PagedResponseDto.from(page));

            when(profileComparisonService.compare(
                    eq("viewer@example.com"), eq("target"), any(Pageable.class)))
                    .thenReturn(comparison);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/compare", "target"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.affinityScore").value(71))
                    .andExpect(jsonPath("$.commonGamesCount").value(1))
                    .andExpect(jsonPath("$.viewerLibrarySize").value(4))
                    .andExpect(jsonPath("$.targetLibrarySize").value(4))
                    .andExpect(jsonPath("$.commonGames.content[0].title").value("Elden Ring"))
                    .andExpect(jsonPath("$.commonGames.content[0].viewerStatus").value("COMPLETED"))
                    .andExpect(jsonPath("$.commonGames.content[0].targetStatus").value("ARE_PLAYING"))
                    .andExpect(jsonPath("$.commonGames.content[0].viewerRating").value(5.0))
                    .andExpect(jsonPath("$.commonGames.content[0].targetRating").value(4.0))
                    .andExpect(jsonPath("$.commonGames.content[0].ratingDiff").value(1.0))
                    .andExpect(jsonPath("$.commonGames.metadata.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 400 when comparing with own profile")
        @WithMockUser(username = "viewer@example.com")
        void compareProfiles_shouldReturn400ForSelfCompare() throws Exception {
            // Given
            when(profileComparisonService.compare(
                    eq("viewer@example.com"), eq("viewer"), any(Pageable.class)))
                    .thenThrow(new IllegalArgumentException("Cannot compare a profile with itself"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/compare", "viewer"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when target profile is private and viewer is not a follower")
        @WithMockUser(username = "viewer@example.com")
        void compareProfiles_shouldReturn403ForPrivateProfile() throws Exception {
            // Given
            when(profileComparisonService.compare(
                    eq("viewer@example.com"), eq("privateuser"), any(Pageable.class)))
                    .thenThrow(new ProfilePrivateException("privateuser"));

            // When / Then
            mockMvc.perform(get("/api/users/{username}/compare", "privateuser"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return score 0 when users share no games")
        @WithMockUser(username = "viewer@example.com")
        void compareProfiles_shouldReturnZeroScoreWhenNoCommonGames() throws Exception {
            // Given
            Page<CommonGameEntryDto> empty = new PageImpl<>(
                    List.of(), PageRequest.of(0, 20), 0);
            ProfileComparisonDto comparison = new ProfileComparisonDto(
                    0, 0, 3, 3, PagedResponseDto.from(empty));

            when(profileComparisonService.compare(
                    eq("viewer@example.com"), eq("target"), any(Pageable.class)))
                    .thenReturn(comparison);

            // When / Then
            mockMvc.perform(get("/api/users/{username}/compare", "target"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.affinityScore").value(0))
                    .andExpect(jsonPath("$.commonGamesCount").value(0))
                    .andExpect(jsonPath("$.commonGames.content").isEmpty());
        }
    }
}
