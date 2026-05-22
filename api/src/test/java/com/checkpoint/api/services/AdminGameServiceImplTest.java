package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.dto.admin.CreateGameRequestDto;
import com.checkpoint.api.dto.admin.UpdateGameRequestDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameReferencedException;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.CompanyRepository;
import com.checkpoint.api.repositories.FavoriteRepository;
import com.checkpoint.api.repositories.GameListEntryRepository;
import com.checkpoint.api.repositories.GenreRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.impl.AdminGameServiceImpl;

/**
 * Unit tests for {@link AdminGameServiceImpl} focused on the manual CRUD flow
 * added in TE-312 (create / update / delete with integrity check).
 */
@ExtendWith(MockitoExtension.class)
class AdminGameServiceImplTest {

    @Mock private IgdbApiClient igdbApiClient;
    @Mock private GameImportService gameImportService;
    @Mock private VideoGameRepository videoGameRepository;
    @Mock private GenreRepository genreRepository;
    @Mock private PlatformRepository platformRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private UserGameRepository userGameRepository;
    @Mock private UserGamePlayRepository userGamePlayRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private BacklogRepository backlogRepository;
    @Mock private WishRepository wishRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private RateRepository rateRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private GameListEntryRepository gameListEntryRepository;

    private AdminGameServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminGameServiceImpl(
                igdbApiClient, gameImportService, videoGameRepository,
                genreRepository, platformRepository, companyRepository,
                userGameRepository, userGamePlayRepository, reviewRepository,
                backlogRepository, wishRepository, favoriteRepository,
                rateRepository, likeRepository, gameListEntryRepository
        );
    }

    private Genre genre(UUID id, String name) {
        Genre g = new Genre();
        g.setId(id);
        g.setName(name);
        return g;
    }

    private Platform platform(UUID id, String name) {
        Platform p = new Platform();
        p.setId(id);
        p.setName(name);
        return p;
    }

    private Company company(UUID id, String name) {
        Company c = new Company();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private VideoGame newGame(UUID id, String title) {
        VideoGame g = new VideoGame();
        g.setId(id);
        g.setTitle(title);
        g.setGenres(new HashSet<>());
        g.setPlatforms(new HashSet<>());
        g.setCompanies(new HashSet<>());
        g.setDlcs(new HashSet<>());
        return g;
    }

    @Nested
    @DisplayName("createGame")
    class CreateGameTests {

        @Test
        @DisplayName("Should persist a new game when title is unique and IDs resolve")
        void shouldCreateGame() {
            UUID genreId = UUID.randomUUID();
            CreateGameRequestDto req = new CreateGameRequestDto(
                    "  Hollow Knight ", "A metroidvania", "cover.jpg", null,
                    null, 25L, 18L, 60L, LocalDate.of(2017, 2, 24),
                    Set.of(genreId), Set.of(), Set.of()
            );

            when(videoGameRepository.existsByTitleIgnoreCase("Hollow Knight")).thenReturn(false);
            when(genreRepository.findAllById(req.genreIds())).thenReturn(List.of(genre(genreId, "Metroidvania")));
            when(videoGameRepository.save(any(VideoGame.class))).thenAnswer(inv -> {
                VideoGame g = inv.getArgument(0);
                g.setId(UUID.randomUUID());
                return g;
            });

            VideoGame saved = service.createGame(req);

            assertThat(saved.getTitle()).isEqualTo("Hollow Knight");
            assertThat(saved.getDescription()).isEqualTo("A metroidvania");
            assertThat(saved.getReleaseDate()).isEqualTo(LocalDate.of(2017, 2, 24));
            assertThat(saved.getGenres()).hasSize(1);
            verify(videoGameRepository).save(any(VideoGame.class));
        }

        @Test
        @DisplayName("Should reject a duplicate title (case-insensitive)")
        void shouldRejectDuplicateTitle() {
            CreateGameRequestDto req = new CreateGameRequestDto(
                    "Existing", null, null, null, null, null, null, null, null,
                    Set.of(), Set.of(), Set.of()
            );
            when(videoGameRepository.existsByTitleIgnoreCase("Existing")).thenReturn(true);

            assertThatThrownBy(() -> service.createGame(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(videoGameRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject unknown genre IDs")
        void shouldRejectUnknownGenreIds() {
            UUID a = UUID.randomUUID();
            UUID b = UUID.randomUUID();
            CreateGameRequestDto req = new CreateGameRequestDto(
                    "Brand New", null, null, null, null, null, null, null, null,
                    Set.of(a, b), Set.of(), Set.of()
            );
            when(videoGameRepository.existsByTitleIgnoreCase("Brand New")).thenReturn(false);
            when(genreRepository.findAllById(anyCollection())).thenReturn(List.of(genre(a, "Action")));

            assertThatThrownBy(() -> service.createGame(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("genre");

            verify(videoGameRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateGame")
    class UpdateGameTests {

        @Test
        @DisplayName("Should replace scalar fields and reconcile M2M sets")
        void shouldUpdateGame() {
            UUID id = UUID.randomUUID();
            UUID platformId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            VideoGame existing = newGame(id, "Old Title");
            existing.setIgdbId(123L);

            UpdateGameRequestDto req = new UpdateGameRequestDto(
                    "New Title", "desc", "cover.jpg", "art.jpg", "yt",
                    10L, 6L, 30L, LocalDate.of(2024, 1, 1),
                    Set.of(), Set.of(platformId), Set.of(companyId)
            );

            when(videoGameRepository.findByIdWithRelationships(id)).thenReturn(Optional.of(existing));
            when(videoGameRepository.existsByTitleIgnoreCaseAndIdNot("New Title", id)).thenReturn(false);
            when(platformRepository.findAllById(req.platformIds())).thenReturn(List.of(platform(platformId, "PC")));
            when(companyRepository.findAllById(req.companyIds())).thenReturn(List.of(company(companyId, "Acme")));
            when(videoGameRepository.save(any(VideoGame.class))).thenAnswer(inv -> inv.getArgument(0));

            VideoGame updated = service.updateGame(id, req);

            assertThat(updated.getTitle()).isEqualTo("New Title");
            assertThat(updated.getDescription()).isEqualTo("desc");
            assertThat(updated.getIgdbId()).isEqualTo(123L); // preserved
            assertThat(updated.getPlatforms()).hasSize(1);
            assertThat(updated.getCompanies()).hasSize(1);
            assertThat(updated.getGenres()).isEmpty();
        }

        @Test
        @DisplayName("Should throw GameNotFoundException when the game does not exist")
        void shouldThrowWhenMissing() {
            UUID id = UUID.randomUUID();
            UpdateGameRequestDto req = new UpdateGameRequestDto(
                    "Anything", null, null, null, null, null, null, null, null,
                    Set.of(), Set.of(), Set.of()
            );
            when(videoGameRepository.findByIdWithRelationships(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateGame(id, req))
                    .isInstanceOf(GameNotFoundException.class);
        }

        @Test
        @DisplayName("Should reject when the new title is already used by another game")
        void shouldRejectDuplicateTitleOnUpdate() {
            UUID id = UUID.randomUUID();
            VideoGame existing = newGame(id, "Old");
            UpdateGameRequestDto req = new UpdateGameRequestDto(
                    "Taken", null, null, null, null, null, null, null, null,
                    Set.of(), Set.of(), Set.of()
            );

            when(videoGameRepository.findByIdWithRelationships(id)).thenReturn(Optional.of(existing));
            when(videoGameRepository.existsByTitleIgnoreCaseAndIdNot("Taken", id)).thenReturn(true);

            assertThatThrownBy(() -> service.updateGame(id, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("title");

            verify(videoGameRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteGame")
    class DeleteGameTests {

        @Test
        @DisplayName("Should delete the game when no references exist")
        void shouldDeleteGameWhenNoReferences() {
            UUID id = UUID.randomUUID();
            VideoGame game = newGame(id, "Orphan");
            when(videoGameRepository.findById(id)).thenReturn(Optional.of(game));
            // All counts default to 0L from Mockito

            service.deleteGame(id);

            verify(videoGameRepository).delete(game);
        }

        @Test
        @DisplayName("Should throw GameNotFoundException when the game does not exist")
        void shouldThrowWhenMissing() {
            UUID id = UUID.randomUUID();
            when(videoGameRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteGame(id))
                    .isInstanceOf(GameNotFoundException.class);

            verify(videoGameRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should refuse to delete when references are present and report each blocking category")
        void shouldRefuseDeleteWhenReferenced() {
            UUID id = UUID.randomUUID();
            VideoGame game = newGame(id, "Popular");
            VideoGame dlc = newGame(UUID.randomUUID(), "DLC");
            game.getDlcs().add(dlc);

            when(videoGameRepository.findById(id)).thenReturn(Optional.of(game));
            when(userGameRepository.countByVideoGameId(id)).thenReturn(3L);
            when(reviewRepository.countByVideoGameId(id)).thenReturn(2L);

            assertThatThrownBy(() -> service.deleteGame(id))
                    .isInstanceOfSatisfying(GameReferencedException.class, ex -> {
                        assertThat(ex.getBlockingReferences())
                                .containsEntry("library", 3L)
                                .containsEntry("reviews", 2L)
                                .containsEntry("dlcs", 1L);
                    });

            verify(videoGameRepository, never()).delete(any());
        }
    }
}
