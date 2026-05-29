package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.dto.export.UserDataExportDto;
import com.checkpoint.api.entities.Backlog;
import com.checkpoint.api.entities.Comment;
import com.checkpoint.api.entities.Favorite;
import com.checkpoint.api.entities.GameList;
import com.checkpoint.api.entities.GameListEntry;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.SocialLink;
import com.checkpoint.api.entities.Tag;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.entities.Wish;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.enums.Priority;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.impl.DataExportServiceImpl;

/**
 * Unit tests for {@link DataExportServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class DataExportServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private DataExportServiceImpl service;

    private User testUser;
    private VideoGame gameZelda;
    private VideoGame gameHalo;
    private Platform platformSwitch;

    @BeforeEach
    void setUp() {
        service = new DataExportServiceImpl(userRepository);

        testUser = new User("alice", "alice@test.com", "encoded-password");
        testUser.setId(UUID.randomUUID());
        testUser.setBio("loves indie games");
        testUser.setPicture("/uploads/profiles/alice.jpg");
        testUser.setIsPrivate(false);
        testUser.setLevel(7);
        testUser.setXpPoint(420);
        testUser.setCreatedAt(LocalDateTime.now().minusYears(1));

        gameZelda = newGame("Tears of the Kingdom");
        gameHalo = newGame("Halo Infinite");
        platformSwitch = newPlatform("Switch");
    }

    @Test
    @DisplayName("Throws UserNotFoundException when no user matches the email")
    void exportForUser_throwsWhenUserMissing() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exportForUser("ghost@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Aggregates every personal data category with resolved game titles")
    void exportForUser_aggregatesAllCategories() {
        testUser.setUserGames(Set.of(newUserGame(gameZelda, PlayStatus.COMPLETED)));

        UserGamePlay play = newPlay(gameZelda, platformSwitch, PlayStatus.PLAYED, 1200, 5);
        Tag funTag = newTag("fun");
        Tag chillTag = newTag("chill");
        play.setTags(new HashSet<>(Set.of(funTag, chillTag)));
        testUser.setGamePlays(Set.of(play));

        testUser.setReviews(Set.of(newReview(gameZelda, "Amazing game", false)));

        Review reviewForComment = newReview(gameHalo, "Solid sequel", false);
        reviewForComment.setId(UUID.randomUUID());
        Comment commentOnReview = new Comment("Agreed!", testUser);
        commentOnReview.setId(UUID.randomUUID());
        commentOnReview.setReview(reviewForComment);
        commentOnReview.setCreatedAt(LocalDateTime.now().minusDays(2));
        commentOnReview.setUpdatedAt(LocalDateTime.now().minusDays(2));
        testUser.setComments(Set.of(commentOnReview));

        testUser.setWishes(Set.of(newWish(gameHalo, Priority.HIGH)));
        testUser.setBacklogs(Set.of(newBacklog(gameHalo, Priority.MEDIUM)));
        testUser.setFavorites(Set.of(newFavorite(gameZelda, 1)));

        GameList list = newGameList("My GOTYs", "best of the year", false);
        GameListEntry entry = new GameListEntry(list, gameZelda, 1);
        entry.setAddedAt(LocalDateTime.now().minusDays(3));
        list.setEntries(List.of(entry));
        testUser.setGameLists(Set.of(list));

        testUser.setTags(Set.of(funTag, chillTag));
        testUser.setRates(Set.of(newRate(gameZelda, 5)));
        testUser.setSocialLinks(Set.of(newSocialLink("https://example.com/alice")));

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));

        UserDataExportDto dto = service.exportForUser("alice@test.com");

        assertThat(dto).isNotNull();
        assertThat(dto.exportedAt()).isNotNull();

        assertThat(dto.profile().username()).isEqualTo("alice");
        assertThat(dto.profile().email()).isEqualTo("alice@test.com");
        assertThat(dto.profile().bio()).isEqualTo("loves indie games");
        assertThat(dto.profile().level()).isEqualTo(7);

        assertThat(dto.library()).singleElement()
                .satisfies(entry1 -> {
                    assertThat(entry1.gameTitle()).isEqualTo("Tears of the Kingdom");
                    assertThat(entry1.status()).isEqualTo(PlayStatus.COMPLETED);
                });

        assertThat(dto.playLogs()).singleElement()
                .satisfies(p -> {
                    assertThat(p.gameTitle()).isEqualTo("Tears of the Kingdom");
                    assertThat(p.platform()).isEqualTo("Switch");
                    assertThat(p.timePlayed()).isEqualTo(1200);
                    assertThat(p.score()).isEqualTo(5);
                    assertThat(p.tags()).containsExactly("chill", "fun");
                });

        assertThat(dto.reviews()).singleElement()
                .satisfies(r -> {
                    assertThat(r.gameTitle()).isEqualTo("Tears of the Kingdom");
                    assertThat(r.content()).isEqualTo("Amazing game");
                });

        assertThat(dto.comments()).singleElement()
                .satisfies(c -> {
                    assertThat(c.content()).isEqualTo("Agreed!");
                    assertThat(c.reviewId()).isEqualTo(reviewForComment.getId());
                    assertThat(c.gameListId()).isNull();
                });

        assertThat(dto.wishlist()).singleElement()
                .satisfies(w -> {
                    assertThat(w.gameTitle()).isEqualTo("Halo Infinite");
                    assertThat(w.priority()).isEqualTo(Priority.HIGH);
                });

        assertThat(dto.backlogs()).singleElement()
                .satisfies(b -> {
                    assertThat(b.gameTitle()).isEqualTo("Halo Infinite");
                    assertThat(b.priority()).isEqualTo(Priority.MEDIUM);
                });

        assertThat(dto.favorites()).singleElement()
                .satisfies(f -> {
                    assertThat(f.gameTitle()).isEqualTo("Tears of the Kingdom");
                    assertThat(f.displayOrder()).isEqualTo(1);
                });

        assertThat(dto.gameLists()).singleElement()
                .satisfies(gl -> {
                    assertThat(gl.title()).isEqualTo("My GOTYs");
                    assertThat(gl.entries()).singleElement()
                            .satisfies(ge -> {
                                assertThat(ge.gameTitle()).isEqualTo("Tears of the Kingdom");
                                assertThat(ge.position()).isEqualTo(1);
                            });
                });

        assertThat(dto.tags()).extracting("name").containsExactly("chill", "fun");

        assertThat(dto.ratings()).singleElement()
                .satisfies(r -> {
                    assertThat(r.gameTitle()).isEqualTo("Tears of the Kingdom");
                    assertThat(r.score()).isEqualTo(5);
                });

        assertThat(dto.socialLinks()).singleElement()
                .satisfies(s -> assertThat(s.url()).isEqualTo("https://example.com/alice"));
    }

    @Test
    @DisplayName("Returns empty lists when the user has no data")
    void exportForUser_returnsEmptyListsForBlankUser() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));

        UserDataExportDto dto = service.exportForUser("alice@test.com");

        assertThat(dto.library()).isEmpty();
        assertThat(dto.playLogs()).isEmpty();
        assertThat(dto.reviews()).isEmpty();
        assertThat(dto.comments()).isEmpty();
        assertThat(dto.wishlist()).isEmpty();
        assertThat(dto.backlogs()).isEmpty();
        assertThat(dto.favorites()).isEmpty();
        assertThat(dto.gameLists()).isEmpty();
        assertThat(dto.tags()).isEmpty();
        assertThat(dto.ratings()).isEmpty();
        assertThat(dto.socialLinks()).isEmpty();
    }

    private VideoGame newGame(String title) {
        VideoGame g = new VideoGame(title, "desc", LocalDate.now());
        g.setId(UUID.randomUUID());
        return g;
    }

    private Platform newPlatform(String name) {
        Platform p = new Platform(name);
        p.setId(UUID.randomUUID());
        return p;
    }

    private UserGame newUserGame(VideoGame game, PlayStatus status) {
        UserGame ug = new UserGame(testUser, game, status);
        ug.setId(UUID.randomUUID());
        ug.setCreatedAt(LocalDateTime.now().minusDays(5));
        ug.setUpdatedAt(LocalDateTime.now().minusDays(5));
        return ug;
    }

    private UserGamePlay newPlay(VideoGame game, Platform platform, PlayStatus status,
                                 Integer timePlayed, Integer score) {
        UserGamePlay p = new UserGamePlay(testUser, game, platform, status);
        p.setId(UUID.randomUUID());
        p.setIsReplay(false);
        p.setTimePlayed(timePlayed);
        p.setScore(score);
        p.setOwnership("owned");
        p.setCreatedAt(LocalDateTime.now().minusDays(10));
        p.setUpdatedAt(LocalDateTime.now().minusDays(10));
        return p;
    }

    private Review newReview(VideoGame game, String content, boolean spoilers) {
        Review r = new Review(content, spoilers, testUser, game);
        r.setId(UUID.randomUUID());
        r.setCreatedAt(LocalDateTime.now().minusDays(7));
        r.setUpdatedAt(LocalDateTime.now().minusDays(7));
        return r;
    }

    private Wish newWish(VideoGame game, Priority priority) {
        Wish w = new Wish(testUser, game);
        w.setId(UUID.randomUUID());
        w.setPriority(priority);
        w.setCreatedAt(LocalDateTime.now().minusDays(3));
        w.setUpdatedAt(LocalDateTime.now().minusDays(3));
        return w;
    }

    private Backlog newBacklog(VideoGame game, Priority priority) {
        Backlog b = new Backlog(testUser, game);
        b.setId(UUID.randomUUID());
        b.setPriority(priority);
        b.setCreatedAt(LocalDateTime.now().minusDays(2));
        b.setUpdatedAt(LocalDateTime.now().minusDays(2));
        return b;
    }

    private Favorite newFavorite(VideoGame game, Integer order) {
        Favorite f = new Favorite(testUser, game, order);
        f.setId(UUID.randomUUID());
        f.setCreatedAt(LocalDateTime.now().minusDays(4));
        f.setUpdatedAt(LocalDateTime.now().minusDays(4));
        return f;
    }

    private GameList newGameList(String title, String description, boolean isPrivate) {
        GameList gl = new GameList(title, testUser);
        gl.setId(UUID.randomUUID());
        gl.setDescription(description);
        gl.setIsPrivate(isPrivate);
        gl.setCreatedAt(LocalDateTime.now().minusDays(6));
        gl.setUpdatedAt(LocalDateTime.now().minusDays(6));
        return gl;
    }

    private Tag newTag(String name) {
        Tag t = new Tag(name, testUser);
        t.setId(UUID.randomUUID());
        t.setCreatedAt(LocalDateTime.now().minusDays(8));
        t.setUpdatedAt(LocalDateTime.now().minusDays(8));
        return t;
    }

    private Rate newRate(VideoGame game, Integer score) {
        Rate r = new Rate(testUser, game, score);
        r.setId(UUID.randomUUID());
        r.setCreatedAt(LocalDateTime.now().minusDays(9));
        r.setUpdatedAt(LocalDateTime.now().minusDays(9));
        return r;
    }

    private SocialLink newSocialLink(String url) {
        SocialLink s = new SocialLink(url, testUser);
        s.setId(UUID.randomUUID());
        s.setCreatedAt(LocalDateTime.now().minusDays(1));
        s.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return s;
    }
}
