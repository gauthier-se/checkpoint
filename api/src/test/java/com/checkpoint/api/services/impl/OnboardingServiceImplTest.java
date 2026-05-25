package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.dto.onboarding.OnboardingDto;
import com.checkpoint.api.dto.onboarding.OnboardingSteps;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Unit tests for {@link OnboardingServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingServiceImpl")
class OnboardingServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private OnboardingServiceImpl onboardingService;

    private User user;

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingServiceImpl(userRepository);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@test.com");
        user.setPseudo("alice");
        user.setOnboardingSteps(new HashMap<>());
    }

    @Nested
    @DisplayName("getOnboarding")
    class GetOnboarding {

        @Test
        @DisplayName("returns the current snapshot")
        void shouldReturnSnapshot() {
            user.getOnboardingSteps().put(OnboardingSteps.WELCOME, true);
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            OnboardingDto dto = onboardingService.getOnboarding("alice@test.com");

            assertThat(dto.completedAt()).isNull();
            assertThat(dto.steps()).containsEntry(OnboardingSteps.WELCOME, true);
        }

        @Test
        @DisplayName("throws when the user does not exist")
        void shouldThrowWhenMissing() {
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> onboardingService.getOnboarding("ghost@test.com"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markStepDone")
    class MarkStepDone {

        @Test
        @DisplayName("flips a step from missing to true and persists")
        void shouldMarkDone() {
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            onboardingService.markStepDone("alice@test.com", OnboardingSteps.PICTURE);

            assertThat(user.getOnboardingSteps()).containsEntry(OnboardingSteps.PICTURE, true);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("is idempotent — no save when the step is already true")
        void shouldBeIdempotent() {
            user.getOnboardingSteps().put(OnboardingSteps.PICTURE, true);
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            onboardingService.markStepDone("alice@test.com", OnboardingSteps.PICTURE);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("silently ignores an unknown step")
        void shouldIgnoreUnknownStep() {
            onboardingService.markStepDone("alice@test.com", "not-a-step");

            verify(userRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("silently ignores when the user does not exist")
        void shouldIgnoreMissingUser() {
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            onboardingService.markStepDone("ghost@test.com", OnboardingSteps.PICTURE);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateStep")
    class UpdateStep {

        @Test
        @DisplayName("records an explicit skip (done=false)")
        void shouldRecordSkip() {
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            OnboardingDto dto = onboardingService.updateStep(
                    "alice@test.com", OnboardingSteps.BIO, false);

            assertThat(dto.steps()).containsEntry(OnboardingSteps.BIO, false);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("rejects an unknown step")
        void shouldRejectUnknownStep() {
            assertThatThrownBy(() ->
                    onboardingService.updateStep("alice@test.com", "bogus", true))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("sets completedAt to now")
        void shouldSetCompletedAt() {
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            OnboardingDto dto = onboardingService.complete("alice@test.com");

            assertThat(dto.completedAt()).isNotNull();
            assertThat(user.getOnboardingCompletedAt()).isNotNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("is idempotent when already completed")
        void shouldBeIdempotent() {
            user.setOnboardingCompletedAt(java.time.LocalDateTime.now().minusDays(1));
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            onboardingService.complete("alice@test.com");

            verify(userRepository, never()).save(any());
        }
    }
}
