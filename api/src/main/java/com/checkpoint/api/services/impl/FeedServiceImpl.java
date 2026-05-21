package com.checkpoint.api.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.social.FeedItemDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.mapper.FeedMapper;
import com.checkpoint.api.repositories.FeedRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.FeedService;

/**
 * Implementation of {@link FeedService}.
 * Provides activity feed and friends trending data by querying across
 * multiple activity tables filtered to the authenticated user's follow graph.
 */
@Service
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedServiceImpl.class);

    private static final int FEED_WINDOW_DAYS = 30;
    private static final int DEFAULT_FEED_SIZE = 20;
    private static final int MAX_FEED_SIZE = 50;

    private static final int TRENDING_WINDOW_DAYS = 7;
    private static final int DEFAULT_TRENDING_SIZE = 7;
    private static final int MAX_TRENDING_SIZE = 20;

    private static final int MAX_POPULAR_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final VideoGameRepository videoGameRepository;
    private final FeedMapper feedMapper;

    public FeedServiceImpl(UserRepository userRepository,
                           FeedRepository feedRepository,
                           VideoGameRepository videoGameRepository,
                           FeedMapper feedMapper) {
        this.userRepository = userRepository;
        this.feedRepository = feedRepository;
        this.videoGameRepository = videoGameRepository;
        this.feedMapper = feedMapper;
    }

    @Override
    public PagedResponseDto<FeedItemDto> getFeed(String userEmail, int page, int size) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(Math.max(1, size), MAX_FEED_SIZE);

        log.debug("Fetching feed for user {} - page: {}, size: {}", userEmail, validatedPage, validatedSize);

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        List<UUID> followingIds = userRepository.findFollowingIdsByUserId(currentUser.getId());
        if (followingIds.isEmpty()) {
            log.debug("User {} follows nobody, returning empty feed", userEmail);
            Pageable pageable = PageRequest.of(validatedPage, validatedSize);
            return PagedResponseDto.from(Page.empty(pageable));
        }

        LocalDateTime since = LocalDateTime.now().minusDays(FEED_WINDOW_DAYS);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        Page<Object[]> rawPage = feedRepository.findFeedItems(followingIds, since, pageable);

        List<Object[]> rows = rawPage.getContent();

        // Batch-fetch users and games to avoid N+1
        List<UUID> userIds = rows.stream()
                .map(row -> (UUID) row[3])
                .distinct()
                .toList();
        List<UUID> gameIds = rows.stream()
                .map(row -> row[4] != null ? (UUID) row[4] : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, User> userCache = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, VideoGame> gameCache = gameIds.isEmpty()
                ? Collections.emptyMap()
                : videoGameRepository.findAllById(gameIds).stream()
                        .collect(Collectors.toMap(VideoGame::getId, Function.identity()));

        List<FeedItemDto> feedItems = rows.stream()
                .map(row -> feedMapper.toFeedItemDto(row, userCache, gameCache))
                .filter(Objects::nonNull)
                .toList();

        return PagedResponseDto.from(
                new org.springframework.data.domain.PageImpl<>(feedItems, pageable, rawPage.getTotalElements())
        );
    }

    @Override
    public List<GameCardDto> getFriendsTrendingGames(String userEmail, int size) {
        int validatedSize = Math.min(Math.max(1, size), MAX_TRENDING_SIZE);

        log.debug("Fetching friends trending games for user {} - size: {}", userEmail, validatedSize);

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        List<UUID> followingIds = userRepository.findFollowingIdsByUserId(currentUser.getId());
        if (followingIds.isEmpty()) {
            log.debug("User {} follows nobody, returning empty friends trending", userEmail);
            return Collections.emptyList();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);
        List<Object[]> results = videoGameRepository.findFriendsTrendingGames(followingIds, since, validatedSize);

        return results.stream()
                .map(this::mapToGameCardDto)
                .toList();
    }

    @Override
    public PagedResponseDto<GameCardDto> getFriendsPopularGames(String userEmail, int page, int size) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(Math.max(1, size), MAX_POPULAR_PAGE_SIZE);

        log.debug("Fetching friends popular games for user {} - page: {}, size: {}", userEmail, validatedPage, validatedSize);

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        List<UUID> followingIds = userRepository.findFollowingIdsByUserId(currentUser.getId());
        if (followingIds.isEmpty()) {
            log.debug("User {} follows nobody, returning empty popular games page", userEmail);
            return PagedResponseDto.from(Page.empty(pageable));
        }

        LocalDateTime since = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);
        Page<Object[]> rawPage = videoGameRepository.findFriendsTrendingGamesPage(followingIds, since, pageable);

        List<GameCardDto> cards = rawPage.getContent().stream()
                .map(this::mapToGameCardDto)
                .toList();

        return PagedResponseDto.from(
                new org.springframework.data.domain.PageImpl<>(cards, pageable, rawPage.getTotalElements())
        );
    }

    /**
     * Maps a native SQL result row to a GameCardDto.
     *
     * @param row the result row (id, title, coverUrl, releaseDate, averageRating, ratingCount)
     * @return the mapped GameCardDto
     */
    private GameCardDto mapToGameCardDto(Object[] row) {
        UUID id = (UUID) row[0];
        String title = (String) row[1];
        String coverUrl = (String) row[2];
        LocalDate releaseDate = row[3] != null ? ((java.sql.Date) row[3]).toLocalDate() : null;
        Double averageRating = row[4] != null ? ((Number) row[4]).doubleValue() : null;
        Long ratingCount = row[5] != null ? ((Number) row[5]).longValue() : 0L;

        return new GameCardDto(id, title, coverUrl, releaseDate, averageRating, ratingCount);
    }
}
