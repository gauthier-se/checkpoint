package com.checkpoint.api.services.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.onboarding.OnboardingDto;
import com.checkpoint.api.dto.onboarding.OnboardingSteps;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.OnboardingService;

/**
 * Implementation of {@link OnboardingService}.
 *
 * <p>State lives in two columns on {@code users}:
 * <ul>
 *     <li>{@code onboarding_completed_at} — {@code null} while the user has not finished or
 *         dismissed the flow.</li>
 *     <li>{@code onboarding_steps} — a {@code jsonb} map of step key → {@code true}/{@code false}.
 *         Missing key means "not yet seen".</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class OnboardingServiceImpl implements OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingServiceImpl.class);

    private final UserRepository userRepository;

    public OnboardingServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OnboardingDto getOnboarding(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));
        return toDto(user);
    }

    @Override
    @Transactional
    public void markStepDone(String userEmail, String step) {
        if (!OnboardingSteps.isValid(step)) {
            return;
        }
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return;
        }
        Map<String, Boolean> steps = ensureStepsMap(user);
        if (Boolean.TRUE.equals(steps.get(step))) {
            return;
        }
        steps.put(step, true);
        userRepository.save(user);
        log.debug("Marked onboarding step '{}' as done for user {}", step, userEmail);
    }

    @Override
    @Transactional
    public OnboardingDto updateStep(String userEmail, String step, boolean done) {
        if (!OnboardingSteps.isValid(step)) {
            throw new IllegalArgumentException("Unknown onboarding step: " + step);
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));
        Map<String, Boolean> steps = ensureStepsMap(user);
        steps.put(step, done);
        userRepository.save(user);
        return toDto(user);
    }

    @Override
    @Transactional
    public OnboardingDto complete(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));
        if (user.getOnboardingCompletedAt() == null) {
            user.setOnboardingCompletedAt(LocalDateTime.now());
            userRepository.save(user);
        }
        return toDto(user);
    }

    private Map<String, Boolean> ensureStepsMap(User user) {
        Map<String, Boolean> steps = user.getOnboardingSteps();
        if (steps == null) {
            steps = new HashMap<>();
            user.setOnboardingSteps(steps);
        }
        return steps;
    }

    private OnboardingDto toDto(User user) {
        Map<String, Boolean> steps = user.getOnboardingSteps() != null
                ? new HashMap<>(user.getOnboardingSteps())
                : new HashMap<>();
        return new OnboardingDto(user.getOnboardingCompletedAt(), steps);
    }
}
