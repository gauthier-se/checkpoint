package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

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

import com.checkpoint.api.dto.notification.NotificationPreferencesDto;
import com.checkpoint.api.dto.notification.UpdateNotificationPreferencesDto;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.NotificationPreferencesService;

/**
 * Unit tests for {@link NotificationPreferencesController}.
 */
@WebMvcTest(NotificationPreferencesController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationPreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationPreferencesService preferencesService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("GET /api/me/notification-preferences")
    class GetPreferences {

        @Test
        @DisplayName("should return the user's preferences DTO")
        @WithMockUser(username = "user@example.com")
        void getPreferences_shouldReturn200WithDto() throws Exception {
            // Given
            NotificationPreferencesDto dto = new NotificationPreferencesDto(
                    true, true, true, true, true, true, true, true);
            when(preferencesService.getOrCreate(eq("user@example.com"))).thenReturn(dto);

            // When / Then
            mockMvc.perform(get("/api/me/notification-preferences"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.followEnabled").value(true))
                    .andExpect(jsonPath("$.likeReviewEnabled").value(true))
                    .andExpect(jsonPath("$.likeListEnabled").value(true))
                    .andExpect(jsonPath("$.likeGameEnabled").value(true))
                    .andExpect(jsonPath("$.commentReplyEnabled").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/me/notification-preferences")
    class UpdatePreferences {

        @Test
        @DisplayName("should return the updated DTO")
        @WithMockUser(username = "user@example.com")
        void updatePreferences_shouldReturnUpdatedDto() throws Exception {
            // Given
            UpdateNotificationPreferencesDto body = new UpdateNotificationPreferencesDto(
                    false, true, true, true, false, null, null, null);
            NotificationPreferencesDto updated = new NotificationPreferencesDto(
                    false, true, true, true, false, true, true, true);

            when(preferencesService.update(eq("user@example.com"), any(UpdateNotificationPreferencesDto.class)))
                    .thenReturn(updated);

            // When / Then
            mockMvc.perform(put("/api/me/notification-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.followEnabled").value(false))
                    .andExpect(jsonPath("$.commentReplyEnabled").value(false));

            verify(preferencesService).update(eq("user@example.com"), any(UpdateNotificationPreferencesDto.class));
        }

        @Test
        @DisplayName("should accept a partial body with null fields")
        @WithMockUser(username = "user@example.com")
        void updatePreferences_shouldHandlePartialBody() throws Exception {
            // Given
            UpdateNotificationPreferencesDto body = new UpdateNotificationPreferencesDto(
                    false, null, null, null, null, null, null, null);
            NotificationPreferencesDto updated = new NotificationPreferencesDto(
                    false, true, true, true, true, true, true, true);

            when(preferencesService.update(eq("user@example.com"), any(UpdateNotificationPreferencesDto.class)))
                    .thenReturn(updated);

            // When / Then
            mockMvc.perform(put("/api/me/notification-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.followEnabled").value(false))
                    .andExpect(jsonPath("$.likeReviewEnabled").value(true));
        }
    }
}
