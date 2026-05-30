package com.seyzeriat.desktop.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.NewsResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.service.NewsService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for the news management view. Lists all news (drafts +
 * published), supports search by title and filter by status, and opens
 * the editor dialog for create/edit operations. Publish/unpublish and
 * delete actions are exposed per row.
 *
 * <p>Search and status filtering are applied client-side on the current
 * page — the admin API does not expose server-side filters.</p>
 */
public class NewsManagementController {

    private static final String FILTER_ALL = "Tous";
    private static final String FILTER_DRAFTS = "Brouillons";
    private static final String FILTER_PUBLISHED = "Publiés";

    private static final int PAGE_SIZE = 20;
    private static final String DEFAULT_SORT = "createdAt,desc";

    @FXML private TableView<NewsResult> newsTable;
    @FXML private TableColumn<NewsResult, String> titleColumn;
    @FXML private TableColumn<NewsResult, String> statusColumn;
    @FXML private TableColumn<NewsResult, String> authorColumn;
    @FXML private TableColumn<NewsResult, String> sourceColumn;
    @FXML private TableColumn<NewsResult, String> publishedAtColumn;
    @FXML private TableColumn<NewsResult, String> updatedAtColumn;
    @FXML private TableColumn<NewsResult, Void> actionColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button newButton;

    @FXML private Label statusLabel;
    @FXML private Button prevButton;
    @FXML private Label pageLabel;
    @FXML private Button nextButton;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final NewsService newsService;
    private HelloApplication application;

    public NewsManagementController(NewsService newsService) {
        this.newsService = newsService;
    }

