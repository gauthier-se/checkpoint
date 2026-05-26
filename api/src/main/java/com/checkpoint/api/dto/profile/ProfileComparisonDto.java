package com.checkpoint.api.dto.profile;

import com.checkpoint.api.dto.catalog.PagedResponseDto;

/**
 * DTO representing the comparison between the authenticated viewer and another user.
 *
 * @param affinityScore     the overall affinity score (0–100), blending library overlap
 *                          (Jaccard, 60%) and rating similarity (40%)
 * @param commonGamesCount  the number of games both users have in their libraries
 * @param viewerLibrarySize the total number of games in the viewer's library
 * @param targetLibrarySize the total number of games in the compared user's library
 * @param commonGames       the paginated common games, sorted by rating disagreement first
 */
public record ProfileComparisonDto(
        int affinityScore,
        int commonGamesCount,
        int viewerLibrarySize,
        int targetLibrarySize,
        PagedResponseDto<CommonGameEntryDto> commonGames
) {}
