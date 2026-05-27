package com.checkpoint.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

/**
 * Integration tests for the Member discovery feature.
 * Uses H2 in-memory database with full Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:membertest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.search.backend.type=lucene",
        "spring.jpa.properties.hibernate.search.backend.directory.type=local-heap"
})
class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoGameRepository videoGameRepository;

    @Autowired
    private UserGameRepository userGameRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EntityManager entityManager;

    private User popularUser;
    private User reviewerUser;
    private User viewer;
    private User quietUser;
    private VideoGame game1;
    private VideoGame game2;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        userGameRepository.deleteAll();
        videoGameRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        popularUser = createUser("popular@example.com", "popularGamer", 5);
        reviewerUser = createUser("reviewer@example.com", "topReviewer", 3);
        viewer = createUser("viewer@example.com", "viewer", 1);
        quietUser = createUser("quiet@example.com", "quietUser", 2);

        // Create games
        game1 = createGame("Elden Ring");
        game2 = createGame("Zelda TOTK");

        // popularUser has 2 followers (reviewer + viewer follow popular)
        entityManager.createNativeQuery(
                "INSERT INTO user_follows (follower_id, following_id) VALUES (:fid, :tid)")
                .setParameter("fid", reviewerUser.getId())
                .setParameter("tid", popularUser.getId())
                .executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO user_follows (follower_id, following_id) VALUES (:fid, :tid)")
                .setParameter("fid", viewer.getId())
                .setParameter("tid", popularUser.getId())
                .executeUpdate();

        // reviewerUser has 1 follower (quietUser follows reviewer)
        entityManager.createNativeQuery(
                "INSERT INTO user_follows (follower_id, following_id) VALUES (:fid, :tid)")
                .setParameter("fid", quietUser.getId())
                .setParameter("tid", reviewerUser.getId())
                .executeUpdate();

        // Create reviews: reviewerUser has 2 reviews, popularUser has 1
        createReview(reviewerUser, game1, "Great game!");
        createReview(reviewerUser, game2, "Also great!");
        createReview(popularUser, game1, "Love it!");

        // Create user_games for suggested members:
        // viewer has game1, popularUser has game1, quietUser has game1 + game2
        userGameRepository.save(new UserGame(viewer, game1, GameStatus.PLAYING));
        userGameRepository.save(new UserGame(popularUser, game1, GameStatus.COMPLETED));
        userGameRepository.save(new UserGame(quietUser, game1, GameStatus.PLAYING));
        userGameRepository.save(new UserGame(quietUser, game2, GameStatus.PLAYING));

        entityManager.flush();
        entityManager.clear();
    }

    private User createUser(String email, String pseudo, int level) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setPseudo(pseudo);
        user.setLevel(level);
        user.setIsPrivate(false);
        return userRepository.save(user);
    }

    private VideoGame createGame(String title) {
        VideoGame game = new VideoGame();
        game.setTitle(title);
        return videoGameRepository.save(game);
    }

    private Review createReview(User user, VideoGame game, String content) {
        Review review = new Review();
        review.setUser(user);
        review.setVideoGame(game);
        review.setContent(content);
        review.setHaveSpoilers(false);
        return reviewRepository.save(review);
    }

    @Nested
    @DisplayName("GET /api/members/popular")
    class GetPopularMembers {

        @Test
        @DisplayName("should return members sorted by follower count descending")
        void shouldReturnMembersSortedByFollowerCount() throws Exception {
            mockMvc.perform(get("/api/members/popular").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[0].pseudo").value("popularGamer"))
                    .andExpect(jsonPath("$[0].followerCount").value(2))
                    .andExpect(jsonPath("$[0].isFollowing").doesNotExist());
        }

        @Test
        @DisplayName("should include isFollowing when authenticated")
        @WithMockUser(username = "viewer@example.com")
        void shouldIncludeIsFollowingWhenAuthenticated() throws Exception {
            mockMvc.perform(get("/api/members/popular").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].pseudo").value("popularGamer"))
                    .andExpect(jsonPath("$[0].isFollowing").value(true));
        }

        @Test
        @DisplayName("should respect size parameter")
        void shouldRespectSizeParameter() throws Exception {
            mockMvc.perform(get("/api/members/popular").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/members/top-reviewers")
    class GetTopReviewers {

        @Test
        @DisplayName("should return members sorted by review count descending")
        void shouldReturnMembersSortedByReviewCount() throws Exception {
            mockMvc.perform(get("/api/members/top-reviewers").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].pseudo").value("topReviewer"))
                    .andExpect(jsonPath("$[0].reviewCount").value(2));
        }

        @Test
        @DisplayName("should include isFollowing when authenticated")
        @WithMockUser(username = "viewer@example.com")
        void shouldIncludeIsFollowingWhenAuthenticated() throws Exception {
            mockMvc.perform(get("/api/members/top-reviewers").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].pseudo").value("topReviewer"))
                    .andExpect(jsonPath("$[0].isFollowing").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/members/suggested")
    class GetSuggestedMembers {

        @Test
        @DisplayName("should return suggestions based on shared games when authenticated")
        @WithMockUser(username = "viewer@example.com")
        void shouldReturnSuggestionsBasedOnSharedGames() throws Exception {
            // viewer has game1, popularUser and quietUser also have game1
            // viewer already follows popularUser, so only quietUser should appear
            mockMvc.perform(get("/api/members/suggested").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].pseudo").value("quietUser"))
                    .andExpect(jsonPath("$[0].isFollowing").value(false));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/members/suggested"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/members")
    class SearchMembers {

        @Test
        @DisplayName("should return paginated list of all members")
        void shouldReturnPaginatedMembers() throws Exception {
            mockMvc.perform(get("/api/members")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(4))
                    .andExpect(jsonPath("$.metadata.totalElements").value(4))
                    .andExpect(jsonPath("$.metadata.first").value(true));
        }

        @Test
        @DisplayName("should filter members by pseudo search")
        void shouldFilterByPseudoSearch() throws Exception {
            mockMvc.perform(get("/api/members")
                            .param("search", "popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].pseudo").value("popularGamer"));
        }

        @Test
        @DisplayName("should perform case-insensitive search")
        void shouldPerformCaseInsensitiveSearch() throws Exception {
            mockMvc.perform(get("/api/members")
                            .param("search", "POPULAR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].pseudo").value("popularGamer"));
        }

        @Test
        @DisplayName("should return empty results for non-matching search")
        void shouldReturnEmptyForNonMatchingSearch() throws Exception {
            mockMvc.perform(get("/api/members")
                            .param("search", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.metadata.totalElements").value(0));
        }

        @Test
        @DisplayName("should include isFollowing when authenticated")
        @WithMockUser(username = "viewer@example.com")
        void shouldIncludeIsFollowingWhenAuthenticated() throws Exception {
            mockMvc.perform(get("/api/members")
                            .param("search", "popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].isFollowing").value(true));
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void shouldRespectPaginationParameters() throws Exception {
            mockMvc.perform(get("/api/members")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.metadata.totalPages").value(2))
                    .andExpect(jsonPath("$.metadata.hasNext").value(true));
        }
    }
}
