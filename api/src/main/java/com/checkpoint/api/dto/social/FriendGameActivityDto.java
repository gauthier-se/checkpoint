package com.checkpoint.api.dto.social;

import java.util.List;
import java.util.Map;

import com.checkpoint.api.enums.PlayStatus;

/**
 * Aggregated engagement for the game-detail "Friend Activity" panel.
 *
 * @param totalCount          total number of distinct friends represented
 * @param countsByPlayStatus  count of friends whose primary play status is each value
 * @param friends             ordered list of friend entries (most recent play first)
 */
public record FriendGameActivityDto(
        int totalCount,
        Map<PlayStatus, Long> countsByPlayStatus,
        List<FriendActivityEntryDto> friends
) {}
