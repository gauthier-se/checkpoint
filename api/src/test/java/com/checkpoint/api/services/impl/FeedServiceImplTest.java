package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.social.FeedGameDto;
import com.checkpoint.api.dto.social.FeedItemDto;
import com.checkpoint.api.dto.social.FeedUserDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.FeedItemType;
import com.checkpoint.api.mapper.FeedMapper;
import com.checkpoint.api.repositories.FeedRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

/**
 * Unit tests for {@link FeedServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class FeedServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private FeedMapper feedMapper;

    private FeedServiceImpl feedService;

    private User currentUser;
    private User friendUser;
    private VideoGame game;

    @BeforeEach
    void setUp() {
        feedService = new FeedServiceImpl(userRepository, feedRepository, videoGameRepository, feedMapper);

        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setEmail("user@test.com");
        currentUser.setPseudo("testuser");

        friendUser = new User();
        friendUser.setId(UUID.randomUUID());
        friendUser.setEmail("friend@test.com");
        friendUser.setPseudo("frienduser");
        friendUser.setPicture("avatar.jpg");

        game = new VideoGame();
        game.setId(UUID.randomUUID());
        game.setTitle("Elden Ring");
        game.setCoverUrl("cover.jpg");
    }

    @Test
    @DisplayName("getFeed should return feed items from followed users")
    void getFeed_shouldReturnFeedItems() {
        // Given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findFollowingIdsByUserId(currentUser.getId()))
                .thenReturn(List.of(friendUser.getId()));

        UUID feedItemId = UUID.randomUUID();
        Object[] row = new Object[]{
                feedItemId, "RATING", Timestamp.valueOf(LocalDateTime.now()),
                friendUser.getId(), game.getId(), "5", null
        };

        Pageable pageable = PageRequest.of(0, 20);
        List<Object[]> rows = Collections.singletonList(row);
        Page<Object[]> rawPage = new PageImpl<>(rows, pageable, 1);

        when(feedRepository.findFeedItems(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(rawPage);
        when(userRepository.findAllById(anyList())).thenReturn(List.of(friendUser));
        when(videoGameRepository.findAllById(anyList())).thenReturn(List.of(game));

        FeedItemDto expectedItem = new FeedItemDto(
                feedItemId, FeedItemType.RATING, LocalDateTime.now(),
                new FeedUserDto(friendUser.getId(), "frienduser", "avatar.jpg"),
                new FeedGameDto(game.getId(), "Elden Ring", "cover.jpg", null),
                null, 5, null, null, null, null
        );
        when(feedMapper.toFeedItemDto(any(Object[].class), any(Map.class), any(Map.class)))
                .thenReturn(expectedItem);

        // When
        PagedResponseDto<FeedItemDto> result = feedService.getFeed("user@test.com", 0, 20);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).type()).isEqualTo(FeedItemType.RATING);
        assertThat(result.content().get(0).score()).isEqualTo(5);
        assertThat(result.metadata().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getFeed should return empty when user follows nobody")
    void getFeed_shouldReturnEmptyWhenNoFollowing() {
        // Given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findFollowingIdsByUserId(currentUser.getId()))
                .thenReturn(Collections.emptyList());

        // When
        PagedResponseDto<FeedItemDto> result = feedService.getFeed("user@test.com", 0, 20);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.metadata().totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("getFriendsTrendingGames should return trending games among friends")
    void getFriendsTrendingGames_shouldReturnGames() {
        // Given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findFollowingIdsByUserId(currentUser.getId()))
                .thenReturn(List.of(friendUser.getId()));

        Object[] row = new Object[]{
                game.getId(), "Elden Ring", "cover.jpg",
                java.sql.Date.valueOf("2022-02-25"), 4.9, 5000L
        };
        List<Object[]> trendingRows = Collections.singletonList(row);
        when(videoGameRepository.findFriendsTrendingGames(anyList(), any(LocalDateTime.class), anyInt()))
                .thenReturn(trendingRows);

        // When
        List<GameCardDto> result = feedService.getFriendsTrendingGames("user@test.com", 7);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Elden Ring");
        assertThat(result.get(0).ratingCount()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("getFriendsTrendingGames should return empty list when user follows nobody")
    void getFriendsTrendingGames_shouldReturnEmptyListWhenNoFollowing() {
        // Given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findFollowingIdsByUserId(currentUser.getId()))
                .thenReturn(Collections.emptyList());

        // When
        List<GameCardDto> result = feedService.getFriendsTrendingGames("user@test.com", 7);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getFriendsPopularGames should return paginated trending games among friends")
    void getFriendsPopularGames_shouldReturnPaginatedGames() {
        // Given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findFollowingIdsByUserId(currentUser.getId()))
                .thenReturn(List.of(friendUser.getId()));

        Object[] row = new Object[]{
                game.getId(), "Elden Ring", "cover.jpg",
                java.sql.Date.valueOf("2022-02-25"), 4.9, 5000L
        };
        Pageable pageable = PageRequest.of(0, 32);
        List<Object[]> rows = Collections.singletonList(row);
        Page<Object[]> rawPage = new PageImpl<>(rows, pageable, 1);

        when(videoGameRepository.findFriendsTrendingGamesPage(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(rawPage);

        // When
        PagedResponseDto<GameCardDto> result = feedService.getFriendsPopularGames("user@test.com", 0, 32);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).title()).isEqualTo("Elden Ring");
        assertThat(result.content().get(0).ratingCount()).isEqualTo(5000L);
        assertThat(result.metadata().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getFriendsPopularGames should return empty page when user follows nobody")
    void getFriendsPopularGames_shouldReturnEmptyWhenNoFollowing() {
        // Given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findFollowingIdsByUserId(currentUser.getId()))
                .thenReturn(Collections.emptyList());

        // When
        PagedResponseDto<GameCardDto> result = feedService.getFriendsPopularGames("user@test.com", 0, 32);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.metadata().totalElements()).isEqualTo(0);
        verify(videoGameRepository, never())
                .findFriendsTrendingGamesPage(anyList(), any(LocalDateTime.class), any(Pageable.class));
    }
}
