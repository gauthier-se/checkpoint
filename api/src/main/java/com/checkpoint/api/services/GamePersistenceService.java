package com.checkpoint.api.services;

import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbTimeToBeatDto;
import com.checkpoint.api.entities.VideoGame;

/**
 * Persists a single game (and its relationships) from IGDB data in its own
 * transaction.
 *
 * <p>This lives in a dedicated bean — separate from {@link GameImportService} —
 * on purpose. Spring's {@code @Transactional} only takes effect when a method is
 * invoked through the Spring proxy; a self-invocation (one method calling another
 * on {@code this}) bypasses the proxy and the annotation is ignored. By keeping
 * the per-game write here, the bulk-import loop in {@code GameImportService} calls
 * it across a bean boundary, so each game really does commit in its own
 * transaction. That keeps transactions small, makes progress durable mid-import,
 * and prevents one failing game from rolling back the others.</p>
 */
public interface GamePersistenceService {

    /**
     * Creates or updates a single game (upsert by IGDB id) together with its
     * genres, platforms and companies, in a brand-new transaction.
     *
     * @param dto         the IGDB game data
     * @param timeToBeat  the pre-fetched time-to-beat data, or {@code null} if unavailable
     * @return the persisted entity
     */
    VideoGame importOne(IgdbGameDto dto, IgdbTimeToBeatDto timeToBeat);
}
