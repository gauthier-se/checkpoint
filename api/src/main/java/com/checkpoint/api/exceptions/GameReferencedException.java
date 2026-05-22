package com.checkpoint.api.exceptions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thrown when an admin tries to delete a video game that is still referenced
 * by user-owned data (libraries, play logs, reviews, backlogs, wishlists,
 * favorites, ratings, likes, list entries) or by child DLC entries.
 *
 * Carries the per-category blocking counts so the API can return a 409 with
 * enough information for the client to display a clear message.
 */
public class GameReferencedException extends RuntimeException {

    private final UUID gameId;
    private final Map<String, Long> blockingReferences;

    public GameReferencedException(UUID gameId, Map<String, Long> blockingReferences) {
        super("Game " + gameId + " is referenced and cannot be deleted: "
                + (blockingReferences == null ? "{}" : blockingReferences));
        this.gameId = gameId;
        this.blockingReferences = blockingReferences == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(blockingReferences));
    }

    public UUID getGameId() {
        return gameId;
    }

    public Map<String, Long> getBlockingReferences() {
        return blockingReferences;
    }
}
