package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.playlog.PlayLogDetailDto;
import com.checkpoint.api.services.PlayLogService;

/**
 * Public REST controller for reading a single play log by ID.
 *
 * <p>Reads are public; authentication is optional and only used to populate
 * viewer-specific flags ({@code isOwner}, {@code isLikedByViewer}).</p>
 */
@Tag(name = "Play Logs", description = "Public play log entries")
@RestController
@RequestMapping("/api/plays")
public class PlayLogController {

    private static final Logger log = LoggerFactory.getLogger(PlayLogController.class);

    private final PlayLogService playLogService;

    public PlayLogController(PlayLogService playLogService) {
        this.playLogService = playLogService;
    }

    /**
     * Returns the public detail of a play log.
     *
     * @param playId      the play log ID
     * @param userDetails the authenticated viewer, or null if anonymous
     * @return the play log detail with 200 status
     */
    @GetMapping("/{playId}")
    public ResponseEntity<PlayLogDetailDto> getPlayLogDetail(
            @PathVariable UUID playId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        log.info("GET /api/plays/{} - viewer: {}", playId, viewerEmail != null ? viewerEmail : "anonymous");

        PlayLogDetailDto detail = playLogService.getPlayLogDetail(playId, viewerEmail);
        return ResponseEntity.ok(detail);
    }
}
