package com.checkpoint.api.services.impl;

import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.leaderboard.LeaderboardEntryDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.LeaderboardService;
import com.checkpoint.api.services.LeaderboardSortBy;

/**
 * Implementation of {@link LeaderboardService}.
 */
@Service
@Transactional(readOnly = true)
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardServiceImpl.class);

    private final UserRepository userRepository;

    public LeaderboardServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<LeaderboardEntryDto> getLeaderboard(LeaderboardSortBy sortBy, int limit) {
        log.info("Fetching leaderboard (sortBy={}, limit={})", sortBy, limit);

        Pageable pageable = PageRequest.of(0, limit);
        Page<User> page = switch (sortBy) {
            case XP -> userRepository.findLeaderboardByXp(pageable);
            case LEVEL -> userRepository.findLeaderboardByLevel(pageable);
        };

        List<User> users = page.getContent();
        return IntStream.range(0, users.size())
                .mapToObj(i -> toDto(i + 1, users.get(i)))
                .toList();
    }

    private LeaderboardEntryDto toDto(int rank, User user) {
        return new LeaderboardEntryDto(
                rank,
                user.getId(),
                user.getPseudo(),
                user.getPicture(),
                user.getLevel(),
                user.getXpPoint()
        );
    }
}
