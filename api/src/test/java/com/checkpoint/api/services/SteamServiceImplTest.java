package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.client.SteamApiClient;
import com.checkpoint.api.dto.steam.SteamAccountDto;
import com.checkpoint.api.dto.steam.SteamPlayerSummaryDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.InvalidSteamIdException;
import com.checkpoint.api.exceptions.SteamApiException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.impl.SteamServiceImpl;

/**
 * Unit tests for {@link SteamServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class SteamServiceImplTest {

    private static final String EMAIL = "alice@test.com";
    private static final String STEAM_ID = "76561198000000000";

    @Mock
    private UserRepository userRepository;

    @Mock
    private SteamApiClient steamApiClient;

    @InjectMocks
    private SteamServiceImpl steamService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail(EMAIL);
        user.setPseudo("alice");
    }

    private SteamPlayerSummaryDto summary() {
        return new SteamPlayerSummaryDto(
                STEAM_ID,
                "AliceOnSteam",
                "https://steamcommunity.com/id/alice",
                "https://avatar.url/s.jpg",
                "https://avatar.url/m.jpg",
                "https://avatar.url/full.jpg");
    }

    @Test
    @DisplayName("linkSteamAccount persists steamId and returns DTO when Steam recognizes the ID")
    void linkSteamAccount_success() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        SteamAccountDto dto = steamService.linkSteamAccount(EMAIL, STEAM_ID);

        assertThat(dto.steamId()).isEqualTo(STEAM_ID);
        assertThat(dto.steamDisplayName()).isEqualTo("AliceOnSteam");
        assertThat(user.getSteamId()).isEqualTo(STEAM_ID);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("linkSteamAccount throws InvalidSteamIdException when Steam returns no player")
    void linkSteamAccount_invalidId() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> steamService.linkSteamAccount(EMAIL, STEAM_ID))
                .isInstanceOf(InvalidSteamIdException.class);

        assertThat(user.getSteamId()).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("linkSteamAccount throws UserNotFoundException when email is unknown")
    void linkSteamAccount_userNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> steamService.linkSteamAccount(EMAIL, STEAM_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(steamApiClient, never()).fetchPlayerSummary(any());
    }

    @Test
    @DisplayName("linkVerifiedSteamAccount stores steamId even if Steam profile fetch fails")
    void linkVerifiedSteamAccount_steamFetchFails() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID))
                .thenThrow(new SteamApiException("boom"));

        SteamAccountDto dto = steamService.linkVerifiedSteamAccount(EMAIL, STEAM_ID);

        assertThat(dto.steamId()).isEqualTo(STEAM_ID);
        assertThat(dto.steamDisplayName()).isNull();
        assertThat(user.getSteamId()).isEqualTo(STEAM_ID);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("linkVerifiedSteamAccount enriches DTO when Steam profile fetch succeeds")
    void linkVerifiedSteamAccount_success() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        SteamAccountDto dto = steamService.linkVerifiedSteamAccount(EMAIL, STEAM_ID);

        assertThat(dto.steamDisplayName()).isEqualTo("AliceOnSteam");
        assertThat(user.getSteamId()).isEqualTo(STEAM_ID);
    }

    @Test
    @DisplayName("unlinkSteamAccount clears the steamId field")
    void unlinkSteamAccount_success() {
        user.setSteamId(STEAM_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        steamService.unlinkSteamAccount(EMAIL);

        assertThat(user.getSteamId()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("findUserBySteamId delegates to the repository")
    void findUserBySteamId_delegates() {
        when(userRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.of(user));

        assertThat(steamService.findUserBySteamId(STEAM_ID)).contains(user);
    }

    @Test
    @DisplayName("getLinkedAccount returns enriched DTO when Steam responds")
    void getLinkedAccount_success() {
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        Optional<SteamAccountDto> result = steamService.getLinkedAccount(STEAM_ID);

        assertThat(result).isPresent();
        assertThat(result.get().steamDisplayName()).isEqualTo("AliceOnSteam");
    }

    @Test
    @DisplayName("getLinkedAccount returns DTO with null display name when Steam throws")
    void getLinkedAccount_steamThrows() {
        when(steamApiClient.fetchPlayerSummary(STEAM_ID))
                .thenThrow(new SteamApiException("boom"));

        Optional<SteamAccountDto> result = steamService.getLinkedAccount(STEAM_ID);

        assertThat(result).isPresent();
        assertThat(result.get().steamId()).isEqualTo(STEAM_ID);
        assertThat(result.get().steamDisplayName()).isNull();
    }

    @Test
    @DisplayName("getLinkedAccount returns empty for null or blank steamId")
    void getLinkedAccount_nullOrBlank() {
        assertThat(steamService.getLinkedAccount(null)).isEmpty();
        assertThat(steamService.getLinkedAccount(" ")).isEmpty();
    }
}
