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
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.social.CommentRequestDto;
import com.checkpoint.api.dto.social.CommentResponseDto;
import com.checkpoint.api.services.CommentService;

import jakarta.validation.Valid;

/**
 * REST controller for comments on reviews and game lists.
 * GET endpoints are public; POST/PUT/DELETE require authentication.
 */
@Tag(name = "Reviews and Comments", description = "Comments on reviews")
@RestController
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CommentService commentService;

    /**
     * Constructs a new CommentController.
     *
     * @param commentService the comment service
     */
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Retrieves a paginated list of top-level comments for a review.
     *
     * @param reviewId    the review ID
     * @param userDetails the authenticated user principal (nullable)
     * @param page        the page number (0-based)
     * @param size        the page size
     * @return the paginated comments
     */
    @GetMapping("/api/reviews/{reviewId}/comments")
    public ResponseEntity<PagedResponseDto<CommentResponseDto>> getReviewComments(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {

        log.info("GET /api/reviews/{}/comments - page: {}, size: {}", reviewId, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        Pageable pageable = createPageable(page, size);
        Page<CommentResponseDto> comments = commentService.getReviewComments(reviewId, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(comments));
    }

    /**
     * Adds a comment to a review.
     *
     * @param userDetails the authenticated user principal
     * @param reviewId    the review ID
     * @param request     the comment request body
     * @return the created comment
     */
    @PostMapping("/api/reviews/{reviewId}/comments")
    public ResponseEntity<CommentResponseDto> addReviewComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID reviewId,
            @Valid @RequestBody CommentRequestDto request) {

        log.info("POST /api/reviews/{}/comments - user: {}", reviewId, userDetails.getUsername());

        CommentResponseDto response = commentService.addReviewComment(
                userDetails.getUsername(), reviewId, request.content());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a paginated list of top-level comments for a game list.
     *
     * @param listId      the game list ID
     * @param userDetails the authenticated user principal (nullable)
     * @param page        the page number (0-based)
     * @param size        the page size
     * @return the paginated comments
     */
    @GetMapping("/api/lists/{listId}/comments")
    public ResponseEntity<PagedResponseDto<CommentResponseDto>> getListComments(
            @PathVariable UUID listId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {

        log.info("GET /api/lists/{}/comments - page: {}, size: {}", listId, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        Pageable pageable = createPageable(page, size);
        Page<CommentResponseDto> comments = commentService.getListComments(listId, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(comments));
    }

    /**
     * Adds a comment to a game list.
     *
     * @param userDetails the authenticated user principal
     * @param listId      the game list ID
     * @param request     the comment request body
     * @return the created comment
     */
    @PostMapping("/api/lists/{listId}/comments")
    public ResponseEntity<CommentResponseDto> addListComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID listId,
            @Valid @RequestBody CommentRequestDto request) {

        log.info("POST /api/lists/{}/comments - user: {}", listId, userDetails.getUsername());

        CommentResponseDto response = commentService.addListComment(
                userDetails.getUsername(), listId, request.content());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Adds a reply to an existing comment.
     *
     * @param userDetails the authenticated user principal
     * @param commentId   the parent comment ID
     * @param request     the comment request body
     * @return the created reply
     */
    @PostMapping("/api/comments/{commentId}/replies")
    public ResponseEntity<CommentResponseDto> addReply(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequestDto request) {

        log.info("POST /api/comments/{}/replies - user: {}", commentId, userDetails.getUsername());

        CommentResponseDto response = commentService.addReply(
                userDetails.getUsername(), commentId, request.content());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a paginated list of replies for a comment.
     *
     * @param commentId   the parent comment ID
     * @param userDetails the authenticated user principal (nullable)
     * @param page        the page number (0-based)
     * @param size        the page size
     * @return the paginated replies
     */
    @GetMapping("/api/comments/{commentId}/replies")
    public ResponseEntity<PagedResponseDto<CommentResponseDto>> getReplies(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {

        log.info("GET /api/comments/{}/replies - page: {}, size: {}", commentId, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        Pageable pageable = createPageable(page, size);
        Page<CommentResponseDto> replies = commentService.getReplies(commentId, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(replies));
    }

    /**
     * Updates a comment. Only the comment owner can perform this action.
     *
     * @param userDetails the authenticated user principal
     * @param commentId   the comment ID
     * @param request     the comment request body with updated content
     * @return the updated comment
     */
    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequestDto request) {

        log.info("PUT /api/comments/{} - user: {}", commentId, userDetails.getUsername());

        CommentResponseDto response = commentService.updateComment(
                userDetails.getUsername(), commentId, request.content());

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a comment. Only the comment owner can perform this action.
     *
     * @param userDetails the authenticated user principal
     * @param commentId   the comment ID
     * @return 204 No Content
     */
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID commentId) {

        log.info("DELETE /api/comments/{} - user: {}", commentId, userDetails.getUsername());

        commentService.deleteComment(userDetails.getUsername(), commentId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a Pageable with default sort by createdAt descending.
     *
     * @param page the page number
     * @param size the page size
     * @return a Pageable instance
     */
    private Pageable createPageable(int page, int size) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        return PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
