package com.seyzeriat.desktop.controller;

import java.io.IOException;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.dto.ReviewReportResult;
import com.seyzeriat.desktop.dto.ReviewResult;
import com.seyzeriat.desktop.service.ReviewService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller for the review reports detail view.
 *
 * <p>Displays a recap of a reported review and the paginated list of
 * reports filed against it (reporter, reason, date).</p>
 */
public class ReviewReportsController {

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private Label gameLabel;
    @FXML private Label authorLabel;
    @FXML private Label contentLabel;

    @FXML private TableView<ReviewReportResult> reportsTable;
    @FXML private TableColumn<ReviewReportResult, String> reporterColumn;
    @FXML private TableColumn<ReviewReportResult, String> reasonColumn;
    @FXML private TableColumn<ReviewReportResult, String> dateColumn;

    @FXML private Button prevButton;
    @FXML private Label pageLabel;
    @FXML private Button nextButton;
    @FXML private Button refreshButton;

    private final ReviewService reviewService;
    private HelloApplication application;

    public ReviewReportsController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }
    private ReviewResult review;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);

        reporterColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getReporterUsername()));
        reasonColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getReason()));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt()));
    }

    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    /**
     * Loads the reports for the given review.
     *
     * @param review the reviewed content whose reports should be displayed
     */
    public void loadReview(ReviewResult review) {
        this.review = review;
        displayRecap(review);
        fetchReports(0);
    }

    @FXML
    private void onBack() {
        if (application == null) {
            return;
        }

        try {
            FXMLLoader loader = application.createLoader("/com/seyzeriat/desktop/reviews-view.fxml");
            Node reviewsView = loader.load();

            ReviewModerationController controller = loader.getController();
            controller.setApplication(application);

            application.setContent(reviewsView);
        } catch (IOException e) {
            statusLabel.setText("Erreur lors du retour : " + e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        fetchReports(currentPage);
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            fetchReports(currentPage - 1);
        }
    }

    @FXML
    private void onNextPage() {
        fetchReports(currentPage + 1);
    }

    private void displayRecap(ReviewResult review) {
        titleLabel.setText("Signalements — " + (review.getGameTitle() != null ? review.getGameTitle() : ""));
        gameLabel.setText(review.getGameTitle() != null ? review.getGameTitle() : "—");
        authorLabel.setText(review.getAuthorUsername() != null ? review.getAuthorUsername() : "—");
        contentLabel.setText(review.getContent() != null ? review.getContent() : "—");
    }

    private void fetchReports(int page) {
        if (review == null) {
            return;
        }

        setLoading(true);
        statusLabel.setText("Chargement des signalements...");

        Task<PagedResponse<ReviewReportResult>> fetchTask = new Task<>() {
            @Override
            protected PagedResponse<ReviewReportResult> call() throws Exception {
                return reviewService.getReviewReports(review.getId(), page, PAGE_SIZE);
            }
        };

        fetchTask.setOnSucceeded(event -> Platform.runLater(() -> {
            PagedResponse<ReviewReportResult> response = fetchTask.getValue();
            setLoading(false);

            reportsTable.getItems().setAll(response.getContent());
            currentPage = response.getMetadata().getPage();

            prevButton.setDisable(currentPage == 0);
            nextButton.setDisable(currentPage >= response.getMetadata().getTotalPages() - 1);
            pageLabel.setText("Page " + (currentPage + 1) + " / " + Math.max(1, response.getMetadata().getTotalPages()));

            statusLabel.setText(response.getMetadata().getTotalElements() + " signalement(s).");
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

        new Thread(fetchTask, "fetch-review-reports-thread").start();
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
