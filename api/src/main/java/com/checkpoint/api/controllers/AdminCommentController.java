package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.services.AdminCommentService;

/**
 * REST controller for admin comment management operations.
 * All endpoints require the {@code ROLE_ADMIN} authority.
 */
@Tag(name = "Admin", description = "Admin: comment moderation")
@RestController
@RequestMapping("/api/admin/comments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController {

    private static final Logger log = LoggerFactory.getLogger(AdminCommentController.class);

    private final AdminCommentService adminCommentService;

    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    /**
     * Deletes a comment by its ID, bypassing the ownership check.
     *
     * @param id the ID of the comment to delete
     * @return 204 No Content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id) {
        log.info("Admin request: deleting comment with id {}", id);

        adminCommentService.deleteComment(id);

        return ResponseEntity.noContent().build();
    }
}
