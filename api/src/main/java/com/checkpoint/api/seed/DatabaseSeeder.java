package com.checkpoint.api.seed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.entities.Backlog;
import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.entities.Comment;
import com.checkpoint.api.entities.Favorite;
import com.checkpoint.api.entities.GameList;
import com.checkpoint.api.entities.GameListEntry;
import com.checkpoint.api.entities.Like;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.Role;
import com.checkpoint.api.entities.Tag;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.entities.Wish;
import com.checkpoint.api.enums.BadgeCode;
import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.repositories.BadgeRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.RoleRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

/**
 * Populates the database with realistic fake users, social activity, and
 * collections for demo and load-testing purposes.
 *
 * <p>Activated only when the {@code seed} Spring profile is active:
 * <pre>
 *     ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
 * </pre>
 *
 * <p>The runner is fully idempotent and safe to invoke repeatedly:
 * <ol>
 *   <li>Ensures the {@code USER} and {@code ADMIN} roles exist.</li>
 *   <li>Bootstraps a default admin account ({@code admin / Password1!}) when no
 *       admin user exists yet — so a fresh database can be brought to a usable
 *       state without manual SQL.</li>
 *   <li>Aborts gracefully if no games are imported yet, with a message pointing
 *       to the bulk-import workflow (TE-241).</li>
 *   <li>Aborts if non-admin users already exist (treated as "data already seeded").</li>
 *   <li>Otherwise generates ~50 fake users plus the full social graph: play
 *       logs, reviews, ratings, comments, likes, follows, themed lists, tags,
 *       and the personal collections (backlog/wish/favorite).</li>
 * </ol>
 *
 * <p>The single {@link Transactional} on {@link #run(ApplicationArguments)}
 * means a failure mid-run rolls everything back, leaving the database
 * untouched for a clean retry.
 */
