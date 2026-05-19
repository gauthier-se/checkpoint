package com.checkpoint.api.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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

import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.entities.Like;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.entities.Wish;
import com.checkpoint.api.repositories.BadgeRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;

/**
 * Integration tests for the Profile feature.
 * Uses H2 in-memory database with full Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:profiletest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.search.backend.type=lucene",
        "spring.jpa.properties.hibernate.search.backend.directory.type=local-heap"
})
class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoGameRepository videoGameRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private UserGamePlayRepository userGamePlayRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private User otherUser;
    private VideoGame likedGame;
    private VideoGame reviewedGame;
    private VideoGame replayGame;

    @BeforeEach
    void setUp() {
        likeRepository.deleteAll();
        userGamePlayRepository.deleteAll();
        reviewRepository.deleteAll();
        wishRepository.deleteAll();
        videoGameRepository.deleteAll();
        platformRepository.deleteAll();
        // Clear join tables and users before badges
        userRepository.deleteAll();
        badgeRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("gamer@example.com");
        testUser.setPassword("password");
        testUser.setPseudo("gamer123");
        testUser.setBio("I love video games");
        testUser.setLevel(3);
        testUser.setXpPoint(2500);
        testUser.setIsPrivate(false);
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setPseudo("otheruser");
        otherUser.setIsPrivate(false);
        otherUser = userRepository.save(otherUser);

        // Add a badge to testUser
        Badge badge = new Badge();
        badge.setCode("FIRST_REVIEW");
        badge.setName("First Review");
        badge.setDescription("Write your first review");
        badge = badgeRepository.save(badge);
        testUser.addBadge(badge);
        testUser = userRepository.save(testUser);

        // Add a game to wishlist
        VideoGame game = new VideoGame();
        game.setTitle("Elden Ring");
        game = videoGameRepository.save(game);

        Wish wish = new Wish();
        wish.setUser(testUser);
        wish.setVideoGame(game);
        wishRepository.save(wish);

        // Set up a follow relationship via native query to avoid lazy init
        entityManager.createNativeQuery("INSERT INTO user_follows (follower_id, following_id) VALUES (:followerId, :followingId)")
                .setParameter("followerId", otherUser.getId())
                .setParameter("followingId", testUser.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // Refresh entities after native query
        testUser = userRepository.findById(testUser.getId()).orElseThrow();
        otherUser = userRepository.findById(otherUser.getId()).orElseThrow();
    }

    private VideoGame makeGame(String title, String coverUrl) {
        VideoGame g = new VideoGame();
        g.setTitle(title);
        g.setCoverUrl(coverUrl);
        return g;
    }

    /**
     * Seeds three plays + one VideoGame like used by the recent-activity tests.
     * {@code created_at} is overridden via native SQL because {@code @PrePersist}
     * on {@link UserGamePlay} reassigns the column on save.
     */
    private void seedRecentActivity() {
        Platform platform = platformRepository.save(new Platform("PC"));

        likedGame = videoGameRepository.save(makeGame("Hades II", "/covers/hades.png"));
        reviewedGame = videoGameRepository.save(makeGame("Outer Wilds", "/covers/outer.png"));
        replayGame = videoGameRepository.save(makeGame("Hollow Knight", "/covers/hk.png"));

        LocalDateTime now = LocalDateTime.now();

        UserGamePlay playLiked = new UserGamePlay(testUser, likedGame, platform);
        playLiked.setScore(9);
        playLiked.setIsReplay(false);
        playLiked = userGamePlayRepository.save(playLiked);
        overrideCreatedAt(playLiked.getId(), now);

        UserGamePlay playReviewed = new UserGamePlay(testUser, reviewedGame, platform);
        playReviewed.setScore(null);
        playReviewed.setIsReplay(false);
        playReviewed = userGamePlayRepository.save(playReviewed);
        overrideCreatedAt(playReviewed.getId(), now.minusHours(1));

        Review review = new Review("A masterpiece", false, testUser, reviewedGame, playReviewed);
        playReviewed.setReview(review);
        userGamePlayRepository.save(playReviewed);

        UserGamePlay playReplay = new UserGamePlay(testUser, replayGame, platform);
        playReplay.setScore(7);
        playReplay.setIsReplay(true);
        playReplay = userGamePlayRepository.save(playReplay);
        overrideCreatedAt(playReplay.getId(), now.minusHours(2));

        likeRepository.save(Like.forVideoGame(testUser, likedGame));

        entityManager.flush();
        entityManager.clear();
    }

    private void overrideCreatedAt(java.util.UUID playId, LocalDateTime ts) {
        entityManager.createNativeQuery(
                        "UPDATE user_game_plays SET created_at = :ts WHERE id = :id")
                .setParameter("ts", ts)
                .setParameter("id", playId)
                .executeUpdate();
    }

    @Nested
    @DisplayName("GET /api/users/{username}")
    class GetUserProfile {

        @Test
        @DisplayName("should return full profile for anonymous viewer")
        void shouldReturnProfileForAnonymousViewer() throws Exception {
            mockMvc.perform(get("/api/users/{username}", "gamer123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("gamer123"))
                    .andExpect(jsonPath("$.bio").value("I love video games"))
                    .andExpect(jsonPath("$.level").value(3))
                    .andExpect(jsonPath("$.xpPoint").value(2500))
                    .andExpect(jsonPath("$.xpThreshold").value(3000))
                    .andExpect(jsonPath("$.followerCount").value(1))
                    .andExpect(jsonPath("$.wishlistCount").value(1))
                    .andExpect(jsonPath("$.badges.length()").value(1))
                    .andExpect(jsonPath("$.badges[0].name").value("First Review"))
                    .andExpect(jsonPath("$.isFollowing").isEmpty())
                    .andExpect(jsonPath("$.isOwner").value(false));
        }

        @Test
        @DisplayName("should return isOwner true when viewer is profile owner")
        @WithMockUser(username = "gamer@example.com")
        void shouldReturnIsOwnerWhenViewerIsOwner() throws Exception {
            mockMvc.perform(get("/api/users/{username}", "gamer123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isOwner").value(true));
        }

        @Test
        @DisplayName("should return isFollowing when authenticated viewer follows the user")
        @WithMockUser(username = "other@example.com")
        void shouldReturnIsFollowingWhenViewerFollows() throws Exception {
            mockMvc.perform(get("/api/users/{username}", "gamer123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isFollowing").value(true))
                    .andExpect(jsonPath("$.isOwner").value(false));
        }

        @Test
        @DisplayName("should return 404 for non-existing user")
        void shouldReturn404ForNonExistingUser() throws Exception {
            mockMvc.perform(get("/api/users/{username}", "nobody"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return recentPlays sorted by createdAt desc with correct flags")
        void shouldReturnRecentPlaysWithFlags() throws Exception {
            seedRecentActivity();

            mockMvc.perform(get("/api/users/{username}", "gamer123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recentPlays.length()").value(3))
                    // Order: liked (newest), reviewed, replay (oldest).
                    .andExpect(jsonPath("$.recentPlays[0].title").value("Hades II"))
                    .andExpect(jsonPath("$.recentPlays[0].score").value(9))
                    .andExpect(jsonPath("$.recentPlays[0].hasReview").value(false))
                    .andExpect(jsonPath("$.recentPlays[0].isReplay").value(false))
                    .andExpect(jsonPath("$.recentPlays[0].isLiked").value(true))
                    .andExpect(jsonPath("$.recentPlays[1].title").value("Outer Wilds"))
                    .andExpect(jsonPath("$.recentPlays[1].score").doesNotExist())
                    .andExpect(jsonPath("$.recentPlays[1].hasReview").value(true))
                    .andExpect(jsonPath("$.recentPlays[1].isReplay").value(false))
                    .andExpect(jsonPath("$.recentPlays[1].isLiked").value(false))
                    .andExpect(jsonPath("$.recentPlays[2].title").value("Hollow Knight"))
                    .andExpect(jsonPath("$.recentPlays[2].score").value(7))
                    .andExpect(jsonPath("$.recentPlays[2].hasReview").value(false))
                    .andExpect(jsonPath("$.recentPlays[2].isReplay").value(true))
                    .andExpect(jsonPath("$.recentPlays[2].isLiked").value(false))
                    // All three games are present in the response (sanity check on batched mapping).
                    .andExpect(jsonPath("$.recentPlays[*].title",
                            containsInAnyOrder("Hades II", "Outer Wilds", "Hollow Knight")));
        }

        @Test
        @DisplayName("should hide recentPlays from non-owner when profile is private")
        void shouldMaskRecentPlaysForPrivateProfile() throws Exception {
            seedRecentActivity();
            testUser.setIsPrivate(true);
            userRepository.save(testUser);

            mockMvc.perform(get("/api/users/{username}", "gamer123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isPrivate").value(true))
                    .andExpect(jsonPath("$.recentPlays.length()").value(0));
        }

        @Test
        @DisplayName("should include recentPlays for owner viewing own private profile")
        @WithMockUser(username = "gamer@example.com")
        void shouldReturnRecentPlaysForOwnerOnPrivateProfile() throws Exception {
            seedRecentActivity();
            testUser.setIsPrivate(true);
            userRepository.save(testUser);

            mockMvc.perform(get("/api/users/{username}", "gamer123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isOwner").value(true))
                    .andExpect(jsonPath("$.recentPlays.length()").value(3));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}/reviews")
    class GetUserReviews {

        @Test
        @DisplayName("should return empty reviews for user with no reviews")
        void shouldReturnEmptyReviews() throws Exception {
            mockMvc.perform(get("/api/users/{username}/reviews", "gamer123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.metadata.totalElements").value(0));
        }

        @Test
        @DisplayName("should return 403 for private profile reviews")
        void shouldReturn403ForPrivateProfileReviews() throws Exception {
            testUser.setIsPrivate(true);
            userRepository.save(testUser);

            mockMvc.perform(get("/api/users/{username}/reviews", "gamer123"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should allow owner to access own private profile reviews")
        @WithMockUser(username = "gamer@example.com")
        void shouldAllowOwnerToAccessPrivateReviews() throws Exception {
            testUser.setIsPrivate(true);
            userRepository.save(testUser);

            mockMvc.perform(get("/api/users/{username}/reviews", "gamer123"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}/wishlist")
    class GetUserWishlist {

        @Test
        @DisplayName("should return wishlist for public profile")
        void shouldReturnWishlistForPublicProfile() throws Exception {
            mockMvc.perform(get("/api/users/{username}/wishlist", "gamer123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Elden Ring"));
        }

        @Test
        @DisplayName("should return 403 for private profile wishlist")
        void shouldReturn403ForPrivateProfileWishlist() throws Exception {
            testUser.setIsPrivate(true);
            userRepository.save(testUser);

            mockMvc.perform(get("/api/users/{username}/wishlist", "gamer123"))
                    .andExpect(status().isForbidden());
        }
    }
}
