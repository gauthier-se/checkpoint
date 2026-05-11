package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.NotificationPreferencesRepository;
import com.checkpoint.api.repositories.NotificationRepository;
import com.checkpoint.api.repositories.PasswordResetTokenRepository;
import com.checkpoint.api.repositories.RefreshTokenRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.impl.AccountServiceImpl;

/**
 * Unit tests for {@link AccountServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @Mock
    private StorageService storageService;

    private AccountServiceImpl service;

    private User testUser;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                notificationRepository,
                notificationPreferencesRepository,
                storageService);

        testUser = new User("alice", "alice@test.com", "encoded-password");
        testUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Throws UserNotFoundException when no user matches the email")
    void deleteCurrentUser_throwsWhenUserMissing() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCurrentUser("ghost@test.com"))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(Mockito.any(User.class));
        verify(notificationRepository, never()).deleteAllForUser(Mockito.any(UUID.class));
    }

    @Test
    @DisplayName("Deletes related rows in the FK-safe order before removing the user")
    void deleteCurrentUser_deletesInExpectedOrder() {
        UUID userId = testUser.getId();
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));

        service.deleteCurrentUser("alice@test.com");

        InOrder inOrder = Mockito.inOrder(
                notificationRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                notificationPreferencesRepository,
                userRepository);
        inOrder.verify(notificationRepository).deleteAllForUser(userId);
        inOrder.verify(refreshTokenRepository).deleteByUserId(userId);
        inOrder.verify(passwordResetTokenRepository).deleteByUserId(userId);
        inOrder.verify(notificationPreferencesRepository).deleteByUserId(userId);
        inOrder.verify(userRepository).deleteFollowsInvolvingUser(userId);
        inOrder.verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("Skips storage deletion when the user has no profile picture")
    void deleteCurrentUser_skipsStorageWhenNoPicture() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));

        service.deleteCurrentUser("alice@test.com");

        verify(storageService, never()).delete(Mockito.anyString());
    }

    @Test
    @DisplayName("Deletes the stored profile picture when present")
    void deleteCurrentUser_deletesStoredPicture() {
        testUser.setPicture("/uploads/profiles/abc.jpg");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));

        service.deleteCurrentUser("alice@test.com");

        verify(storageService).delete("profiles/abc.jpg");
    }

    @Test
    @DisplayName("Continues erasing the account even if profile-picture deletion fails")
    void deleteCurrentUser_continuesWhenStorageFails() {
        testUser.setPicture("/uploads/profiles/abc.jpg");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));
        Mockito.doThrow(new RuntimeException("disk full"))
                .when(storageService).delete("profiles/abc.jpg");

        service.deleteCurrentUser("alice@test.com");

        verify(userRepository).delete(testUser);
    }
}
