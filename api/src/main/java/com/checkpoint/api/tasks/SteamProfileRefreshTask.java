package com.checkpoint.api.tasks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.client.SteamApiClient;
import com.checkpoint.api.dto.steam.SteamPlayerSummaryDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.SteamApiException;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Scheduled task that keeps cached Steam profile fields ({@code steamDisplayName},
 * {@code steamAvatarUrl}, {@code steamProfileUrl}) on the {@code User} entity fresh.
 *
 * <p>Runs daily at 04:00. Picks up to {@code steam.refresh.batch-size} users whose
 * {@code steamSyncedAt} is older than 24 hours and re-fetches their summary from Steam.
 * The underlying {@link SteamApiClient} enforces a 1 req/s rate limit, so a batch of N
 * users takes roughly N seconds — keep the batch size well under the daily window.</p>
 *
 * <p>If Steam returns no player (unknown SteamID), or throws, the entity is left unchanged
 * so {@code /me} keeps serving the previously cached values.</p>
 */
@Component
public class SteamProfileRefreshTask {

    private static final Logger log = LoggerFactory.getLogger(SteamProfileRefreshTask.class);

    private final UserRepository userRepository;
    private final SteamApiClient steamApiClient;
    private final int batchSize;

    public SteamProfileRefreshTask(UserRepository userRepository,
                                   SteamApiClient steamApiClient,
                                   @Value("${steam.refresh.batch-size:200}") int batchSize) {
        this.userRepository = userRepository;
        this.steamApiClient = steamApiClient;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void refreshStaleProfiles() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<User> stale = userRepository.findSteamLinkedUsersStaleBefore(
                cutoff, PageRequest.of(0, batchSize));

        if (stale.isEmpty()) {
            log.debug("Steam profile refresh: no stale users");
            return;
        }

        log.info("Steam profile refresh: {} user(s) to process", stale.size());
        int refreshed = 0;
        int skipped = 0;
        for (User user : stale) {
            if (refreshOne(user)) {
                refreshed++;
            } else {
                skipped++;
            }
        }
        log.info("Steam profile refresh: {} refreshed, {} left unchanged", refreshed, skipped);
    }

    private boolean refreshOne(User user) {
        try {
            Optional<SteamPlayerSummaryDto> summary = steamApiClient.fetchPlayerSummary(user.getSteamId());
            if (summary.isEmpty()) {
                log.warn("Steam profile refresh: SteamID {} not recognized — leaving cache unchanged",
                        user.getSteamId());
                return false;
            }
            SteamPlayerSummaryDto s = summary.get();
            user.setSteamDisplayName(s.personaName());
            user.setSteamAvatarUrl(s.avatarMedium());
            user.setSteamProfileUrl(s.profileUrl());
            user.setSteamSyncedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        } catch (SteamApiException e) {
            log.warn("Steam profile refresh failed for SteamID {}: {} — leaving cache unchanged",
                    user.getSteamId(), e.getMessage());
            return false;
        }
    }
}
