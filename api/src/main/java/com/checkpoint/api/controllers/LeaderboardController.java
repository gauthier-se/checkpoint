package com.checkpoint.api.controllers;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.leaderboard.LeaderboardEntryDto;
import com.checkpoint.api.services.LeaderboardService;
import com.checkpoint.api.services.LeaderboardSortBy;

/**
 * REST controller for the public XP / level leaderboard.
 *
 * <p>The endpoint is publicly accessible (no auth required). Banned users
 * are filtered out at the repository layer.</p>
 */
@Tag(name = "Gamification", description = "Leaderboards")
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardController.class);

    private static final int DEFAULT_LIMIT = 50;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Returns the leaderboard top-N.
     *
     * @param sortBy ranking criterion: {@code xp} (default) or {@code level}
     * @param limit  number of entries to return, 1..100 (default 50)
     * @return the ordered list of leaderboard entries
     */
    @GetMapping
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "xp") String sortBy,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {

        log.info("GET /api/leaderboard - sortBy: {}, limit: {}", sortBy, limit);

        LeaderboardSortBy parsed = parseSortBy(sortBy);
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "limit must be between " + MIN_LIMIT + " and " + MAX_LIMIT);
        }
        return ResponseEntity.ok(leaderboardService.getLeaderboard(parsed, limit));
    }

    private LeaderboardSortBy parseSortBy(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "xp" -> LeaderboardSortBy.XP;
            case "level" -> LeaderboardSortBy.LEVEL;
            default -> throw new IllegalArgumentException(
                    "sortBy must be 'xp' or 'level'");
        };
    }
}
