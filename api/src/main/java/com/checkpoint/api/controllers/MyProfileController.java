package com.checkpoint.api.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.UpdateProfileDto;
import com.checkpoint.api.services.ProfileService;

import jakarta.validation.Valid;

/**
 * REST controller for authenticated user profile management.
 *
 * <p>Provides endpoints for the current user to update their profile
 * information and manage their profile picture.</p>
 */
@Tag(name = "Account and Profile", description = "Current user profile editing")
@RestController
@RequestMapping("/api/me")
public class MyProfileController {

    private static final Logger log = LoggerFactory.getLogger(MyProfileController.class);

    private final ProfileService profileService;

    /**
     * Constructs a new MyProfileController.
     *
     * @param profileService the profile service
     */
    public MyProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Updates the authenticated user's profile information (pseudo, bio, privacy).
     *
     * @param userDetails the authenticated user
     * @param request     the profile update data
     * @return the updated profile DTO
     */
    @PutMapping("/profile")
    public ResponseEntity<ProfileUpdatedDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileDto request) {

        log.info("PUT /api/me/profile - user: {}", userDetails.getUsername());

        ProfileUpdatedDto updated = profileService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Uploads or replaces the authenticated user's profile picture.
     *
     * @param userDetails the authenticated user
     * @param file        the uploaded image file
     * @return a map containing the new picture URL
     */
    @PostMapping("/picture")
    public ResponseEntity<Map<String, String>> updatePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        log.info("POST /api/me/picture - user: {}", userDetails.getUsername());

        String pictureUrl = profileService.updatePicture(userDetails.getUsername(), file);
        return ResponseEntity.ok(Map.of("picture", pictureUrl));
    }

    /**
     * Removes the authenticated user's profile picture.
     *
     * @param userDetails the authenticated user
     * @return no content
     */
    @DeleteMapping("/picture")
    public ResponseEntity<Void> deletePicture(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("DELETE /api/me/picture - user: {}", userDetails.getUsername());

        profileService.deletePicture(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
