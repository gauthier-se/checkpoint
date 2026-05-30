package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.admin.CreateGameRequestDto;
import com.checkpoint.api.dto.admin.ExternalGameDto;
import com.checkpoint.api.dto.admin.ImportJobStatusDto;
import com.checkpoint.api.dto.admin.UpdateGameRequestDto;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.ExternalApiUnavailableException;
import com.checkpoint.api.exceptions.ExternalGameNotFoundException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameReferencedException;
import com.checkpoint.api.exceptions.ImportAlreadyRunningException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.AdminGameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Unit tests for {@link AdminGameController}.
 */
@WebMvcTest(AdminGameController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminGameService adminGameService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    private List<ExternalGameDto> sampleExternalGames;
    private VideoGame sampleVideoGame;

    @BeforeEach
    void setUp() {
        // Sample external games for search results
        sampleExternalGames = List.of(
                new ExternalGameDto(1942L, "The Witcher 3", 2015,
                        "https://images.igdb.com/cover1.jpg"),
                new ExternalGameDto(26226L, "The Witcher 3: Hearts of Stone", 2015,
                        "https://images.igdb.com/cover2.jpg"),
                new ExternalGameDto(26227L, "The Witcher 3: Blood and Wine", 2016,
                        "https://images.igdb.com/cover3.jpg")
        );

        // Sample video game entity
        sampleVideoGame = new VideoGame();
        sampleVideoGame.setId(UUID.randomUUID());
        sampleVideoGame.setTitle("The Witcher 3: Wild Hunt");
        sampleVideoGame.setDescription("An epic RPG adventure");
        sampleVideoGame.setReleaseDate(LocalDate.of(2015, 5, 19));
        sampleVideoGame.setCoverUrl("https://images.igdb.com/cover1.jpg");
        // Initialize empty sets to avoid NPE in mapping
        sampleVideoGame.setGenres(new HashSet<>());
        sampleVideoGame.setPlatforms(new HashSet<>());
        sampleVideoGame.setCompanies(new HashSet<>());
    }

    @Nested
    @DisplayName("GET /api/admin/external-games/search")
    class SearchExternalGamesTests {

        @Test
        @DisplayName("Should return search results for valid query")
        void shouldReturnSearchResults() throws Exception {
            when(adminGameService.searchExternalGames(eq("witcher"), anyInt()))
                    .thenReturn(sampleExternalGames);

            mockMvc.perform(get("/api/admin/external-games/search")
                            .param("query", "witcher"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].externalId").value(1942))
                    .andExpect(jsonPath("$[0].title").value("The Witcher 3"))
                    .andExpect(jsonPath("$[1].externalId").value(26226))
                    .andExpect(jsonPath("$[2].externalId").value(26227));

            verify(adminGameService).searchExternalGames("witcher", 20);
        }

        @Test
        @DisplayName("Should use custom limit when provided")
        void shouldUseCustomLimit() throws Exception {
            when(adminGameService.searchExternalGames(anyString(), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/admin/external-games/search")
                            .param("query", "zelda")
                            .param("limit", "10"))
                    .andExpect(status().isOk());

            verify(adminGameService).searchExternalGames("zelda", 10);
        }

        @Test
        @DisplayName("Should cap limit at maximum (50)")
        void shouldCapLimitAtMaximum() throws Exception {
            when(adminGameService.searchExternalGames(anyString(), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/admin/external-games/search")
                            .param("query", "mario")
                            .param("limit", "100"))
                    .andExpect(status().isOk());

            verify(adminGameService).searchExternalGames("mario", 50);
        }

        @Test
        @DisplayName("Should return empty list when no results")
        void shouldReturnEmptyList() throws Exception {
            when(adminGameService.searchExternalGames(anyString(), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/admin/external-games/search")
                            .param("query", "nonexistentgame12345"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return 400 when query parameter is missing")
        void shouldReturn400WhenQueryMissing() throws Exception {
            mockMvc.perform(get("/api/admin/external-games/search"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 503 when external API is unavailable")
        void shouldReturn503WhenExternalApiUnavailable() throws Exception {
            when(adminGameService.searchExternalGames(anyString(), anyInt()))
                    .thenThrow(new ExternalApiUnavailableException("IGDB API is unavailable"));

            mockMvc.perform(get("/api/admin/external-games/search")
                            .param("query", "zelda"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("Service Unavailable"))
                    .andExpect(jsonPath("$.message").value("IGDB API is unavailable"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/games/import/{externalId}")
    class ImportGameTests {

        @Test
        @DisplayName("Should import game successfully")
        void shouldImportGameSuccessfully() throws Exception {
            when(adminGameService.importGameByExternalId(1942L))
                    .thenReturn(sampleVideoGame);

            mockMvc.perform(post("/api/admin/games/import/1942"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(sampleVideoGame.getId().toString()))
                    .andExpect(jsonPath("$.title").value("The Witcher 3: Wild Hunt"))
                    .andExpect(jsonPath("$.releaseDate").value("2015-05-19"))
                    .andExpect(jsonPath("$.coverUrl").value("https://images.igdb.com/cover1.jpg"));

            verify(adminGameService).importGameByExternalId(1942L);
        }

        @Test
        @DisplayName("Should return 404 when external game not found")
        void shouldReturn404WhenExternalGameNotFound() throws Exception {
            when(adminGameService.importGameByExternalId(99999L))
                    .thenThrow(new ExternalGameNotFoundException(99999L));

            mockMvc.perform(post("/api/admin/games/import/99999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("External game not found with ID: 99999"));
        }

        @Test
        @DisplayName("Should return 503 when external API is unavailable during import")
        void shouldReturn503WhenExternalApiUnavailableDuringImport() throws Exception {
            when(adminGameService.importGameByExternalId(1942L))
                    .thenThrow(new ExternalApiUnavailableException("IGDB API is unavailable"));

            mockMvc.perform(post("/api/admin/games/import/1942"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("Service Unavailable"));
        }
    }

    private static ImportJobStatusDto pendingJob(String type, int limit, int minRatingCount) {
        return new ImportJobStatusDto(
                UUID.randomUUID().toString(), type, "PENDING",
                limit, minRatingCount, 0, 0, 0, 0, 0,
                List.of(), null, Instant.now(), null);
    }

    @Nested
    @DisplayName("POST /api/admin/games/import/top-rated")
    class BulkImportTopRatedTests {

        @Test
        @DisplayName("Should start an async top-rated import and return 202 with the job")
        void shouldStartTopRatedImport() throws Exception {
            ImportJobStatusDto job = pendingJob("TOP_RATED", 50, 200);
            when(adminGameService.startTopRatedImport(50, 200)).thenReturn(job);

            mockMvc.perform(post("/api/admin/games/import/top-rated")
                            .param("limit", "50")
                            .param("minRatingCount", "200"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.jobId").value(job.jobId()))
                    .andExpect(jsonPath("$.type").value("TOP_RATED"))
                    .andExpect(jsonPath("$.state").value("PENDING"));

            verify(adminGameService).startTopRatedImport(50, 200);
        }

        @Test
        @DisplayName("Should use defaults when no parameters are provided")
        void shouldUseDefaults() throws Exception {
            when(adminGameService.startTopRatedImport(anyInt(), anyInt()))
                    .thenReturn(pendingJob("TOP_RATED", 100, 100));

            mockMvc.perform(post("/api/admin/games/import/top-rated"))
                    .andExpect(status().isAccepted());

            verify(adminGameService).startTopRatedImport(100, 100);
        }

        @Test
        @DisplayName("Should cap limit at 5000")
        void shouldCapLimit() throws Exception {
            when(adminGameService.startTopRatedImport(anyInt(), anyInt()))
                    .thenReturn(pendingJob("TOP_RATED", 5000, 50));

            mockMvc.perform(post("/api/admin/games/import/top-rated")
                            .param("limit", "99999")
                            .param("minRatingCount", "50"))
                    .andExpect(status().isAccepted());

            verify(adminGameService).startTopRatedImport(5000, 50);
        }

        @Test
        @DisplayName("Should return 409 when an import is already running")
        void shouldReturn409WhenAlreadyRunning() throws Exception {
            when(adminGameService.startTopRatedImport(anyInt(), anyInt()))
                    .thenThrow(new ImportAlreadyRunningException());

            mockMvc.perform(post("/api/admin/games/import/top-rated"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Conflict"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/games/import/jobs/{jobId}")
    class ImportJobStatusEndpointTests {

        @Test
        @DisplayName("Should return the job status when it exists")
        void shouldReturnJobStatus() throws Exception {
            ImportJobStatusDto job = pendingJob("TOP_RATED", 100, 100);
            UUID id = UUID.fromString(job.jobId());
            when(adminGameService.findImportJob(id)).thenReturn(Optional.of(job));

            mockMvc.perform(get("/api/admin/games/import/jobs/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobId").value(job.jobId()));
        }

        @Test
        @DisplayName("Should return 404 when the job is unknown")
        void shouldReturn404WhenUnknown() throws Exception {
            UUID id = UUID.randomUUID();
            when(adminGameService.findImportJob(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/admin/games/import/jobs/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/games")
    class CreateGameTests {

        private final ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @Test
        @DisplayName("Should create a new game and return 201")
        void shouldCreateGame() throws Exception {
            CreateGameRequestDto request = new CreateGameRequestDto(
                    "Hollow Knight", "A metroidvania", "cover.jpg", null,
                    null, 25L, 18L, 60L, LocalDate.of(2017, 2, 24),
                    java.util.Set.of(), java.util.Set.of(), java.util.Set.of()
            );

            when(adminGameService.createGame(any(CreateGameRequestDto.class)))
                    .thenReturn(sampleVideoGame);

            mockMvc.perform(post("/api/admin/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(sampleVideoGame.getId().toString()))
                    .andExpect(jsonPath("$.title").value("The Witcher 3: Wild Hunt"));

            verify(adminGameService).createGame(any(CreateGameRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturn400WhenTitleBlank() throws Exception {
            CreateGameRequestDto request = new CreateGameRequestDto(
                    "  ", null, null, null, null, null, null, null, null,
                    java.util.Set.of(), java.util.Set.of(), java.util.Set.of()
            );

            mockMvc.perform(post("/api/admin/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when service rejects duplicate title")
        void shouldReturn400OnDuplicateTitle() throws Exception {
            CreateGameRequestDto request = new CreateGameRequestDto(
                    "Existing", null, null, null, null, null, null, null, null,
                    java.util.Set.of(), java.util.Set.of(), java.util.Set.of()
            );
            when(adminGameService.createGame(any(CreateGameRequestDto.class)))
                    .thenThrow(new IllegalArgumentException("A game with this title already exists"));

            mockMvc.perform(post("/api/admin/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/games/{id}")
    class UpdateGameTests {

        private final ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @Test
        @DisplayName("Should update an existing game and return 200")
        void shouldUpdateGame() throws Exception {
            UpdateGameRequestDto request = new UpdateGameRequestDto(
                    "The Witcher 3: Wild Hunt", null, null, null, null,
                    null, null, null, null,
                    java.util.Set.of(), java.util.Set.of(), java.util.Set.of()
            );
            when(adminGameService.updateGame(eq(sampleVideoGame.getId()), any(UpdateGameRequestDto.class)))
                    .thenReturn(sampleVideoGame);

            mockMvc.perform(put("/api/admin/games/" + sampleVideoGame.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(sampleVideoGame.getId().toString()));
        }

        @Test
        @DisplayName("Should return 404 when the game does not exist")
        void shouldReturn404WhenMissing() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateGameRequestDto request = new UpdateGameRequestDto(
                    "Anything", null, null, null, null, null, null, null, null,
                    java.util.Set.of(), java.util.Set.of(), java.util.Set.of()
            );
            when(adminGameService.updateGame(eq(id), any(UpdateGameRequestDto.class)))
                    .thenThrow(new GameNotFoundException(id));

            mockMvc.perform(put("/api/admin/games/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/games/{id}")
    class DeleteGameTests {

        @Test
        @DisplayName("Should delete the game and return 204")
        void shouldDeleteGame() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/admin/games/" + id))
                    .andExpect(status().isNoContent());

            verify(adminGameService).deleteGame(id);
        }

        @Test
        @DisplayName("Should return 404 when the game does not exist")
        void shouldReturn404WhenMissing() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new GameNotFoundException(id)).when(adminGameService).deleteGame(id);

            mockMvc.perform(delete("/api/admin/games/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 409 with blockingReferences when the game is referenced")
        void shouldReturn409WhenReferenced() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new GameReferencedException(id, Map.of("library", 3L, "reviews", 2L)))
                    .when(adminGameService).deleteGame(id);

            mockMvc.perform(delete("/api/admin/games/" + id))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.blockingReferences.library").value(3))
                    .andExpect(jsonPath("$.blockingReferences.reviews").value(2));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/games/import/recent")
    class BulkImportRecentTests {

        @Test
        @DisplayName("Should start an async recent import and return 202 with the job")
        void shouldStartRecentImport() throws Exception {
            ImportJobStatusDto job = pendingJob("RECENT", 20, 0);
            when(adminGameService.startRecentImport(20)).thenReturn(job);

            mockMvc.perform(post("/api/admin/games/import/recent")
                            .param("limit", "20"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.type").value("RECENT"))
                    .andExpect(jsonPath("$.state").value("PENDING"));

            verify(adminGameService).startRecentImport(20);
        }

        @Test
        @DisplayName("Should use default limit when not provided")
        void shouldUseDefaultLimit() throws Exception {
            when(adminGameService.startRecentImport(anyInt()))
                    .thenReturn(pendingJob("RECENT", 100, 0));

            mockMvc.perform(post("/api/admin/games/import/recent"))
                    .andExpect(status().isAccepted());

            verify(adminGameService).startRecentImport(100);
        }

        @Test
        @DisplayName("Should cap limit at 500")
        void shouldCapLimit() throws Exception {
            when(adminGameService.startRecentImport(anyInt()))
                    .thenReturn(pendingJob("RECENT", 500, 0));

            mockMvc.perform(post("/api/admin/games/import/recent")
                            .param("limit", "9999"))
                    .andExpect(status().isAccepted());

            verify(adminGameService).startRecentImport(500);
        }
    }
}
