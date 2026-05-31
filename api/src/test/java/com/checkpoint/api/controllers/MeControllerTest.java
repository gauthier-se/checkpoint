package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.export.UserDataExportDto;
import com.checkpoint.api.dto.export.UserExportProfile;
import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.UpdateProfileDto;
import com.checkpoint.api.dto.social.FeedGameDto;
import com.checkpoint.api.dto.social.FeedItemDto;
import com.checkpoint.api.dto.social.FeedUserDto;
import com.checkpoint.api.enums.FeedItemType;
import com.checkpoint.api.exceptions.PseudoAlreadyExistsException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.AccountService;
import com.checkpoint.api.services.AuthService;
import com.checkpoint.api.services.DataExportService;
import com.checkpoint.api.services.FeedService;
import com.checkpoint.api.services.ProfileService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link MeController} (current user's account, profile, and feed).
 */
@WebMvcTest(MeController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private DataExportService dataExportService;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private FeedService feedService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @DisplayName("DELETE /api/v1/me - deletes the account and clears auth cookies")
    @WithMockUser(username = "alice@test.com")
    void deleteAccount_returns204AndClearsCookies() throws Exception {
        mockMvc.perform(delete("/api/v1/me")
                        .cookie(new Cookie("checkpoint_refresh", "refresh-token-value")))
                .andExpect(status().isNoContent());

        verify(accountService).deleteCurrentUser("alice@test.com");
        verify(authService).clearAuthCookie(eq("refresh-token-value"), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/me - still clears cookies when no refresh cookie is sent")
    @WithMockUser(username = "alice@test.com")
    void deleteAccount_handlesMissingRefreshCookie() throws Exception {
        mockMvc.perform(delete("/api/v1/me"))
                .andExpect(status().isNoContent());

        verify(accountService).deleteCurrentUser("alice@test.com");
        verify(authService).clearAuthCookie(eq(null), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("GET /api/v1/me/export - returns the export as a JSON attachment")
    @WithMockUser(username = "alice@test.com")
    void exportData_returnsJsonAttachment() throws Exception {
        UserDataExportDto export = new UserDataExportDto(
                new UserExportProfile(null, "alice", "alice@test.com", null, null,
                        false, 1, 0, LocalDateTime.now()),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                LocalDateTime.now());
        when(dataExportService.exportForUser("alice@test.com")).thenReturn(export);

        mockMvc.perform(get("/api/v1/me/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Disposition",
                        Matchers.allOf(
                                Matchers.startsWith("attachment; filename=\"checkpoint-export-"),
                                Matchers.endsWith(".json\""))))
                .andExpect(jsonPath("$.profile.username").value("alice"))
                .andExpect(jsonPath("$.exportedAt").exists());

        verify(dataExportService).exportForUser("alice@test.com");
    }

    @Nested
    @DisplayName("PUT /api/v1/me/profile")
    class UpdateProfile {

        @Test
        @DisplayName("Should update profile successfully")
        @WithMockUser(username = "alice@test.com")
        void updateProfile_shouldReturnUpdatedProfile() throws Exception {
            // Given
            UpdateProfileDto request = new UpdateProfileDto("newpseudo", "My new bio", false);
            ProfileUpdatedDto response = new ProfileUpdatedDto("newpseudo", "My new bio", null, false);

            when(profileService.updateProfile(eq("alice@test.com"), any(UpdateProfileDto.class)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(put("/api/v1/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("newpseudo"))
                    .andExpect(jsonPath("$.bio").value("My new bio"))
                    .andExpect(jsonPath("$.isPrivate").value(false));

            verify(profileService).updateProfile(eq("alice@test.com"), any(UpdateProfileDto.class));
        }

        @Test
        @DisplayName("Should return 400 when pseudo is blank")
        @WithMockUser(username = "alice@test.com")
        void updateProfile_shouldReturn400WhenPseudoBlank() throws Exception {
            // Given
            UpdateProfileDto request = new UpdateProfileDto("", "bio", false);

            // When / Then
            mockMvc.perform(put("/api/v1/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when pseudo already exists")
        @WithMockUser(username = "alice@test.com")
        void updateProfile_shouldReturn409WhenPseudoTaken() throws Exception {
            // Given
            UpdateProfileDto request = new UpdateProfileDto("takenpseudo", "bio", false);

            when(profileService.updateProfile(eq("alice@test.com"), any(UpdateProfileDto.class)))
                    .thenThrow(new PseudoAlreadyExistsException("takenpseudo"));

            // When / Then
            mockMvc.perform(put("/api/v1/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/me/picture")
    class UpdatePicture {

        @Test
        @DisplayName("Should upload picture successfully")
        @WithMockUser(username = "alice@test.com")
        void updatePicture_shouldReturnPictureUrl() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", "fake-image-data".getBytes());

            when(profileService.updatePicture(eq("alice@test.com"), any()))
                    .thenReturn("/uploads/profiles/uuid.jpg");

            // When / Then
            mockMvc.perform(multipart("/api/v1/me/picture").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.picture").value("/uploads/profiles/uuid.jpg"));

            verify(profileService).updatePicture(eq("alice@test.com"), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/me/picture")
    class DeletePicture {

        @Test
        @DisplayName("Should delete picture successfully")
        @WithMockUser(username = "alice@test.com")
        void deletePicture_shouldReturn204() throws Exception {
            // Given
            doNothing().when(profileService).deletePicture("alice@test.com");

            // When / Then
            mockMvc.perform(delete("/api/v1/me/picture"))
                    .andExpect(status().isNoContent());

            verify(profileService).deletePicture("alice@test.com");
        }
    }

    @Test
    @DisplayName("GET /api/v1/me/feed should return paginated feed")
    @WithMockUser(username = "user@test.com")
    void getFeed_shouldReturnPaginatedFeed() throws Exception {
        // Given
        FeedUserDto feedUser = new FeedUserDto(UUID.randomUUID(), "gamer42", "avatar.jpg");
        FeedGameDto feedGame = new FeedGameDto(UUID.randomUUID(), "Elden Ring", "cover.jpg", null);
        FeedItemDto item = new FeedItemDto(
                UUID.randomUUID(), FeedItemType.RATING, LocalDateTime.now(),
                feedUser, feedGame, null, 5, null, null, null, null, null
        );

        List<FeedItemDto> items = List.of(item);
        Page<FeedItemDto> page = new PageImpl<>(items, PageRequest.of(0, 20), 1);
        PagedResponseDto<FeedItemDto> response = PagedResponseDto.from(page);

        when(feedService.getFeed(eq("user@test.com"), anyInt(), anyInt(), any())).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/me/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").value("RATING"))
                .andExpect(jsonPath("$.content[0].score").value(5))
                .andExpect(jsonPath("$.content[0].user.pseudo").value("gamer42"))
                .andExpect(jsonPath("$.content[0].game.title").value("Elden Ring"))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/me/feed should return empty when user follows nobody")
    @WithMockUser(username = "lonely@test.com")
    void getFeed_shouldReturnEmptyWhenNoFollowing() throws Exception {
        // Given
        Page<FeedItemDto> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        PagedResponseDto<FeedItemDto> response = PagedResponseDto.from(emptyPage);

        when(feedService.getFeed(eq("lonely@test.com"), anyInt(), anyInt(), any())).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/me/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.metadata.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/me/feed should pass the type filter to the service")
    @WithMockUser(username = "user@test.com")
    void getFeed_shouldPassTypeFilterToService() throws Exception {
        // Given
        Page<FeedItemDto> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        PagedResponseDto<FeedItemDto> response = PagedResponseDto.from(emptyPage);

        when(feedService.getFeed(eq("user@test.com"), anyInt(), anyInt(), any())).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/me/feed").param("type", "RATING"))
                .andExpect(status().isOk());

        verify(feedService).getFeed(eq("user@test.com"), anyInt(), anyInt(), eq(FeedItemType.RATING));
    }

    @Test
    @DisplayName("GET /api/v1/me/friends/trending-games should return games")
    @WithMockUser(username = "user@test.com")
    void getFriendsTrendingGames_shouldReturnGames() throws Exception {
        // Given
        GameCardDto game = new GameCardDto(
                UUID.randomUUID(), "Elden Ring", "cover.jpg",
                LocalDate.of(2022, 2, 25), 4.9, 5000L
        );

        when(feedService.getFriendsTrendingGames(eq("user@test.com"), anyInt()))
                .thenReturn(List.of(game));

        // When / Then
        mockMvc.perform(get("/api/v1/me/friends/trending-games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Elden Ring"))
                .andExpect(jsonPath("$[0].ratingCount").value(5000));
    }

    @Test
    @DisplayName("GET /api/v1/me/friends/trending-games should return empty when user follows nobody")
    @WithMockUser(username = "lonely@test.com")
    void getFriendsTrendingGames_shouldReturnEmptyWhenNoFollowing() throws Exception {
        // Given
        when(feedService.getFriendsTrendingGames(eq("lonely@test.com"), anyInt()))
                .thenReturn(Collections.emptyList());

        // When / Then
        mockMvc.perform(get("/api/v1/me/friends/trending-games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/me/friends/popular-games should return paginated games")
    @WithMockUser(username = "user@test.com")
    void getFriendsPopularGames_shouldReturnPaginatedGames() throws Exception {
        // Given
        GameCardDto game = new GameCardDto(
                UUID.randomUUID(), "Elden Ring", "cover.jpg",
                LocalDate.of(2022, 2, 25), 4.9, 5000L
        );
        Page<GameCardDto> page = new PageImpl<>(List.of(game), PageRequest.of(0, 32), 1);
        PagedResponseDto<GameCardDto> response = PagedResponseDto.from(page);

        when(feedService.getFriendsPopularGames(eq("user@test.com"), anyInt(), anyInt()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/me/friends/popular-games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Elden Ring"))
                .andExpect(jsonPath("$.content[0].ratingCount").value(5000))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/me/friends/popular-games should return empty when user follows nobody")
    @WithMockUser(username = "lonely@test.com")
    void getFriendsPopularGames_shouldReturnEmptyWhenNoFollowing() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 32), 0);
        PagedResponseDto<GameCardDto> response = PagedResponseDto.from(emptyPage);

        when(feedService.getFriendsPopularGames(eq("lonely@test.com"), anyInt(), anyInt()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/me/friends/popular-games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.metadata.totalElements").value(0));
    }
}
