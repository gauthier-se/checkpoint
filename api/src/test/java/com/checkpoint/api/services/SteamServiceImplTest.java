package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.client.SteamApiClient;
import com.checkpoint.api.dto.igdb.IgdbExternalGameDto;
import com.checkpoint.api.dto.steam.SteamAccountDto;
import com.checkpoint.api.dto.steam.SteamOwnedGameDto;
import com.checkpoint.api.dto.steam.SteamPlayerSummaryDto;
import com.checkpoint.api.dto.steam.SteamSyncSummaryDto;
import com.checkpoint.api.entities.Backlog;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.Priority;
import com.checkpoint.api.exceptions.InvalidSteamIdException;
import com.checkpoint.api.exceptions.SteamAccountNotLinkedException;
import com.checkpoint.api.exceptions.SteamApiException;
import com.checkpoint.api.exceptions.SteamLibraryPrivateException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.GameImportService;
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

    @Mock
    private IgdbApiClient igdbApiClient;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private BacklogRepository backlogRepository;

    @Mock
    private GameImportService gameImportService;

    @Mock
    private OnboardingService onboardingService;

    @InjectMocks
    private SteamServiceImpl steamService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail(EMAIL);
        user.setPseudo("alice");
        user.setId(UUID.randomUUID());
    }

    private SteamPlayerSummaryDto summary() {
        return summary(3);
    }

    private SteamPlayerSummaryDto summary(Integer visibility) {
        return new SteamPlayerSummaryDto(
                STEAM_ID,
                "AliceOnSteam",
                "https://steamcommunity.com/id/alice",
                "https://avatar.url/s.jpg",
                "https://avatar.url/m.jpg",
                "https://avatar.url/full.jpg",
                visibility);
    }

    private SteamOwnedGameDto ownedGame(long appId, String name) {
        return new SteamOwnedGameDto(appId, name, 0L, "icon");
    }

    private VideoGame videoGameWith(long igdbId) {
        VideoGame vg = new VideoGame();
        vg.setId(UUID.randomUUID());
        vg.setIgdbId(igdbId);
        vg.setTitle("Game " + igdbId);
        return vg;
    }

    @Test
    @DisplayName("linkSteamAccount persists steamId, cached profile fields, and returns DTO when Steam recognizes the ID")
    void linkSteamAccount_success() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        SteamAccountDto dto = steamService.linkSteamAccount(EMAIL, STEAM_ID);

        assertThat(dto.steamId()).isEqualTo(STEAM_ID);
        assertThat(dto.steamDisplayName()).isEqualTo("AliceOnSteam");
        assertThat(user.getSteamId()).isEqualTo(STEAM_ID);
        assertThat(user.getSteamDisplayName()).isEqualTo("AliceOnSteam");
        assertThat(user.getSteamAvatarUrl()).isEqualTo("https://avatar.url/m.jpg");
        assertThat(user.getSteamProfileUrl()).isEqualTo("https://steamcommunity.com/id/alice");
        assertThat(user.getSteamSyncedAt()).isNotNull();
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
    @DisplayName("linkSteamAccount extracts SteamID64 from a steamcommunity.com/profiles/... URL")
    void linkSteamAccount_acceptsProfileUrl() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        SteamAccountDto dto = steamService.linkSteamAccount(
                EMAIL, "https://steamcommunity.com/profiles/" + STEAM_ID + "/");

        assertThat(dto.steamId()).isEqualTo(STEAM_ID);
        verify(steamApiClient, never()).resolveVanityUrl(any());
    }

    @Test
    @DisplayName("linkSteamAccount resolves a steamcommunity.com/id/<vanity> URL via ResolveVanityURL")
    void linkSteamAccount_acceptsVanityUrl() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.resolveVanityUrl("alice")).thenReturn(Optional.of(STEAM_ID));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        SteamAccountDto dto = steamService.linkSteamAccount(
                EMAIL, "https://steamcommunity.com/id/alice/");

        assertThat(dto.steamId()).isEqualTo(STEAM_ID);
        verify(steamApiClient).resolveVanityUrl("alice");
    }

    @Test
    @DisplayName("linkSteamAccount resolves a bare vanity name via ResolveVanityURL")
    void linkSteamAccount_acceptsBareVanityName() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.resolveVanityUrl("alice")).thenReturn(Optional.of(STEAM_ID));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        SteamAccountDto dto = steamService.linkSteamAccount(EMAIL, "alice");

        assertThat(dto.steamId()).isEqualTo(STEAM_ID);
        verify(steamApiClient).resolveVanityUrl("alice");
    }

    @Test
    @DisplayName("linkSteamAccount throws InvalidSteamIdException when Steam reports no vanity match")
    void linkSteamAccount_unknownVanity() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.resolveVanityUrl("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> steamService.linkSteamAccount(EMAIL, "ghost"))
                .isInstanceOf(InvalidSteamIdException.class)
                .hasMessageContaining("ghost");

        verify(steamApiClient, never()).fetchPlayerSummary(any());
    }

    @Test
    @DisplayName("linkSteamAccount rejects garbage input without calling Steam")
    void linkSteamAccount_garbageInput() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> steamService.linkSteamAccount(EMAIL, "not@a@valid#thing!"))
                .isInstanceOf(InvalidSteamIdException.class);

        verify(steamApiClient, never()).resolveVanityUrl(any());
        verify(steamApiClient, never()).fetchPlayerSummary(any());
    }

    @Test
    @DisplayName("linkSteamAccount treats a 17-digit numeric input as a SteamID64, not a vanity")
    void linkSteamAccount_numericInputIsTreatedAsSteamId() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        steamService.linkSteamAccount(EMAIL, STEAM_ID);

        verify(steamApiClient).fetchPlayerSummary(STEAM_ID);
        verify(steamApiClient, never()).resolveVanityUrl(any());
    }

    @Test
    @DisplayName("linkVerifiedSteamAccount stores steamId and stamps syncedAt even if Steam profile fetch fails")
    void linkVerifiedSteamAccount_steamFetchFails() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID))
                .thenThrow(new SteamApiException("boom"));

        SteamAccountDto dto = steamService.linkVerifiedSteamAccount(EMAIL, STEAM_ID);

        assertThat(dto.steamId()).isEqualTo(STEAM_ID);
        assertThat(dto.steamDisplayName()).isNull();
        assertThat(user.getSteamId()).isEqualTo(STEAM_ID);
        assertThat(user.getSteamDisplayName()).isNull();
        assertThat(user.getSteamAvatarUrl()).isNull();
        assertThat(user.getSteamProfileUrl()).isNull();
        assertThat(user.getSteamSyncedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("linkVerifiedSteamAccount enriches DTO and persists cached fields when Steam profile fetch succeeds")
    void linkVerifiedSteamAccount_success() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        SteamAccountDto dto = steamService.linkVerifiedSteamAccount(EMAIL, STEAM_ID);

        assertThat(dto.steamDisplayName()).isEqualTo("AliceOnSteam");
        assertThat(user.getSteamId()).isEqualTo(STEAM_ID);
        assertThat(user.getSteamDisplayName()).isEqualTo("AliceOnSteam");
        assertThat(user.getSteamAvatarUrl()).isEqualTo("https://avatar.url/m.jpg");
        assertThat(user.getSteamProfileUrl()).isEqualTo("https://steamcommunity.com/id/alice");
        assertThat(user.getSteamSyncedAt()).isNotNull();
    }

    @Test
    @DisplayName("unlinkSteamAccount clears the steamId and all cached profile fields")
    void unlinkSteamAccount_success() {
        user.setSteamId(STEAM_ID);
        user.setSteamDisplayName("AliceOnSteam");
        user.setSteamAvatarUrl("https://avatar.url/m.jpg");
        user.setSteamProfileUrl("https://steamcommunity.com/id/alice");
        user.setSteamSyncedAt(java.time.LocalDateTime.now());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        steamService.unlinkSteamAccount(EMAIL);

        assertThat(user.getSteamId()).isNull();
        assertThat(user.getSteamDisplayName()).isNull();
        assertThat(user.getSteamAvatarUrl()).isNull();
        assertThat(user.getSteamProfileUrl()).isNull();
        assertThat(user.getSteamSyncedAt()).isNull();
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

    @Test
    @DisplayName("syncSteamLibrary throws SteamAccountNotLinkedException when user has no steamId")
    void syncSteamLibrary_throwsWhenNoSteamAccountLinked() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> steamService.syncSteamLibrary(EMAIL))
                .isInstanceOf(SteamAccountNotLinkedException.class);

        verify(steamApiClient, never()).fetchPlayerSummary(any());
        verify(steamApiClient, never()).getOwnedGames(any());
    }

    @Test
    @DisplayName("syncSteamLibrary throws SteamLibraryPrivateException when communityVisibilityState != 3")
    void syncSteamLibrary_throwsWhenProfilePrivate() {
        user.setSteamId(STEAM_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary(1)));

        assertThatThrownBy(() -> steamService.syncSteamLibrary(EMAIL))
                .isInstanceOf(SteamLibraryPrivateException.class);

        verify(steamApiClient, never()).getOwnedGames(any());
    }

    @Test
    @DisplayName("syncSteamLibrary returns zero-counts summary when the library is empty")
    void syncSteamLibrary_returnsZeroSummaryWhenLibraryEmpty() {
        user.setSteamId(STEAM_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));
        when(steamApiClient.getOwnedGames(STEAM_ID)).thenReturn(List.of());

        SteamSyncSummaryDto result = steamService.syncSteamLibrary(EMAIL);

        assertThat(result).isEqualTo(new SteamSyncSummaryDto(0, 0, 0, 0));
        verify(igdbApiClient, never()).findIgdbIdsForSteamAppIds(anyList());
        verify(backlogRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("syncSteamLibrary imports missing games and adds matched games to the backlog with MEDIUM priority")
    void syncSteamLibrary_matchesAndImportsHappyPath() {
        user.setSteamId(STEAM_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));
        // 3 owned: 100, 200, 300. IGDB matches: 100->1000, 200->2000. 300 unmatched.
        when(steamApiClient.getOwnedGames(STEAM_ID)).thenReturn(List.of(
                ownedGame(100L, "Game A"),
                ownedGame(200L, "Game B"),
                ownedGame(300L, "Obscure Steam tool")));
        when(igdbApiClient.findIgdbIdsForSteamAppIds(anyList())).thenReturn(List.of(
                new IgdbExternalGameDto(1L, "100", 1000L),
                new IgdbExternalGameDto(2L, "200", 2000L)));

        // First lookup: only IGDB 1000 exists locally; 2000 needs import.
        VideoGame existingLocal = videoGameWith(1000L);
        VideoGame importedAfter = videoGameWith(2000L);
        when(videoGameRepository.findAllByIgdbIdIn(anyCollection()))
                .thenReturn(List.of(existingLocal))                 // pre-import check
                .thenReturn(List.of(existingLocal, importedAfter)); // post-import resolution

        when(backlogRepository.findExistingVideoGameIds(eq(user.getId()), anyCollection()))
                .thenReturn(List.of());

        SteamSyncSummaryDto result = steamService.syncSteamLibrary(EMAIL);

        assertThat(result).isEqualTo(new SteamSyncSummaryDto(3, 2, 0, 1));
        verify(gameImportService).importGamesByIds(List.of(2000L));

        ArgumentCaptor<List<Backlog>> captor = ArgumentCaptor.forClass(List.class);
        verify(backlogRepository).saveAll(captor.capture());
        List<Backlog> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(b -> b.getPriority() == Priority.MEDIUM);
        assertThat(saved).allMatch(b -> b.getUser() == user);
        assertThat(saved.stream().map(b -> b.getVideoGame().getIgdbId()))
                .containsExactlyInAnyOrder(1000L, 2000L);
    }

    @Test
    @DisplayName("syncSteamLibrary skips games already in the user's backlog and does not override their priority")
    void syncSteamLibrary_skipsGamesAlreadyInCollection() {
        user.setSteamId(STEAM_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));
        when(steamApiClient.getOwnedGames(STEAM_ID)).thenReturn(List.of(
                ownedGame(100L, "Game A"),
                ownedGame(200L, "Game B")));
        when(igdbApiClient.findIgdbIdsForSteamAppIds(anyList())).thenReturn(List.of(
                new IgdbExternalGameDto(1L, "100", 1000L),
                new IgdbExternalGameDto(2L, "200", 2000L)));

        VideoGame g1 = videoGameWith(1000L);
        VideoGame g2 = videoGameWith(2000L);
        // Both games already exist locally — no IGDB import needed.
        when(videoGameRepository.findAllByIgdbIdIn(anyCollection())).thenReturn(List.of(g1, g2));
        // g1 is already in the backlog; g2 is new.
        when(backlogRepository.findExistingVideoGameIds(eq(user.getId()), anyCollection()))
                .thenReturn(List.of(g1.getId()));

        SteamSyncSummaryDto result = steamService.syncSteamLibrary(EMAIL);

        assertThat(result).isEqualTo(new SteamSyncSummaryDto(2, 1, 1, 0));
        verify(gameImportService, never()).importGamesByIds(any());

        ArgumentCaptor<List<Backlog>> captor = ArgumentCaptor.forClass(List.class);
        verify(backlogRepository).saveAll(captor.capture());
        List<Backlog> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getVideoGame().getIgdbId()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("syncSteamLibrary forwards the full appId list to IGDB for large libraries (batching is the client's job)")
    void syncSteamLibrary_handlesLargeLibraryBatching() {
        user.setSteamId(STEAM_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));

        List<SteamOwnedGameDto> owned = Stream.iterate(1L, i -> i + 1)
                .limit(750)
                .map(i -> ownedGame(i, "Game " + i))
                .toList();
        when(steamApiClient.getOwnedGames(STEAM_ID)).thenReturn(owned);
        when(igdbApiClient.findIgdbIdsForSteamAppIds(anyList())).thenReturn(List.of());

        SteamSyncSummaryDto result = steamService.syncSteamLibrary(EMAIL);

        assertThat(result.total()).isEqualTo(750);
        assertThat(result.imported()).isZero();
        assertThat(result.unmatched()).isEqualTo(750);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(igdbApiClient).findIgdbIdsForSteamAppIds(captor.capture());
        assertThat(captor.getValue()).hasSize(750);
    }

    @Test
    @DisplayName("syncSteamLibrary counts unmatched correctly when IGDB returns no matches at all")
    void syncSteamLibrary_allUnmatched() {
        user.setSteamId(STEAM_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(steamApiClient.fetchPlayerSummary(STEAM_ID)).thenReturn(Optional.of(summary()));
        when(steamApiClient.getOwnedGames(STEAM_ID)).thenReturn(List.of(
                ownedGame(100L, "Obscure Demo"),
                ownedGame(200L, "Test Tool")));
        when(igdbApiClient.findIgdbIdsForSteamAppIds(anyList())).thenReturn(new ArrayList<>());

        SteamSyncSummaryDto result = steamService.syncSteamLibrary(EMAIL);

        assertThat(result).isEqualTo(new SteamSyncSummaryDto(2, 0, 0, 2));
        verify(gameImportService, never()).importGamesByIds(any());
        verify(backlogRepository, never()).saveAll(any());
    }
}
