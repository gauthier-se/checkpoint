package com.seyzeriat.desktop.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seyzeriat.desktop.dto.AnalyticsResult;
import com.seyzeriat.desktop.dto.BulkImportResult;
import com.seyzeriat.desktop.dto.CatalogOption;
import com.seyzeriat.desktop.dto.ExternalGameResult;
import com.seyzeriat.desktop.dto.GameDetailResult;
import com.seyzeriat.desktop.dto.GameFormPayload;
import com.seyzeriat.desktop.dto.GameSummaryResult;
import com.seyzeriat.desktop.dto.ImportedGameResult;
import com.seyzeriat.desktop.dto.NewsRequestPayload;
import com.seyzeriat.desktop.dto.NewsResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.dto.ReportDetailResult;
import com.seyzeriat.desktop.dto.ReportResult;
import com.seyzeriat.desktop.dto.ReviewReportResult;
import com.seyzeriat.desktop.dto.ReviewResult;
import com.seyzeriat.desktop.dto.UserDetailResult;
import com.seyzeriat.desktop.dto.UserResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for communicating with the Checkpoint REST API.
 *
 * <p>Every outgoing request is intercepted to inject the JWT
 * {@code Authorization: Bearer {token}} header when a token is available.
 * If the server responds with 401 or 403, an {@link UnauthorizedException}
 * is thrown so the UI can redirect to the login screen.</p>
 */
public class ApiService {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AuthService authService;

