package com.seyzeriat.desktop.exception;

import java.util.Map;

public class GameReferencedException extends Exception {
    private final Map<String, Long> blockingReferences;

    public GameReferencedException(Map<String, Long> blockingReferences) {
        super("Game is still referenced and cannot be deleted.");
        this.blockingReferences = blockingReferences;
    }

    public Map<String, Long> getBlockingReferences() {
        return blockingReferences;
    }
}
