package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.leaderboard.LeaderboardEntryDto;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.LeaderboardService;
import com.checkpoint.api.services.LeaderboardSortBy;

/**
 * Unit tests for {@link LeaderboardController}.
 */
@WebMvcTest(LeaderboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    private LeaderboardEntryDto entry(int rank, String pseudo, int level, int xp) {
        return new LeaderboardEntryDto(rank, UUID.randomUUID(), pseudo, null, level, xp);
    }

    @Nested
    @DisplayName("GET /api/leaderboard")
    class GetLeaderboard {

        @Test
        @DisplayName("defaults to sortBy=xp and limit=50")
        void defaultsToXpAndFifty() throws Exception {
            when(leaderboardService.getLeaderboard(LeaderboardSortBy.XP, 50))
                    .thenReturn(List.of(entry(1, "alpha", 10, 9000), entry(2, "bravo", 8, 7000)));

            mockMvc.perform(get("/api/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].rank").value(1))
                    .andExpect(jsonPath("$[0].pseudo").value("alpha"))
                    .andExpect(jsonPath("$[0].xpPoint").value(9000))
                    .andExpect(jsonPath("$[1].rank").value(2));

            verify(leaderboardService).getLeaderboard(LeaderboardSortBy.XP, 50);
        }

        @Test
        @DisplayName("passes sortBy=level to the service")
        void passesLevelSort() throws Exception {
            when(leaderboardService.getLeaderboard(LeaderboardSortBy.LEVEL, 50))
                    .thenReturn(List.of(entry(1, "alpha", 99, 100)));

            mockMvc.perform(get("/api/leaderboard").param("sortBy", "level"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].level").value(99));

            verify(leaderboardService).getLeaderboard(LeaderboardSortBy.LEVEL, 50);
        }

        @Test
        @DisplayName("accepts a custom limit")
        void acceptsCustomLimit() throws Exception {
            when(leaderboardService.getLeaderboard(eq(LeaderboardSortBy.XP), eq(20)))
                    .thenReturn(List.of(entry(1, "alpha", 5, 500)));

            mockMvc.perform(get("/api/leaderboard").param("limit", "20"))
                    .andExpect(status().isOk());

            verify(leaderboardService).getLeaderboard(LeaderboardSortBy.XP, 20);
        }

        @Test
        @DisplayName("sortBy is case-insensitive")
        void sortByIsCaseInsensitive() throws Exception {
            when(leaderboardService.getLeaderboard(LeaderboardSortBy.LEVEL, 50))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/leaderboard").param("sortBy", "LEVEL"))
                    .andExpect(status().isOk());

            verify(leaderboardService).getLeaderboard(LeaderboardSortBy.LEVEL, 50);
        }

        @Test
        @DisplayName("returns 400 when limit is below 1")
        void rejectsZeroLimit() throws Exception {
            mockMvc.perform(get("/api/leaderboard").param("limit", "0"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(leaderboardService);
        }

        @Test
        @DisplayName("returns 400 when limit is above 100")
        void rejectsLimitAboveMax() throws Exception {
            mockMvc.perform(get("/api/leaderboard").param("limit", "101"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(leaderboardService);
        }

        @Test
        @DisplayName("returns 400 when sortBy is invalid")
        void rejectsInvalidSortBy() throws Exception {
            mockMvc.perform(get("/api/leaderboard").param("sortBy", "garbage"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(leaderboardService);
        }
    }
}
