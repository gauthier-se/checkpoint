package com.checkpoint.api.services.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.client.SteamApiClient;
import com.checkpoint.api.dto.steam.SteamAccountDto;
import com.checkpoint.api.dto.steam.SteamPlayerSummaryDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.InvalidSteamIdException;
import com.checkpoint.api.exceptions.SteamApiException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.SteamService;

/**
 * Implementation of {@link SteamService}.
 */
@Service
@Transactional(readOnly = true)
public class SteamServiceImpl implements SteamService {

    private static final Logger log = LoggerFactory.getLogger(SteamServiceImpl.class);

    private final UserRepository userRepository;
    private final SteamApiClient steamApiClient;

    public SteamServiceImpl(UserRepository userRepository, SteamApiClient steamApiClient) {
        this.userRepository = userRepository;
        this.steamApiClient = steamApiClient;
    }

    @Override
    @Transactional
    public SteamAccountDto linkSteamAccount(String email, String steamId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        SteamPlayerSummaryDto summary = steamApiClient.fetchPlayerSummary(steamId)
                .orElseThrow(() -> new InvalidSteamIdException(
                        "Steam does not recognize SteamID: " + steamId));

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

    private SteamAccountDto persistLink(User user, SteamPlayerSummaryDto summary) {
        user.setSteamId(summary.steamId());
        user.setSteamDisplayName(summary.personaName());
        user.setSteamAvatarUrl(summary.avatarMedium());
        user.setSteamProfileUrl(summary.profileUrl());
        user.setSteamSyncedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Linked Steam account {} to user {}", summary.steamId(), user.getEmail());
        return new SteamAccountDto(
                summary.steamId(),
                summary.personaName(),
                summary.profileUrl(),
                summary.avatarMedium());
    }
}
