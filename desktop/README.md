<h1 align="center">CheckPoint — Desktop Admin</h1>

<p align="center">
  <strong>JavaFX administration application for CheckPoint</strong>
</p>

---

## Purpose

The desktop module is the **administration console** for CheckPoint. It is a
[JavaFX](https://openjfx.io/) application used by administrators to moderate
content, manage the game catalog, curate news, and monitor platform analytics.
It talks to the [CheckPoint REST API](../api) over HTTP and authenticates with a
JWT obtained through the admin login flow.

## Prerequisites

- **Java 24** (the module targets `--release 24`)
- **Maven** (a wrapper is available at the repository root, or use a local `mvn`)
- A running **CheckPoint API** reachable at `http://localhost:8080` (see [`../api`](../api))
- An **admin account** to log in with

## Running

```bash
cd desktop

# Run the application
mvn clean javafx:run
```

The API base URL is currently hard-coded to `http://localhost:8080/api`
(`BaseApiClient.BASE_URL`). Make sure the API is up before logging in.

To build a runnable artifact:

```bash
mvn clean package
```

## Architecture

The application follows a layered structure with a small hand-rolled dependency
injection container.

| Layer | Location | Responsibility |
|-------|----------|----------------|
| **App shell** | `HelloApplication`, `Launcher` | Builds the main window, sidebar navigation, and loads views into the content area |
| **Controllers** | `controller/` | One JavaFX controller per FXML view; handle user interaction and bind data |
| **Services** | `service/` | Interfaces describing the operations the UI needs (auth, games, users, news, reports, reviews, analytics) |
| **API clients** | `service/impl/` | `*ApiClient` implementations that call the REST API; all extend `BaseApiClient` |
| **DTOs** | `dto/` | Records mapping API request/response payloads |
| **Exceptions** | `exception/` | Typed errors (`ApiException`, `AuthenticationException`, `UnauthorizedException`, `GameReferencedException`) |
| **DI** | `di/DependencyContainer` | Singleton wiring services to their API-client implementations and creating controllers |
| **Auth** | `service/TokenManager` | Holds the JWT for the authenticated session and injects it into API calls |

Views are defined as **FXML** files under
`src/main/resources/com/seyzeriat/desktop/`, with shared styling in
`styles.css`. After login, the shell renders a **sidebar** whose buttons swap
the active view in the central content area.

## Views

| View (FXML) | Controller | Manages |
|-------------|------------|---------|
| `login-view` | `LoginController` | Admin authentication (email/password, 2FA) |
| `users-view` | `UserManagementController` | User list/search, ban & unban |
| `user-detail-view` | `UserDetailController` | A single user's details and history |
| `manage-games-view` | `ManageGamesController` | Game catalog browsing and CRUD entry points |
| `game-form-view` | `GameFormController` | Create / edit a game manually |
| `import-games-view` | `ImportGamesController` | Search and import external games (IGDB) |
| `bulk-import-view` | `BulkImportController` | Bulk import jobs (top-rated / recent) |
| `reviews-view` | `ReviewModerationController` | Review moderation |
| `review-reports-view` | `ReviewReportsController` | Reports filed against reviews |
| `reports-view` | `ReportModerationController` | Reports moderation queue |
| `news-management-view` | `NewsManagementController` | News article management |
| `news-editor-dialog` | `NewsEditorDialogController` | Create / edit a news article (dialog) |
| `analytics-view` | `AnalyticsController` | Analytics dashboard with charts |

## Related documentation

- [Root README](../README.md) — project overview and full setup
- [Contributing guide](../CONTRIBUTING.md) — conventions and workflow
- [API HTTP files](../doc/http/) — ready-to-run requests for the admin endpoints this app consumes
