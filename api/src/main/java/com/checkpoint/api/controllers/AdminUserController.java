package com.checkpoint.api.controllers;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.checkpoint.api.dto.admin.AdminUserDetailDto;
import com.checkpoint.api.dto.admin.AdminUserDto;
import com.checkpoint.api.dto.admin.AdminUserEditDto;
import com.checkpoint.api.services.AdminUserService;

/**
 * REST controller for admin user management operations.
 * All endpoints require the {@code ROLE_ADMIN} authority.
 */
@Tag(name = "Admin", description = "Admin: user management")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Returns all registered users.
     *
     * @return list of users with ID, username, email, and ban status
     */
    @Operation(summary = "List all users (admin)")
    @GetMapping
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        log.info("Admin request: fetching all users");

        List<AdminUserDto> users = adminUserService.getAllUsers();

        log.info("Returning {} users", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * Returns detailed profile information for a specific user.
     *
     * @param id the user's UUID
     * @return the detailed user profile
     */
    @Operation(summary = "Get a user's details (admin)")
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailDto> getUserById(@PathVariable UUID id) {
        log.info("Admin request: fetching user detail for {}", id);

        AdminUserDetailDto user = adminUserService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    /**
     * Edits a user's profile fields (clear bio, clear picture, toggle private).
     *
     * @param id  the user's UUID
     * @param dto the edit fields
     * @return the updated user detail
     */
    @Operation(summary = "Edit a user's profile fields (admin)")
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserDetailDto> editUser(@PathVariable UUID id,
                                                      @RequestBody AdminUserEditDto dto) {
        log.info("Admin request: editing user {}", id);

        AdminUserDetailDto updated = adminUserService.editUser(id, dto);

        return ResponseEntity.ok(updated);
    }

    /**
     * Bans a user account.
     *
     * @param id the user's UUID
     * @return 204 No Content on success
     */
    @Operation(summary = "Ban a user account (admin)")
    @PostMapping("/{id}/ban")
    public ResponseEntity<Void> banUser(@PathVariable UUID id) {
        log.info("Admin request: banning user {}", id);

        adminUserService.banUser(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Unbans a user account.
     *
     * @param id the user's UUID
     * @return 204 No Content on success
     */
    @Operation(summary = "Unban a user account (admin)")
    @PostMapping("/{id}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID id) {
        log.info("Admin request: unbanning user {}", id);

        adminUserService.unbanUser(id);

        return ResponseEntity.noContent().build();
    }
}
