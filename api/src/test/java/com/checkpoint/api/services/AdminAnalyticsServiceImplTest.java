package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.admin.AdminAnalyticsDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.repositories.ReportRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.impl.AdminAnalyticsServiceImpl;

/**
 * Unit tests for {@link AdminAnalyticsServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private VideoGameRepository videoGameRepository;

    private AdminAnalyticsServiceImpl adminAnalyticsService;

    @BeforeEach
    void setUp() {
        adminAnalyticsService = new AdminAnalyticsServiceImpl(
                userRepository, reviewRepository, reportRepository, videoGameRepository);
    }

    private User createUser(UUID id, String pseudo) {
        User user = new User();
        user.setId(id);
        user.setPseudo(pseudo);
        return user;
    }

    @Test
    @DisplayName("Should aggregate counts and top-5 lists into the analytics DTO")
    void shouldAggregateAnalytics() {
        // Given
        UUID gameId1 = UUID.randomUUID();
        UUID gameId2 = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(userRepository.count()).thenReturn(42L);
        when(userRepository.countByBannedFalse()).thenReturn(40L);
        when(videoGameRepository.countParentGames()).thenReturn(100L);
        when(reviewRepository.count()).thenReturn(250L);
        when(reportRepository.count()).thenReturn(3L);

        Page<Object[]> topGamesPage = new PageImpl<>(List.of(
                new Object[]{gameId1, "Hades", 17L},
                new Object[]{gameId2, "Celeste", 12L}
        ));
        when(reviewRepository.findTopReviewedGames(any(Pageable.class))).thenReturn(topGamesPage);

        Page<Object[]> topReviewersPage = new PageImpl<>(List.of(
                new Object[]{createUser(userId1, "alice"), 9L},
                new Object[]{createUser(userId2, "bob"), 7L}
        ));
        when(userRepository.findTopReviewers(any(Pageable.class))).thenReturn(topReviewersPage);

        // When
        AdminAnalyticsDto result = adminAnalyticsService.getAnalytics();

        // Then
        assertThat(result.totalUsers()).isEqualTo(42L);
        assertThat(result.activeUsers()).isEqualTo(40L);
        assertThat(result.totalGames()).isEqualTo(100L);
        assertThat(result.totalReviews()).isEqualTo(250L);
        assertThat(result.openReports()).isEqualTo(3L);

        assertThat(result.topReviewedGames()).hasSize(2);
        assertThat(result.topReviewedGames().get(0).id()).isEqualTo(gameId1);
        assertThat(result.topReviewedGames().get(0).title()).isEqualTo("Hades");
        assertThat(result.topReviewedGames().get(0).reviewCount()).isEqualTo(17L);

        assertThat(result.topReviewers()).hasSize(2);
        assertThat(result.topReviewers().get(0).id()).isEqualTo(userId1);
        assertThat(result.topReviewers().get(0).username()).isEqualTo("alice");
        assertThat(result.topReviewers().get(0).reviewCount()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Should request top-5 entries from each ranking query")
    void shouldRequestTopFiveEntries() {
        // Given
        when(reviewRepository.findTopReviewedGames(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(userRepository.findTopReviewers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        adminAnalyticsService.getAnalytics();

        // Then
        ArgumentCaptor<Pageable> gamesPageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Pageable> reviewersPageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findTopReviewedGames(gamesPageable.capture());
        verify(userRepository).findTopReviewers(reviewersPageable.capture());

        assertThat(gamesPageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(gamesPageable.getValue().getPageNumber()).isZero();
        assertThat(reviewersPageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(reviewersPageable.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("Should return empty lists when no top entries are found")
    void shouldHandleEmptyTopLists() {
        // Given
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByBannedFalse()).thenReturn(0L);
        when(videoGameRepository.countParentGames()).thenReturn(0L);
        when(reviewRepository.count()).thenReturn(0L);
        when(reportRepository.count()).thenReturn(0L);
        when(reviewRepository.findTopReviewedGames(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(userRepository.findTopReviewers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        AdminAnalyticsDto result = adminAnalyticsService.getAnalytics();

        // Then
        assertThat(result.topReviewedGames()).isEmpty();
        assertThat(result.topReviewers()).isEmpty();
    }
}
