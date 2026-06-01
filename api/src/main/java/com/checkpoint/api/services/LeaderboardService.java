package com.checkpoint.api.services;

import java.util.List;

import com.checkpoint.api.dto.leaderboard.LeaderboardEntryDto;

/**
 * Service for the public XP / level leaderboard.
 */
public interface LeaderboardService {

    /**
     * Returns the top users ranked by the given criterion.
     *
     * @param sortBy ranking criterion (XP or LEVEL)
     * @param limit  maximum number of entries to return
     * @return an ordered list of leaderboard entries, ranks 1..N
     */
    List<LeaderboardEntryDto> getLeaderboard(LeaderboardSortBy sortBy, int limit);

    /**
     * Returns the users that the given viewer follows, ranked by the given criterion.
     *
     * @param viewerEmail the authenticated viewer's email
     * @param sortBy      ranking criterion (XP or LEVEL)
     * @param limit       maximum number of entries to return
     * @return an ordered list of leaderboard entries (followed users only), ranks 1..N
     */
    List<LeaderboardEntryDto> getFollowingLeaderboard(String viewerEmail, LeaderboardSortBy sortBy, int limit);
}
