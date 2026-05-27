package com.checkpoint.api.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.client.SteamApiClient;
import com.checkpoint.api.dto.igdb.IgdbExternalGameDto;
import com.checkpoint.api.dto.onboarding.OnboardingSteps;
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
import com.checkpoint.api.services.OnboardingService;
import com.checkpoint.api.services.SteamService;

/**
 * Implementation of {@link SteamService}.
 */
@Service
@Transactional(readOnly = true)
public class SteamServiceImpl implements SteamService {

    private static final Logger log = LoggerFactory.getLogger(SteamServiceImpl.class);

    private static final Pattern STEAM_ID_64_PATTERN = Pattern.compile("\\d{17}");
    private static final Pattern PROFILE_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?steamcommunity\\.com/profiles/(\\d{17})/?$");
    private static final Pattern VANITY_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?steamcommunity\\.com/id/([A-Za-z0-9_-]{2,32})/?$");
    private static final Pattern VANITY_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,32}$");

    /**
     * Steam {@code communityvisibilitystate} value that grants access to owned games.
     * Anything else (1 = private, 2 = friends-only) blocks {@code GetOwnedGames}.
     */
    private static final int STEAM_VISIBILITY_PUBLIC = 3;

    private final UserRepository userRepository;
    private final SteamApiClient steamApiClient;
    private final IgdbApiClient igdbApiClient;
    private final VideoGameRepository videoGameRepository;
    private final BacklogRepository backlogRepository;
    private final GameImportService gameImportService;
    private final OnboardingService onboardingService;

    public SteamServiceImpl(UserRepository userRepository,
                            SteamApiClient steamApiClient,
                            IgdbApiClient igdbApiClient,
                            VideoGameRepository videoGameRepository,
                            BacklogRepository backlogRepository,
                            GameImportService gameImportService,
                            OnboardingService onboardingService) {
        this.userRepository = userRepository;
        this.steamApiClient = steamApiClient;
        this.igdbApiClient = igdbApiClient;
        this.videoGameRepository = videoGameRepository;
        this.backlogRepository = backlogRepository;
        this.gameImportService = gameImportService;
        this.onboardingService = onboardingService;
    }

    @Override
    @Transactional
    public SteamAccountDto linkSteamAccount(String email, String steamId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        String resolvedSteamId = normalizeSteamId(steamId);

        SteamPlayerSummaryDto summary = steamApiClient.fetchPlayerSummary(resolvedSteamId)
                .orElseThrow(() -> new InvalidSteamIdException(
                        "Steam does not recognize SteamID: " + resolvedSteamId));

        return persistLink(user, summary);
    }

    @Override
    @Transactional
    public SteamAccountDto linkVerifiedSteamAccount(String email, String steamId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // OpenID has already established ownership; we just enrich the response with the display
        // name on a best-effort basis.
        SteamPlayerSummaryDto summary = null;
        try {
            summary = steamApiClient.fetchPlayerSummary(steamId).orElse(null);
        } catch (SteamApiException e) {
            log.warn("Could not fetch Steam profile after OpenID link: {}", e.getMessage());
        }

        if (summary != null) {
            return persistLink(user, summary);
        }
        user.setSteamId(steamId);
        user.setSteamDisplayName(null);
        user.setSteamAvatarUrl(null);
        user.setSteamProfileUrl(null);
        user.setSteamSyncedAt(LocalDateTime.now());
        userRepository.save(user);
        onboardingService.markStepDone(email, OnboardingSteps.STEAM);
        return new SteamAccountDto(steamId, null, null, null);
    }

    @Override
    @Transactional
    public void unlinkSteamAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        user.setSteamId(null);
        user.setSteamDisplayName(null);
        user.setSteamAvatarUrl(null);
        user.setSteamProfileUrl(null);
        user.setSteamSyncedAt(null);
        userRepository.save(user);
        log.info("Unlinked Steam account for user {}", email);
    }

    @Override
    public Optional<User> findUserBySteamId(String steamId) {
        return userRepository.findBySteamId(steamId);
    }

    @Override
    public Optional<SteamAccountDto> getLinkedAccount(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            return Optional.empty();
        }
        try {
            return steamApiClient.fetchPlayerSummary(steamId)
                    .map(summary -> new SteamAccountDto(
                            summary.steamId(),
                            summary.personaName(),
                            summary.profileUrl(),
                            summary.avatarMedium()));
        } catch (SteamApiException e) {
            log.warn("Failed to enrich Steam account for {}: {}", steamId, e.getMessage());
            return Optional.of(new SteamAccountDto(steamId, null, null, null));
        }
    }

    /**
     * Normalizes a user-supplied Steam identifier to a 17-digit SteamID64.
     *
     * <p>Accepts a bare SteamID64, a {@code steamcommunity.com/profiles/<id>} URL,
     * a {@code steamcommunity.com/id/<vanity>} URL, or a bare vanity name. Vanity
     * inputs are resolved via Steam's {@code ResolveVanityURL} endpoint. The
     * 17-digit check runs first so a numeric input is never sent to vanity
     * resolution.</p>
     */
    private String normalizeSteamId(String raw) {
        String input = raw == null ? "" : raw.trim();

        if (STEAM_ID_64_PATTERN.matcher(input).matches()) {
            return input;
        }

        Matcher profileMatch = PROFILE_URL_PATTERN.matcher(input);
        if (profileMatch.matches()) {
            return profileMatch.group(1);
        }

        Matcher vanityUrlMatch = VANITY_URL_PATTERN.matcher(input);
        if (vanityUrlMatch.matches()) {
            return resolveVanity(vanityUrlMatch.group(1));
        }

        if (VANITY_NAME_PATTERN.matcher(input).matches()) {
            return resolveVanity(input);
        }

        throw new InvalidSteamIdException(
                "Could not recognize Steam input. Provide a SteamID64, a profile URL, or a vanity name.");
    }

    private String resolveVanity(String vanity) {
        return steamApiClient.resolveVanityUrl(vanity)
                .orElseThrow(() -> new InvalidSteamIdException(
                        "Steam does not recognize vanity name: " + vanity));
    }

    @Override
    @Transactional
    public SteamSyncSummaryDto syncSteamLibrary(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));

        String steamId = user.getSteamId();
        if (steamId == null || steamId.isBlank()) {
            throw new SteamAccountNotLinkedException(
                    "No Steam account linked. Link your Steam account first.");
        }

        SteamPlayerSummaryDto summary = steamApiClient.fetchPlayerSummary(steamId)
                .orElseThrow(() -> new SteamApiException(
                        "Steam did not return a profile for SteamID " + steamId));
        if (summary.communityVisibilityState() == null
                || summary.communityVisibilityState() != STEAM_VISIBILITY_PUBLIC) {
            throw new SteamLibraryPrivateException(
                    "Your Steam library is private. Change visibility to Public on Steam, then retry.");
        }

        List<SteamOwnedGameDto> owned = steamApiClient.getOwnedGames(steamId);
        int total = owned.size();
        if (total == 0) {
            log.info("Steam sync for {} found no owned games", userEmail);
            return new SteamSyncSummaryDto(0, 0, 0, 0);
        }

        List<Long> appIds = owned.stream()
                .map(SteamOwnedGameDto::appId)
                .filter(Objects::nonNull)
                .toList();

        List<IgdbExternalGameDto> externalGames = igdbApiClient.findIgdbIdsForSteamAppIds(appIds);

        Set<Long> matchedIgdbIds = new HashSet<>();
        Set<Long> matchedAppIds = new HashSet<>();
        for (IgdbExternalGameDto row : externalGames) {
            Long appId = parseAppId(row.uid());
            if (appId == null) {
                continue;
            }
            matchedAppIds.add(appId);
            matchedIgdbIds.add(row.game());
        }
        int unmatched = total - matchedAppIds.size();

        if (matchedIgdbIds.isEmpty()) {
            log.info("Steam sync for {}: total={}, no IGDB matches", userEmail, total);
            return new SteamSyncSummaryDto(total, 0, 0, unmatched);
        }

        // Identify which matched IGDB IDs are not yet in the local DB and import them.
        List<VideoGame> alreadyLocal = videoGameRepository.findAllByIgdbIdIn(matchedIgdbIds);
        Set<Long> localIgdbIds = alreadyLocal.stream()
                .map(VideoGame::getIgdbId)
                .collect(Collectors.toSet());
        List<Long> toImport = matchedIgdbIds.stream()
                .filter(id -> !localIgdbIds.contains(id))
                .toList();
        if (!toImport.isEmpty()) {
            log.info("Steam sync for {}: importing {} new games from IGDB", userEmail, toImport.size());
            gameImportService.importGamesByIds(toImport);
        }

        // Re-fetch so we have the full set of resolved VideoGame entities (including the freshly imported ones).
        List<VideoGame> resolvedGames = videoGameRepository.findAllByIgdbIdIn(matchedIgdbIds);
        List<UUID> resolvedVideoGameIds = resolvedGames.stream()
                .map(VideoGame::getId)
                .toList();
        Set<UUID> existingVideoGameIds = new HashSet<>(
                backlogRepository.findExistingVideoGameIds(user.getId(), resolvedVideoGameIds));

        List<Backlog> toAdd = new ArrayList<>();
        for (VideoGame game : resolvedGames) {
            if (!existingVideoGameIds.contains(game.getId())) {
                Backlog backlog = new Backlog(user, game);
                backlog.setPriority(Priority.MEDIUM);
                toAdd.add(backlog);
            }
        }
        if (!toAdd.isEmpty()) {
            backlogRepository.saveAll(toAdd);
        }

        int imported = toAdd.size();
        int skipped = resolvedGames.size() - imported;

        log.info("Steam sync for {}: total={}, imported={}, skipped={}, unmatched={}",
                userEmail, total, imported, skipped, unmatched);
        return new SteamSyncSummaryDto(total, imported, skipped, unmatched);
    }

    private Long parseAppId(String uid) {
        if (uid == null || uid.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(uid.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private SteamAccountDto persistLink(User user, SteamPlayerSummaryDto summary) {
        user.setSteamId(summary.steamId());
        user.setSteamDisplayName(summary.personaName());
        user.setSteamAvatarUrl(summary.avatarMedium());
        user.setSteamProfileUrl(summary.profileUrl());
        user.setSteamSyncedAt(LocalDateTime.now());
        userRepository.save(user);
        onboardingService.markStepDone(user.getEmail(), OnboardingSteps.STEAM);
        log.info("Linked Steam account {} to user {}", summary.steamId(), user.getEmail());
        return new SteamAccountDto(
                summary.steamId(),
                summary.personaName(),
                summary.profileUrl(),
                summary.avatarMedium());
    }
}
