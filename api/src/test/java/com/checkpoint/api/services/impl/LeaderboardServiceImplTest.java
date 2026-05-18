package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.leaderboard.LeaderboardEntryDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.LeaderboardSortBy;

/**
 * Unit tests for {@link LeaderboardServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private LeaderboardServiceImpl service;

    private User user(String pseudo, int level, int xp) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setPseudo(pseudo);
        u.setLevel(level);
        u.setXpPoint(xp);
        return u;
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new LeaderboardServiceImpl(userRepository);
    }

    @Test
    @DisplayName("XP sort calls findLeaderboardByXp and assigns ranks 1..N")
    void xpSort_assignsRanks() {
        Page<User> page = new PageImpl<>(List.of(
                user("alpha", 5, 9000),
                user("bravo", 4, 7000),
                user("charlie", 3, 5000)));
        when(userRepository.findLeaderboardByXp(any(Pageable.class))).thenReturn(page);

        List<LeaderboardEntryDto> result = service.getLeaderboard(LeaderboardSortBy.XP, 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(0).pseudo()).isEqualTo("alpha");
        assertThat(result.get(0).xpPoint()).isEqualTo(9000);
        assertThat(result.get(1).rank()).isEqualTo(2);
        assertThat(result.get(2).rank()).isEqualTo(3);
        assertThat(result.get(2).pseudo()).isEqualTo("charlie");

        verify(userRepository, never()).findLeaderboardByLevel(any());
    }

    @Test
    @DisplayName("LEVEL sort calls findLeaderboardByLevel")
    void levelSort_callsLevelRepository() {
        Page<User> page = new PageImpl<>(List.of(user("alpha", 99, 100)));
        when(userRepository.findLeaderboardByLevel(any(Pageable.class))).thenReturn(page);

        List<LeaderboardEntryDto> result = service.getLeaderboard(LeaderboardSortBy.LEVEL, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).level()).isEqualTo(99);
        verify(userRepository, never()).findLeaderboardByXp(any());
    }

    @Test
    @DisplayName("forwards the limit to a PageRequest of size=limit, page=0")
    void forwardsLimitToPageable() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findLeaderboardByXp(any(Pageable.class))).thenReturn(page);

        service.getLeaderboard(LeaderboardSortBy.XP, 25);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findLeaderboardByXp(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(25);
    }

    @Test
    @DisplayName("returns an empty list when no users match")
    void emptyResult() {
        when(userRepository.findLeaderboardByXp(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.getLeaderboard(LeaderboardSortBy.XP, 50)).isEmpty();
    }
}
