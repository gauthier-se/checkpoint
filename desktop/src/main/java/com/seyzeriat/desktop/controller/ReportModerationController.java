package com.seyzeriat.desktop.controller;

import java.util.Optional;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.dto.ReportDetailResult;
import com.seyzeriat.desktop.dto.ReportResult;
import com.seyzeriat.desktop.service.ReportService;
import com.seyzeriat.desktop.service.ReviewService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
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
 * Controller for the report moderation view.
 *
 * <p>Handles displaying paginated generic reports (reviews, comments, etc.)
 * and provides actions to dismiss reports or delete the reported content.</p>
 */
public class ReportModerationController {

    @FXML private TableView<ReportResult> reportsTable;
    @FXML private TableColumn<ReportResult, String> dateColumn;
    @FXML private TableColumn<ReportResult, String> typeColumn;
    @FXML private TableColumn<ReportResult, String> reporterColumn;
    @FXML private TableColumn<ReportResult, String> reasonColumn;
    @FXML private TableColumn<ReportResult, String> contentColumn;
    @FXML private TableColumn<ReportResult, Void> actionColumn;

    @FXML private Label statusLabel;
    @FXML private Button prevButton;
    @FXML private Label pageLabel;
    @FXML private Button nextButton;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final ReportService reportService;
    private final ReviewService reviewService;
    private HelloApplication application;

    /**
     * Constructs the controller with the required services.
     *
     * @param reportService the service handling generic reports
     * @param reviewService the service handling reviews and comments deletion
     */
    public ReportModerationController(ReportService reportService, ReviewService reviewService) {
        this.reportService = reportService;
        this.reviewService = reviewService;
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
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt()));
        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getType()));
        reporterColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getReporterUsername()));
        reasonColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getReason()));
        contentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getContentPreview()));

        setupActionColumn();

        fetchReports(currentPage);
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button dismissBtn = new Button("Ignorer");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox buttons = new HBox(10, dismissBtn, deleteBtn);

            {
                dismissBtn.getStyleClass().add("search-button");
                deleteBtn.getStyleClass().add("destructive-button");

                dismissBtn.setOnAction(event -> {
                    ReportResult report = getTableView().getItems().get(getIndex());
                    confirmAndDismiss(report);
                });

                deleteBtn.setOnAction(event -> {
                    ReportResult report = getTableView().getItems().get(getIndex());
                    confirmAndDeleteContent(report);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item != null || getIndex() >= getTableView().getItems().size() || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
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

    /**
     * Sets the main application instance.
     *
     * @param application the main application instance
     */
    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    private void fetchReports(int page) {
        setLoading(true);
        statusLabel.setText("Chargement des signalements...");

        Task<PagedResponse<ReportResult>> fetchTask = new Task<>() {
            @Override
            protected PagedResponse<ReportResult> call() throws Exception {
                return reportService.getReports(page, PAGE_SIZE);
            }
        };

        fetchTask.setOnSucceeded(event -> Platform.runLater(() -> {
            PagedResponse<ReportResult> response = fetchTask.getValue();
            setLoading(false);

            reportsTable.getItems().setAll(response.getContent());
            currentPage = response.getMetadata().getPage();

            // Update pagination UI
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

        new Thread(fetchTask, "fetch-reports-thread").start();
    }

    private void confirmAndDismiss(ReportResult report) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Ignorer le signalement de " + report.getReporterUsername() + " ?");
        alert.setContentText("Le signalement sera supprimé mais le contenu signalé restera en place.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            dismissReport(report.getId());
        }
    }

    private void confirmAndDeleteContent(ReportResult report) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le contenu signalé (" + report.getType() + ") ?");
        alert.setContentText("Le contenu signalé et tous ses signalements associés seront supprimés. Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteReportedContent(report);
        }
    }

    private void dismissReport(String id) {
        setLoading(true);
        statusLabel.setText("Suppression du signalement...");

        Task<Void> dismissTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                reportService.dismissReport(id);
                return null;
            }
        };

        dismissTask.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText("Signalement ignoré avec succès.");
            fetchReports(currentPage);
        }));

        dismissTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = dismissTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(dismissTask, "dismiss-report-thread").start();
    }

    private void deleteReportedContent(ReportResult report) {
        setLoading(true);
        statusLabel.setText("Suppression du contenu...");

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ReportDetailResult detail = reportService.getReportById(report.getId());
                String targetId = detail.getTargetId();
                if (targetId == null) {
                    throw new IllegalStateException("Report " + report.getId() + " has no target id");
                }
                if ("review".equals(detail.getType())) {
                    reviewService.deleteReview(targetId);
                } else if ("comment".equals(detail.getType())) {
                    reviewService.deleteComment(targetId);
                } else {
                    throw new IllegalStateException("Unknown report type: " + detail.getType());
                }
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText("Contenu supprimé avec succès.");
            fetchReports(currentPage);
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

        new Thread(deleteTask, "delete-content-thread").start();
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