@Component
@Profile("seed")
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private static final long RANDOM_SEED = 42L;
    private static final int USER_COUNT = 50;
    private static final int LIST_COUNT_MIN = 10;
    private static final int LIST_COUNT_MAX = 15;
    private static final int FAVORITES_HARD_CAP = 5;

    private final RoleRepository roleRepository;
    private final VideoGameRepository videoGameRepository;
    private final PlatformRepository platformRepository;
    private final BadgeRepository badgeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public DatabaseSeeder(RoleRepository roleRepository,
                          VideoGameRepository videoGameRepository,
                          PlatformRepository platformRepository,
                          BadgeRepository badgeRepository,
                          PasswordEncoder passwordEncoder,
                          EntityManager entityManager) {
        this.roleRepository = roleRepository;
        this.videoGameRepository = videoGameRepository;
        this.platformRepository = platformRepository;
        this.badgeRepository = badgeRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long startTime = System.currentTimeMillis();
        log.info("=== CheckPoint database seeder ===");

        SeedRandom random = new SeedRandom(RANDOM_SEED);

        Role userRole = ensureRole("USER");
        Role adminRole = ensureRole("ADMIN");

        ensureAdminUser(adminRole);
        seedBadges();

        List<VideoGame> games = videoGameRepository.findAll();
        if (games.isEmpty()) {
            log.warn("No video games imported. Run TE-241 bulk import (or import manually) and rerun the seeder.");
            return;
        }

        Long nonAdminCount = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.role.name <> 'ADMIN'", Long.class)
                .getSingleResult();
        if (nonAdminCount > 0) {
            log.info("Seed data already present ({} non-admin users found), skipping.", nonAdminCount);
            return;
        }

        List<Platform> platforms = platformRepository.findAll();
        if (platforms.isEmpty()) {
            log.warn("No platforms in DB — game imports must populate platforms before the seeder can attach play logs. Skipping.");
            return;
        }

        log.info("Found {} games and {} platforms. Generating fake data...", games.size(), platforms.size());

        List<User> users = createUsers(userRole, random);
        entityManager.flush();
        log.info("Created {} users", users.size());

        createFollows(users, random);
        entityManager.flush();
        log.info("Wired follow graph");

        Counter content = createPerUserContent(users, games, platforms, random);
        entityManager.flush();
        log.info("Per-user content: {} backlogs, {} wishes, {} favorites, {} userGames, {} playLogs, {} reviews, {} rates, {} tags",
                content.backlogs, content.wishes, content.favorites, content.userGames,
                content.playLogs, content.reviews, content.rates, content.tags);

        List<GameList> gameLists = createGameLists(users, games, random);
        entityManager.flush();
        log.info("Created {} game lists", gameLists.size());

        int comments = createComments(users, gameLists, random);
        entityManager.flush();
        log.info("Created {} comments", comments);

        int likes = createLikes(users, games, gameLists, random);
        entityManager.flush();
        log.info("Created {} likes", likes);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("=== Seeder completed in {} ms ===", elapsed);
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    /**
     * Inserts a {@code badges} row for every {@link BadgeCode} that does not yet
     * exist. Idempotent: re-running the seeder does not duplicate rows.
     */
    private void seedBadges() {
        int created = 0;
        for (BadgeCode code : BadgeCode.values()) {
            if (badgeRepository.findByCode(code.name()).isEmpty()) {
                badgeRepository.save(new Badge(
                        code.name(),
                        code.getDefaultName(),
                        code.getDefaultDescription(),
                        null));
                created++;
            }
        }
        log.info("Badge catalog: {} new badge(s) inserted ({} total in catalog).",
                created, BadgeCode.values().length);
    }

    /**
     * Creates a default admin user when no admin exists yet so the desktop app
     * can sign in to import games on a fresh database.
     */
    private void ensureAdminUser(Role adminRole) {
        Long adminCount = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.role.name = 'ADMIN'", Long.class)
                .getSingleResult();
        if (adminCount > 0) {
            log.info("Admin user(s) already exist (count={}), skipping admin bootstrap.", adminCount);
            return;
        }
        User admin = new User("admin", "admin@checkpoint.local",
                passwordEncoder.encode(SeedContent.SHARED_PASSWORD));
        admin.setRole(adminRole);
        admin.setBio("Default admin account created by the seeder.");
        entityManager.persist(admin);
        entityManager.flush();
        log.info("Created default admin user: pseudo='admin', email='admin@checkpoint.local', password='{}'.",
                SeedContent.SHARED_PASSWORD);
        log.info("→ Login as admin, import games (TE-241 bulk import or manual), then rerun the seeder for fake user data.");
    }

    private List<User> createUsers(Role userRole, SeedRandom random) {
        String sharedHash = passwordEncoder.encode(SeedContent.SHARED_PASSWORD);
        List<User> users = new ArrayList<>(USER_COUNT);
        Set<String> takenPseudos = new HashSet<>();
        Set<String> takenEmails = new HashSet<>();

        int idx = 0;
        while (users.size() < USER_COUNT) {
            String basePseudo = SeedContent.PSEUDOS.get(idx % SeedContent.PSEUDOS.size());
            String pseudo = idx < SeedContent.PSEUDOS.size()
                    ? basePseudo
                    : basePseudo + (idx / SeedContent.PSEUDOS.size() + 1);
            idx++;
            if (!takenPseudos.add(pseudo)) {
                continue;
            }
            String email = pseudo.toLowerCase() + "@checkpoint.local";
            if (!takenEmails.add(email)) {
                continue;
            }
            User user = new User(pseudo, email, sharedHash);
            user.setRole(userRole);
            user.setBio(random.pick(SeedContent.BIO_TEMPLATES));
            user.setPicture(SeedContent.unsplashUrl(random.pick(SeedContent.UNSPLASH_PHOTO_IDS)));
            user.setIsPrivate(random.chance(0.15));
            int xp = random.intBetween(0, 5000);
            user.setXpPoint(xp);
            user.setLevel(1 + xp / 500);
            entityManager.persist(user);
            users.add(user);
        }
        return users;
    }

    private void createFollows(List<User> users, SeedRandom random) {
        List<User> popular = random.pickN(users, 5);
        for (User user : users) {
            int targetCount = random.intBetween(0, 8);
            Set<UUID> alreadyFollowing = new HashSet<>();
            alreadyFollowing.add(user.getId());
            int attempts = 0;
            while (alreadyFollowing.size() - 1 < targetCount && attempts < targetCount * 4) {
                User other = random.chance(0.4) ? random.pick(popular) : random.pick(users);
                if (alreadyFollowing.add(other.getId())) {
                    user.follow(other);
                }
                attempts++;
            }
        }
    }

    private Counter createPerUserContent(List<User> users,
                                         List<VideoGame> games,
                                         List<Platform> platforms,
                                         SeedRandom random) {
        Counter c = new Counter();
        List<PlayStatus> playStatuses = List.of(
                PlayStatus.ARE_PLAYING, PlayStatus.PLAYED, PlayStatus.COMPLETED,
                PlayStatus.RETIRED, PlayStatus.SHELVED, PlayStatus.ABANDONED);
        double[] playStatusWeights = {0.30, 0.25, 0.20, 0.05, 0.05, 0.15};

        for (User user : users) {
            generateBacklog(user, games, random, c);
            generateWishes(user, games, random, c);
            generateFavorites(user, games, random, c);
            List<UserGamePlay> playLogs = generatePlayLogs(user, games, platforms, playStatuses, playStatusWeights, random, c);
            generateReviews(user, playLogs, random, c);
            generateRates(user, playLogs, random, c);
            generateAndAttachTags(user, playLogs, random, c);
        }
        return c;
    }

    private void generateBacklog(User user, List<VideoGame> games, SeedRandom random, Counter c) {
        for (VideoGame g : random.pickN(games, random.intBetween(1, 6))) {
            entityManager.persist(new Backlog(user, g));
            c.backlogs++;
        }
    }

    private void generateWishes(User user, List<VideoGame> games, SeedRandom random, Counter c) {
        for (VideoGame g : random.pickN(games, random.intBetween(0, 5))) {
            entityManager.persist(new Wish(user, g));
            c.wishes++;
        }
    }

    private void generateFavorites(User user, List<VideoGame> games, SeedRandom random, Counter c) {
        int count = Math.min(FAVORITES_HARD_CAP, random.intBetween(0, FAVORITES_HARD_CAP));
        List<VideoGame> picks = random.pickN(games, count);
        for (int i = 0; i < picks.size(); i++) {
            entityManager.persist(new Favorite(user, picks.get(i), i));
            c.favorites++;
        }
    }

    private List<UserGamePlay> generatePlayLogs(User user,
                                                List<VideoGame> games,
                                                List<Platform> platforms,
                                                List<PlayStatus> playStatuses,
                                                double[] playStatusWeights,
                                                SeedRandom random,
                                                Counter c) {
        int count = random.intBetween(3, 10);
        List<VideoGame> playedGames = random.pickN(games, count);
        List<UserGamePlay> playLogs = new ArrayList<>(playedGames.size());

        for (VideoGame game : playedGames) {
            PlayStatus status = random.weightedPick(playStatuses, playStatusWeights);
            Platform platform = random.pick(platforms);
            UserGamePlay play = new UserGamePlay(user, game, platform, status);

            boolean isFinishedRun = status == PlayStatus.COMPLETED
                    || status == PlayStatus.PLAYED
                    || status == PlayStatus.RETIRED;
            if (isFinishedRun || random.chance(0.3)) {
                play.setScore(random.intBetween(1, 10));
            }

            play.setTimePlayed(random.intBetween(60, 7200));

            int daysAgoStart = random.intBetween(30, 730);
            LocalDate startDate = LocalDate.now().minusDays(daysAgoStart);
            play.setStartDate(startDate);
            if (status != PlayStatus.ARE_PLAYING) {
                int span = random.intBetween(1, Math.max(1, daysAgoStart - 1));
                play.setEndDate(startDate.plusDays(Math.min(span, 365)));
            }

            entityManager.persist(play);
            playLogs.add(play);
            c.playLogs++;

            UserGame userGame = new UserGame(user, game, mapPlayToGameStatus(status));
            entityManager.persist(userGame);
            c.userGames++;
        }
        return playLogs;
    }

    private void generateReviews(User user, List<UserGamePlay> playLogs, SeedRandom random, Counter c) {
        for (UserGamePlay play : playLogs) {
            if (!random.chance(0.35)) {
                continue;
            }
            String content = random.pick(SeedContent.REVIEW_SNIPPETS);
            boolean spoilers = random.chance(0.10);
            Review review = new Review(content, spoilers, user, play.getVideoGame(), play);
            entityManager.persist(review);
            c.reviews++;
        }
    }

    private void generateRates(User user, List<UserGamePlay> playLogs, SeedRandom random, Counter c) {
        Set<UUID> ratedGameIds = new HashSet<>();
        for (UserGamePlay play : playLogs) {
            if (!ratedGameIds.add(play.getVideoGame().getId())) {
                continue;
            }
            if (!random.chance(0.80)) {
                continue;
            }
            entityManager.persist(new Rate(user, play.getVideoGame(), random.intBetween(1, 10)));
            c.rates++;
        }
    }

    private void generateAndAttachTags(User user, List<UserGamePlay> playLogs, SeedRandom random, Counter c) {
        int tagCount = random.intBetween(3, 6);
        List<String> names = random.pickN(SeedContent.TAG_NAMES, tagCount);
        List<Tag> userTags = new ArrayList<>(names.size());
        for (String name : names) {
            Tag tag = new Tag(name, user);
            entityManager.persist(tag);
            userTags.add(tag);
            c.tags++;
        }
        if (userTags.isEmpty()) {
            return;
        }
        for (UserGamePlay play : playLogs) {
            if (!random.chance(0.40)) {
                continue;
            }
            int attach = random.intBetween(0, Math.min(3, userTags.size()));
            for (Tag t : random.pickN(userTags, attach)) {
                play.addTag(t);
            }
        }
    }

    private List<GameList> createGameLists(List<User> users, List<VideoGame> games, SeedRandom random) {
        int listCount = random.intBetween(LIST_COUNT_MIN, LIST_COUNT_MAX);
        List<GameList> lists = new ArrayList<>(listCount);
        Set<String> usedTitles = new HashSet<>();

        for (int i = 0; i < listCount; i++) {
            String baseTitle = SeedContent.LIST_TITLES.get(i % SeedContent.LIST_TITLES.size());
            String title = usedTitles.add(baseTitle) ? baseTitle : baseTitle + " — vol. " + (i + 1);
            usedTitles.add(title);

            User author = random.pick(users);
            GameList list = new GameList(title, author);
            if (random.chance(0.7)) {
                list.setDescription(random.pick(SeedContent.LIST_DESCRIPTIONS));
            }
            list.setIsPrivate(random.chance(0.10));
            entityManager.persist(list);
            lists.add(list);

            int entryCount = random.intBetween(5, 15);
            int position = 0;
            for (VideoGame g : random.pickN(games, entryCount)) {
                entityManager.persist(new GameListEntry(list, g, position++));
            }
        }
        return lists;
    }

    private int createComments(List<User> users, List<GameList> gameLists, SeedRandom random) {
        int total = 0;
        List<Review> reviews = entityManager
                .createQuery("SELECT r FROM Review r", Review.class)
                .getResultList();

        for (Review review : reviews) {
            if (!random.chance(0.7)) {
                continue;
            }
            int rootCount = random.intBetween(1, 4);
            for (int i = 0; i < rootCount; i++) {
                Comment root = Comment.onReview(random.pick(SeedContent.COMMENT_SNIPPETS),
                        random.pick(users), review);
                entityManager.persist(root);
                total++;
                total += addReplies(root, users, random);
            }
        }

        for (GameList list : gameLists) {
            if (!random.chance(0.6)) {
                continue;
            }
            int rootCount = random.intBetween(1, 3);
            for (int i = 0; i < rootCount; i++) {
                Comment root = Comment.onList(random.pick(SeedContent.COMMENT_SNIPPETS),
                        random.pick(users), list);
                entityManager.persist(root);
                total++;
                total += addReplies(root, users, random);
            }
        }
        return total;
    }

    private int addReplies(Comment root, List<User> users, SeedRandom random) {
        int replyCount = random.intBetween(0, 2);
        for (int i = 0; i < replyCount; i++) {
            Comment reply = Comment.asReply(random.pick(SeedContent.COMMENT_SNIPPETS),
                    random.pick(users), root);
            entityManager.persist(reply);
        }
        return replyCount;
    }

    private int createLikes(List<User> users, List<VideoGame> games, List<GameList> gameLists, SeedRandom random) {
        int total = 0;

        for (User user : users) {
            int count = random.intBetween(0, 15);
            Set<UUID> liked = new HashSet<>();
            for (int i = 0; i < count; i++) {
                VideoGame g = random.pick(games);
                if (liked.add(g.getId())) {
                    entityManager.persist(Like.forVideoGame(user, g));
                    total++;
                }
            }
        }

        List<Review> reviews = entityManager
                .createQuery("SELECT r FROM Review r", Review.class)
                .getResultList();
        for (Review review : reviews) {
            int likers = random.intBetween(0, 15);
            Set<UUID> seen = new HashSet<>();
            for (int i = 0; i < likers; i++) {
                User u = random.pick(users);
                if (seen.add(u.getId())) {
                    entityManager.persist(Like.forReview(u, review));
                    total++;
                }
            }
        }

        List<Comment> comments = entityManager
                .createQuery("SELECT c FROM Comment c", Comment.class)
                .getResultList();
        for (Comment comment : comments) {
            int likers = random.intBetween(0, 5);
            Set<UUID> seen = new HashSet<>();
            for (int i = 0; i < likers; i++) {
                User u = random.pick(users);
                if (seen.add(u.getId())) {
                    entityManager.persist(Like.forComment(u, comment));
                    total++;
                }
            }
        }

        for (GameList list : gameLists) {
            int likers = random.intBetween(0, 12);
            Set<UUID> seen = new HashSet<>();
            for (int i = 0; i < likers; i++) {
                User u = random.pick(users);
                if (seen.add(u.getId())) {
                    entityManager.persist(Like.forGameList(u, list));
                    total++;
                }
            }
        }

        return total;
    }

    private GameStatus mapPlayToGameStatus(PlayStatus playStatus) {
        return switch (playStatus) {
            case COMPLETED, PLAYED -> GameStatus.COMPLETED;
            case ARE_PLAYING, SHELVED -> GameStatus.PLAYING;
            case ABANDONED, RETIRED -> GameStatus.DROPPED;
        };
    }

    /**
     * Mutable counter aggregating per-entity-type insertion stats.
     * Local to the seeder; allows the orchestrator to log totals at the end.
     */
    private static final class Counter {
        int backlogs;
        int wishes;
        int favorites;
        int userGames;
        int playLogs;
        int reviews;
        int rates;
        int tags;
    }
}
