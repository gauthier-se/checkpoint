package com.checkpoint.api.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbTimeToBeatDto;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.GameImportService;
import com.checkpoint.api.services.GamePersistenceService;
import com.checkpoint.api.services.ImportProgressListener;

/**
 * Implementation of {@link GameImportService}.
 * Orchestrates imports from IGDB: it fetches data and time-to-beat statistics,
 * then delegates the per-game write to {@link GamePersistenceService} (which runs
 * each game in its own transaction). This class is intentionally NOT
 * {@code @Transactional} — a bulk import of thousands of games must not run in a
 * single giant transaction.
 */
@Service
public class GameImportServiceImpl implements GameImportService {

    private static final Logger log = LoggerFactory.getLogger(GameImportServiceImpl.class);

    private final IgdbApiClient igdbApiClient;
    private final GamePersistenceService gamePersistenceService;
    private final VideoGameRepository videoGameRepository;

    public GameImportServiceImpl(
            IgdbApiClient igdbApiClient,
            GamePersistenceService gamePersistenceService,
            VideoGameRepository videoGameRepository) {
        this.igdbApiClient = igdbApiClient;
        this.gamePersistenceService = gamePersistenceService;
        this.videoGameRepository = videoGameRepository;
    }

    @Override
    public List<VideoGame> importRecentlyReleasedGames(int limit) {
        log.info("Importing {} recently released games", limit);
        return importGames(igdbApiClient.fetchRecentlyReleasedGames(limit));
    }

    @Override
    public List<VideoGame> importGamesByIds(List<Long> igdbIds) {
        log.info("Importing {} games by IDs", igdbIds.size());
        return importGames(igdbApiClient.fetchGamesByIds(igdbIds));
    }

    @Override
    public List<VideoGame> searchAndImportGames(String query, int limit) {
        log.info("Searching and importing games matching '{}'", query);
        return importGames(igdbApiClient.searchGames(query, limit));
    }

    @Override
    public List<VideoGame> importTopRatedGames(int limit, int minRatingCount) {
        log.info("Importing top {} rated games (min {} ratings)", limit, minRatingCount);
        return importGames(igdbApiClient.fetchTopRatedGames(limit, minRatingCount));
    }

    @Override
    public BulkImportStats bulkImport(List<IgdbGameDto> games) {
        return bulkImport(games, ImportProgressListener.NOOP);
    }

    @Override
    public BulkImportStats bulkImport(List<IgdbGameDto> games, ImportProgressListener progress) {
        int totalFetched = games.size();
        int imported = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        Map<Long, IgdbTimeToBeatDto> timeToBeat = prefetchTimeToBeat(games);

        for (IgdbGameDto dto : games) {
            try {
                if (videoGameRepository.existsByIgdbId(dto.id())) {
                    skipped++;
                    progress.skipped();
                    continue;
                }
                gamePersistenceService.importOne(dto, timeToBeat.get(dto.id()));
                imported++;
                progress.imported();
            } catch (Exception e) {
                failed++;
                String label = dto.name() != null && !dto.name().isBlank()
                        ? dto.name()
                        : "IGDB#" + dto.id();
                errors.add(label);
                progress.failed(label);
                log.error("Bulk import failed for '{}' (IGDB ID: {}): {}",
                        dto.name(), dto.id(), e.getMessage(), e);
            } finally {
                progress.processed();
            }
        }

        log.info("Bulk import completed: {} imported, {} skipped, {} failed out of {} fetched",
                imported, skipped, failed, totalFetched);

        return new BulkImportStats(totalFetched, imported, skipped, failed, errors);
    }

    /**
     * Imports a list of games (upsert), prefetching time-to-beat once for the whole batch.
     * Individual failures are logged and skipped.
     *
     * @param games list of IGDB game DTOs to import
     * @return list of persisted VideoGame entities
     */
    private List<VideoGame> importGames(List<IgdbGameDto> games) {
        Map<Long, IgdbTimeToBeatDto> timeToBeat = prefetchTimeToBeat(games);

        List<VideoGame> result = new ArrayList<>();
        int failed = 0;
        for (IgdbGameDto dto : games) {
            try {
                result.add(gamePersistenceService.importOne(dto, timeToBeat.get(dto.id())));
            } catch (Exception e) {
                failed++;
                log.error("Failed to import game '{}' (IGDB ID: {}): {}",
                        dto.name(), dto.id(), e.getMessage(), e);
            }
        }

        log.info("Import completed: {} persisted, {} failed out of {} total",
                result.size(), failed, games.size());

        return result;
    }

    /**
     * Fetches time-to-beat data for every game in one batched operation, so the
     * import makes one request per 500 games instead of one request per game.
     */
    private Map<Long, IgdbTimeToBeatDto> prefetchTimeToBeat(List<IgdbGameDto> games) {
        List<Long> ids = games.stream()
                .map(IgdbGameDto::id)
                .filter(Objects::nonNull)
                .toList();
        return igdbApiClient.fetchTimeToBeatForGames(ids);
    }
}
