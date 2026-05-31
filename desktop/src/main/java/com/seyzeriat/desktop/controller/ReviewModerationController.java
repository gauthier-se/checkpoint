package com.seyzeriat.desktop.controller;

import java.io.IOException;
import java.util.Optional;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.dto.ReviewResult;
import com.seyzeriat.desktop.service.ReviewService;
import com.seyzeriat.desktop.service.UserService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/**
 * Controller for the review moderation view.
 *
 * <p>Handles displaying paginated reported reviews, and provides actions
 * to view reports, view the author, ban the author, or delete the review.</p>
 */
public class ReviewModerationController {

    @FXML private TableView<ReviewResult> reviewsTable;
    @FXML private TableColumn<ReviewResult, String> reportCountColumn;
    @FXML private TableColumn<ReviewResult, String> gameColumn;
    @FXML private TableColumn<ReviewResult, String> authorColumn;
    @FXML private TableColumn<ReviewResult, String> contentColumn;
    @FXML private TableColumn<ReviewResult, Void> actionColumn;

    @FXML private Label statusLabel;
    @FXML private Button prevButton;
    @FXML private Label pageLabel;
    @FXML private Button nextButton;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final ReviewService reviewService;
    private final UserService userService;
    private HelloApplication application;

    /**
     * Constructs the controller with the required services.
     *
     * @param reviewService the service handling review-related operations
     * @param userService   the service handling user-related operations
     */
    public ReviewModerationController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);

        // Configure table columns
        reportCountColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getReportCount())));
        gameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getGameTitle()));
        authorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAuthorUsername()));
        contentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getContent()));

        setupActionColumn();

        fetchReviews(currentPage);
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button reportsBtn = new Button("Signalements");
            private final Button authorBtn = new Button("Auteur");
            private final Button banBtn = new Button("Bannir");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox container = new HBox(6, reportsBtn, authorBtn, banBtn, deleteBtn);

            {
                reportsBtn.getStyleClass().add("search-button");
                authorBtn.getStyleClass().add("search-button");
                banBtn.getStyleClass().add("destructive-button");
                deleteBtn.getStyleClass().add("destructive-button");

                reportsBtn.setOnAction(event -> showReports(getTableView().getItems().get(getIndex())));
                authorBtn.setOnAction(event -> showAuthor(getTableView().getItems().get(getIndex())));
                banBtn.setOnAction(event -> confirmAndBan(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(event -> confirmAndDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item != null || getIndex() >= getTableView().getItems().size() || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    @FXML
    private void onRefresh() {
        fetchReviews(currentPage);
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            fetchReviews(currentPage - 1);
        }
    }

    @FXML
    private void onNextPage() {
        fetchReviews(currentPage + 1);
    }

    /**
     * Sets the main application instance.
     *
     * @param application the main application instance
     */
    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    private void fetchReviews(int page) {
        setLoading(true);
        statusLabel.setText("Chargement des avis signalés...");

        Task<PagedResponse<ReviewResult>> fetchTask = new Task<>() {
            @Override
            protected PagedResponse<ReviewResult> call() throws Exception {
                return reviewService.getReportedReviews(page, PAGE_SIZE);
            }
        };

        fetchTask.setOnSucceeded(event -> Platform.runLater(() -> {
            PagedResponse<ReviewResult> response = fetchTask.getValue();
            setLoading(false);

            reviewsTable.getItems().setAll(response.getContent());
            currentPage = response.getMetadata().getPage();

            // Update pagination UI
            prevButton.setDisable(currentPage == 0);
            nextButton.setDisable(currentPage >= response.getMetadata().getTotalPages() - 1);
            pageLabel.setText("Page " + (currentPage + 1) + " / " + Math.max(1, response.getMetadata().getTotalPages()));

            statusLabel.setText(response.getMetadata().getTotalElements() + " avis signalé(s).");
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

        new Thread(fetchTask, "fetch-reported-reviews-thread").start();
    }

    private void showReports(ReviewResult review) {
        if (application == null) {
            return;
        }

        try {
            FXMLLoader loader = application.createLoader("/com/seyzeriat/desktop/review-reports-view.fxml");
            Node detailView = loader.load();

            ReviewReportsController controller = loader.getController();
            controller.setApplication(application);
            controller.loadReview(review);

            application.setContent(detailView);
        } catch (IOException e) {
            statusLabel.setText("Erreur lors de l'ouverture des signalements : " + e.getMessage());
        }
    }

    private void showAuthor(ReviewResult review) {
        if (application == null) {
            return;
        }

        if (review.getAuthorId() == null) {
            statusLabel.setText("Auteur indisponible pour cet avis.");
            return;
        }

        try {
            FXMLLoader loader = application.createLoader("/com/seyzeriat/desktop/user-detail-view.fxml");
            Node detailView = loader.load();

            UserDetailController controller = loader.getController();
            controller.setApplication(application);
            controller.loadUser(review.getAuthorId());

            application.setContent(detailView);
        } catch (IOException e) {
            statusLabel.setText("Erreur lors de l'ouverture du profil : " + e.getMessage());
        }
    }

    private void confirmAndBan(ReviewResult review) {
        if (review.getAuthorId() == null) {
            statusLabel.setText("Auteur indisponible pour cet avis.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Bannir l'utilisateur " + review.getAuthorUsername() + " ?");
        alert.setContentText("L'utilisateur ne pourra plus se connecter à CheckPoint.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            banAuthor(review.getAuthorId());
        }
    }

    private void banAuthor(String authorId) {
        setLoading(true);
        statusLabel.setText("Bannissement en cours...");

        Task<Void> banTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.banUser(authorId);
                return null;
            }
        };

        banTask.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText("Utilisateur banni avec succès.");
            fetchReviews(currentPage);
        }));

        banTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = banTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(banTask, "ban-review-author-thread").start();
    }

    private void confirmAndDelete(ReviewResult review) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'avis de " + review.getAuthorUsername() + " sur " + review.getGameTitle() + " ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteReview(review.getId());
        }
    }

    private void deleteReview(String id) {
        setLoading(true);
        statusLabel.setText("Suppression en cours...");

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                reviewService.deleteReview(id);
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText("Avis supprimé avec succès.");
            fetchReviews(currentPage); // reload current page (auto-refresh)
        }));

        deleteTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = deleteTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur lors de la suppression : " + ex.getMessage());
        }));

        new Thread(deleteTask, "delete-review-thread").start();
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        refreshButton.setDisable(loading);
    }

    private void redirectToLogin() {
        if (application != null) {
            application.showLoginView();
        }
    }
}
