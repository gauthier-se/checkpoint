package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.steam.SteamAccountDto;
import com.checkpoint.api.dto.steam.SteamSyncSummaryDto;
import com.checkpoint.api.exceptions.InvalidSteamIdException;
import com.checkpoint.api.exceptions.SteamAccountNotLinkedException;
import com.checkpoint.api.exceptions.SteamLibraryPrivateException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.SteamService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link SteamController}.
 */
@WebMvcTest(SteamController.class)
@AutoConfigureMockMvc(addFilters = false)
class SteamControllerTest {

    private static final String EMAIL = "alice@test.com";
    private static final String STEAM_ID = "76561198000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SteamService steamService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("POST /api/me/steam/link")
    class Link {

        @Test
        @DisplayName("returns 200 and the linked account on success")
        @WithMockUser(username = EMAIL)
        void link_success() throws Exception {
            SteamAccountDto dto = new SteamAccountDto(
                    STEAM_ID, "AliceOnSteam", "https://profile.url", "https://avatar.url");
            when(steamService.linkSteamAccount(eq(EMAIL), eq(STEAM_ID))).thenReturn(dto);

            String body = objectMapper.writeValueAsString(
                    java.util.Map.of("steamId", STEAM_ID));

            mockMvc.perform(post("/api/me/steam/link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.steamId").value(STEAM_ID))
                    .andExpect(jsonPath("$.steamDisplayName").value("AliceOnSteam"));

            verify(steamService).linkSteamAccount(EMAIL, STEAM_ID);
        }

        @Test
        @DisplayName("returns 400 when steamId is blank (validation)")
        @WithMockUser(username = EMAIL)
        void link_blankId() throws Exception {
            String body = objectMapper.writeValueAsString(
                    java.util.Map.of("steamId", "   "));

            mockMvc.perform(post("/api/me/steam/link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when steamId exceeds the size limit (validation)")
        @WithMockUser(username = EMAIL)
        void link_tooLongId() throws Exception {
            String body = objectMapper.writeValueAsString(
                    java.util.Map.of("steamId", "a".repeat(257)));

            mockMvc.perform(post("/api/me/steam/link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when Steam rejects the ID")
        @WithMockUser(username = EMAIL)
        void link_invalidSteamId() throws Exception {
            when(steamService.linkSteamAccount(eq(EMAIL), eq(STEAM_ID)))
                    .thenThrow(new InvalidSteamIdException("Steam does not recognize SteamID: " + STEAM_ID));

            String body = objectMapper.writeValueAsString(
                    java.util.Map.of("steamId", STEAM_ID));

            mockMvc.perform(post("/api/me/steam/link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/me/steam/unlink")
    class Unlink {

        @Test
        @DisplayName("returns 204 on success")
        @WithMockUser(username = EMAIL)
        void unlink_success() throws Exception {
            doNothing().when(steamService).unlinkSteamAccount(EMAIL);

            mockMvc.perform(delete("/api/me/steam/unlink"))
                    .andExpect(status().isNoContent());

            verify(steamService).unlinkSteamAccount(EMAIL);
        }
    }

    @Nested
    @DisplayName("POST /api/me/steam/sync")
    class Sync {

        @Test
        @DisplayName("returns 200 and the sync summary on success")
        @WithMockUser(username = EMAIL)
        void sync_success() throws Exception {
            SteamSyncSummaryDto summary = new SteamSyncSummaryDto(10, 7, 2, 1);
            when(steamService.syncSteamLibrary(EMAIL)).thenReturn(summary);

            mockMvc.perform(post("/api/me/steam/sync"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(10))
                    .andExpect(jsonPath("$.imported").value(7))
                    .andExpect(jsonPath("$.skipped").value(2))
                    .andExpect(jsonPath("$.unmatched").value(1));

            verify(steamService).syncSteamLibrary(EMAIL);
        }

        @Test
        @DisplayName("returns 400 when the user has no Steam account linked")
        @WithMockUser(username = EMAIL)
        void sync_returnsBadRequestWhenNotLinked() throws Exception {
            when(steamService.syncSteamLibrary(EMAIL))
                    .thenThrow(new SteamAccountNotLinkedException("No Steam account linked."));

            mockMvc.perform(post("/api/me/steam/sync"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when the Steam profile is private")
        @WithMockUser(username = EMAIL)
        void sync_returnsBadRequestWhenLibraryPrivate() throws Exception {
            when(steamService.syncSteamLibrary(EMAIL))
                    .thenThrow(new SteamLibraryPrivateException("Your Steam library is private."));

            mockMvc.perform(post("/api/me/steam/sync"))
                    .andExpect(status().isBadRequest());
        }
    }
}
