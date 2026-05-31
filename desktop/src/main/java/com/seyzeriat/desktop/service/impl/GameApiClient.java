package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.seyzeriat.desktop.dto.CatalogOption;
import com.seyzeriat.desktop.dto.ExternalGameResult;
import com.seyzeriat.desktop.dto.GameDetailResult;
import com.seyzeriat.desktop.dto.GameFormPayload;
import com.seyzeriat.desktop.dto.GameSummaryResult;
import com.seyzeriat.desktop.dto.ImportJobStatus;
import com.seyzeriat.desktop.dto.ImportedGameResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.GameReferencedException;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.GameService;

/**
 * API client implementation for {@link GameService}.
 * Handles HTTP communication with the backend to search, import, and manage games.
 */
public class GameApiClient extends BaseApiClient implements GameService {

    /**
     * Constructs a new GameApiClient with the specified authentication service.
     *
     * @param authService the authentication service to use for securing requests
     */
    public GameApiClient(AuthenticationService authService) {
        super(authService);
    }

    /**
     * Constructs a new GameApiClient with the specified authentication service and HTTP client.
     *
     * @param authService the authentication service
     * @param httpClient  the HTTP client to use for requests
     */
    public GameApiClient(AuthenticationService authService, java.net.http.HttpClient httpClient) {
        super(authService, httpClient);
    }

    /**
     * Searches for games in an external system.
     *
     * @param query the search query
     * @param limit the maximum number of results to return
     * @return a list of external game results
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public List<ExternalGameResult> searchExternalGames(String query, int limit) throws IOException, InterruptedException, UnauthorizedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = BASE_URL + "/admin/external-games/search?query=" + encodedQuery + "&limit=" + limit;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Search failed with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<ExternalGameResult>>() {});
    }

    /**
     * Imports a game from an external system.
     *
     * @param externalId the external identifier of the game to import
     * @return the result of the imported game
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public ImportedGameResult importGame(Long externalId) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/games/import/" + externalId;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 201 && response.statusCode() != 200) {
            throw new IOException("Import failed with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), ImportedGameResult.class);
    }

    /**
     * Starts a job to import top-rated games.
     *
     * @param limit the maximum number of games to import
     * @param minRatingCount the minimum rating count for the games to be imported
     * @return the status of the import job
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public ImportJobStatus startTopRatedImport(int limit, int minRatingCount) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/games/import/top-rated?limit=" + limit + "&minRatingCount=" + minRatingCount;
        return startImportJob(url);
    }

    /**
     * Starts a job to import recent games.
     *
     * @param limit the maximum number of recent games to import
     * @return the status of the import job
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public ImportJobStatus startRecentImport(int limit) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/games/import/recent?limit=" + limit;
        return startImportJob(url);
    }

    private ImportJobStatus startImportJob(String url) throws IOException, InterruptedException, UnauthorizedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() == 409) {
            throw new IOException("Un import est déjà en cours. Attendez qu'il se termine avant d'en lancer un autre.");
        }
        if (response.statusCode() != 202 && response.statusCode() != 200) {
            throw new IOException("Import start failed with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), ImportJobStatus.class);
    }

    /**
     * Retrieves the status of a specific import job.
     *
     * @param jobId the unique identifier of the import job
     * @return the current status of the job
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public ImportJobStatus getImportJob(String jobId) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/games/import/jobs/" + jobId;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch import job with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), ImportJobStatus.class);
    }

    /**
     * Retrieves a paginated list of games.
     *
     * @param page the page number to retrieve
     * @param size the maximum number of items per page
     * @return a paged response containing game summaries
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public PagedResponse<GameSummaryResult> getGames(int page, int size) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/games?page=" + page + "&size=" + size + "&sort=title,asc";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch games with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<GameSummaryResult>>() {});
    }

    /**
     * Retrieves detailed information about a specific game.
     *
     * @param id the unique identifier of the game
     * @return the detailed result for the specified game
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public GameDetailResult getGameDetail(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/games/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch game detail with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), GameDetailResult.class);
    }

    /**
     * Creates a new game.
     *
     * @param payload the payload containing the details for the new game
     * @return the detailed result for the newly created game
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public GameDetailResult createGame(GameFormPayload payload) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/games";
        String body = objectMapper.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 201 && response.statusCode() != 200) {
            throw new IOException("Failed to create game with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), GameDetailResult.class);
    }

    /**
     * Updates an existing game.
     *
     * @param id the unique identifier of the game to update
     * @param payload the payload containing the updated details
     * @return the detailed result for the updated game
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public GameDetailResult updateGame(String id, GameFormPayload payload) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/games/" + id;
        String body = objectMapper.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to update game with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), GameDetailResult.class);
    }

    /**
     * Deletes a specific game.
     *
     * @param id the unique identifier of the game to delete
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     * @throws GameReferencedException if the game cannot be deleted because it is referenced elsewhere
     */
    @Override
    public void deleteGame(String id) throws IOException, InterruptedException, UnauthorizedException, GameReferencedException {
        String url = BASE_URL + "/admin/games/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() == 409) {
            Map<String, Long> blocking = new LinkedHashMap<>();
            try {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode node = root.path("blockingReferences");
                if (node.isObject()) {
                    node.fieldNames().forEachRemaining(name -> blocking.put(name, node.path(name).asLong()));
                }
            } catch (IOException parseFailure) {
                // best-effort — if the body isn't JSON we still throw with empty map
            }
            throw new GameReferencedException(blocking);
        }

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to delete game with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Retrieves a list of available genres.
     *
     * @return a list of genre options
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public List<CatalogOption> getGenres() throws IOException, InterruptedException, UnauthorizedException {
        return fetchCatalogOptions("/genres");
    }

    /**
     * Retrieves a list of available platforms.
     *
     * @return a list of platform options
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public List<CatalogOption> getPlatforms() throws IOException, InterruptedException, UnauthorizedException {
        return fetchCatalogOptions("/platforms");
    }

    /**
     * Retrieves a list of available companies.
     *
     * @return a list of company options
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public List<CatalogOption> getCompanies() throws IOException, InterruptedException, UnauthorizedException {
        return fetchCatalogOptions("/companies");
    }

    private List<CatalogOption> fetchCatalogOptions(String path) throws IOException, InterruptedException, UnauthorizedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch " + path + " with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<CatalogOption>>() {});
    }
}
