package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.social.FriendActivityEntryDto;
import com.checkpoint.api.dto.social.FriendGameActivityDto;
import com.checkpoint.api.dto.social.FriendWantToPlayDto;
import com.checkpoint.api.dto.social.FriendWishlistEntryDto;
import com.checkpoint.api.enums.FriendCollectionType;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.GameSocialService;

/**
 * Unit tests for {@link GameSocialController}.
 */
@WebMvcTest(controllers = GameSocialController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameSocialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameSocialService gameSocialService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    private UUID gameId;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/friends-activity")
    class GetFriendsActivity {

        @Test
        @DisplayName("should return 200 with friend entries and per-status counts when viewer is authenticated")
        @WithMockUser(username = "viewer@example.com")
        void shouldReturnFriendsActivity() throws Exception {
            UUID friendId = UUID.randomUUID();
            UUID playId = UUID.randomUUID();
            FriendActivityEntryDto entry = new FriendActivityEntryDto(
                    friendId, "alice", "alice.png", PlayStatus.COMPLETED, 4.5, true, playId);
            FriendGameActivityDto payload = new FriendGameActivityDto(
                    1, Map.of(PlayStatus.COMPLETED, 1L), List.of(entry));

            when(gameSocialService.getFriendsActivity(eq(gameId), eq("viewer@example.com")))
                    .thenReturn(payload);

            mockMvc.perform(get("/api/games/{gameId}/friends-activity", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.countsByPlayStatus.COMPLETED").value(1))
                    .andExpect(jsonPath("$.friends[0].pseudo").value("alice"))
                    .andExpect(jsonPath("$.friends[0].primaryPlayStatus").value("COMPLETED"))
                    .andExpect(jsonPath("$.friends[0].rating").value(4.5))
                    .andExpect(jsonPath("$.friends[0].hasReview").value(true))
                    .andExpect(jsonPath("$.friends[0].latestPlayId").value(playId.toString()));
        }

        @Test
        @DisplayName("should return 200 with empty payload when anonymous")
        void shouldReturnEmptyWhenAnonymous() throws Exception {
            when(gameSocialService.getFriendsActivity(eq(gameId), eq(null)))
                    .thenReturn(new FriendGameActivityDto(0, Map.of(), List.of()));

            mockMvc.perform(get("/api/games/{gameId}/friends-activity", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.friends").isEmpty());
        }

        @Test
        @DisplayName("should return 404 when the game does not exist")
        @WithMockUser(username = "viewer@example.com")
        void shouldReturn404WhenGameMissing() throws Exception {
            when(gameSocialService.getFriendsActivity(eq(gameId), any()))
                    .thenThrow(new GameNotFoundException(gameId));

            mockMvc.perform(get("/api/games/{gameId}/friends-activity", gameId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/friends-want-to-play")
    class GetFriendsWantToPlay {

        @Test
        @DisplayName("should return 200 with wishlist + backlog counts and entries")
        @WithMockUser(username = "viewer@example.com")
        void shouldReturnWantToPlay() throws Exception {
            FriendWishlistEntryDto entry = new FriendWishlistEntryDto(
                    UUID.randomUUID(), "bob", null, FriendCollectionType.WISHLIST);
            FriendWantToPlayDto payload = new FriendWantToPlayDto(1, 1L, 0L, List.of(entry));

            when(gameSocialService.getFriendsWantToPlay(eq(gameId), eq("viewer@example.com")))
                    .thenReturn(payload);

            mockMvc.perform(get("/api/games/{gameId}/friends-want-to-play", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.wishlistCount").value(1))
                    .andExpect(jsonPath("$.backlogCount").value(0))
                    .andExpect(jsonPath("$.friends[0].pseudo").value("bob"))
                    .andExpect(jsonPath("$.friends[0].collectionType").value("WISHLIST"));
        }

        @Test
        @DisplayName("should return 200 with empty payload when anonymous")
        void shouldReturnEmptyWhenAnonymous() throws Exception {
            when(gameSocialService.getFriendsWantToPlay(eq(gameId), eq(null)))
                    .thenReturn(new FriendWantToPlayDto(0, 0L, 0L, List.of()));

            mockMvc.perform(get("/api/games/{gameId}/friends-want-to-play", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.wishlistCount").value(0))
                    .andExpect(jsonPath("$.backlogCount").value(0))
                    .andExpect(jsonPath("$.friends").isEmpty());
        }
    }
}
