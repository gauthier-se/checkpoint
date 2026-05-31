package com.seyzeriat.desktop.exception;

import java.util.Map;

/**
 * Exception thrown when a game cannot be deleted because it is still referenced by other entities.
 */
public class GameReferencedException extends Exception {
    private final Map<String, Long> blockingReferences;

    /**
     * Constructs a new GameReferencedException with the given blocking references.
     *
     * @param blockingReferences a map containing the types and counts of blocking references
     */
    public GameReferencedException(Map<String, Long> blockingReferences) {
        super("Game is still referenced and cannot be deleted.");
        this.blockingReferences = blockingReferences;
    }

    /**
     * Gets the map of references that are preventing the deletion of the game.
     *
     * @return a map of blocking references
     */
    public Map<String, Long> getBlockingReferences() {
        return blockingReferences;
    }
}
