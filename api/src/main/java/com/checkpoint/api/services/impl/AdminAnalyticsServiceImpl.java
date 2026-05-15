package com.checkpoint.api.services.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.admin.AdminAnalyticsDto;
import com.checkpoint.api.dto.admin.AdminAnalyticsDto.TopGame;
import com.checkpoint.api.dto.admin.AdminAnalyticsDto.TopReviewer;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.repositories.ReportRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.AdminAnalyticsService;

/**
 * Implementation of {@link AdminAnalyticsService}.
 * Fetches counts and top-5 rankings from the relevant repositories.
 */
@Service
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final int TOP_LIMIT = 5;

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final VideoGameRepository videoGameRepository;

    public AdminAnalyticsServiceImpl(UserRepository userRepository,
                                     ReviewRepository reviewRepository,
                                     ReportRepository reportRepository,
                                     VideoGameRepository videoGameRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
        this.videoGameRepository = videoGameRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsDto getAnalytics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByBannedFalse();
        long totalGames = videoGameRepository.countParentGames();
        long totalReviews = reviewRepository.count();
        long openReports = reportRepository.count();

        Pageable topFive = PageRequest.of(0, TOP_LIMIT);

        List<TopGame> topReviewedGames = reviewRepository.findTopReviewedGames(topFive)
                .stream()
                .map(row -> new TopGame(
                        (UUID) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();

        List<TopReviewer> topReviewers = userRepository.findTopReviewers(topFive)
                .stream()
                .map(row -> {
                    User user = (User) row[0];
                    long count = ((Number) row[1]).longValue();
                    return new TopReviewer(user.getId(), user.getPseudo(), count);
                })
                .toList();

        return new AdminAnalyticsDto(
                totalUsers,
                activeUsers,
                totalGames,
                totalReviews,
                openReports,
                topReviewedGames,
                topReviewers
        );
    }
}
