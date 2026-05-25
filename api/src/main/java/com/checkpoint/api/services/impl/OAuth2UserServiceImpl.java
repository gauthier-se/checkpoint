package com.checkpoint.api.services.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.onboarding.OnboardingSteps;
import com.checkpoint.api.entities.AuthProvider;
import com.checkpoint.api.entities.Role;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.UserBannedException;
import com.checkpoint.api.repositories.RoleRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.OAuth2UserService;

/**
 * Default implementation of {@link OAuth2UserService}.
 *
 * <p>Resolution order on each OAuth2 callback:</p>
 * <ol>
 *   <li>Lookup by {@code (provider, providerId)} — returning user.</li>
 *   <li>Lookup by email — link the OAuth identity to that account by setting
 *       its provider/providerId fields. This covers users who initially
 *       registered with a password and now sign in through Google/Twitch
 *       on the same email.</li>
 *   <li>Otherwise, create a brand-new account with no password,
 *       a generated unique pseudo, and the {@code USER} role.</li>
 * </ol>
 */
@Service
public class OAuth2UserServiceImpl implements OAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public OAuth2UserServiceImpl(UserRepository userRepository,
                                 RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public User loadOrCreateUser(AuthProvider provider, String providerId, String email,
                                 String name, String picture) {
        if (provider == null || providerId == null || email == null || email.isBlank()) {
            throw new IllegalArgumentException("OAuth2 provider, providerId and email are required");
        }

        Optional<User> byProvider = userRepository.findByProviderAndProviderId(provider, providerId);
        if (byProvider.isPresent()) {
            User user = byProvider.get();
            rejectIfBanned(user);
            return initRole(user);
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            rejectIfBanned(existing);
            // Link the OAuth identity to the existing account.
            existing.setProvider(provider);
            existing.setProviderId(providerId);
            if (existing.getPicture() == null && picture != null) {
                existing.setPicture(picture);
            }
            log.info("Linked OAuth2 identity {}:{} to existing user {}", provider, providerId, email);
            return initRole(userRepository.save(existing));
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER")));

        String pseudo = generateUniquePseudo(name, email);

        User created = new User(pseudo, email, provider, providerId);
        created.setPicture(picture);
        created.setRole(userRole);
        // The OAuth provider already gave us an avatar — count the picture step as done.
        if (picture != null && !picture.isBlank()) {
            created.getOnboardingSteps().put(OnboardingSteps.PICTURE, true);
        }
        log.info("Created new OAuth2 user {} via {}", email, provider);
        return initRole(userRepository.save(created));
    }

    /**
     * Forces initialization of the lazy {@link Role} proxy so that callers
     * outside the transaction can still read its fields (e.g. the role name
     * used to build the OAuth2 principal's authorities).
     */
    private User initRole(User user) {
        if (user.getRole() != null) {
            user.getRole().getName();
        }
        return user;
    }

    private void rejectIfBanned(User user) {
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new UserBannedException(user.getEmail());
        }
    }

    /**
     * Builds a pseudo guaranteed not to collide with an existing one.
     * Falls back to the email local-part when the provider doesn't return a name,
     * sanitises the candidate to keep it printable, and appends a numeric suffix
     * until uniqueness is reached.
     */
    private String generateUniquePseudo(String nameHint, String email) {
        String base = nameHint != null && !nameHint.isBlank()
                ? nameHint
                : email.substring(0, email.indexOf('@'));
        String sanitized = base.replaceAll("[^A-Za-z0-9_-]", "").trim();
        if (sanitized.isEmpty()) {
            sanitized = "user";
        }

        String candidate = sanitized;
        int suffix = 1;
        while (userRepository.existsByPseudo(candidate)) {
            suffix++;
            candidate = sanitized + suffix;
        }
        return candidate;
    }
}
