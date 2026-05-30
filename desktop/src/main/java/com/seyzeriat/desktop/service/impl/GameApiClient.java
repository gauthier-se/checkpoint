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

public class GameApiClient extends BaseApiClient implements GameService {

    public GameApiClient(AuthenticationService authService) {
        super(authService);
    }

    public GameApiClient(AuthenticationService authService, java.net.http.HttpClient httpClient) {
        super(authService, httpClient);
    }

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

    @Override
    public ImportJobStatus startTopRatedImport(int limit, int minRatingCount) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/games/import/top-rated?limit=" + limit + "&minRatingCount=" + minRatingCount;
        return startImportJob(url);
    }

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

    @Override
    public List<CatalogOption> getGenres() throws IOException, InterruptedException, UnauthorizedException {
        return fetchCatalogOptions("/genres");
    }

    @Override
    public List<CatalogOption> getPlatforms() throws IOException, InterruptedException, UnauthorizedException {
        return fetchCatalogOptions("/platforms");
    }

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