    private int currentPage = 0;
    private int totalPages = 1;
    private long totalElements = 0;
    private final List<NewsResult> currentPageData = new ArrayList<>();

    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);

        statusFilter.setItems(FXCollections.observableArrayList(FILTER_ALL, FILTER_DRAFTS, FILTER_PUBLISHED));
        statusFilter.setValue(FILTER_ALL);
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyClientFilters());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyClientFilters());

        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle()));
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isPublished() ? "Publié" : "Brouillon"));
        authorColumn.setCellValueFactory(cellData -> {
            NewsResult news = cellData.getValue();
            String author = news.getAuthor() != null ? news.getAuthor().getPseudo() : "";
            return new SimpleStringProperty(author == null ? "" : author);
        });
        sourceColumn.setCellValueFactory(cellData -> {
            NewsResult news = cellData.getValue();
            String label = news.getSource() == null ? "MANUAL" : news.getSource();
            if (!"MANUAL".equals(label) && news.getFeedName() != null && !news.getFeedName().isBlank()) {
                label = news.getFeedName();
            }
            return new SimpleStringProperty(label);
        });
        publishedAtColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatDateTime(cellData.getValue().getPublishedAt())));
        updatedAtColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatDateTime(cellData.getValue().getUpdatedAt())));

        setupActionColumn();

        fetchNews(currentPage);
    }

    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button toggleBtn = new Button();
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox container = new HBox(6, editBtn, toggleBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("search-button");
                deleteBtn.getStyleClass().add("destructive-button");

                editBtn.setOnAction(event -> openEditor(getTableView().getItems().get(getIndex())));
                toggleBtn.setOnAction(event -> {
                    NewsResult news = getTableView().getItems().get(getIndex());
                    if (news.isPublished()) {
                        confirmAndUnpublish(news);
                    } else {
                        confirmAndPublish(news);
                    }
                });
                deleteBtn.setOnAction(event -> confirmAndDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()
                        || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    NewsResult news = getTableView().getItems().get(getIndex());
                    if (news.isPublished()) {
                        toggleBtn.setText("Dépublier");
                        toggleBtn.getStyleClass().removeAll("login-button");
                        toggleBtn.getStyleClass().add("search-button");
                    } else {
                        toggleBtn.setText("Publier");
                        toggleBtn.getStyleClass().removeAll("search-button");
                        toggleBtn.getStyleClass().add("login-button");
                    }
                    setGraphic(container);
                }
            }
        });
    }

    @FXML
    private void onRefresh() {
        fetchNews(currentPage);
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            fetchNews(currentPage - 1);
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages - 1) {
            fetchNews(currentPage + 1);
        }
    }

    @FXML
    private void onNew() {
        openEditor(null);
    }

    private void fetchNews(int page) {
        setLoading(true);
        statusLabel.setText("Chargement des actualités...");

        Task<PagedResponse<NewsResult>> fetchTask = new Task<>() {
            @Override
            protected PagedResponse<NewsResult> call() throws Exception {
                return newsService.getNews(page, PAGE_SIZE, DEFAULT_SORT);
            }
        };

        fetchTask.setOnSucceeded(event -> Platform.runLater(() -> {
            PagedResponse<NewsResult> response = fetchTask.getValue();
            setLoading(false);

            currentPageData.clear();
            currentPageData.addAll(response.getContent());

            currentPage = response.getMetadata().getPage();
            totalPages = Math.max(1, response.getMetadata().getTotalPages());
            totalElements = response.getMetadata().getTotalElements();

            applyClientFilters();

            prevButton.setDisable(currentPage == 0);
            nextButton.setDisable(currentPage >= totalPages - 1);
            pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        }));

        fetchTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = fetchTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(fetchTask, "fetch-news-thread").start();
    }

    private void applyClientFilters() {
        String search = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String filter = statusFilter.getValue() == null ? FILTER_ALL : statusFilter.getValue();

        List<NewsResult> filtered = new ArrayList<>();
        for (NewsResult news : currentPageData) {
            if (FILTER_DRAFTS.equals(filter) && news.isPublished()) {
                continue;
            }
            if (FILTER_PUBLISHED.equals(filter) && !news.isPublished()) {
                continue;
            }
            if (!search.isEmpty()) {
                String title = news.getTitle() == null ? "" : news.getTitle().toLowerCase(Locale.ROOT);
                if (!title.contains(search)) {
                    continue;
                }
            }
            filtered.add(news);
        }

        newsTable.getItems().setAll(filtered);

        if (currentPageData.isEmpty()) {
            statusLabel.setText("Aucune actualité.");
        } else if (filtered.size() == currentPageData.size()) {
            statusLabel.setText(totalElements + " actualité(s) au total.");
        } else {
            statusLabel.setText(filtered.size() + " sur " + currentPageData.size()
                    + " (page) — " + totalElements + " au total.");
        }
    }

    private void openEditor(NewsResult news) {
        if (application == null) {
            return;
        }
        try {
            FXMLLoader loader = application.createLoader("/com/seyzeriat/desktop/news-editor-dialog.fxml");
            Node root = loader.load();

            NewsEditorDialogController controller = loader.getController();
            controller.setNews(news);
            controller.setOnSaved(() -> fetchNews(currentPage));
            controller.setOnUnauthorized(this::redirectToLogin);

            Stage dialog = new Stage();
            dialog.setTitle(news == null ? "Nouvelle actualité" : "Modifier l'actualité");
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (newsTable.getScene() != null) {
                dialog.initOwner(newsTable.getScene().getWindow());
            }

            Scene scene = new Scene((javafx.scene.Parent) root);
            if (newsTable.getScene() != null
                    && !newsTable.getScene().getStylesheets().isEmpty()) {
                scene.getStylesheets().addAll(newsTable.getScene().getStylesheets());
            }
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (IOException e) {
            statusLabel.setText("Erreur lors de l'ouverture de l'éditeur : " + e.getMessage());
        }
    }

    private void confirmAndPublish(NewsResult news) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Publier l'actualité « " + news.getTitle() + " » ?");
        alert.setContentText("Elle sera immédiatement visible sur le site.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runNewsAction(() -> { newsService.publishNews(news.getId()); },
                    "Publication en cours...",
                    "Actualité publiée avec succès.",
                    "Erreur lors de la publication");
        }
    }

    private void confirmAndUnpublish(NewsResult news) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Dépublier l'actualité « " + news.getTitle() + " » ?");
        alert.setContentText("Elle ne sera plus visible sur le site.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runNewsAction(() -> { newsService.unpublishNews(news.getId()); },
                    "Dépublication en cours...",
                    "Actualité dépubliée avec succès.",
                    "Erreur lors de la dépublication");
        }
    }

    private void confirmAndDelete(NewsResult news) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'actualité « " + news.getTitle() + " » ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runNewsAction(() -> newsService.deleteNews(news.getId()),
                    "Suppression en cours...",
                    "Actualité supprimée avec succès.",
                    "Erreur lors de la suppression");
        }
    }

    /**
     * Generic helper running an action on a background thread, then refreshing
     * the current page on success and surfacing errors on the status label.
     */
    private void runNewsAction(NewsAction action, String runningMessage,
                               String successMessage, String errorPrefix) {
        setLoading(true);
        statusLabel.setText(runningMessage);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                action.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText(successMessage);
            fetchNews(currentPage);
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = task.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText(errorPrefix + " : " + ex.getMessage());
        }));

        new Thread(task, "news-action-thread").start();
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        refreshButton.setDisable(loading);
        newButton.setDisable(loading);
    }

    private void redirectToLogin() {
        if (application != null) {
            application.showLoginView();
        }
    }

    /**
     * Trims an ISO-8601 timestamp returned by the API to a {@code yyyy-MM-dd HH:mm}
     * representation. Returns an empty string when the input is null or unparseable.
     */
    private String formatDateTime(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        // The API returns LocalDateTime as e.g. "2026-04-30T08:11:32.306".
        // Take the date and time-of-day with minute precision: yyyy-MM-ddTHH:mm.
        int tIdx = iso.indexOf('T');
        if (tIdx < 0) {
            return iso;
        }
        String date = iso.substring(0, tIdx);
        String rest = iso.substring(tIdx + 1);
        String time = rest.length() >= 5 ? rest.substring(0, 5) : rest;
        return date + " " + time;
    }

    /**
     * Functional helper allowing API calls that throw checked exceptions
     * to be passed to {@link #runNewsAction(NewsAction, String, String, String)}.
     */
    @FunctionalInterface
    private interface NewsAction {
        void run() throws Exception;
    }
}