    public ApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.authService = new AuthService();
    }

    /**
     * Search for external games by keyword.
     *
     * @param query the search keyword
     * @param limit maximum number of results
     * @return list of matching external games
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if the token is expired or invalid
     */
    public List<ExternalGameResult> searchExternalGames(String query, int limit)
            throws IOException, InterruptedException, UnauthorizedException {

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = BASE_URL + "/api/admin/external-games/search?query=" + encodedQuery + "&limit=" + limit;

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
     * Import a game by its external ID.
     *
     * @param externalId the external (IGDB) game ID
     * @return the imported game details
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if the token is expired or invalid
     */
    public ImportedGameResult importGame(Long externalId)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/games/import/" + externalId;

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
     * Bulk-imports the top-rated games from IGDB. May take several minutes
     * for large batches due to IGDB rate limiting (1 req/sec).
     *
     * @param limit          number of games to fetch (max 500)
     * @param minRatingCount minimum IGDB rating count to qualify as popular
     * @return summary of the operation
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if the token is expired or invalid
     */
    public BulkImportResult bulkImportTopRated(int limit, int minRatingCount)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/games/import/top-rated"
                + "?limit=" + limit + "&minRatingCount=" + minRatingCount;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Bulk top-rated import failed with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), BulkImportResult.class);
    }

    /**
     * Bulk-imports recently released games from IGDB. May take several
     * minutes for large batches due to IGDB rate limiting (1 req/sec).
     *
     * @param limit number of games to fetch (max 500)
     * @return summary of the operation
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if the token is expired or invalid
     */
    public BulkImportResult bulkImportRecent(int limit)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/games/import/recent?limit=" + limit;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Bulk recent import failed with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), BulkImportResult.class);
    }

    /**
     * Fetches the platform-wide analytics snapshot for the admin dashboard.
     *
     * @return the analytics payload (KPIs and top-5 lists)
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if the token is expired / invalid or the user lacks ROLE_ADMIN
     */
    public AnalyticsResult getAnalytics() throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/analytics";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch analytics with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), AnalyticsResult.class);
    }

    /**
     * Fetches all registered users from the admin API.
     * The request includes the JWT Bearer token and the backend verifies
     * the user has the {@code ROLE_ADMIN} authority before returning the list.
     *
     * @return list of users with ID, username, and email
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if the token is expired / invalid or the user lacks ROLE_ADMIN
     */
    public List<UserResult> getUsers() throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/users";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch users with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<UserResult>>() {});
    }

    /**
     * Fetches a paginated list of all user reviews from the admin API.
     *
     * @param page the page number (0-based)
     * @param size number of items per page
     * @return the paginated reviews
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public PagedResponse<ReviewResult> getReviews(int page, int size) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/reviews?page=" + page + "&size=" + size;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch reviews with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<ReviewResult>>() {});
    }

    /**
     * Fetches a paginated list of reported reviews from the admin API.
     *
     * @param page the page number (0-based)
     * @param size number of items per page
     * @return the paginated reported reviews
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public PagedResponse<ReviewResult> getReportedReviews(int page, int size) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/reviews/reported?page=" + page + "&size=" + size;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch reported reviews with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<ReviewResult>>() {});
    }

    /**
     * Fetches a paginated list of reports filed against a specific review.
     *
     * @param reviewId the review ID
     * @param page     the page number (0-based)
     * @param size     number of items per page
     * @return the paginated reports for the review
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public PagedResponse<ReviewReportResult> getReviewReports(String reviewId, int page, int size)
            throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/reviews/" + reviewId + "/reports?page=" + page + "&size=" + size;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch review reports with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<ReviewReportResult>>() {});
    }

    /**
     * Deletes a review using the admin API.
     *
     * @param id the review ID
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public void deleteReview(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/reviews/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to delete review with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Fetches a paginated list of all reports from the admin API.
     *
     * @param page the page number (0-based)
     * @param size number of items per page
     * @return the paginated reports
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public PagedResponse<ReportResult> getReports(int page, int size)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/reports?page=" + page + "&size=" + size;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch reports with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<ReportResult>>() {});
    }

    /**
     * Dismisses (deletes) a report using the admin API.
     *
     * @param id the report ID
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public void dismissReport(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/reports/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to dismiss report with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Fetches the full details of a specific report, including the target's ID and full content.
     *
     * @param id the report ID
     * @return the report details
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public ReportDetailResult getReportById(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/reports/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch report detail with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), ReportDetailResult.class);
    }

    /**
     * Deletes a comment using the admin API, bypassing the ownership check.
     *
     * @param id the comment ID
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public void deleteComment(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/comments/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to delete comment with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Fetches detailed profile information for a specific user.
     *
     * @param id the user ID
     * @return the detailed user profile
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public UserDetailResult getUserDetail(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/users/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch user detail with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), UserDetailResult.class);
    }

    /**
     * Edits a user's profile fields via the admin API.
     *
     * @param id   the user ID
     * @param body the JSON body with edit fields
     * @return the updated user detail
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public UserDetailResult editUser(String id, String body) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/users/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to edit user with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), UserDetailResult.class);
    }

    /**
     * Bans a user account via the admin API.
     *
     * @param id the user ID
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public void banUser(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/users/" + id + "/ban";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to ban user with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Unbans a user account via the admin API.
     *
     * @param id the user ID
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public void unbanUser(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/users/" + id + "/unban";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to unban user with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Fetches a paginated list of all news articles (drafts + published) from the admin API.
     *
     * @param page the page number (0-based)
     * @param size number of items per page
     * @param sort sort string in the form {@code field,direction} (e.g. {@code createdAt,desc})
     * @return the paginated news articles
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public PagedResponse<NewsResult> getNews(int page, int size, String sort)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/news?page=" + page + "&size=" + size
                + "&sort=" + URLEncoder.encode(sort, StandardCharsets.UTF_8);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<NewsResult>>() {});
    }

    /**
     * Creates a new news article (draft) via the admin API.
     *
     * @param payload the news creation request body
     * @return the created news article
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public NewsResult createNews(NewsRequestPayload payload)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/news";
        String body = objectMapper.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 201 && response.statusCode() != 200) {
            throw new IOException("Failed to create news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), NewsResult.class);
    }

    /**
     * Updates an existing news article via the admin API.
     *
     * @param id      the news article ID
     * @param payload the news update request body
     * @return the updated news article
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public NewsResult updateNews(String id, NewsRequestPayload payload)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/news/" + id;
        String body = objectMapper.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to update news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), NewsResult.class);
    }

    /**
     * Deletes a news article via the admin API.
     *
     * @param id the news article ID
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public void deleteNews(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/api/admin/news/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to delete news with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Publishes a news article via the admin API.
     *
     * @param id the news article ID
     * @return the published news article
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public NewsResult publishNews(String id)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/news/" + id + "/publish";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to publish news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), NewsResult.class);
    }

    /**
     * Unpublishes a news article via the admin API.
     *
     * @param id the news article ID
     * @return the unpublished news article
     * @throws IOException           if the request fails
     * @throws InterruptedException  if the request is interrupted
     * @throws UnauthorizedException if token is missing/expired or user lacks ROLE_ADMIN
     */
    public NewsResult unpublishNews(String id)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/news/" + id + "/unpublish";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to unpublish news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), NewsResult.class);
    }

    // ─── Manage Games (admin manual CRUD) ──────────────────────────────

    /**
     * Fetches a paginated list of games for the manage-games view.
     * Uses the public {@code /api/games} endpoint.
     */
    public PagedResponse<GameSummaryResult> getGames(int page, int size)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/games?page=" + page + "&size=" + size + "&sort=title,asc";

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
     * Fetches the full detail of a single game (used to pre-fill the edit form).
     */
    public GameDetailResult getGameDetail(String id)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/games/" + id;

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
     * Creates a game via the admin manual create endpoint.
     */
    public GameDetailResult createGame(GameFormPayload payload)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/games";
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
     * Updates an existing game via the admin endpoint.
     */
    public GameDetailResult updateGame(String id, GameFormPayload payload)
            throws IOException, InterruptedException, UnauthorizedException {

        String url = BASE_URL + "/api/admin/games/" + id;
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
     * Deletes a game via the admin endpoint. Throws
     * {@link GameReferencedException} when the API returns 409 with a
     * per-category blocking-references body.
     */
    public void deleteGame(String id)
            throws IOException, InterruptedException, UnauthorizedException, GameReferencedException {

        String url = BASE_URL + "/api/admin/games/" + id;

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
     * Returns the list of genre pickable options (id + name).
     */
    public List<CatalogOption> getGenres()
            throws IOException, InterruptedException, UnauthorizedException {
        return fetchCatalogOptions("/api/genres");
    }

    /**
     * Returns the list of platform pickable options (id + name).
     */
    public List<CatalogOption> getPlatforms()
            throws IOException, InterruptedException, UnauthorizedException {
        return fetchCatalogOptions("/api/platforms");
    }

    /**
     * Returns the list of company pickable options (id + name).
     */
    public List<CatalogOption> getCompanies()
            throws IOException, InterruptedException, UnauthorizedException {
        return fetchCatalogOptions("/api/companies");
    }

    private List<CatalogOption> fetchCatalogOptions(String path)
            throws IOException, InterruptedException, UnauthorizedException {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch " + path + " with status "
                    + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<CatalogOption>>() {});
    }

    // ─── Auth interceptor helpers ──────────────────────────────────────

    /**
     * Adds the JWT Authorization header and sends the request, retrying once after a
     * token refresh if the server returns 401. Throws {@link UnauthorizedException} on
     * 401 (after failed refresh) or 403.
     */
    private HttpResponse<String> sendWithAuth(HttpRequest.Builder builder)
            throws IOException, InterruptedException, UnauthorizedException {
        addAuthHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            try {
                authService.refreshTokens();
                addAuthHeader(builder);
                response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (AuthService.AuthenticationException e) {
                TokenManager.getInstance().clear();
                throw new UnauthorizedException("Session expirée. Veuillez vous reconnecter.");
            }
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            TokenManager.getInstance().clear();
            throw new UnauthorizedException(
                    "Session expirée ou accès refusé (HTTP " + response.statusCode() + "). Veuillez vous reconnecter.");
        }

        return response;
    }

    /**
     * Injects the JWT {@code Authorization: Bearer {token}} header into the
     * request builder if a token is available in {@link TokenManager}.
     */
    private void addAuthHeader(HttpRequest.Builder builder) {
        String token = TokenManager.getInstance().getToken();
        if (token != null && !token.isBlank()) {
            builder.setHeader("Authorization", "Bearer " + token);
        }
    }

    /**
     * Thrown when the API returns 401 or 403, indicating the JWT is expired
     * or the user lacks permissions. The UI should catch this and redirect
     * to the login screen.
     */
    public static class UnauthorizedException extends Exception {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when the admin delete endpoint returns 409 because the game is
     * still referenced by user-owned data or DLC entries. Carries the
     * per-category counts so the UI can build a clear French message.
     */
    public static class GameReferencedException extends Exception {
        private final Map<String, Long> blockingReferences;

        public GameReferencedException(Map<String, Long> blockingReferences) {
            super("Game is still referenced and cannot be deleted");
            this.blockingReferences = blockingReferences == null ? Map.of() : blockingReferences;
        }

        public Map<String, Long> getBlockingReferences() {
            return blockingReferences;
        }
    }
}
