package com.checkpoint.api.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.client.SteamApiClient;
import com.checkpoint.api.dto.steam.SteamPlayerSummaryDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.SteamApiException;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Unit tests for {@link SteamProfileRefreshTask}.
 */
@ExtendWith(MockitoExtension.class)
class SteamProfileRefreshTaskTest {

    private static final String STEAM_ID = "76561198000000000";

    @Mock
    private UserRepository userRepository;

    @Mock
    private SteamApiClient steamApiClient;

    private SteamProfileRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new SteamProfileRefreshTask(userRepository, steamApiClient, 200);
    }

    private User staleUser() {
        User user = new User();
        user.setEmail("alice@test.com");
        user.setSteamId(STEAM_ID);
        user.setSteamDisplayName("OldName");
        user.setSteamAvatarUrl("https://old/avatar.jpg");
        user.setSteamProfileUrl("https://old/profile");
        user.setSteamSyncedAt(LocalDateTime.now().minusDays(2));
        return user;
    }

    private SteamPlayerSummaryDto freshSummary() {
        return new SteamPlayerSummaryDto(
                STEAM_ID,
                "FreshName",
                "https://steamcommunity.com/id/fresh",
                "https://avatar.url/s.jpg",
                "https://avatar.url/m.jpg",
                "https://avatar.url/full.jpg");
    }

    @Test
    @DisplayName("refreshStaleProfiles updates cached fields and advances steamSyncedAt on successful fetch")
    void refresh_updatesStaleEntries() {
        User user = staleUser();
        LocalDateTime previousSync = user.getSteamSyncedAt();
        when(userRepository.findSteamLinkedUsersStaleBefore(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(freshSummary()));

        task.refreshStaleProfiles();

        assertThat(user.getSteamDisplayName()).isEqualTo("FreshName");
        assertThat(user.getSteamAvatarUrl()).isEqualTo("https://avatar.url/m.jpg");
        assertThat(user.getSteamProfileUrl()).isEqualTo("https://steamcommunity.com/id/fresh");
        assertThat(user.getSteamSyncedAt()).isAfter(previousSync);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("refreshStaleProfiles leaves entry unchanged and skips save when Steam throws")
    void refresh_leavesEntryUnchangedOnFailure() {
        User user = staleUser();
        LocalDateTime previousSync = user.getSteamSyncedAt();
        when(userRepository.findSteamLinkedUsersStaleBefore(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID))
                .thenThrow(new SteamApiException("boom"));

        task.refreshStaleProfiles();

        assertThat(user.getSteamDisplayName()).isEqualTo("OldName");
        assertThat(user.getSteamAvatarUrl()).isEqualTo("https://old/avatar.jpg");
        assertThat(user.getSteamProfileUrl()).isEqualTo("https://old/profile");
        assertThat(user.getSteamSyncedAt()).isEqualTo(previousSync);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("refreshStaleProfiles leaves entry unchanged when Steam returns no player")
    void refresh_leavesEntryUnchangedWhenEmpty() {
        User user = staleUser();
        LocalDateTime previousSync = user.getSteamSyncedAt();
        when(userRepository.findSteamLinkedUsersStaleBefore(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.empty());

        task.refreshStaleProfiles();

        assertThat(user.getSteamDisplayName()).isEqualTo("OldName");
        assertThat(user.getSteamSyncedAt()).isEqualTo(previousSync);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("refreshStaleProfiles does nothing when no stale users are returned")
    void refresh_noStaleUsers() {
        when(userRepository.findSteamLinkedUsersStaleBefore(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        task.refreshStaleProfiles();

        verify(steamApiClient, never()).fetchPlayerSummary(any());
        verify(userRepository, never()).save(any(User.class));
    }
}
