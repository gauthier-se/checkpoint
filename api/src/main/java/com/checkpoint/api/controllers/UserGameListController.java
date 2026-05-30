package com.checkpoint.api.controllers;

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
import com.checkpoint.api.dto.list.AddGameToListRequestDto;
import com.checkpoint.api.dto.list.CreateGameListRequestDto;
import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.dto.list.GameListDetailDto;
import com.checkpoint.api.dto.list.ReorderGamesRequestDto;
import com.checkpoint.api.dto.list.UpdateGameListRequestDto;
import com.checkpoint.api.services.GameListService;

import jakarta.validation.Valid;

/**
 * REST controller for authenticated user game list operations.
 * All endpoints require authentication.
 */
@Tag(name = "Lists", description = "Current user custom game lists")
@RestController
@RequestMapping("/api/me/lists")
public class UserGameListController {

    private static final Logger log = LoggerFactory.getLogger(UserGameListController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final GameListService gameListService;

    /**
     * Constructs a new UserGameListController.
     *
     * @param gameListService the game list service
     */
    public UserGameListController(GameListService gameListService) {
        this.gameListService = gameListService;
    }

    /**
     * Creates a new game list for the authenticated user.
     *
     * @param userDetails the authenticated user
     * @param request     the creation request
     * @return the created list detail with 201 status
     */
    @PostMapping
    public ResponseEntity<GameListDetailDto> createList(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateGameListRequestDto request) {

        log.info("POST /api/me/lists - user: {}, title: '{}'", userDetails.getUsername(), request.title());

        GameListDetailDto created = gameListService.createList(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns a paginated list of the authenticated user's game lists.
     *
     * @param userDetails the authenticated user
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @return paginated list of game list card DTOs
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<GameListCardDto>> getMyLists(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {

        log.info("GET /api/me/lists - user: {}, page: {}, size: {}", userDetails.getUsername(), page, size);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GameListCardDto> lists = gameListService.getUserLists(userDetails.getUsername(), pageable);

        return ResponseEntity.ok(PagedResponseDto.from(lists));
    }

    /**
     * Updates an existing game list. Only the owner can update.
     *
     * @param userDetails the authenticated user
     * @param listId      the list ID
     * @param request     the update request
     * @return the updated list detail
     */
    @PutMapping("/{listId}")
    public ResponseEntity<GameListDetailDto> updateList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID listId,
            @RequestBody UpdateGameListRequestDto request) {

        log.info("PUT /api/me/lists/{} - user: {}", listId, userDetails.getUsername());

        GameListDetailDto updated = gameListService.updateList(userDetails.getUsername(), listId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a game list. Only the owner can delete.
     *
     * @param userDetails the authenticated user
     * @param listId      the list ID
     * @return 204 No Content
     */
    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID listId) {

        log.info("DELETE /api/me/lists/{} - user: {}", listId, userDetails.getUsername());

        gameListService.deleteList(userDetails.getUsername(), listId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a video game to a list. Only the owner can add games.
     *
     * @param userDetails the authenticated user
     * @param listId      the list ID
     * @param request     the add game request
     * @return the updated list detail with 201 status
     */
    @PostMapping("/{listId}/games")
    public ResponseEntity<GameListDetailDto> addGameToList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID listId,
            @Valid @RequestBody AddGameToListRequestDto request) {

        log.info("POST /api/me/lists/{}/games - user: {}, gameId: {}",
                listId, userDetails.getUsername(), request.videoGameId());

        GameListDetailDto updated = gameListService.addGameToList(userDetails.getUsername(), listId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    /**
     * Removes a video game from a list. Only the owner can remove games.
     *
     * @param userDetails the authenticated user
     * @param listId      the list ID
     * @param videoGameId the video game ID to remove
     * @return 204 No Content
     */
    @DeleteMapping("/{listId}/games/{videoGameId}")
    public ResponseEntity<Void> removeGameFromList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID listId,
            @PathVariable UUID videoGameId) {

        log.info("DELETE /api/me/lists/{}/games/{} - user: {}", listId, videoGameId, userDetails.getUsername());

        gameListService.removeGameFromList(userDetails.getUsername(), listId, videoGameId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reorders games in a list. Only the owner can reorder.
     *
     * @param userDetails the authenticated user
     * @param listId      the list ID
     * @param request     the reorder request with ordered video game IDs
     * @return the updated list detail
     */
    @PutMapping("/{listId}/games/reorder")
    public ResponseEntity<GameListDetailDto> reorderGames(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID listId,
            @Valid @RequestBody ReorderGamesRequestDto request) {

        log.info("PUT /api/me/lists/{}/games/reorder - user: {}", listId, userDetails.getUsername());

        GameListDetailDto updated = gameListService.reorderGames(userDetails.getUsername(), listId, request);
        return ResponseEntity.ok(updated);
    }
}
