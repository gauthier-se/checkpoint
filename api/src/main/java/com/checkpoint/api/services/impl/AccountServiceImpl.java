package com.checkpoint.api.services.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.NotificationPreferencesRepository;
import com.checkpoint.api.repositories.NotificationRepository;
import com.checkpoint.api.repositories.PasswordResetTokenRepository;
import com.checkpoint.api.repositories.RefreshTokenRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.AccountService;
import com.checkpoint.api.services.StorageService;

/**
 * Implementation of {@link AccountService}.
 *
 * <p>The deletion sequence in {@link #deleteCurrentUser(String)} is intentional:
 * most of the user's owned collections are cascade-deleted by JPA when the
 * {@code User} row is removed, but a few references are not cascade-managed
 * and must be cleaned explicitly beforehand (refresh tokens, password reset
 * tokens, notification preferences, notifications where the user is sender,
 * and {@code user_follows} rows pointing at the user). Doing the cleanup
 * explicitly also makes the order assertable in unit tests.</p>
 */
@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;
    private final StorageService storageService;

    public AccountServiceImpl(UserRepository userRepository,
                              RefreshTokenRepository refreshTokenRepository,
                              PasswordResetTokenRepository passwordResetTokenRepository,
                              NotificationRepository notificationRepository,
                              NotificationPreferencesRepository notificationPreferencesRepository,
                              StorageService storageService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.notificationRepository = notificationRepository;
        this.notificationPreferencesRepository = notificationPreferencesRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public void deleteCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        UUID userId = user.getId();

        log.info("Erasing account for user {} ({})", user.getPseudo(), userId);

        deleteProfilePictureIfPresent(user);

        // 1. Notifications — recipient cascade is on User, but sender FK is not.
        //    Delete both sides in a single query before the user row is gone.
        notificationRepository.deleteAllForUser(userId);

        // 2. Auth tokens — neither RefreshToken nor PasswordResetToken
        //    is cascade-managed by the User entity.
        refreshTokenRepository.deleteByUserId(userId);
        passwordResetTokenRepository.deleteByUserId(userId);

        // 3. Notification preferences — OneToOne with no cascade from User.
        notificationPreferencesRepository.deleteByUserId(userId);

        // 4. Follow relationships — clear both owning and inverse sides of
        //    the user_follows join table. JPA only owns the follower side.
        userRepository.deleteFollowsInvolvingUser(userId);

        // 5. Delete the user. JPA cascades to every collection annotated
        //    with CascadeType.ALL + orphanRemoval on the User entity:
        //    likes, comments, reports filed, reviews (and their nested
        //    comments/likes/reports), userGamePlays, userGames, wishes,
        //    backlogs, favorites, gameLists (and entries), tags, rates,
        //    news, socialLinks.
        userRepository.delete(user);

        log.info("Account erasure complete for user {}", userId);
    }

    private void deleteProfilePictureIfPresent(User user) {
        if (user.getPicture() == null) {
            return;
        }
        try {
            String storagePath = user.getPicture().replaceFirst("^/uploads/", "");
            storageService.delete(storagePath);
        } catch (Exception ex) {
            log.warn("Failed to delete profile picture during account erasure for user {}: {}",
                    user.getId(), ex.getMessage());
        }
    }
}
