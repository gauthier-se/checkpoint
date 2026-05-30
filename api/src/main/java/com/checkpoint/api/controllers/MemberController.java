package com.checkpoint.api.controllers;

import java.util.List;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.social.MemberCardDto;
import com.checkpoint.api.services.MemberService;

/**
 * REST controller for member discovery endpoints.
 * Provides popular members, top reviewers, personalized suggestions, and search/browse.
 */
@Tag(name = "Account and Profile", description = "Community member directory")
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    private static final int DEFAULT_SIZE = 10;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * Returns the most popular members ranked by follower count.
     *
     * @param size        the number of members to return (default 10, max 100)
     * @param userDetails the authenticated viewer (nullable)
     * @return a list of popular member cards
     */
    @GetMapping("/popular")
    public ResponseEntity<List<MemberCardDto>> getPopularMembers(
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("GET /api/members/popular - size: {}, viewer: {}", validatedSize, viewerEmail);

        Pageable pageable = PageRequest.of(0, validatedSize);
        List<MemberCardDto> members = memberService.getPopularMembers(pageable, viewerEmail);

        return ResponseEntity.ok(members);
    }

    /**
     * Returns the top reviewers ranked by review count.
     *
     * @param size        the number of members to return (default 10, max 100)
     * @param userDetails the authenticated viewer (nullable)
     * @return a list of top reviewer member cards
     */
    @GetMapping("/top-reviewers")
    public ResponseEntity<List<MemberCardDto>> getTopReviewers(
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("GET /api/members/top-reviewers - size: {}, viewer: {}", validatedSize, viewerEmail);

        Pageable pageable = PageRequest.of(0, validatedSize);
        List<MemberCardDto> members = memberService.getTopReviewers(pageable, viewerEmail);

        return ResponseEntity.ok(members);
    }

    /**
     * Returns personalized member suggestions based on shared games.
     * Requires authentication.
     *
     * @param size        the number of suggestions to return (default 10, max 100)
     * @param userDetails the authenticated user (required)
     * @return a list of suggested member cards, or 401 if not authenticated
     */
    @GetMapping("/suggested")
    public ResponseEntity<List<MemberCardDto>> getSuggestedMembers(
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        String viewerEmail = userDetails.getUsername();

        log.info("GET /api/members/suggested - size: {}, viewer: {}", validatedSize, viewerEmail);

        Pageable pageable = PageRequest.of(0, validatedSize);
        List<MemberCardDto> members = memberService.getSuggestedMembers(pageable, viewerEmail);

        return ResponseEntity.ok(members);
    }

    /**
     * Returns the most recently registered members.
     *
     * @param size        the number of members to return (default 10, max 100)
     * @param userDetails the authenticated viewer (nullable)
     * @return a list of recently joined member cards
     */
    @GetMapping("/recent")
    public ResponseEntity<List<MemberCardDto>> getRecentMembers(
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("GET /api/members/recent - size: {}, viewer: {}", validatedSize, viewerEmail);

        Pageable pageable = PageRequest.of(0, validatedSize);
        List<MemberCardDto> members = memberService.getRecentMembers(pageable, viewerEmail);

        return ResponseEntity.ok(members);
    }

    /**
     * Searches and browses members with optional pseudo filter.
     *
     * @param search      the search term for pseudo (optional)
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param userDetails the authenticated viewer (nullable)
     * @return a paginated list of member cards
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<MemberCardDto>> searchMembers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);
        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("GET /api/members - search: {}, page: {}, size: {}, viewer: {}",
                search, validatedPage, validatedSize, viewerEmail);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.ASC, "pseudo"));
        Page<MemberCardDto> membersPage = memberService.searchMembers(search, pageable, viewerEmail);

        return ResponseEntity.ok(PagedResponseDto.from(membersPage));
    }
}
