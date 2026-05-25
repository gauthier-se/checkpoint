package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

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

import com.checkpoint.api.dto.onboarding.OnboardingDto;
import com.checkpoint.api.dto.onboarding.OnboardingStepUpdateDto;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.OnboardingService;

/**
 * Unit tests for {@link OnboardingController}.
 */
@WebMvcTest(OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false)
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OnboardingService onboardingService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Nested
    @DisplayName("GET /api/me/onboarding")
    class GetOnboarding {

        @Test
        @DisplayName("Should return the user's onboarding snapshot")
        @WithMockUser(username = "alice@test.com")
        void shouldReturnSnapshot() throws Exception {
            OnboardingDto dto = new OnboardingDto(null, Map.of("welcome", true, "picture", false));
            when(onboardingService.getOnboarding("alice@test.com")).thenReturn(dto);

            mockMvc.perform(get("/api/me/onboarding"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completedAt").doesNotExist())
                    .andExpect(jsonPath("$.steps.welcome").value(true))
                    .andExpect(jsonPath("$.steps.picture").value(false));

            verify(onboardingService).getOnboarding("alice@test.com");
        }
    }

    @Nested
    @DisplayName("PATCH /api/me/onboarding")
    class UpdateStep {

        @Test
        @DisplayName("Should record a step as done")
        @WithMockUser(username = "alice@test.com")
        void shouldRecordStepDone() throws Exception {
            OnboardingStepUpdateDto body = new OnboardingStepUpdateDto("bio", true);
            OnboardingDto dto = new OnboardingDto(null, Map.of("bio", true));
            when(onboardingService.updateStep(eq("alice@test.com"), eq("bio"), eq(true)))
                    .thenReturn(dto);

            mockMvc.perform(patch("/api/me/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.steps.bio").value(true));
        }

        @Test
        @DisplayName("Should record a step as skipped")
        @WithMockUser(username = "alice@test.com")
        void shouldRecordStepSkipped() throws Exception {
            OnboardingStepUpdateDto body = new OnboardingStepUpdateDto("twofa", false);
            OnboardingDto dto = new OnboardingDto(null, Map.of("twofa", false));
            when(onboardingService.updateStep(eq("alice@test.com"), eq("twofa"), eq(false)))
                    .thenReturn(dto);

            mockMvc.perform(patch("/api/me/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.steps.twofa").value(false));
        }

        @Test
        @DisplayName("Should reject a body with an empty step")
        @WithMockUser(username = "alice@test.com")
        void shouldReject400OnEmptyStep() throws Exception {
            OnboardingStepUpdateDto body = new OnboardingStepUpdateDto("", true);

            mockMvc.perform(patch("/api/me/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/me/onboarding/complete")
    class Complete {

        @Test
        @DisplayName("Should set completedAt and return the snapshot")
        @WithMockUser(username = "alice@test.com")
        void shouldComplete() throws Exception {
            OnboardingDto dto = new OnboardingDto(LocalDateTime.now(), Map.of());
            when(onboardingService.complete("alice@test.com")).thenReturn(dto);

            mockMvc.perform(post("/api/me/onboarding/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completedAt").exists());
        }
    }
}
