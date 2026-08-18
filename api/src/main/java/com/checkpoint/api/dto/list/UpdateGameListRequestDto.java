package com.checkpoint.api.dto.list;

/**
 * Request DTO for updating an existing game list.
 * All fields are optional: only non-null values are applied.
 */
public record UpdateGameListRequestDto(
        String title,
        String description,
        Boolean isPrivate
) {}
