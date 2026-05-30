package com.checkpoint.api.controllers;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.playlog.GamePlayLogRequestDto;
import com.checkpoint.api.dto.playlog.GamePlayLogResponseDto;
import com.checkpoint.api.services.GamePlayLogService;

import jakarta.validation.Valid;

/**
 * REST controller for the authenticated user's game play logs.
 */
@Tag(name = "Play Logs", description = "Current user play log entries")
@RestController
@RequestMapping("/api/me/plays")
public class GamePlayLogController {

    private static final Logger log = LoggerFactory.getLogger(GamePlayLogController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "updatedAt,desc";

    private final GamePlayLogService gamePlayLogService;

    public GamePlayLogController(GamePlayLogService gamePlayLogService) {
        this.gamePlayLogService = gamePlayLogService;
    }

    /**
     * Logs a new play session.
     */
    @PostMapping
    public ResponseEntity<GamePlayLogResponseDto> logPlay(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GamePlayLogRequestDto request) {

        log.info("POST /api/me/plays - user: {}", userDetails.getUsername());

        GamePlayLogResponseDto response = gamePlayLogService.logPlay(userDetails.getUsername(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing play session.
     */
    @PutMapping("/{playId}")
    public ResponseEntity<GamePlayLogResponseDto> updatePlayLog(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID playId,
            @Valid @RequestBody GamePlayLogRequestDto request) {

        log.info("PUT /api/me/plays/{} - user: {}", playId, userDetails.getUsername());

        GamePlayLogResponseDto response = gamePlayLogService.updatePlayLog(userDetails.getUsername(), playId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a play session.
     */
    @DeleteMapping("/{playId}")
    public ResponseEntity<Void> deletePlayLog(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID playId) {

        log.info("DELETE /api/me/plays/{} - user: {}", playId, userDetails.getUsername());

        gamePlayLogService.deletePlayLog(userDetails.getUsername(), playId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all play sessions for the user (paginated).
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<GamePlayLogResponseDto>> getUserPlayLog(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/me/plays - user: {}, page: {}, size: {}, sort: {}", userDetails.getUsername(), page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<GamePlayLogResponseDto> playLogs = gamePlayLogService.getUserPlayLog(userDetails.getUsername(), pageable);

        return ResponseEntity.ok(PagedResponseDto.from(playLogs));
    }

    /**
     * Retrieves all play sessions by the user for a specific game.
     */
    @GetMapping("/game/{videoGameId}")
    public ResponseEntity<List<GamePlayLogResponseDto>> getGamePlayHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId) {

        log.info("GET /api/me/plays/game/{} - user: {}", videoGameId, userDetails.getUsername());

        List<GamePlayLogResponseDto> history = gamePlayLogService.getGamePlayHistory(userDetails.getUsername(), videoGameId);

        return ResponseEntity.ok(history);
    }

    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String mappedField = mapSortField(sortField);

        return PageRequest.of(page, size, Sort.by(direction, mappedField));
    }

    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            case "timeplayed", "time_played" -> "timePlayed";
            case "startdate", "start_date" -> "startDate";
            case "enddate", "end_date" -> "endDate";
            case "status" -> "status";
            default -> "updatedAt";
        };
    }
}
