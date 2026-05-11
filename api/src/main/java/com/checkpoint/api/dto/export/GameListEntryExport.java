package com.checkpoint.api.dto.export;

import java.util.UUID;

public record GameListEntryExport(
        UUID gameId,
        String gameTitle,
        Integer position) {
}
